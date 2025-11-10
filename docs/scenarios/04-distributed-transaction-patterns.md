# Scenario 04: 분산 트랜잭션 패턴과 데이터 일관성

## 🎯 실무 상황

**배경**:
- 이커머스 플랫폼에서 주문 생성 시 여러 마이크로서비스가 관여:
  1. **Order Service**: 주문 생성
  2. **Inventory Service**: 재고 차감
  3. **Payment Service**: 결제 처리
  4. **Point Service**: 포인트 차감/적립

**현재 문제점**:
```java
// 분산 환경에서 발생하는 데이터 불일치 문제
@Transactional  // 단일 DB에만 적용됨!
public OrderResponse createOrder(OrderRequest request) {
    // 1. 주문 생성 (Order DB)
    Order order = orderRepository.save(new Order(request));

    // 2. 재고 차감 (Inventory Service 호출)
    inventoryClient.decreaseStock(order.getProductId(), order.getQuantity());
    // ❌ 네트워크 장애 발생 시 롤백 불가!

    // 3. 결제 처리 (Payment Service 호출)
    paymentClient.processPayment(order.getAmount());
    // ❌ 재고는 차감되었는데 결제 실패하면?

    return OrderResponse.from(order);
}
```

**실제 장애 사례**:
- 재고는 차감되었는데 결제가 실패하여 재고 부족 현상 발생
- 결제는 완료되었는데 주문이 생성되지 않아 고객 불만 발생
- 시스템 장애로 인한 데이터 정합성 깨짐

**CTO의 요구사항**:
"분산 환경에서도 데이터 일관성을 보장해야 합니다.
Saga 패턴을 적용하여 실패 시 보상 트랜잭션을 수행하고,
최종 일관성(Eventual Consistency)을 보장해주세요."

## 📚 학습 목표

- [ ] Saga 패턴 (Choreography vs Orchestration) 이해
- [ ] 보상 트랜잭션(Compensating Transaction) 구현
- [ ] 멱등성(Idempotency) 보장
- [ ] 최종 일관성(Eventual Consistency) 달성
- [ ] 2PC vs Saga 패턴 비교

## 🔧 구현 단계

### Step 1: Saga 패턴 기본 구조 (Choreography)

**이벤트 기반 Saga 흐름 설계**:

```
주문 생성 → [OrderCreated] → 재고 차감 → [StockDecreased] → 결제 처리 → [PaymentCompleted]
                 ↓ 실패                    ↓ 실패                   ↓ 실패
              주문 취소              재고 복구 이벤트         결제 취소 이벤트
```

**도메인 이벤트 정의**:
```java
// domain/order/event/OrderEvents.java
public sealed interface OrderEvent permits
    OrderCreated,
    OrderCompleted,
    OrderCancelled {

    String eventId();
    Long orderId();
    LocalDateTime occurredAt();
}

@Builder
public record OrderCreated(
    String eventId,
    Long orderId,
    Long userId,
    Long productId,
    int quantity,
    BigDecimal amount,
    LocalDateTime occurredAt
) implements OrderEvent {}

@Builder
public record OrderCompleted(
    String eventId,
    Long orderId,
    LocalDateTime occurredAt
) implements OrderEvent {}

@Builder
public record OrderCancelled(
    String eventId,
    Long orderId,
    String reason,
    LocalDateTime occurredAt
) implements OrderEvent {}
```

```java
// domain/inventory/event/InventoryEvents.java
public sealed interface InventoryEvent permits
    StockDecreased,
    StockRestored {

    String eventId();
    Long productId();
}

@Builder
public record StockDecreased(
    String eventId,
    Long orderId,
    Long productId,
    int quantity,
    LocalDateTime occurredAt
) implements InventoryEvent {}

@Builder
public record StockRestored(
    String eventId,
    Long orderId,
    Long productId,
    int quantity,
    String reason,
    LocalDateTime occurredAt
) implements InventoryEvent {}
```

```java
// domain/payment/event/PaymentEvents.java
public sealed interface PaymentEvent permits
    PaymentCompleted,
    PaymentRefunded {

    String eventId();
    Long orderId();
}

@Builder
public record PaymentCompleted(
    String eventId,
    Long orderId,
    BigDecimal amount,
    String transactionId,
    LocalDateTime occurredAt
) implements PaymentEvent {}

@Builder
public record PaymentRefunded(
    String eventId,
    Long orderId,
    BigDecimal amount,
    String reason,
    LocalDateTime occurredAt
) implements PaymentEvent {}
```

### Step 2: Order Service - Saga 시작점

```java
// application/order/OrderFacade.java
@RequiredArgsConstructor
@Component
public class OrderFacade {

    private final OrderService orderService;
    private final OrderEventProducer eventProducer;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        // 1. 주문 생성 (PENDING 상태)
        Order order = orderService.create(request);

        // 2. Saga 시작 이벤트 발행
        OrderCreated event = OrderCreated.builder()
            .eventId(UUID.randomUUID().toString())
            .orderId(order.getId())
            .userId(request.userId())
            .productId(request.productId())
            .quantity(request.quantity())
            .amount(request.amount())
            .occurredAt(LocalDateTime.now())
            .build();

        eventProducer.publish("order.created", event);

        return OrderResponse.from(order);
    }
}

// domain/order/Order.java
@Entity
@Table(name = "orders")
@Getter
public class Order {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;  // PENDING, COMPLETED, CANCELLED

    private Long userId;
    private Long productId;
    private int quantity;
    private BigDecimal amount;

    public void complete() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only PENDING orders can be completed");
        }
        this.status = OrderStatus.COMPLETED;
    }

    public void cancel(String reason) {
        if (this.status == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel COMPLETED order");
        }
        this.status = OrderStatus.CANCELLED;
        this.cancellationReason = reason;
    }
}
```

### Step 3: Inventory Service - 재고 차감 및 보상

```java
// interfaces/consumer/OrderEventConsumer.java
@RequiredArgsConstructor
@Component
public class OrderEventConsumer {

    private final InventoryService inventoryService;
    private final InventoryEventProducer eventProducer;
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(
        topics = "order.created",
        groupId = "inventory-saga-group"
    )
    public void handleOrderCreated(@Payload OrderCreated event) {
        // 멱등성 체크
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.info("Already processed event: {}", event.eventId());
            return;
        }

        try {
            // 재고 차감 시도
            inventoryService.decreaseStock(event.productId(), event.quantity());

            // 성공 이벤트 발행
            StockDecreased successEvent = StockDecreased.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(event.orderId())
                .productId(event.productId())
                .quantity(event.quantity())
                .occurredAt(LocalDateTime.now())
                .build();

            eventProducer.publish("inventory.stock-decreased", successEvent);

            // 처리 완료 기록
            processedEventRepository.save(
                new ProcessedEvent(event.eventId(), "inventory-consumer")
            );

            log.info("Stock decreased successfully: orderId={}", event.orderId());

        } catch (OutOfStockException e) {
            log.error("Out of stock: orderId={}, productId={}",
                event.orderId(), event.productId());

            // ❌ 재고 부족 - 보상 트랜잭션 시작
            publishCompensationEvent(event, "OUT_OF_STOCK");
        }
    }

    // 보상 트랜잭션: 결제 실패 시 재고 복구
    @KafkaListener(
        topics = "payment.failed",
        groupId = "inventory-compensation-group"
    )
    public void handlePaymentFailed(@Payload PaymentFailed event) {
        log.info("Compensating inventory: orderId={}", event.orderId());

        try {
            // 재고 복구
            inventoryService.restoreStock(event.productId(), event.quantity());

            // 복구 완료 이벤트 발행
            StockRestored restoredEvent = StockRestored.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(event.orderId())
                .productId(event.productId())
                .quantity(event.quantity())
                .reason("PAYMENT_FAILED")
                .occurredAt(LocalDateTime.now())
                .build();

            eventProducer.publish("inventory.stock-restored", restoredEvent);

            log.info("Stock restored successfully: orderId={}", event.orderId());

        } catch (Exception e) {
            log.error("Failed to restore stock: orderId={}", event.orderId(), e);
            // DLQ로 이동하여 수동 처리
            throw e;
        }
    }

    private void publishCompensationEvent(OrderCreated event, String reason) {
        OrderCancelled cancelEvent = OrderCancelled.builder()
            .eventId(UUID.randomUUID().toString())
            .orderId(event.orderId())
            .reason(reason)
            .occurredAt(LocalDateTime.now())
            .build();

        eventProducer.publish("order.cancelled", cancelEvent);
    }
}
```

### Step 4: Payment Service - 결제 처리 및 보상

```java
// interfaces/consumer/InventoryEventConsumer.java
@RequiredArgsConstructor
@Component
public class InventoryEventConsumer {

    private final PaymentService paymentService;
    private final PaymentEventProducer eventProducer;

    @KafkaListener(
        topics = "inventory.stock-decreased",
        groupId = "payment-saga-group"
    )
    public void handleStockDecreased(@Payload StockDecreased event) {
        log.info("Processing payment: orderId={}", event.orderId());

        try {
            // 외부 결제 API 호출
            PaymentResult result = paymentService.processPayment(
                event.orderId(),
                event.amount()
            );

            // 결제 성공
            PaymentCompleted completedEvent = PaymentCompleted.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(event.orderId())
                .amount(event.amount())
                .transactionId(result.transactionId())
                .occurredAt(LocalDateTime.now())
                .build();

            eventProducer.publish("payment.completed", completedEvent);

            log.info("Payment completed: orderId={}, txId={}",
                event.orderId(), result.transactionId());

        } catch (PaymentException e) {
            log.error("Payment failed: orderId={}", event.orderId(), e);

            // ❌ 결제 실패 - 보상 이벤트 발행
            PaymentFailed failedEvent = PaymentFailed.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(event.orderId())
                .productId(event.productId())
                .quantity(event.quantity())
                .reason(e.getMessage())
                .occurredAt(LocalDateTime.now())
                .build();

            eventProducer.publish("payment.failed", failedEvent);
        }
    }

    // 보상 트랜잭션: 주문 취소 시 결제 환불
    @KafkaListener(
        topics = "order.cancelled",
        groupId = "payment-compensation-group"
    )
    public void handleOrderCancelled(@Payload OrderCancelled event) {
        log.info("Refunding payment: orderId={}", event.orderId());

        try {
            // 결제 환불 처리
            paymentService.refund(event.orderId());

            PaymentRefunded refundedEvent = PaymentRefunded.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(event.orderId())
                .amount(event.amount())
                .reason(event.reason())
                .occurredAt(LocalDateTime.now())
                .build();

            eventProducer.publish("payment.refunded", refundedEvent);

            log.info("Payment refunded: orderId={}", event.orderId());

        } catch (Exception e) {
            log.error("Failed to refund: orderId={}", event.orderId(), e);
            throw e;  // DLQ 이동
        }
    }
}
```

### Step 5: Order Service - Saga 완료 처리

```java
// interfaces/consumer/SagaEventConsumer.java
@RequiredArgsConstructor
@Component
public class SagaEventConsumer {

    private final OrderService orderService;

    // ✅ Saga 성공: 결제 완료 시 주문 완료
    @KafkaListener(
        topics = "payment.completed",
        groupId = "order-saga-completion-group"
    )
    public void handlePaymentCompleted(@Payload PaymentCompleted event) {
        log.info("Completing order: orderId={}", event.orderId());

        orderService.completeOrder(event.orderId());

        // 주문 완료 이벤트 발행 (포인트 적립 등 후속 처리)
        OrderCompleted completedEvent = OrderCompleted.builder()
            .eventId(UUID.randomUUID().toString())
            .orderId(event.orderId())
            .occurredAt(LocalDateTime.now())
            .build();

        eventProducer.publish("order.completed", completedEvent);
    }

    // ❌ Saga 실패: 재고/결제 실패 시 주문 취소
    @KafkaListener(
        topics = {"order.cancelled", "payment.failed"},
        groupId = "order-saga-cancellation-group"
    )
    public void handleSagaFailure(@Payload OrderEvent event) {
        log.info("Cancelling order due to saga failure: orderId={}", event.orderId());

        orderService.cancelOrder(event.orderId(), "SAGA_FAILURE");
    }
}

// domain/order/OrderService.java
@RequiredArgsConstructor
@Component
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public void completeOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

        order.complete();
        log.info("Order completed: orderId={}", orderId);
    }

    @Transactional
    public void cancelOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

        order.cancel(reason);
        log.info("Order cancelled: orderId={}, reason={}", orderId, reason);
    }
}
```

### Step 6: Saga 상태 추적 (Orchestration 패턴)

**Choreography vs Orchestration**:
- Choreography (위 구현): 각 서비스가 이벤트를 구독하고 다음 액션 결정
- Orchestration (아래 구현): 중앙 Orchestrator가 Saga 흐름 제어

```java
// domain/saga/OrderSagaOrchestrator.java
@RequiredArgsConstructor
@Component
public class OrderSagaOrchestrator {

    private final SagaStateRepository sagaStateRepository;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final OrderService orderService;

    @Transactional
    public void startSaga(OrderCreated event) {
        // 1. Saga 상태 초기화
        SagaState saga = SagaState.builder()
            .sagaId(UUID.randomUUID().toString())
            .orderId(event.orderId())
            .status(SagaStatus.STARTED)
            .currentStep(SagaStep.ORDER_CREATED)
            .build();

        sagaStateRepository.save(saga);

        // 2. 재고 차감 단계 실행
        executeInventoryStep(saga, event);
    }

    private void executeInventoryStep(SagaState saga, OrderCreated event) {
        try {
            saga.moveToStep(SagaStep.DECREASING_STOCK);
            sagaStateRepository.save(saga);

            inventoryClient.decreaseStock(event.productId(), event.quantity());

            saga.moveToStep(SagaStep.STOCK_DECREASED);
            sagaStateRepository.save(saga);

            // 3. 결제 단계로 진행
            executePaymentStep(saga, event);

        } catch (Exception e) {
            log.error("Inventory step failed: sagaId={}", saga.getSagaId(), e);
            compensateOrder(saga);
        }
    }

    private void executePaymentStep(SagaState saga, OrderCreated event) {
        try {
            saga.moveToStep(SagaStep.PROCESSING_PAYMENT);
            sagaStateRepository.save(saga);

            paymentClient.processPayment(event.orderId(), event.amount());

            saga.moveToStep(SagaStep.PAYMENT_COMPLETED);
            saga.complete();
            sagaStateRepository.save(saga);

            // 4. 주문 완료
            orderService.completeOrder(event.orderId());

        } catch (Exception e) {
            log.error("Payment step failed: sagaId={}", saga.getSagaId(), e);
            compensateInventory(saga, event);
        }
    }

    private void compensateInventory(SagaState saga, OrderCreated event) {
        try {
            saga.moveToStep(SagaStep.COMPENSATING_INVENTORY);
            sagaStateRepository.save(saga);

            inventoryClient.restoreStock(event.productId(), event.quantity());

            compensateOrder(saga);

        } catch (Exception e) {
            log.error("Compensation failed: sagaId={}", saga.getSagaId(), e);
            saga.fail("COMPENSATION_FAILED");
            sagaStateRepository.save(saga);
            // 수동 개입 필요 - 알림 발송
        }
    }

    private void compensateOrder(SagaState saga) {
        saga.fail("SAGA_FAILED");
        sagaStateRepository.save(saga);

        orderService.cancelOrder(saga.getOrderId(), "SAGA_FAILURE");
    }
}

// domain/saga/SagaState.java
@Entity
@Table(name = "saga_states")
@Getter
@Builder
public class SagaState {

    @Id
    private String sagaId;

    private Long orderId;

    @Enumerated(EnumType.STRING)
    private SagaStatus status;  // STARTED, COMPLETED, FAILED, COMPENSATING

    @Enumerated(EnumType.STRING)
    private SagaStep currentStep;

    private String failureReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void moveToStep(SagaStep step) {
        this.currentStep = step;
        this.updatedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = SagaStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        this.status = SagaStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = LocalDateTime.now();
    }
}

enum SagaStep {
    ORDER_CREATED,
    DECREASING_STOCK,
    STOCK_DECREASED,
    PROCESSING_PAYMENT,
    PAYMENT_COMPLETED,
    COMPENSATING_INVENTORY,
    COMPENSATING_ORDER
}

enum SagaStatus {
    STARTED,
    COMPLETED,
    FAILED,
    COMPENSATING
}
```

### Step 7: 멱등성 보장 메커니즘

```java
// infrastructure/idempotency/IdempotencyHandler.java
@RequiredArgsConstructor
@Component
public class IdempotencyHandler {

    private final ProcessedEventRepository repository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final Duration IDEMPOTENCY_WINDOW = Duration.ofDays(7);

    public boolean isAlreadyProcessed(String eventId, String consumerName) {
        // 1. Redis 캐시 확인 (빠른 조회)
        String cacheKey = "idempotency:" + consumerName + ":" + eventId;
        Boolean cached = redisTemplate.hasKey(cacheKey);

        if (Boolean.TRUE.equals(cached)) {
            return true;
        }

        // 2. DB 확인
        boolean exists = repository.existsByEventIdAndConsumerName(eventId, consumerName);

        if (exists) {
            // 캐시에 저장
            redisTemplate.opsForValue().set(cacheKey, "1", IDEMPOTENCY_WINDOW);
        }

        return exists;
    }

    @Transactional
    public void markAsProcessed(String eventId, String consumerName) {
        // 1. DB에 저장
        ProcessedEvent processed = ProcessedEvent.builder()
            .eventId(eventId)
            .consumerName(consumerName)
            .processedAt(LocalDateTime.now())
            .build();

        repository.save(processed);

        // 2. 캐시에도 저장
        String cacheKey = "idempotency:" + consumerName + ":" + eventId;
        redisTemplate.opsForValue().set(cacheKey, "1", IDEMPOTENCY_WINDOW);
    }
}

// Aspect로 멱등성 자동 적용
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final IdempotencyHandler handler;

    @Around("@annotation(idempotent)")
    public Object ensureIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent)
        throws Throwable {

        // 메서드 파라미터에서 이벤트 추출
        Object[] args = joinPoint.getArgs();
        OrderEvent event = (OrderEvent) args[0];

        String consumerName = idempotent.consumerName();

        // 이미 처리된 이벤트인지 확인
        if (handler.isAlreadyProcessed(event.eventId(), consumerName)) {
            log.info("Event already processed: eventId={}, consumer={}",
                event.eventId(), consumerName);
            return null;
        }

        // 실제 비즈니스 로직 실행
        Object result = joinPoint.proceed();

        // 처리 완료 기록
        handler.markAsProcessed(event.eventId(), consumerName);

        return result;
    }
}

// 사용 예시
@Component
public class InventoryConsumer {

    @Idempotent(consumerName = "inventory-consumer")
    @KafkaListener(topics = "order.created", groupId = "inventory-saga-group")
    public void handleOrderCreated(@Payload OrderCreated event) {
        // 멱등성이 자동으로 보장됨
        inventoryService.decreaseStock(event.productId(), event.quantity());
    }
}
```

### Step 8: 모니터링 및 알림

```java
// support/monitoring/SagaMonitor.java
@RequiredArgsConstructor
@Component
public class SagaMonitor {

    private final SagaStateRepository sagaRepository;
    private final MeterRegistry meterRegistry;
    private final AlertService alertService;

    @Scheduled(fixedDelay = 60000)  // 1분마다
    public void monitorStuckSagas() {
        // 10분 이상 완료되지 않은 Saga 조회
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);

        List<SagaState> stuckSagas = sagaRepository
            .findByStatusAndUpdatedAtBefore(SagaStatus.STARTED, threshold);

        if (!stuckSagas.isEmpty()) {
            log.warn("Found {} stuck sagas", stuckSagas.size());

            for (SagaState saga : stuckSagas) {
                // Slack 알림
                alertService.sendToSlack(
                    "Stuck Saga Detected",
                    String.format("Saga ID: %s, Order ID: %d, Step: %s",
                        saga.getSagaId(),
                        saga.getOrderId(),
                        saga.getCurrentStep())
                );
            }
        }
    }

    @EventListener
    public void recordSagaMetrics(SagaCompletedEvent event) {
        // Saga 성공률 측정
        Counter.builder("saga.completed")
            .tag("status", event.getStatus().name())
            .register(meterRegistry)
            .increment();

        // Saga 처리 시간 측정
        Timer.builder("saga.duration")
            .tag("status", event.getStatus().name())
            .register(meterRegistry)
            .record(event.getDuration());
    }
}
```

## 📊 패턴 비교

### 2PC (Two-Phase Commit) vs Saga

| 구분 | 2PC | Saga |
|------|-----|------|
| **일관성** | Strong Consistency | Eventual Consistency |
| **성능** | 느림 (블로킹) | 빠름 (논블로킹) |
| **가용성** | 낮음 (Coordinator SPOF) | 높음 (분산 처리) |
| **복잡도** | 낮음 | 높음 (보상 로직 필요) |
| **사용 사례** | 금융 거래 (정확성 최우선) | 이커머스 주문 (가용성 우선) |

### Choreography vs Orchestration

| 구분 | Choreography | Orchestration |
|------|--------------|---------------|
| **제어 방식** | 분산 (이벤트 기반) | 중앙 집중 (Orchestrator) |
| **결합도** | 낮음 | 높음 |
| **가시성** | 낮음 (흐름 파악 어려움) | 높음 (한눈에 파악 가능) |
| **확장성** | 높음 | 보통 |
| **디버깅** | 어려움 | 쉬움 |
| **추천 상황** | 단순한 Saga (3단계 이하) | 복잡한 Saga (4단계 이상) |

## 🎤 면접 예상 질문

### Q1: 분산 트랜잭션에서 ACID를 보장할 수 있나요?
**답변 포인트**:
- 전통적인 ACID는 불가능 (Atomicity, Isolation 보장 어려움)
- BASE 모델 사용 (Basically Available, Soft state, Eventually consistent)
- Saga 패턴으로 최종 일관성(Eventual Consistency) 달성
- 비즈니스 요구사항에 따라 선택 (금융: 2PC, 이커머스: Saga)

### Q2: Saga 패턴에서 보상 트랜잭션 실패 시 어떻게 하나요?
**답변 포인트**:
- 재시도 메커니즘 (지수 백오프)
- 최종 실패 시 DLQ로 이동
- 수동 개입 알림 (Slack, PagerDuty)
- 보상 트랜잭션은 항상 성공하도록 설계 (멱등성 보장)
- 최악의 경우 수동 데이터 정합성 복구

### Q3: Choreography와 Orchestration 중 어떤 것을 선택했나요?
**답변 포인트**:
- 처음에는 Choreography로 시작 (3단계: 주문-재고-결제)
- 포인트, 쿠폰, 알림 등 단계가 늘어나면서 복잡도 증가
- Orchestration으로 전환하여 가시성 향상
- Saga 상태를 DB에 저장하여 디버깅 용이

### Q4: 멱등성을 어떻게 보장하나요?
**답변 포인트**:
- 이벤트마다 고유 ID 부여 (UUID)
- 처리 전 중복 체크 (Redis + DB)
- Aspect로 자동화하여 실수 방지
- 7일간 이력 보관 (처리 완료 후에도 중복 방지)

### Q5: 최종 일관성까지 얼마나 걸리나요?
**답변 포인트**:
- 정상 케이스: 평균 500ms (주문→재고→결제→완료)
- 재시도 포함: 최대 30초 (3번 재시도, 지수 백오프)
- 모니터링: 10분 이상 미완료 시 알림
- 비즈니스적으로 허용 가능한 수준 (실시간성이 중요하지 않음)

### Q6: Outbox 패턴을 적용해봤나요?
**답변 포인트**:
- 현재는 이벤트 발행 실패 시 로그만 기록
- 개선안: Transactional Outbox 패턴 적용
  - 주문 생성과 이벤트 저장을 같은 트랜잭션으로
  - CDC(Change Data Capture) 또는 Polling Publisher로 발행
  - 이벤트 발행 보장 (at-least-once)

```java
@Transactional
public OrderResponse createOrder(OrderRequest request) {
    // 1. 주문 생성
    Order order = orderRepository.save(new Order(request));

    // 2. Outbox에 이벤트 저장 (같은 트랜잭션!)
    OutboxEvent outbox = OutboxEvent.builder()
        .aggregateId(order.getId())
        .aggregateType("Order")
        .eventType("OrderCreated")
        .payload(mapper.writeValueAsString(OrderCreated.from(order)))
        .status(OutboxStatus.PENDING)
        .build();
    outboxRepository.save(outbox);

    // 3. 별도 스케줄러가 Outbox를 폴링해서 Kafka로 발행
    return OrderResponse.from(order);
}
```

### Q7: Saga가 실패한 경우 고객 경험은 어떻게 되나요?
**답변 포인트**:
- 주문은 즉시 응답 (PENDING 상태)
- 실패 시 주문 상태가 CANCELLED로 변경
- 고객에게 알림 발송 ("재고 부족으로 주문이 취소되었습니다")
- 결제는 사전승인 방식 사용 (실제 청구는 완료 후)
- 환불 프로세스 자동화

## 🚀 추가 개선 과제

1. **Saga 시각화 도구**
   - Saga 상태를 실시간으로 모니터링하는 대시보드
   - 각 단계별 소요 시간 측정

2. **Circuit Breaker 패턴 적용**
   - 외부 서비스 장애 시 즉시 실패 처리
   - 불필요한 재시도 방지

3. **Saga Timeout 설정**
   - 단계별 타임아웃 설정 (재고: 3초, 결제: 10초)
   - 타임아웃 발생 시 자동 보상

4. **Event Sourcing 도입**
   - 모든 상태 변경을 이벤트로 저장
   - 특정 시점의 상태 재구성 가능
   - 감사 로그 자동 생성

5. **정합성 검증 배치**
   - 매일 새벽 전체 데이터 정합성 체크
   - 불일치 발견 시 자동 보정 또는 알림