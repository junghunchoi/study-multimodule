# 학습 가이드 문서

이 디렉토리는 실무 중심의 백엔드 학습을 위한 가이드와 시나리오를 제공합니다.

## 📚 문서 구조

```
docs/
├── README.md                    (이 파일)
├── learning-roadmap.md          (전체 학습 로드맵)
├── project-ideas.md             (실전 프로젝트 아이디어)
├── scenarios/                   (실무 시나리오별 가이드)
│   ├── 01-caching-strategy.md
│   ├── 02-event-driven-architecture.md
│   └── 03-high-traffic-handling.md
└── interview-prep/              (면접 준비 자료)
    ├── kafka.md
    └── spring-multimodule.md
```

## 🚀 빠른 시작

### 1단계: 학습 로드맵 확인
👉 [learning-roadmap.md](learning-roadmap.md)

전체 학습 과정을 4단계(Phase 1-4)로 나누어 설명합니다:
- Phase 1: 기초 다지기 (1-2주)
- Phase 2: 실무 시나리오 구현 (3-4주)
- Phase 3: 운영 및 모니터링 (2-3주)
- Phase 4: 고급 주제 (2-3주)

### 2단계: 실무 시나리오 학습
각 시나리오는 **실제 회사에서 겪을 법한 상황**을 기반으로 작성되었습니다.

| 시나리오 | 주요 기술 | 난이도 | 예상 시간 |
|---------|---------|--------|----------|
| [캐싱 전략](scenarios/01-caching-strategy.md) | Redis, Cache-Aside | ⭐⭐ | 1주 |
| [이벤트 드리븐](scenarios/02-event-driven-architecture.md) | Kafka, Producer/Consumer | ⭐⭐⭐ | 2주 |
| [대용량 트래픽](scenarios/03-high-traffic-handling.md) | 분산 락, Rate Limiting | ⭐⭐⭐ | 2주 |

### 3단계: 면접 준비
기술별 면접 질문과 모범 답변:
- [Kafka 면접 질문](interview-prep/kafka.md)
- [Spring 멀티모듈 면접 질문](interview-prep/spring-multimodule.md)

### 4단계: 프로젝트 구현
👉 [project-ideas.md](project-ideas.md)

포트폴리오로 만들 수 있는 5가지 프로젝트 아이디어를 제공합니다.

---

## 🎯 학습 목표

이 가이드를 완료하면 다음을 달성할 수 있습니다:

### 기술적 역량
- ✅ Spring 멀티모듈 프로젝트 설계 및 구현
- ✅ Kafka 기반 이벤트 드리븐 아키텍처 구축
- ✅ Redis를 활용한 캐싱 및 동시성 제어
- ✅ MySQL 쿼리 최적화 및 인덱싱
- ✅ Docker 기반 개발 환경 구축
- ✅ Prometheus + Grafana 모니터링 구축
- ✅ 통합 테스트 및 부하 테스트 작성

### 실무 역량
- ✅ 대용량 트래픽 처리 경험
- ✅ 동시성 문제 해결 능력
- ✅ 장애 대응 및 모니터링 능력
- ✅ 성능 최적화 경험

### 면접 준비
- ✅ 각 기술에 대한 깊이 있는 이해
- ✅ 실무 경험 기반 답변 가능
- ✅ 구체적인 수치로 성과 어필

---

## 📖 학습 방법

### 추천 학습 순서

#### 초급자 (백엔드 경력 1년 미만)
```
1. 멀티모듈 이해 → 테스트 전략 학습
2. 캐싱 전략 구현
3. Kafka 기본 개념 학습
4. 간단한 이벤트 드리븐 구현
```

#### 중급자 (백엔드 경력 1-3년)
```
1. 전체 로드맵 확인
2. 캐싱 → 이벤트 드리븐 → 동시성 제어 순서로 구현
3. 모니터링 대시보드 구축
4. 프로젝트 1개 완성
```

#### 고급자 (백엔드 경력 3년 이상)
```
1. 고급 시나리오 중심으로 학습
2. 성능 개선 프로젝트 진행
3. 아키텍처 설계 경험 축적
4. 면접 준비 및 포트폴리오 정리
```

### 학습 체크리스트

각 시나리오마다 다음을 완료하세요:

- [ ] **코드 구현**: 시나리오의 모든 단계 구현
- [ ] **테스트 작성**: Unit, Integration, E2E 테스트
- [ ] **성능 측정**: Before/After 수치 기록
- [ ] **문서화**: README 또는 주석으로 설명
- [ ] **면접 준비**: 관련 면접 질문에 답변 가능

---

## 🎓 실무 시나리오 상세 설명

### Scenario 01: 캐싱 전략 구현
**실무 상황**: 상품 상세 조회 API가 DB에 부하를 주고 응답이 느림

**학습 내용**:
- Cache-Aside 패턴
- Cache Stampede 해결 (분산 락)
- 캐시 워밍업 전략
- 성능 메트릭 수집

**성과 예시**:
> "Redis 캐싱 도입으로 응답 시간을 150ms → 15ms로 개선했고,
> DB CPU 사용률을 80% → 20%로 감소시켰습니다. 캐시 히트율 95% 달성."

---

### Scenario 02: 이벤트 드리븐 아키텍처
**실무 상황**: 주문 생성 시 여러 후속 처리로 인해 응답이 3초 이상 소요

**학습 내용**:
- Kafka Producer/Consumer 구현
- 멱등성 처리 (중복 메시지 방지)
- Dead Letter Queue 처리
- Consumer Group 전략

**성과 예시**:
> "Kafka 도입으로 주문 생성 API 응답 시간을 3000ms → 210ms로 개선.
> 초당 10,000 TPS 처리 가능한 아키텍처 구축."

---

### Scenario 03: 대용량 트래픽 처리
**실무 상황**: 타임딜 시작 시 재고보다 많은 주문 생성 (Over-selling)

**학습 내용**:
- 비관적 락 vs 낙관적 락
- Redis 분산 락 구현
- Rate Limiting
- 부하 테스트 (Gatling)

**성과 예시**:
> "Redis 분산 락으로 오버셀링 문제를 100% 해결했고,
> Gatling 부하 테스트로 TPS 10,000 처리 검증 완료."

---

## 💼 이직 준비 가이드

### 3개월 로드맵

**1개월차: 기초 + 핵심 시나리오**
- [ ] 멀티모듈 구조 이해
- [ ] 테스트 전략 학습
- [ ] 캐싱 전략 구현
- [ ] 이벤트 드리븐 기본 구현

**2개월차: 고급 시나리오 + 프로젝트**
- [ ] 동시성 제어 구현
- [ ] 쿼리 최적화
- [ ] 모니터링 구축
- [ ] 프로젝트 1개 완성

**3개월차: 포트폴리오 + 면접 준비**
- [ ] GitHub 프로필 정리
- [ ] README 작성
- [ ] 면접 질문 답변 준비
- [ ] 모의 면접 연습

### 포트폴리오 체크리스트

- [ ] **README 작성**: 프로젝트 소개, 아키텍처, 성과
- [ ] **코드 품질**: 테스트 커버리지 70% 이상
- [ ] **문서화**: API 문서, 아키텍처 다이어그램
- [ ] **성과 수치**: Before/After 비교
- [ ] **실행 가능**: Docker Compose로 쉽게 실행
- [ ] **CI/CD**: GitHub Actions 설정

---

## 🔗 관련 리소스

### 공식 문서
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [Redis Best Practices](https://redis.io/docs/management/optimization/)
- [Prometheus Documentation](https://prometheus.io/docs/)

### 추천 도서
- 가상 면접 사례로 배우는 대규모 시스템 설계 기초
- 데이터 중심 애플리케이션 설계
- Release It! 2판
- Real MySQL 8.0

### 온라인 강의
- 인프런: Spring Boot 실전 가이드
- Udemy: Apache Kafka Series

---

## 🤝 기여 및 피드백

이 학습 가이드는 지속적으로 개선됩니다.

**개선 제안**:
- 새로운 시나리오 추가
- 면접 질문 업데이트
- 코드 예제 개선

**문의**:
- GitHub Issues로 질문 및 제안 환영

---

## 📊 학습 진행 현황 기록

### 나의 학습 현황 (체크리스트)

#### Phase 1: 기초 (1-2주)
- [ ] 멀티모듈 구조 이해
- [ ] 3단계 테스트 전략 학습
- [ ] 새로운 도메인 추가 실습

#### Phase 2: 실무 시나리오 (3-4주)
- [ ] 캐싱 전략 구현
- [ ] 이벤트 드리븐 아키텍처 구현
- [ ] 동시성 제어 구현
- [ ] 쿼리 최적화

#### Phase 3: 운영 (2-3주)
- [ ] 모니터링 대시보드 구축
- [ ] 로깅 전략 수립
- [ ] Docker 환경 구성

#### Phase 4: 고급 (2-3주)
- [ ] Circuit Breaker 구현
- [ ] API 버전 관리
- [ ] 부하 테스트

#### 면접 준비
- [ ] Kafka 면접 질문 답변 준비
- [ ] Spring 멀티모듈 면접 질문 답변 준비
- [ ] Redis 면접 질문 답변 준비
- [ ] MySQL 면접 질문 답변 준비

#### 포트폴리오
- [ ] 프로젝트 1개 완성
- [ ] README 작성
- [ ] 발표 자료 준비

---

## 💡 학습 팁

### 효과적인 학습 방법

1. **이론 → 실습 → 문서화 → 면접 준비** 순서로 진행
2. **성과는 반드시 수치화** (응답 시간, TPS, 개선율)
3. **실패 경험도 기록** (문제 → 해결 → 결과)
4. **코드리뷰 요청** (커뮤니티 활용)
5. **꾸준히 커밋** (잔디 심기)

### 면접 준비 팁

1. **STAR 기법 활용**
   - Situation: 어떤 상황이었나?
   - Task: 무엇을 해야 했나?
   - Action: 어떻게 해결했나?
   - Result: 결과는 어땠나?

2. **구체적 수치 강조**
   - "응답 시간 93% 개선"
   - "TPS 10,000 처리 검증"

3. **트레이드오프 언급**
   - "A 방법과 B 방법을 고려했고, C 이유로 A를 선택"

4. **학습 의지 표현**
   - "이 기술을 적용하면서 D를 추가로 학습했습니다"

---

**이제 [learning-roadmap.md](learning-roadmap.md)를 열어서 학습을 시작하세요! 🚀**
