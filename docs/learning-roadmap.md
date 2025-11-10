# 실무 중심 학습 로드맵

## 🎯 학습 목표

이 프로젝트는 **실무에서 바로 사용할 수 있는 기술 스택**을 학습하고, **이직 면접에서 경쟁력**을 갖추는 것을 목표로 합니다.

### 핵심 학습 주제
1. **Spring 멀티모듈** - 대규모 애플리케이션 구조 설계
2. **테스트 전략** - Unit/Integration/E2E 테스트와 Testcontainers
3. **Docker** - 컨테이너 기반 개발 환경 구축
4. **Kafka** - 이벤트 드리븐 아키텍처와 비동기 처리
5. **Redis** - 캐싱 전략과 성능 최적화
6. **MySQL** - 트랜잭션, 인덱싱, 쿼리 최적화
7. **모니터링** - Prometheus와 Grafana를 통한 관측 가능성

---

## 📚 학습 단계별 로드맵

### Phase 1: 기초 다지기 (1-2주)
**목표**: 프로젝트 구조를 이해하고 기본 CRUD 구현

#### 1.1 멀티모듈 이해하기
- [ ] 현재 프로젝트의 모듈 구조 분석 (`apps`, `modules`, `supports`)
- [ ] 각 모듈의 역할과 의존성 관계 파악
- [ ] `build.gradle.kts`의 설정 이해 (Jar vs BootJar)

**실습 과제**:
- [ ] 새로운 도메인 추가 (예: `Product` 도메인)
- [ ] 새로운 support 모듈 추가 (예: `common-utils`)

**면접 연결**: "멀티모듈을 사용한 이유는?", "모듈 간 순환 참조를 어떻게 방지하나요?"

#### 1.2 테스트 전략 이해하기
- [ ] Unit Test 작성 (`ExampleModelTest` 참고)
- [ ] Integration Test 작성 (`ExampleServiceIntegrationTest` 참고)
- [ ] E2E Test 작성 (`ExampleV1ApiE2ETest` 참고)
- [ ] Testcontainers 동작 원리 이해

**실습 과제**:
- [ ] 새로운 도메인에 대한 3단계 테스트 작성
- [ ] `DatabaseCleanUp` 유틸리티 확장

**면접 연결**: "통합 테스트 전략은?", "테스트 격리는 어떻게 하나요?"

---

### Phase 2: 실무 시나리오 구현 (3-4주)
**목표**: 실무에서 자주 마주치는 문제를 해결하며 학습

각 시나리오는 `docs/scenarios/` 폴더에 상세히 정리되어 있습니다.

#### 2.1 캐싱 전략 구현 (Redis)
📖 상세 가이드: [scenarios/01-caching-strategy.md](scenarios/01-caching-strategy.md)

**실무 상황**: 상품 상세 조회 API가 DB에 부하를 주고 있음. 응답 시간 개선 필요.

**학습 내용**:
- Cache-Aside 패턴
- Write-Through vs Write-Behind
- Cache Eviction 전략
- Redis Master-Replica 활용

**구현 과제**:
- [ ] 상품 조회에 Redis 캐싱 적용
- [ ] 캐시 TTL 설정 및 Eviction 정책 구현
- [ ] 캐시 Hit/Miss 비율 모니터링
- [ ] 캐시 워밍업 전략 구현

**면접 질문**:
- "캐시 일관성 문제를 어떻게 해결했나요?"
- "Cache Stampede 현상을 겪은 적 있나요?"

#### 2.2 이벤트 드리븐 아키텍처 (Kafka)
📖 상세 가이드: [scenarios/02-event-driven-architecture.md](scenarios/02-event-driven-architecture.md)

**실무 상황**: 주문 생성 시 여러 후속 처리(재고 차감, 알림 발송, 포인트 적립)가 필요함. 동기 처리로는 응답 시간이 느림.

**학습 내용**:
- Producer/Consumer 패턴
- 이벤트 설계 (Event Sourcing)
- 멱등성 처리
- Dead Letter Queue
- Consumer Group과 파티션 전략

**구현 과제**:
- [ ] 주문 생성 이벤트 발행 (Producer)
- [ ] 재고 차감 Consumer 구현
- [ ] 알림 발송 Consumer 구현
- [ ] 실패 시 재시도 및 DLQ 처리
- [ ] 중복 메시지 처리 (멱등성)

**면접 질문**:
- "Kafka를 사용한 이유는? RabbitMQ와의 차이는?"
- "메시지 중복 처리를 어떻게 보장하나요?"

#### 2.3 대용량 트래픽 처리
📖 상세 가이드: [scenarios/03-high-traffic-handling.md](scenarios/03-high-traffic-handling.md)

**실무 상황**: 특정 시간대(예: 타임딜)에 트래픽이 몰려서 서버가 다운됨.

**학습 내용**:
- Connection Pool 튜닝
- 트랜잭션 범위 최적화
- 비관적/낙관적 락
- 분산 락 (Redis)
- Rate Limiting

**구현 과제**:
- [ ] Redis를 이용한 분산 락 구현
- [ ] Connection Pool 설정 최적화
- [ ] 재고 차감 동시성 제어
- [ ] Rate Limiter 구현 (Bucket4j)

**면접 질문**:
- "동시성 문제를 어떻게 해결했나요?"
- "락 타임아웃 설정은 어떻게 하나요?"

#### 2.4 쿼리 성능 최적화 (MySQL)
📖 상세 가이드: [scenarios/04-query-optimization.md](scenarios/04-query-optimization.md)

**실무 상황**: 주문 목록 조회가 느림. 페이징 처리 시 성능 저하.

**학습 내용**:
- 인덱스 설계 및 최적화
- N+1 문제 해결
- QueryDSL 활용
- Covering Index
- 페이징 최적화 (No Offset)

**구현 과제**:
- [ ] Slow Query 분석 및 인덱스 추가
- [ ] N+1 문제를 Fetch Join으로 해결
- [ ] QueryDSL로 동적 쿼리 구현
- [ ] Cursor 기반 페이징 구현

**면접 질문**:
- "인덱스 설계 원칙은?"
- "N+1 문제를 어떻게 발견하고 해결했나요?"

#### 2.5 트랜잭션 관리
📖 상세 가이드: [scenarios/05-transaction-management.md](scenarios/05-transaction-management.md)

**실무 상황**: 결제 처리 중 오류 발생 시 부분적으로만 롤백되는 문제.

**학습 내용**:
- 트랜잭션 전파(Propagation)
- 격리 수준(Isolation Level)
- 분산 트랜잭션 (Saga 패턴)
- @Transactional 동작 원리

**구현 과제**:
- [ ] 결제 처리 트랜잭션 설계
- [ ] Saga 패턴으로 분산 트랜잭션 구현
- [ ] 보상 트랜잭션(Compensation) 구현
- [ ] 트랜잭션 격리 수준 테스트

**면접 질문**:
- "트랜잭션 전파 레벨을 변경한 경험은?"
- "분산 트랜잭션은 어떻게 처리하나요?"

---

### Phase 3: 운영 및 모니터링 (2-3주)
**목표**: 프로덕션 환경을 고려한 운영 능력 습득

#### 3.1 모니터링 구축
📖 상세 가이드: [scenarios/06-monitoring-observability.md](scenarios/06-monitoring-observability.md)

**실무 상황**: 서비스 장애가 발생했는데 원인을 찾기 어려움.

**학습 내용**:
- Micrometer로 메트릭 수집
- Prometheus 쿼리 작성
- Grafana 대시보드 구성
- Custom 메트릭 생성
- 알람 설정

**구현 과제**:
- [ ] 비즈니스 메트릭 수집 (주문 수, 매출 등)
- [ ] JVM 메트릭 모니터링
- [ ] Kafka Consumer Lag 모니터링
- [ ] Redis 성능 메트릭 수집
- [ ] 임계값 기반 알람 설정

**면접 질문**:
- "어떤 메트릭을 모니터링하나요?"
- "장애 발견을 위한 알람 전략은?"

#### 3.2 로깅 전략
📖 상세 가이드: [scenarios/07-logging-strategy.md](scenarios/07-logging-strategy.md)

**실무 상황**: 에러 로그가 너무 많아서 중요한 로그를 찾기 어려움.

**학습 내용**:
- 구조화된 로깅
- Correlation ID를 통한 요청 추적
- 로그 레벨 전략
- MDC(Mapped Diagnostic Context) 활용

**구현 과제**:
- [ ] Correlation ID 자동 생성 및 전파
- [ ] Kafka 메시지에 Correlation ID 포함
- [ ] 구조화된 JSON 로그 포맷
- [ ] 민감 정보 마스킹

**면접 질문**:
- "분산 환경에서 로그 추적은 어떻게 하나요?"
- "로그 레벨 전략은?"

#### 3.3 Docker 기반 배포
📖 상세 가이드: [scenarios/08-docker-deployment.md](scenarios/08-docker-deployment.md)

**실무 상황**: 로컬/개발/운영 환경의 차이로 인한 문제 발생.

**학습 내용**:
- Multi-stage 빌드
- Docker Compose 활용
- 환경별 설정 관리
- Health Check 구성

**구현 과제**:
- [ ] 애플리케이션 Dockerfile 작성
- [ ] 전체 스택 docker-compose 구성
- [ ] 환경 변수로 설정 관리
- [ ] Health Check 엔드포인트 구현

**면접 질문**:
- "Docker를 사용한 이유는?"
- "컨테이너 최적화는 어떻게 하나요?"

---

### Phase 4: 고급 주제 (2-3주)
**목표**: 시니어 레벨의 아키텍처 설계 능력

#### 4.1 Circuit Breaker 패턴
📖 상세 가이드: [scenarios/09-circuit-breaker.md](scenarios/09-circuit-breaker.md)

**실무 상황**: 외부 API 장애가 전체 서비스로 전파됨.

**학습 내용**:
- Resilience4j Circuit Breaker
- Fallback 전략
- Bulkhead 패턴
- Retry 정책

**구현 과제**:
- [ ] 외부 API 호출에 Circuit Breaker 적용
- [ ] Fallback 응답 구현
- [ ] 상태 변화 모니터링
- [ ] Timeout 설정 최적화

#### 4.2 API 버전 관리
📖 상세 가이드: [scenarios/10-api-versioning.md](scenarios/10-api-versioning.md)

**실무 상황**: 기존 API를 변경해야 하는데 하위 호환성 유지 필요.

**학습 내용**:
- API 버전 관리 전략
- Deprecation 전략
- OpenAPI 문서화

**구현 과제**:
- [ ] V1, V2 API 동시 운영
- [ ] Deprecation Warning 헤더 추가
- [ ] Swagger UI로 버전별 문서화

#### 4.3 테스트 자동화
📖 상세 가이드: [scenarios/11-test-automation.md](scenarios/11-test-automation.md)

**실무 상황**: 배포 전 테스트에 시간이 너무 오래 걸림.

**학습 내용**:
- 테스트 병렬화
- 테스트 데이터 관리
- Contract Testing
- Performance Testing

**구현 과제**:
- [ ] 테스트 병렬 실행 최적화
- [ ] Fixture Factory 패턴 구현
- [ ] JMeter/Gatling으로 성능 테스트

---

## 🎓 면접 준비 체크리스트

각 주제별 상세 면접 질문과 답변은 `docs/interview-prep/` 폴더를 참고하세요.

### 필수 준비 항목
- [ ] [Spring 멀티모듈 면접 질문](interview-prep/spring-multimodule.md)
- [ ] [Kafka 면접 질문](interview-prep/kafka.md)
- [ ] [Redis 면접 질문](interview-prep/redis.md)
- [ ] [MySQL 면접 질문](interview-prep/mysql.md)
- [ ] [테스트 전략 면접 질문](interview-prep/testing.md)
- [ ] [모니터링 면접 질문](interview-prep/monitoring.md)
- [ ] [아키텍처 설계 면접 질문](interview-prep/architecture.md)

### 프로젝트 설명 준비
- [ ] "가장 어려웠던 기술적 도전은?"
- [ ] "성능 개선 경험을 말씀해주세요"
- [ ] "장애 대응 경험은?"
- [ ] "왜 이런 아키텍처를 선택했나요?"

---

## 📊 학습 진행 추적

### 완료 기준
각 시나리오마다:
- ✅ 코드 구현 완료
- ✅ 테스트 코드 작성 완료
- ✅ 문서화 완료 (README 또는 주석)
- ✅ 면접 질문에 답변할 수 있음

### 추천 학습 순서
1. 기초: 멀티모듈 → 테스트 전략
2. 중급: 캐싱 → Kafka → 트랜잭션
3. 고급: 쿼리 최적화 → 동시성 제어
4. 운영: 모니터링 → 로깅 → Docker

---

## 💡 추가 리소스

### 참고 자료
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [Redis Best Practices](https://redis.io/docs/management/optimization/)
- [MySQL Performance Tuning](https://dev.mysql.com/doc/refman/8.0/en/optimization.html)

### 추천 도서
- "가상 면접 사례로 배우는 대규모 시스템 설계 기초"
- "데이터 중심 애플리케이션 설계"
- "Release It! 2판"

### 커뮤니티
- 인프런 Q&A
- Stack Overflow
- GitHub Issues
