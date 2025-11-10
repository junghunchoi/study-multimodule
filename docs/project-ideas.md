# 실전 프로젝트 아이디어

## 🎯 프로젝트 선정 기준

- **실무 중심**: 실제 서비스에서 마주치는 문제
- **기술 스택 활용**: 학습한 모든 기술을 통합적으로 사용
- **포트폴리오 가치**: 이직 시 어필할 수 있는 수준
- **확장 가능**: 단계적으로 기능 추가 가능

---

## 추천 프로젝트 1: 이커머스 플랫폼

### 📝 프로젝트 개요
타임딜과 쿠폰을 지원하는 이커머스 플랫폼

### 🎯 핵심 학습 포인트
- **멀티모듈**: commerce-api, order-api, payment-api 분리
- **Kafka**: 주문 생성 → 재고 차감 → 알림 발송 이벤트 체인
- **Redis**: 상품 캐싱, 분산 락(재고 관리), Rate Limiting
- **MySQL**: 복잡한 주문 조회 쿼리, 인덱스 최적화
- **테스트**: 동시성 테스트, 부하 테스트
- **모니터링**: 주문 TPS, 재고 처리 지연, 캐시 히트율

### 💡 구현 시나리오

#### Phase 1: 기본 기능 (1-2주)
- [ ] 상품 CRUD
- [ ] 주문 생성/조회
- [ ] 재고 관리
- [ ] 테스트 코드 작성

#### Phase 2: 성능 최적화 (1-2주)
- [ ] 상품 조회 캐싱 (Redis)
- [ ] 주문 목록 페이징 최적화 (Cursor 기반)
- [ ] N+1 문제 해결 (QueryDSL Fetch Join)
- [ ] Connection Pool 튜닝

#### Phase 3: 동시성 제어 (1주)
- [ ] 타임딜 재고 관리 (Redis 분산 락)
- [ ] 동시 주문 처리 테스트 (Gatling)
- [ ] 오버셀링 방지 검증

#### Phase 4: 이벤트 드리븐 (1-2주)
- [ ] 주문 생성 이벤트 발행
- [ ] 재고 차감 Consumer
- [ ] 알림 발송 Consumer (이메일, SMS)
- [ ] DLQ 처리 및 재시도

#### Phase 5: 모니터링 (1주)
- [ ] 주문 TPS 메트릭
- [ ] Kafka Consumer Lag 모니터링
- [ ] 캐시 히트율 대시보드
- [ ] 알람 설정 (Lag > 1000)

### 📊 면접 어필 포인트
> "타임딜 기능 구현 중 동시성 문제를 겪었습니다. 재고가 100개인데 150개 주문이 생성되는 문제를
> Redis 분산 락으로 해결했고, Gatling으로 TPS 10,000 부하 테스트를 통과했습니다.
> 또한 주문 생성 API 응답 시간을 3000ms에서 210ms로 개선했습니다."

---

## 추천 프로젝트 2: 실시간 채팅 플랫폼

### 📝 프로젝트 개요
WebSocket 기반 실시간 채팅 + 메시지 검색 + 알림

### 🎯 핵심 학습 포인트
- **WebSocket**: 실시간 양방향 통신
- **Kafka**: 메시지 브로커, 채팅방별 파티셔닝
- **Redis**: 온라인 사용자 관리, 최근 메시지 캐싱
- **MySQL**: 메시지 영구 저장, Full-Text Search
- **멀티모듈**: chat-server, notification-server, search-server

### 💡 주요 기능

#### 1. 실시간 메시지 전송
```
User A → WebSocket → chat-server → Kafka → chat-server → WebSocket → User B
```

#### 2. 메시지 영속성
- Kafka Consumer가 MySQL에 저장
- Redis에 최근 100개 메시지 캐싱

#### 3. 읽지 않은 메시지 카운트
- Redis Sorted Set으로 관리
- 실시간 업데이트

#### 4. 메시지 검색
- MySQL Full-Text Index
- 검색 결과 캐싱

### 📊 면접 어필 포인트
> "WebSocket과 Kafka를 결합하여 확장 가능한 채팅 시스템을 구현했습니다.
> 채팅방별로 파티셔닝하여 메시지 순서를 보장했고, Redis를 활용해
> 읽지 않은 메시지 카운트를 실시간으로 제공했습니다."

---

## 추천 프로젝트 3: 예약 시스템

### 📝 프로젝트 개요
레스토랑/병원/강의 예약 플랫폼 (동시 예약 방지)

### 🎯 핵심 학습 포인트
- **비관적 락**: 동시 예약 방지
- **트랜잭션**: 예약 생성 → 결제 → 알림 (Saga 패턴)
- **스케줄링**: 예약 시간 N분 전 알림
- **캘린더 조회 최적화**: 인덱스 설계

### 💡 주요 시나리오

#### 시나리오 1: 동시 예약 방지
```java
@Transactional
public ReservationResponse reserve(Long slotId) {
    // SELECT ... FOR UPDATE
    TimeSlot slot = slotRepository.findByIdWithLock(slotId);

    if (!slot.isAvailable()) {
        throw new CoreException(ErrorType.ALREADY_RESERVED);
    }

    slot.markAsReserved();
    Reservation reservation = reservationRepository.save(...);

    return ReservationResponse.from(reservation);
}
```

#### 시나리오 2: 예약 취소 보상 트랜잭션
```
예약 → 결제 → 알림 발송
  ↓ (취소)
결제 환불 → 슬롯 해제 → 취소 알림
```

### 📊 면접 어필 포인트
> "동시 예약 문제를 비관적 락으로 해결했고, 예약 취소 시 Saga 패턴으로
> 보상 트랜잭션을 구현했습니다. 예약 가능 시간 조회 쿼리를 인덱스 최적화로
> 500ms → 50ms로 개선했습니다."

---

## 추천 프로젝트 4: 콘텐츠 추천 시스템

### 📝 프로젝트 개요
사용자 행동 기반 실시간 콘텐츠 추천 (유튜브, 넷플릭스 스타일)

### 🎯 핵심 학습 포인트
- **Kafka Streams**: 실시간 이벤트 집계
- **Redis**: 추천 결과 캐싱, 조회 이력 저장
- **배치 처리**: Spring Batch로 추천 모델 학습
- **A/B 테스트**: 추천 알고리즘 성능 비교

### 💡 데이터 파이프라인

```
사용자 행동 (조회, 좋아요, 공유)
  ↓
Kafka Producer (실시간 이벤트 발행)
  ↓
Kafka Streams (집계: 카테고리별 관심도)
  ↓
Redis (실시간 추천 점수 저장)
  ↓
추천 API (Redis 조회 → 상위 N개 반환)
```

### 📊 면접 어필 포인트
> "Kafka Streams로 사용자별 실시간 관심사를 집계하여 추천 시스템을 구현했습니다.
> Redis 캐싱으로 추천 API 응답 시간을 200ms → 15ms로 개선했고,
> 추천 클릭률이 5% → 12%로 증가했습니다."

---

## 추천 프로젝트 5: 알림 허브

### 📝 프로젝트 개요
다중 채널 알림 통합 플랫폼 (이메일, SMS, 푸시, 슬랙)

### 🎯 핵심 학습 포인트
- **Kafka**: 알림 이벤트 브로커
- **Rate Limiting**: 채널별 발송 제한 (SMS 분당 100건)
- **Circuit Breaker**: 외부 API 장애 대응
- **재시도 전략**: 지수 백오프, DLQ

### 💡 아키텍처

```
notification-api (알림 요청 수신)
  ↓
Kafka Topic: notification.requested
  ↓
├── email-consumer (이메일 발송)
├── sms-consumer (SMS 발송, Rate Limit)
├── push-consumer (푸시 발송)
└── slack-consumer (슬랙 발송, Circuit Breaker)
  ↓
발송 결과 저장 (성공/실패/재시도)
```

### 📊 면접 어필 포인트
> "다중 채널 알림 시스템을 Kafka 기반으로 구현했습니다. SMS 채널에
> Rate Limiter를 적용하여 발송 제한을 준수했고, Circuit Breaker로
> 외부 API 장애 시에도 서비스가 다운되지 않도록 보호했습니다."

---

## 🚀 프로젝트 진행 체크리스트

### 필수 구현 항목
- [ ] **멀티모듈 구조** (최소 3개 이상)
- [ ] **3단계 테스트** (Unit, Integration, E2E)
- [ ] **Kafka Producer/Consumer**
- [ ] **Redis 캐싱 전략**
- [ ] **동시성 제어** (락 또는 원자적 연산)
- [ ] **쿼리 최적화** (인덱스, N+1 해결)
- [ ] **Prometheus + Grafana** 대시보드
- [ ] **Docker Compose** 환경 구성

### 추가 어필 항목
- [ ] **부하 테스트** 결과 (Gatling, JMeter)
- [ ] **성능 개선 증거** (Before/After 수치)
- [ ] **장애 대응 경험** (Circuit Breaker, DLQ)
- [ ] **README 문서화** (아키텍처 다이어그램 포함)
- [ ] **API 문서** (Swagger UI)

---

## 📝 포트폴리오 작성 팁

### 1. README 구조
```markdown
# 프로젝트 이름

## 📌 프로젝트 소개
- 개발 기간
- 사용 기술
- 핵심 기능

## 🏗️ 아키텍처
- 멀티모듈 구조 다이어그램
- 이벤트 흐름도

## 🔥 기술적 도전
### 문제 1: 재고 오버셀링
- **상황**: ...
- **해결**: Redis 분산 락
- **결과**: 오버셀링 0건

## 📊 성능 개선
| 지표 | Before | After | 개선율 |
|------|--------|-------|--------|
| 응답 시간 | 3000ms | 210ms | 93% |
| TPS | 100 | 1000 | 900% |

## 🧪 테스트
- 테스트 커버리지: 85%
- 부하 테스트: TPS 10,000 통과
```

### 2. GitHub 구성
```
Repository
├── README.md (프로젝트 소개)
├── docs/
│   ├── architecture.md (아키텍처 설계)
│   ├── performance-test.md (부하 테스트 결과)
│   └── troubleshooting.md (장애 대응 기록)
├── docker/ (개발 환경)
├── apps/ (애플리케이션 모듈)
├── modules/ (공통 모듈)
└── .github/workflows/ (CI/CD)
```

### 3. 면접 준비
각 프로젝트마다 준비할 답변:

1. **기술 선택 이유**
   - "왜 Kafka를 선택했나요?"
   - "Redis를 어떻게 활용했나요?"

2. **문제 해결 경험**
   - "가장 어려웠던 문제는?"
   - "어떻게 해결했나요?"

3. **성능 개선**
   - "어떤 지표를 개선했나요?"
   - "측정 방법은?"

4. **트레이드오프**
   - "다른 방법은 고려했나요?"
   - "왜 이 방법을 선택했나요?"

---

## 💼 이직 타임라인

### 3개월 플랜
- **1개월**: 기초 다지기 + 시나리오 1-3 구현
- **2개월**: 시나리오 4-6 구현 + 프로젝트 1개 완성
- **3개월**: 포트폴리오 정리 + 면접 준비

### 면접 준비 체크리스트
- [ ] 프로젝트 README 작성 완료
- [ ] 주요 코드 설명 가능 (5분 프레젠테이션)
- [ ] 기술 면접 질문 답변 준비 (각 주제별 10개)
- [ ] 포트폴리오 발표 연습 (30분)
- [ ] GitHub 프로필 정리
- [ ] 블로그 기술 포스팅 (선택)

---

## 🎯 목표 직무별 추천

### 백엔드 개발자 (이커머스/핀테크)
- **추천**: 이커머스 플랫폼 또는 예약 시스템
- **강조 포인트**: 동시성, 트랜잭션, 성능

### 플랫폼 개발자
- **추천**: 알림 허브 또는 채팅 플랫폼
- **강조 포인트**: 확장성, 이벤트 드리븐, 모니터링

### 데이터 엔지니어 (백엔드 전환)
- **추천**: 콘텐츠 추천 시스템
- **강조 포인트**: Kafka Streams, 데이터 파이프라인

---

## 📚 참고 자료

- [대규모 시스템 설계 기초](https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=281528474)
- [Release It! 2판](https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=201989031)
- [Real MySQL 8.0](https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=278488709)
- [Kafka: The Definitive Guide](https://www.oreilly.com/library/view/kafka-the-definitive/9781491936153/)
