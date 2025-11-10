# Scenario 01: 캐싱 전략 구현

## 🎯 실무 상황

**배경**:
- 이커머스 플랫폼의 상품 상세 페이지 조회 API가 초당 1,000건 이상 호출됨
- 매번 MySQL에서 조회하여 DB CPU 사용률이 80% 이상 유지
- 평균 응답 시간 150ms → 50ms 이하로 개선 요구

**PM의 요구사항**:
"상품 정보는 자주 변경되지 않으니 캐싱을 적용해서 응답 속도를 개선해주세요.
단, 상품 정보가 업데이트되면 즉시 반영되어야 합니다."

## 📚 학습 목표

- [ ] Cache-Aside 패턴 이해 및 구현
- [ ] Redis Master-Replica 구조 활용
- [ ] Cache Stampede 현상 이해 및 대응
- [ ] 캐시 TTL 및 Eviction 전략 수립
- [ ] 캐시 성능 메트릭 수집

## 🔧 구현 단계

### Step 1: Redis 캐싱 기본 구현

**요구사항**: Product 조회에 Redis 캐싱 적용

```java
// domain/product/ProductService.java
@RequiredArgsConstructor
@Component
public class ProductService {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, Product> redisTemplate;

    private static final String CACHE_KEY_PREFIX = "product:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    public Product getProduct(Long productId) {
        // 1. 캐시 조회 (Cache-Aside 패턴)
        String cacheKey = CACHE_KEY_PREFIX + productId;
        Product cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            return cached;
        }

        // 2. 캐시 미스: DB 조회
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

        // 3. 캐시 저장
        redisTemplate.opsForValue().set(cacheKey, product, CACHE_TTL);

        return product;
    }

    @Transactional
    public void updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

        product.update(request);

        // 캐시 무효화 (Cache Invalidation)
        String cacheKey = CACHE_KEY_PREFIX + productId;
        redisTemplate.delete(cacheKey);
    }
}
```

**테스트 작성**:
```java
@SpringBootTest
class ProductServiceCacheTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RedisTemplate<String, Product> redisTemplate;

    @Test
    @DisplayName("첫 번째 조회는 DB에서, 두 번째 조회는 캐시에서 가져온다")
    void cacheHitTest() {
        // given
        Product product = productRepository.save(new Product("MacBook Pro", 2_500_000));

        // when: 첫 번째 조회 (Cache Miss)
        Product first = productService.getProduct(product.getId());

        // then: 캐시에 저장되었는지 확인
        String cacheKey = "product:" + product.getId();
        Product cached = redisTemplate.opsForValue().get(cacheKey);
        assertThat(cached).isNotNull();

        // when: 두 번째 조회 (Cache Hit)
        Product second = productService.getProduct(product.getId());

        // then: 같은 객체를 반환
        assertThat(second).isEqualTo(first);
    }

    @Test
    @DisplayName("상품 수정 시 캐시가 무효화된다")
    void cacheInvalidationTest() {
        // given
        Product product = productRepository.save(new Product("MacBook Pro", 2_500_000));
        productService.getProduct(product.getId()); // 캐시 저장

        // when: 상품 수정
        productService.updateProduct(product.getId(),
            new ProductUpdateRequest("MacBook Pro M3", 3_000_000));

        // then: 캐시가 삭제됨
        String cacheKey = "product:" + product.getId();
        Product cached = redisTemplate.opsForValue().get(cacheKey);
        assertThat(cached).isNull();
    }
}
```

### Step 2: Cache Stampede 문제 해결

**문제 상황**:
- 인기 상품의 캐시가 만료되는 순간 수백 개의 요청이 동시에 DB를 조회
- DB에 순간적으로 부하가 몰림

**해결 방법**: Distributed Lock을 사용한 동기화

```java
@RequiredArgsConstructor
@Component
public class ProductService {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, Product> redisTemplate;
    private final RedissonClient redissonClient;

    public Product getProduct(Long productId) {
        String cacheKey = CACHE_KEY_PREFIX + productId;
        Product cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            return cached;
        }

        // Cache Stampede 방지: 분산 락 사용
        String lockKey = "lock:product:" + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 락 획득 시도 (최대 5초 대기, 10초 후 자동 해제)
            boolean acquired = lock.tryLock(5, 10, TimeUnit.SECONDS);

            if (!acquired) {
                throw new CoreException(ErrorType.LOCK_ACQUISITION_FAILED);
            }

            // Double-checked locking: 락을 얻은 후 다시 캐시 확인
            cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return cached;
            }

            // DB 조회 및 캐시 저장
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

            redisTemplate.opsForValue().set(cacheKey, product, CACHE_TTL);

            return product;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CoreException(ErrorType.INTERNAL_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### Step 3: 캐시 워밍업 전략

**문제 상황**:
- 서버 재시작 후 캐시가 비어있어 첫 요청들이 느림
- 대용량 트래픽이 몰리는 시간대에 재배포하면 장애 위험

**해결 방법**: 애플리케이션 시작 시 인기 상품 미리 캐싱

```java
@Component
@RequiredArgsConstructor
public class CacheWarmupInitializer {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, Product> redisTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void warmupCache() {
        log.info("Starting cache warmup...");

        // 최근 7일간 조회수가 높은 상위 100개 상품
        List<Product> popularProducts = productRepository
            .findTop100ByOrderByViewCountDesc();

        for (Product product : popularProducts) {
            String cacheKey = "product:" + product.getId();
            redisTemplate.opsForValue().set(cacheKey, product, CACHE_TTL);
        }

        log.info("Cache warmup completed. Cached {} products", popularProducts.size());
    }
}
```

### Step 4: 캐시 성능 메트릭 수집

```java
@Aspect
@Component
@RequiredArgsConstructor
public class CacheMetricsAspect {

    private final MeterRegistry meterRegistry;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;

    @PostConstruct
    public void init() {
        cacheHitCounter = Counter.builder("cache.hit")
            .tag("cache", "product")
            .register(meterRegistry);

        cacheMissCounter = Counter.builder("cache.miss")
            .tag("cache", "product")
            .register(meterRegistry);
    }

    @Around("execution(* com.loopers.domain.product.ProductService.getProduct(..))")
    public Object measureCachePerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        Long productId = (Long) joinPoint.getArgs()[0];
        String cacheKey = "product:" + productId;

        boolean isCacheHit = redisTemplate.hasKey(cacheKey);

        if (isCacheHit) {
            cacheHitCounter.increment();
        } else {
            cacheMissCounter.increment();
        }

        return joinPoint.proceed();
    }
}
```

## 📊 성능 개선 결과 측정

### Before (캐싱 적용 전)
- 평균 응답 시간: 150ms
- DB CPU 사용률: 80%
- 처리량: 1,000 TPS

### After (캐싱 적용 후)
- 평균 응답 시간: 15ms (90% 개선)
- DB CPU 사용률: 20%
- 처리량: 5,000 TPS
- 캐시 Hit Rate: 95%

## 🎤 면접 예상 질문

### Q1: Cache-Aside vs Write-Through 패턴의 차이는?
**답변 포인트**:
- Cache-Aside: 애플리케이션이 캐시와 DB를 직접 제어. Read-heavy 환경에 적합
- Write-Through: 쓰기 시 캐시와 DB에 동시 저장. Write-heavy 환경에 적합
- 우리 프로젝트는 Read 비율이 95%라서 Cache-Aside 선택

### Q2: 캐시 일관성 문제를 어떻게 해결했나요?
**답변 포인트**:
- 쓰기 작업 시 즉시 캐시 무효화 (Cache Invalidation)
- TTL을 짧게 설정하여 최대 불일치 시간 제한
- 중요한 데이터는 DB를 Source of Truth로 유지

### Q3: Cache Stampede 현상을 겪어봤나요?
**답변 포인트**:
- 인기 상품 캐시 만료 시 동시 요청으로 DB 부하 발생
- Distributed Lock(Redisson)으로 해결
- Double-checked locking으로 불필요한 DB 조회 방지

### Q4: Redis Master-Replica를 어떻게 활용했나요?
**답변 포인트**:
- Master: 쓰기 작업 (캐시 저장/삭제)
- Replica: 읽기 작업 (캐시 조회)
- Read 부하 분산으로 성능 향상

### Q5: 캐시 TTL은 어떻게 결정하나요?
**답변 포인트**:
- 데이터 변경 빈도 분석 (우리는 하루 1-2회)
- 메모리 용량 고려
- 비즈니스 요구사항 (실시간성)
- 모니터링 후 조정 (처음 10분 → 최종 30분)

## 🚀 추가 개선 과제

1. **다중 레벨 캐싱**
   - Local Cache (Caffeine) + Remote Cache (Redis)
   - 네트워크 비용 절감

2. **캐시 압축**
   - 큰 객체는 압축하여 메모리 절약

3. **지능형 TTL**
   - 조회 빈도에 따라 동적 TTL 조정
   - 인기 상품은 더 긴 TTL

4. **Pub/Sub를 이용한 캐시 동기화**
   - 다중 인스턴스 환경에서 캐시 일관성 보장
