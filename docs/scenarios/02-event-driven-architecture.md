# Scenario 02: 이벤트 드리븐 아키텍처 (Kafka)

## 🎯 실무 상황

**배경**:
- 주문 생성 API 응답 시간이 3초 이상 소요
- 주문 생성 후 처리해야 할 작업들:
  1. 재고 차감
  2. 결제 처리
  3. 포인트 적립
  4. 알림 발송 (이메일, SMS, 푸시)
  5. 데이터 분석팀에 이벤트 전송

**현재 문제점**:
```java
@Transactional
public OrderResponse createOrder(OrderRequest request) {
    // 1. 주문 생성 (200ms)
    Order order = orderRepository.save(new Order(request));

    // 2. 재고 차감 (500ms) - 외부 API 호출
    inventoryService.decreaseStock(order.getProductId(), order.getQuantity());

    // 3. 결제 처리 (1000ms) - 외부 API 호출
    paymentService.processPayment(order);

    // 4. 포인트 적립 (300ms)
    pointService.earnPoints(order.getUserId(), order.getAmount());

    // 5. 알림 발송 (1000ms)
    notificationService.sendEmail(order);
    notificationService.sendSms(order);

    return OrderResponse.from(order);  // 총 3초 이상 소요!
}
```

**CTO의 요구사항**:
"주문 생성은 1초 이내에 완료되어야 합니다. 후속 처리는 비동기로 처리하고,
실패 시 재시도 메커니즘을 구현해주세요. 그리고 메시지가 중복 처리되지 않도록 해주세요."

## 📚 학습 목표

- [ ] Kafka Producer/Consumer 구현
- [ ] Event Sourcing 패턴 이해
- [ ] 멱등성 처리 (Idempotency)
- [ ] Dead Letter Queue 처리
- [ ] Consumer Group과 파티셔닝 전략

## 🔧 구현 단계

### Step 1: 주문 생성 이벤트 발행

**이벤트 설계**:
```java
// domain/order/event/OrderCreatedEvent.java
@Builder
public record OrderCreatedEvent(
    String eventId,           // 이벤트 고유 ID (멱등성 보장용)
    Long orderId,
    Long userId,
    Long productId,
    int quantity,
    BigDecimal amount,
    LocalDateTime createdAt
) {
    public static OrderCreatedEvent from(Order order) {
        return OrderCreatedEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .orderId(order.getId())
            .userId(order.getUserId())
            .productId(order.getProductId())
            .quantity(order.getQuantity())
            .amount(order.getAmount())
            .createdAt(LocalDateTime.now())
            .build();
    }
}
```

**Producer 구현**:
```java
// infrastructure/kafka/OrderEventProducer.java
@RequiredArgsConstructor
@Component
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    private static final String TOPIC = "order.created";

    public void publishOrderCreated(OrderCreatedEvent event) {
        // 파티션 키: userId (같은 사용자의 이벤트는 순서 보장)
        String partitionKey = event.userId().toString();

        kafkaTemplate.send(TOPIC, partitionKey, event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish OrderCreatedEvent: orderId={}",
                        event.orderId(), ex);
                    // TODO: 실패 시 재시도 로직 또는 별도 저장
                } else {
                    log.info("Published OrderCreatedEvent: orderId={}, offset={}",
                        event.orderId(),
                        result.getRecordMetadata().offset());
                }
            });
    }
}
```

**개선된 주문 생성 API**:
```java
// application/order/OrderFacade.java
@RequiredArgsConstructor
@Component
public class OrderFacade {

    private final OrderService orderService;
    private final OrderEventProducer orderEventProducer;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        // 1. 주문만 생성 (200ms)
        Order order = orderService.create(request);

        // 2. 이벤트 발행 (비동기, 10ms)
        OrderCreatedEvent event = OrderCreatedEvent.from(order);
        orderEventProducer.publishOrderCreated(event);

        // 3. 즉시 응답 (총 210ms)
        return OrderResponse.from(order);
    }
}
```

### Step 2: 재고 차감 Consumer 구현

```java
// apps/commerce-streamer/interfaces/consumer/InventoryConsumer.java
@RequiredArgsConstructor
@Component
public class InventoryConsumer {

    private final InventoryService inventoryService;
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(
        topics = "order.created",
        groupId = "inventory-consumer-group",
        concurrency = "3"  // 병렬 처리
    )
    public void handleOrderCreated(
        @Payload OrderCreatedEvent event,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Received OrderCreatedEvent: orderId={}, partition={}, offset={}",
            event.orderId(), partition, offset);

        try {
            // 멱등성 체크: 이미 처리된 이벤트인지 확인
            if (isAlreadyProcessed(event.eventId())) {
                log.warn("Event already processed: eventId={}", event.eventId());
                return;
            }

            // 재고 차감 처리
            inventoryService.decreaseStock(
                event.productId(),
                event.quantity()
            );

            // 처리 완료 기록 (멱등성 보장)
            markAsProcessed(event.eventId());

            log.info("Inventory decreased successfully: orderId={}", event.orderId());

        } catch (OutOfStockException e) {
            log.error("Out of stock: orderId={}, productId={}",
                event.orderId(), event.productId());
            // DLQ로 전송하지 않고 보상 트랜잭션 실행
            publishInventoryFailedEvent(event);

        } catch (Exception e) {
            log.error("Failed to decrease inventory: orderId={}",
                event.orderId(), e);
            throw e;  // 재시도를 위해 예외 전파
        }
    }

    private boolean isAlreadyProcessed(String eventId) {
        return processedEventRepository.existsByEventId(eventId);
    }

    private void markAsProcessed(String eventId) {
        ProcessedEvent processed = new ProcessedEvent(
            eventId,
            "inventory-consumer",
            LocalDateTime.now()
        );
        processedEventRepository.save(processed);
    }
}
```

**멱등성 보장을 위한 테이블**:
```sql
CREATE TABLE processed_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    consumer_name VARCHAR(100) NOT NULL,
    processed_at DATETIME NOT NULL,
    INDEX idx_event_id (event_id)
);
```

### Step 3: 알림 발송 Consumer (병렬 처리)

```java
// interfaces/consumer/NotificationConsumer.java
@RequiredArgsConstructor
@Component
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(
        topics = "order.created",
        groupId = "notification-consumer-group",
        concurrency = "5"  // 알림은 더 많은 병렬 처리
    )
    public void handleOrderCreated(@Payload OrderCreatedEvent event) {
        if (isAlreadyProcessed(event.eventId())) {
            return;
        }

        try {
            // 이메일, SMS 동시 발송 (CompletableFuture 활용)
            CompletableFuture<Void> emailFuture = CompletableFuture.runAsync(() ->
                notificationService.sendEmail(event)
            );

            CompletableFuture<Void> smsFuture = CompletableFuture.runAsync(() ->
                notificationService.sendSms(event)
            );

            // 모든 알림 완료 대기
            CompletableFuture.allOf(emailFuture, smsFuture).join();

            markAsProcessed(event.eventId());

        } catch (Exception e) {
            log.error("Failed to send notification: orderId={}", event.orderId(), e);
            throw e;  // 재시도
        }
    }
}
```

### Step 4: 재시도 및 DLQ 설정

**application.yml 설정**:
```yaml
spring:
  kafka:
    consumer:
      enable-auto-commit: false  # 수동 커밋
      auto-offset-reset: earliest
      max-poll-records: 100
    listener:
      ack-mode: record  # 레코드 단위 커밋

    # 재시도 설정
    retry:
      topic:
        enabled: true
        attempts: 3  # 최대 3번 재시도
        delay: 1000  # 1초 간격
        multiplier: 2.0  # 지수 백오프
        max-delay: 10000  # 최대 10초

    # DLQ 설정
    dlt:
      enabled: true
      topic-suffix: .dlt  # order.created.dlt
```

**DLQ Consumer**:
```java
@Component
@RequiredArgsConstructor
public class OrderDltConsumer {

    private final AlertService alertService;

    @KafkaListener(
        topics = "order.created.dlt",
        groupId = "dlt-monitoring-group"
    )
    public void handleDlt(
        @Payload OrderCreatedEvent event,
        @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage
    ) {
        log.error("Message moved to DLT: orderId={}, error={}",
            event.orderId(), errorMessage);

        // 1. 에러 로그 DB 저장
        saveErrorLog(event, errorMessage);

        // 2. 개발팀에 알림
        alertService.sendToSlack(
            "주문 처리 실패",
            String.format("Order ID: %d\nError: %s", event.orderId(), errorMessage)
        );

        // 3. 보상 트랜잭션 실행 (필요 시)
        // compensateOrder(event);
    }
}
```

### Step 5: Consumer 성능 모니터링

```java
@Component
@RequiredArgsConstructor
public class KafkaConsumerMetrics {

    private final MeterRegistry meterRegistry;

    @EventListener
    public void onConsumerRecord(ConsumerRecordReceivedEvent event) {
        // Lag 측정
        Counter.builder("kafka.consumer.lag")
            .tag("topic", event.getTopic())
            .tag("partition", String.valueOf(event.getPartition()))
            .register(meterRegistry)
            .increment();

        // 처리 시간 측정
        Timer.builder("kafka.consumer.processing.time")
            .tag("consumer", event.getConsumerGroup())
            .register(meterRegistry)
            .record(event.getProcessingTime());
    }
}
```

## 📊 성능 개선 결과

### Before (동기 처리)
- 주문 생성 응답 시간: 3,000ms
- 처리량: 100 TPS
- 실패 시 전체 롤백

### After (비동기 처리)
- 주문 생성 응답 시간: 210ms (93% 개선)
- 처리량: 1,000 TPS
- 부분 실패 허용 (eventual consistency)
- Consumer Lag: 평균 10ms

## 🎤 면접 예상 질문

### Q1: Kafka를 선택한 이유는? RabbitMQ와의 차이는?
**답변 포인트**:
- 높은 처리량 (초당 수백만 건)
- 파티셔닝을 통한 수평 확장
- 메시지 영속성 (디스크 저장)
- RabbitMQ는 라우팅 유연성은 좋지만 처리량이 낮음
- 우리는 대용량 주문 처리가 필요해서 Kafka 선택

### Q2: 메시지 중복 처리를 어떻게 방지하나요?
**답변 포인트**:
- 이벤트마다 고유 ID 부여 (UUID)
- 처리 완료 후 DB에 eventId 저장
- Consumer에서 처리 전 중복 체크
- 멱등성(Idempotency) 보장

### Q3: Consumer Group은 어떻게 설계했나요?
**답변 포인트**:
- 기능별로 Consumer Group 분리 (inventory, notification, analytics)
- 같은 그룹 내에서는 파티션별로 분산 처리
- concurrency 설정으로 병렬 처리 수 조정
- 재고 차감은 순서가 중요해서 userId로 파티셔닝

### Q4: 재시도 전략은 어떻게 구성했나요?
**답변 포인트**:
- 최대 3번 재시도
- 지수 백오프 (1초 → 2초 → 4초)
- 재시도 실패 시 DLQ로 이동
- DLQ 메시지는 수동으로 재처리 또는 보상 트랜잭션

### Q5: Consumer Lag이 증가하면 어떻게 대응하나요?
**답변 포인트**:
- Grafana 알람으로 Lag > 1000 시 알림
- concurrency 증가로 Consumer 수 늘림
- 파티션 수 증가 (리밸런싱)
- 처리 로직 최적화 (DB 배치 처리 등)

### Q6: 트랜잭션 아웃박스 패턴을 아시나요?
**답변 포인트**:
- 현재는 이벤트 발행 실패 시 로그만 남김
- 개선안: Outbox 테이블에 이벤트 저장 후 별도 폴링으로 발행
- 주문 생성과 이벤트 발행을 하나의 트랜잭션으로 묶음
- 더 강한 일관성 보장

```java
@Transactional
public OrderResponse createOrder(OrderRequest request) {
    // 1. 주문 생성
    Order order = orderRepository.save(new Order(request));

    // 2. Outbox에 이벤트 저장 (같은 트랜잭션)
    OutboxEvent outbox = new OutboxEvent(
        "order.created",
        OrderCreatedEvent.from(order)
    );
    outboxRepository.save(outbox);

    // 3. 별도 스케줄러가 Outbox를 폴링해서 Kafka로 발행
    return OrderResponse.from(order);
}
```

## 🚀 추가 개선 과제

1. **Kafka Streams를 이용한 실시간 집계**
   - 실시간 주문 통계
   - 사용자별 주문 패턴 분석

2. **Schema Registry 도입**
   - Avro 스키마로 이벤트 직렬화
   - 스키마 버전 관리

3. **CQRS 패턴 적용**
   - 주문 조회용 읽기 전용 모델 구축
   - Kafka로 읽기 모델 실시간 동기화

4. **Exactly-Once Semantic 구현**
   - 트랜잭션 Producer 사용
   - Transactional Outbox 패턴
