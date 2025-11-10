# Scenario 03: 대용량 트래픽 처리 및 동시성 제어

## 🎯 실무 상황

**배경**:
- 매일 오후 3시 타임딜 진행 (선착순 100개 한정)
- 평소 TPS 100 → 타임딜 시작 순간 TPS 10,000 급증
- 재고보다 많은 주문이 생성되는 문제 발생 (Over-selling)

**장애 상황**:
```
15:00:00 - 타임딜 시작
15:00:03 - DB Connection Pool 고갈 (HikariCP: wait for connection)
15:00:05 - 응답 시간 10초 이상으로 증가
15:00:10 - 서버 CPU 100%, Out of Memory
15:00:15 - 서버 다운, 재시작 필요
15:00:20 - 재고 100개인데 주문 150개 생성됨!
```

**CTO의 긴급 요구**:
"내일 타임딜까지 재고 오버셀링 문제를 해결해주세요. 그리고 서버가 다운되지 않도록
트래픽을 제어하는 방법도 함께 적용해주세요."

## 📚 학습 목표

- [ ] 비관적 락(Pessimistic Lock) vs 낙관적 락(Optimistic Lock)
- [ ] 분산 락(Distributed Lock) 구현
- [ ] Connection Pool 튜닝
- [ ] Rate Limiting 구현
- [ ] 부하 테스트 및 성능 측정

## 🔧 구현 단계

### Step 1: 문제 재현 - 동시성 이슈 확인

**문제가 있는 코드**:
```java
@Transactional
public OrderResponse purchase(Long productId, int quantity) {
    // 1. 재고 조회
    Product product = productRepository.findById(productId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    // 2. 재고 확인 (여러 스레드가 동시에 통과!)
    if (product.getStock() < quantity) {
        throw new CoreException(ErrorType.OUT_OF_STOCK);
    }

    // 3. 재고 차감
    product.decreaseStock(quantity);

    // 4. 주문 생성
    Order order = new Order(productId, quantity);
    return OrderResponse.from(orderRepository.save(order));
}
```

**동시성 테스트**:
```java
@Test
@DisplayName("100명이 동시에 재고 100개 구매 시 정확히 100개만 판매된다")
void concurrencyTest() throws InterruptedException {
    // given
    Product product = productRepository.save(new Product("MacBook", 100));
    int threadCount = 100;

    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    // when: 100명이 동시에 1개씩 구매 시도
    AtomicInteger successCount = new AtomicInteger();
    AtomicInteger failCount = new AtomicInteger();

    for (int i = 0; i < threadCount; i++) {
        executorService.submit(() -> {
            try {
                orderService.purchase(product.getId(), 1);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await();

    // then
    Product result = productRepository.findById(product.getId()).get();
    assertThat(result.getStock()).isEqualTo(0);  // 실패! 음수가 됨
    assertThat(successCount.get()).isEqualTo(100);  // 실패! 100개 이상 팔림
}
```

### Step 2: 해결 방법 1 - 비관적 락 (Pessimistic Lock)

**JPA를 이용한 비관적 락**:
```java
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);
}

@Transactional
public OrderResponse purchaseWithPessimisticLock(Long productId, int quantity) {
    // SELECT ... FOR UPDATE
    Product product = productRepository.findByIdWithLock(productId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    if (product.getStock() < quantity) {
        throw new CoreException(ErrorType.OUT_OF_STOCK);
    }

    product.decreaseStock(quantity);

    Order order = new Order(productId, quantity);
    return OrderResponse.from(orderRepository.save(order));
}
```

**장점**:
- 구현이 간단
- 데이터 정합성 확실히 보장

**단점**:
- 락 대기로 인한 성능 저하
- 데드락 가능성
- 단일 DB에서만 동작 (분산 환경 X)

### Step 3: 해결 방법 2 - 분산 락 (Distributed Lock with Redis)

```java
@RequiredArgsConstructor
@Component
public class OrderService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final RedissonClient redissonClient;

    public OrderResponse purchaseWithDistributedLock(Long productId, int quantity) {
        String lockKey = "lock:product:" + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 락 획득 시도 (5초 대기, 10초 후 자동 해제)
            boolean acquired = lock.tryLock(5, 10, TimeUnit.SECONDS);

            if (!acquired) {
                throw new CoreException(ErrorType.LOCK_ACQUISITION_FAILED,
                    "재고 확인 중입니다. 잠시 후 다시 시도해주세요.");
            }

            // 락을 획득한 스레드만 실행
            return purchaseInternal(productId, quantity);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CoreException(ErrorType.INTERNAL_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional
    protected OrderResponse purchaseInternal(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

        if (product.getStock() < quantity) {
            throw new CoreException(ErrorType.OUT_OF_STOCK);
        }

        product.decreaseStock(quantity);

        Order order = new Order(productId, quantity);
        return OrderResponse.from(orderRepository.save(order));
    }
}
```

**장점**:
- 분산 환경에서 동작
- DB 락보다 유연함
- 타임아웃 설정 가능

**단점**:
- Redis 장애 시 문제 발생
- 약간의 성능 오버헤드

### Step 4: 해결 방법 3 - Redis를 이용한 재고 관리

**가장 빠른 방법**: Redis Atomic Operation 활용

```java
@RequiredArgsConstructor
@Component
public class RedisStockManager {

    private final RedisTemplate<String, String> redisTemplate;
    private final ProductRepository productRepository;

    private static final String STOCK_KEY_PREFIX = "stock:";

    public boolean decreaseStock(Long productId, int quantity) {
        String stockKey = STOCK_KEY_PREFIX + productId;

        // Redis Lua Script로 원자적 연산
        String luaScript = """
            local stock = redis.call('GET', KEYS[1])
            if not stock then
                return -1  -- 재고 정보 없음
            end
            stock = tonumber(stock)
            local quantity = tonumber(ARGV[1])
            if stock < quantity then
                return 0  -- 재고 부족
            end
            redis.call('DECRBY', KEYS[1], quantity)
            return 1  -- 성공
            """;

        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(luaScript, Long.class),
            Collections.singletonList(stockKey),
            String.valueOf(quantity)
        );

        return result != null && result == 1;
    }

    @PostConstruct
    public void syncStockToRedis() {
        // 애플리케이션 시작 시 DB 재고를 Redis로 동기화
        List<Product> products = productRepository.findAll();
        for (Product product : products) {
            String stockKey = STOCK_KEY_PREFIX + product.getId();
            redisTemplate.opsForValue().set(stockKey, String.valueOf(product.getStock()));
        }
    }

    @Scheduled(fixedDelay = 60000)  // 1분마다
    public void syncStockToDb() {
        // Redis → DB 동기화
        Set<String> stockKeys = redisTemplate.keys(STOCK_KEY_PREFIX + "*");
        if (stockKeys == null) return;

        for (String stockKey : stockKeys) {
            Long productId = Long.parseLong(stockKey.replace(STOCK_KEY_PREFIX, ""));
            String stock = redisTemplate.opsForValue().get(stockKey);

            if (stock != null) {
                productRepository.updateStock(productId, Integer.parseInt(stock));
            }
        }
    }
}
```

**최종 구매 로직**:
```java
@Transactional
public OrderResponse purchaseWithRedis(Long productId, int quantity) {
    // 1. Redis에서 재고 차감 (초고속)
    boolean decreased = redisStockManager.decreaseStock(productId, quantity);

    if (!decreased) {
        throw new CoreException(ErrorType.OUT_OF_STOCK);
    }

    // 2. 주문 생성 (비동기로 처리 가능)
    Product product = productRepository.findById(productId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    Order order = new Order(productId, quantity, product.getPrice());
    return OrderResponse.from(orderRepository.save(order));
}
```

### Step 5: Connection Pool 튜닝

**HikariCP 설정**:
```yaml
spring:
  datasource:
    hikari:
      # 최소 유휴 커넥션 수
      minimum-idle: 10
      # 최대 커넥션 수 (공식: connections = (core_count * 2) + effective_spindle_count)
      # 4 Core CPU → 약 10개
      maximum-pool-size: 20
      # 커넥션 타임아웃 (30초)
      connection-timeout: 30000
      # 커넥션 최대 수명 (30분)
      max-lifetime: 1800000
      # 유휴 커넥션 타임아웃 (10분)
      idle-timeout: 600000
      # 커넥션 유효성 검사
      connection-test-query: SELECT 1
```

**모니터링**:
```java
@Component
@RequiredArgsConstructor
public class HikariMetrics {

    private final DataSource dataSource;
    private final MeterRegistry meterRegistry;

    @PostConstruct
    public void bindHikariMetrics() {
        if (dataSource instanceof HikariDataSource hikari) {
            Gauge.builder("hikari.connections.active",
                    hikari, HikariDataSource::getHikariPoolMXBean)
                .register(meterRegistry);

            Gauge.builder("hikari.connections.idle",
                    hikari, HikariDataSource::getHikariPoolMXBean)
                .register(meterRegistry);
        }
    }
}
```

### Step 6: Rate Limiting 구현

**Bucket4j를 이용한 Rate Limiter**:
```java
@RequiredArgsConstructor
@Component
public class RateLimiter {

    private final RedissonClient redissonClient;

    public boolean tryAcquire(String key, int permits, Duration refillPeriod) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);

        // 초당 100개 요청 허용
        rateLimiter.trySetRate(RateType.OVERALL, 100, 1, RateIntervalUnit.SECONDS);

        return rateLimiter.tryAcquire(permits);
    }
}

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final RateLimiter rateLimiter;

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        // API Rate Limiting
        String key = "rate:order:" + request.getUserId();
        boolean allowed = rateLimiter.tryAcquire(key, 1, Duration.ofSeconds(1));

        if (!allowed) {
            throw new CoreException(ErrorType.TOO_MANY_REQUESTS,
                "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
        }

        return ResponseEntity.ok(orderService.purchase(request));
    }
}
```

### Step 7: 부하 테스트 (Gatling)

```scala
class TimeDealLoadTest extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")

  val scn = scenario("Time Deal Purchase")
    .exec(
      http("Purchase Product")
        .post("/api/v1/orders")
        .body(StringBody("""{"productId": 1, "quantity": 1}"""))
        .check(status.in(200, 400))
    )

  setUp(
    // 10초 동안 0명 → 1000명으로 증가
    scn.inject(rampUsers(1000).during(10))
  ).protocols(httpProtocol)
}
```

## 📊 성능 비교

| 방식 | TPS | 응답시간 (P95) | 정합성 | 분산환경 |
|------|-----|---------------|--------|---------|
| 락 없음 | 10,000 | 50ms | ❌ Over-selling | ❌ |
| 비관적 락 | 500 | 200ms | ✅ | ❌ |
| 분산 락 | 1,000 | 150ms | ✅ | ✅ |
| Redis 재고 | 5,000 | 80ms | ✅ | ✅ |

## 🎤 면접 예상 질문

### Q1: 비관적 락과 낙관적 락의 차이는?
**답변 포인트**:
- 비관적 락: SELECT FOR UPDATE, 충돌 예상 시 사용
- 낙관적 락: @Version, 충돌 적을 때 사용
- 타임딜은 충돌이 많아서 비관적 락 또는 분산 락 사용

### Q2: 분산 락 구현 시 주의할 점은?
**답변 포인트**:
- 타임아웃 설정 필수 (데드락 방지)
- 락 해제 보장 (finally 블록)
- 재진입 불가능 (같은 스레드도 대기)
- Redis 장애 대응 (Redlock 알고리즘)

### Q3: Redis로 재고 관리 시 일관성은 어떻게 보장하나요?
**답변 포인트**:
- Lua Script로 원자적 연산
- 주기적으로 Redis → DB 동기화
- AOF 활성화로 Redis 장애 대비
- DB를 Source of Truth로 유지

### Q4: Connection Pool은 어떻게 튜닝하나요?
**답변 포인트**:
- 공식: (CPU 코어 수 * 2) + Disk 수
- 모니터링으로 Active/Idle 비율 확인
- Wait 시간이 길면 Pool Size 증가
- 너무 크면 DB 부하 증가

## 🚀 추가 개선 과제

1. **대기열 시스템** - Redis Sorted Set 활용
2. **Circuit Breaker** - DB 장애 시 보호
3. **Graceful Degradation** - 부분 기능 제한
4. **Auto Scaling** - 트래픽에 따라 자동 확장
