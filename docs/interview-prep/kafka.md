# Kafka 면접 질문 & 답변

## 기본 개념

### Q1: Kafka를 사용하는 이유는 무엇인가요?
**답변**:
현재 프로젝트에서 주문 생성 시 여러 후속 처리(재고 차감, 알림 발송, 포인트 적립)가 필요했습니다.
동기 처리 시 응답 시간이 3초 이상 소요되어 사용자 경험이 나빴고, Kafka를 도입하여 비동기 처리로
전환한 결과 응답 시간을 210ms로 개선했습니다.

**Kafka의 장점**:
- 높은 처리량 (초당 수백만 메시지)
- 수평 확장 가능 (파티션 추가)
- 메시지 영속성 (디스크 저장)
- 다수의 Consumer 지원
- 순서 보장 (파티션 내)

### Q2: Kafka와 RabbitMQ의 차이점은?
**답변**:

| 특징 | Kafka | RabbitMQ |
|-----|-------|----------|
| 처리량 | 매우 높음 (수백만 TPS) | 중간 (수만 TPS) |
| 메시지 저장 | 디스크에 영구 저장 | 메모리 위주, 선택적 저장 |
| 메시지 순서 | 파티션 내 보장 | 보장 안 됨 (플러그인 필요) |
| 라우팅 | Topic-Partition 기반 | 복잡한 라우팅 규칙 |
| 사용 사례 | 대용량 이벤트 스트리밍 | 복잡한 라우팅, 작업 큐 |

**선택 이유**: 우리 서비스는 주문 이벤트가 초당 수천 건 발생하고, 이벤트 재처리가 필요한 경우가 많아서
영속성과 처리량이 중요했습니다. 따라서 Kafka를 선택했습니다.

---

## Producer

### Q3: Producer의 acks 설정에 대해 설명해주세요
**답변**:

```yaml
spring:
  kafka:
    producer:
      acks: all  # 0, 1, all 중 선택
```

- **acks=0**: 전송 즉시 성공으로 간주 (가장 빠름, 메시지 손실 가능)
- **acks=1**: Leader가 받으면 성공 (Leader 장애 시 메시지 손실 가능)
- **acks=all (-1)**: 모든 In-Sync Replica가 받으면 성공 (가장 안전, 느림)

**우리 프로젝트**: 주문 이벤트는 손실되면 안 되므로 `acks=all` 사용.
성능보다 안정성이 중요한 경우에 적합합니다.

### Q4: Producer의 멱등성(Idempotent)은 무엇인가요?
**답변**:

```yaml
spring:
  kafka:
    producer:
      enable-idempotence: true
```

네트워크 재전송으로 인한 중복 메시지 발생을 방지합니다.

**동작 원리**:
- Producer가 각 메시지에 sequence number 부여
- Broker가 sequence number 확인 후 중복 메시지 제거
- `acks=all`, `max-in-flight-requests-per-connection ≤ 5` 필요

**실무 경험**: 네트워크 불안정으로 Producer가 재시도하여 같은 주문 이벤트가 2번 발행된 적이 있습니다.
멱등성 활성화 후 이 문제가 해결되었습니다.

---

## Consumer

### Q5: Consumer Group은 무엇이고, 어떻게 설계하나요?
**답변**:

**Consumer Group**: 같은 Topic을 구독하는 Consumer들의 그룹.
각 파티션은 그룹 내 하나의 Consumer에만 할당됩니다.

**우리 프로젝트의 설계**:
```
order.created Topic (Partition 3개)
├── inventory-consumer-group
│   └── Consumer 3대 (재고 차감)
├── notification-consumer-group
│   └── Consumer 5대 (알림 발송)
└── analytics-consumer-group
    └── Consumer 1대 (데이터 분석)
```

**설계 원칙**:
1. 기능별로 Consumer Group 분리 (독립적 처리)
2. 파티션 수 ≥ Consumer 수 (병렬 처리)
3. 순서가 중요한 경우 파티션 키 설정 (userId로 파티셔닝)

### Q6: 파티션 키는 어떻게 선택하나요?
**답변**:

```java
kafkaTemplate.send(TOPIC, userId.toString(), event);
```

**파티션 키 선택 기준**:
- **순서 보장이 필요한 경우**: 같은 키는 같은 파티션으로 전송
- **부하 분산**: 키의 분포가 균등해야 함

**우리 프로젝트**:
- 주문 이벤트는 `userId`를 파티션 키로 사용
- 같은 사용자의 주문은 순서대로 처리됨
- 사용자 수가 많아서 부하도 균등하게 분산

### Q7: Consumer Lag이 증가하면 어떻게 대응하나요?
**답변**:

**Consumer Lag**: Producer가 발행한 메시지를 Consumer가 처리하지 못해 쌓인 양

**모니터링**:
```yaml
# Grafana 알람
- alert: KafkaConsumerLagHigh
  expr: kafka_consumer_lag > 1000
  for: 5m
```

**대응 방법**:
1. **즉시 대응**:
   ```yaml
   spring:
     kafka:
       listener:
         concurrency: 5  # 3 → 5로 증가
   ```

2. **파티션 추가** (리밸런싱 발생):
   ```bash
   kafka-topics.sh --alter --topic order.created --partitions 6
   ```

3. **처리 로직 최적화**:
   - DB 배치 처리 (N+1 제거)
   - 불필요한 로직 제거
   - 캐싱 적용

4. **Consumer 스케일 아웃**:
   - 파티션 수만큼 Consumer 추가

**실무 경험**: 대규모 프로모션 시작 후 Lag이 5000까지 증가. concurrency를 3→5로 늘리고,
DB 조회를 Redis로 대체하여 Lag을 100 이하로 안정화했습니다.

---

## 메시지 처리

### Q8: 메시지 중복 처리를 어떻게 방지하나요?
**답변**:

**문제**: Exactly-Once를 완벽히 보장하기 어려움. At-Least-Once 방식 사용 시 중복 가능.

**해결 방법 1: 이벤트 ID 기반 멱등성**:
```java
@KafkaListener(topics = "order.created")
public void handleOrderCreated(OrderCreatedEvent event) {
    // 1. 중복 체크
    if (processedEventRepository.existsByEventId(event.eventId())) {
        log.warn("Already processed: {}", event.eventId());
        return;
    }

    // 2. 처리
    inventoryService.decreaseStock(event.productId(), event.quantity());

    // 3. 처리 완료 기록
    processedEventRepository.save(new ProcessedEvent(event.eventId()));
}
```

**해결 방법 2: DB Unique 제약**:
```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    event_id VARCHAR(255) UNIQUE,  -- 중복 방지
    ...
);
```

### Q9: 재시도와 DLQ(Dead Letter Queue) 전략은?
**답변**:

**재시도 설정**:
```yaml
spring:
  kafka:
    listener:
      ack-mode: record
    retry:
      topic:
        enabled: true
        attempts: 3  # 최대 3번
        delay: 1000  # 1초
        multiplier: 2.0  # 지수 백오프 (1s → 2s → 4s)
        max-delay: 10000
```

**DLQ 처리**:
```java
@KafkaListener(topics = "order.created.dlt")
public void handleDlt(OrderCreatedEvent event,
                      @Header(KafkaHeaders.EXCEPTION_MESSAGE) String error) {
    // 1. 에러 로그 저장
    errorLogRepository.save(new ErrorLog(event, error));

    // 2. 알림 발송
    slackService.sendAlert("주문 처리 실패", event.orderId());

    // 3. 수동 재처리 대기 (관리자 개입)
}
```

**재시도 vs DLQ 구분**:
- **일시적 오류** (네트워크, 타임아웃) → 재시도
- **영구적 오류** (잘못된 데이터, 비즈니스 로직 오류) → 즉시 DLQ

---

## 고급 주제

### Q10: 트랜잭션 아웃박스 패턴이란?
**답변**:

**문제**: DB 트랜잭션 커밋 후 Kafka 발행 실패 시 데이터 불일치

**해결: Transactional Outbox Pattern**:
```java
@Transactional
public OrderResponse createOrder(OrderRequest request) {
    // 1. 주문 생성
    Order order = orderRepository.save(new Order(request));

    // 2. 같은 트랜잭션 내에서 Outbox에 이벤트 저장
    OutboxEvent outbox = new OutboxEvent(
        "order.created",
        OrderCreatedEvent.from(order),
        OutboxStatus.PENDING
    );
    outboxRepository.save(outbox);

    return OrderResponse.from(order);
}

// 별도 스케줄러
@Scheduled(fixedDelay = 1000)
public void publishPendingEvents() {
    List<OutboxEvent> pending = outboxRepository.findByStatus(PENDING);

    for (OutboxEvent event : pending) {
        try {
            kafkaTemplate.send(event.getTopic(), event.getPayload());
            event.setStatus(SENT);
            outboxRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to publish event", e);
        }
    }
}
```

**장점**:
- DB와 이벤트 발행의 원자성 보장
- 발행 실패 시 자동 재시도
- 순서 보장 가능

### Q11: Kafka Streams를 사용해본 적 있나요?
**답변**:

**개선 계획으로 고려 중**:

```java
// 실시간 주문 통계
KStream<String, OrderCreatedEvent> orders = builder.stream("order.created");

// 1분마다 집계
KTable<Windowed<String>, Long> orderCountByMinute = orders
    .groupBy((key, value) -> value.productId().toString())
    .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1)))
    .count();

orderCountByMinute.toStream()
    .foreach((key, count) -> {
        log.info("Product {} orders: {}", key.key(), count);
    });
```

**사용 사례**:
- 실시간 주문 통계
- 이상 거래 탐지
- 사용자 행동 분석

---

## 운영 & 트러블슈팅

### Q12: Kafka 운영 중 겪은 장애 경험은?
**답변 예시**:

**장애 상황**: Consumer가 특정 파티션에서 계속 같은 메시지를 재처리 (Poison Pill)

**원인**: 잘못된 데이터 형식으로 인한 Deserialization 실패

**해결**:
```java
@KafkaListener(topics = "order.created")
public void handleOrderCreated(ConsumerRecord<String, String> record) {
    try {
        OrderCreatedEvent event = objectMapper.readValue(
            record.value(), OrderCreatedEvent.class
        );
        processOrder(event);
    } catch (JsonProcessingException e) {
        // Poison Pill 감지
        log.error("Invalid message format: offset={}", record.offset());
        // DLQ로 전송하고 건너뛰기
        dlqProducer.send("order.created.invalid", record.value());
        // Consumer는 정상 처리된 것으로 간주하여 다음 메시지 처리
    }
}
```

### Q13: Rebalancing은 무엇이고 어떻게 최소화하나요?
**답변**:

**Rebalancing**: Consumer Group의 멤버십이 변경될 때 파티션 재할당

**발생 상황**:
- Consumer 추가/제거
- Consumer 장애 (Heartbeat 실패)
- 파티션 수 변경

**문제점**:
- Rebalancing 중에는 메시지 처리 중단
- Lag 증가

**최소화 방법**:
```yaml
spring:
  kafka:
    consumer:
      # Heartbeat 주기
      heartbeat-interval: 3000
      # Session timeout
      session-timeout: 30000
      # Poll 간격
      max-poll-interval: 300000
```

```java
@KafkaListener(topics = "order.created")
public void handleOrderCreated(OrderCreatedEvent event) {
    // 처리 시간이 긴 작업은 비동기로
    CompletableFuture.runAsync(() -> {
        heavyProcess(event);
    });
    // Consumer는 빠르게 다음 Poll 가능
}
```

---

## 실전 팁

### 면접에서 강조할 포인트

1. **구체적인 숫자**
   - "Kafka 도입으로 응답 시간 3000ms → 210ms로 개선"
   - "초당 10,000 TPS 처리"

2. **문제 해결 경험**
   - Consumer Lag 증가 → concurrency 증가 및 최적화
   - 중복 메시지 → 멱등성 처리 구현

3. **트레이드오프 이해**
   - acks=all (안정성) vs acks=1 (성능)
   - Exactly-Once (복잡) vs At-Least-Once (간단)

4. **모니터링**
   - Grafana로 Lag, TPS 모니터링
   - 알람 설정으로 능동적 대응
