# Spring 멀티모듈 면접 질문 & 답변

## 기본 개념

### Q1: 멀티모듈을 사용하는 이유는 무엇인가요?
**답변**:

**1. 관심사의 분리 (Separation of Concerns)**
```
apps/       → 실행 가능한 애플리케이션
modules/    → 재사용 가능한 인프라 설정
supports/   → 횡단 관심사 (로깅, 모니터링)
```

각 모듈이 명확한 책임을 가지며, 변경의 영향 범위가 제한됩니다.

**2. 의존성 관리**
```kotlin
// commerce-api는 jpa, redis만 의존
dependencies {
    implementation(project(":modules:jpa"))
    implementation(project(":modules:redis"))
}

// commerce-streamer는 kafka도 추가
dependencies {
    implementation(project(":modules:kafka"))
}
```
필요한 모듈만 선택적으로 의존할 수 있습니다.

**3. 빌드 최적화**
- 변경된 모듈만 재빌드 가능
- 병렬 빌드로 속도 향상

**4. 팀 협업**
- 모듈별로 팀 분담 가능
- 충돌 최소화

**우리 프로젝트**: commerce-api와 commerce-streamer가 공통으로 JPA, Redis를 사용하는데,
모듈로 분리하여 설정 코드를 재사용하고 있습니다.

---

### Q2: 모듈 간 의존성을 어떻게 관리하나요?
**답변**:

**의존성 방향 규칙**:
```
apps → modules → supports
     ↘ supports ↗
```

- **apps**: 다른 모듈에 의존 가능, 다른 모듈이 의존 불가
- **modules**: supports에만 의존 가능
- **supports**: 다른 모듈에 의존 불가 (가장 기본)

**순환 참조 방지**:
```kotlin
// ❌ 나쁜 예: modules가 apps에 의존
// modules/jpa/build.gradle.kts
dependencies {
    implementation(project(":apps:commerce-api"))  // 금지!
}

// ✅ 좋은 예: apps가 modules에 의존
// apps/commerce-api/build.gradle.kts
dependencies {
    implementation(project(":modules:jpa"))  // OK
}
```

**Gradle 설정으로 강제**:
```kotlin
// build.gradle.kts
subprojects {
    plugins.withType<JavaPlugin> {
        project.afterEvaluate {
            configurations.all {
                // apps 모듈을 다른 모듈이 의존하지 못하도록
                if (project.parent?.name != "apps") {
                    dependencies.all {
                        if (group == "com.loopers" && name.startsWith("commerce-")) {
                            throw GradleException("Cannot depend on app modules")
                        }
                    }
                }
            }
        }
    }
}
```

---

**우리 프로젝트 설정**:
```kotlin
// build.gradle.kts
subprojects {
    // 기본: 모든 모듈은 Jar 생성
    tasks.withType(Jar::class) { enabled = true }
    tasks.withType(BootJar::class) { enabled = false }

    // apps 모듈만 BootJar 생성
    configure(allprojects.filter { it.parent?.name.equals("apps") }) {
        tasks.withType(Jar::class) { enabled = false }
        tasks.withType(BootJar::class) { enabled = true }
    }
}
```

**결과**:
```bash
./gradlew build

# modules/jpa/build/libs/jpa-xxx.jar           (일반 Jar)
# apps/commerce-api/build/libs/commerce-api-xxx.jar  (BootJar, 실행 가능)
```

---

## 실무 활용

### Q4: 공통 설정은 어떻게 관리하나요?
**답변**:

**1. 공통 의존성 (build.gradle.kts)**:
```kotlin
subprojects {
    dependencies {
        // 모든 모듈에 공통 적용
        implementation("org.springframework.boot:spring-boot-starter")
        implementation("org.projectlombok:lombok")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
    }
}
```

**2. 모듈별 설정 파일**:
```
modules/jpa/src/main/resources/jpa.yml
modules/redis/src/main/resources/redis.yml
modules/kafka/src/main/resources/kafka.yml
```

**3. 애플리케이션에서 import**:
```yaml
# apps/commerce-api/src/main/resources/application.yml
spring:
  config:
    import:
      - jpa.yml
      - redis.yml
      - logging.yml
      - monitoring.yml
```

**장점**:
- 설정 파일이 모듈과 함께 관리됨
- 모듈만 의존하면 설정도 자동으로 포함
- 각 모듈의 설정이 독립적

---

### Q5: test-fixtures는 무엇이고 언제 사용하나요?
**답변**:

**test-fixtures**: 테스트용 유틸리티를 다른 모듈에서 재사용할 수 있게 하는 Gradle 기능

**사용 예시**:
```kotlin
// modules/jpa/build.gradle.kts
plugins {
    `java-library`
    `java-test-fixtures`  // ← test-fixtures 활성화
}
```

**제공하는 유틸리티**:
```
modules/jpa/src/testFixtures/java/
├── DatabaseCleanUp.java           (테이블 초기화)
└── MySqlTestContainersConfig.java (Testcontainers 설정)
```

**다른 모듈에서 사용**:
```kotlin
// apps/commerce-api/build.gradle.kts
dependencies {
    implementation(project(":modules:jpa"))
    testImplementation(testFixtures(project(":modules:jpa")))  // ← 테스트 유틸리티만
}
```

**사용 코드**:
```java
@SpringBootTest
class ProductServiceTest {

    @Autowired
    private DatabaseCleanUp databaseCleanUp;  // jpa 모듈의 testFixtures

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }
}
```

**장점**:
- 테스트 유틸리티 중복 제거
- 모듈과 함께 유지보수
- 프로덕션 코드와 분리 (런타임에 포함 안 됨)

---

### Q6: ComponentScan은 어떻게 동작하나요?
**답변**:

**기본 동작**:
```java
// apps/commerce-api/src/main/java/com/loopers/CommerceApiApplication.java
@SpringBootApplication  // @ComponentScan 포함
public class CommerceApiApplication {
    // com.loopers 패키지 이하를 스캔
}
```

**멀티모듈에서의 스캔 범위**:
```
apps/commerce-api
└── com.loopers.CommerceApiApplication
    ├── com.loopers.domain.* (✅ 스캔됨)
    └── com.loopers.infrastructure.* (✅ 스캔됨)

modules/jpa
└── com.loopers.config.jpa.JpaConfig (✅ 스캔됨 - 같은 패키지)
```

**주의사항**:
```java
// ❌ 다른 패키지면 스캔 안 됨
// modules/jpa/src/main/java/com/other/JpaConfig.java
@Configuration
public class JpaConfig { }  // 스캔 안 됨!

// ✅ 명시적으로 Import
@SpringBootApplication
@Import(JpaConfig.class)
public class CommerceApiApplication { }

// ✅ 또는 같은 패키지 사용
// com.loopers.config.jpa.JpaConfig
```

**우리 프로젝트**: 모든 모듈이 `com.loopers` 패키지를 사용하여 자동 스캔됩니다.

---

## 고급 주제

### Q7: 모듈 간 인터페이스 분리는 어떻게 하나요?
**답변**:

**현재 구조**:
```
apps/commerce-api
├── domain/
│   └── product/
│       ├── ProductRepository.java (인터페이스)
│       └── ProductService.java
└── infrastructure/
    └── product/
        └── ProductRepositoryImpl.java (구현체)
```

**개선안: API 모듈 분리**:
```
commerce-api (인터페이스 모듈)
└── ProductService.java (인터페이스)
    ProductDto.java

commerce-api-impl (구현 모듈)
└── ProductServiceImpl.java
    ProductRepositoryImpl.java

order-api
└── dependencies: commerce-api (인터페이스만 의존)
```

**장점**:
- 구현체의 변경이 인터페이스에 영향 없음
- 다른 모듈이 구현체에 의존하지 않음
- 순환 참조 방지

**단점**:
- 모듈 수 증가
- 복잡도 증가

**적용 기준**: 다른 모듈이 코드 레벨로 의존하는 경우에만 분리

---

### Q8: 버전 관리는 어떻게 하나요?
**답변**:

**현재 방식: gradle.properties**:
```properties
springBootVersion=3.4.4
springCloudDependenciesVersion=2024.0.1
mockitoVersion=5.14.0
```

**장점**:
- 간단함
- 모든 모듈이 같은 버전 사용

**개선안: BOM (Bill of Materials) 모듈**:
```
bom/
└── build.gradle.kts
    dependencies {
        constraints {
            api("org.mockito:mockito-core:5.14.0")
            api("com.fasterxml.jackson.core:jackson-databind:2.15.0")
        }
    }
```

```kotlin
// 다른 모듈
dependencies {
    implementation(platform(project(":bom")))
    implementation("org.mockito:mockito-core")  // 버전 생략
}
```

**장점**:
- 버전 충돌 방지
- 일관된 버전 관리
- 여러 프로젝트에서 재사용 가능

---

## 트러블슈팅

### Q9: 모듈 빌드 순서 문제를 겪은 적 있나요?
**답변**:

**문제 상황**:
```bash
./gradlew :apps:commerce-api:build

# 에러: modules:jpa 를 찾을 수 없음
```

**원인**: jpa 모듈이 먼저 빌드되지 않음

**해결 1: 명시적 의존성**:
```kotlin
// settings.gradle.kts
include(
    ":modules:jpa",      // 먼저 선언
    ":modules:redis",
    ":apps:commerce-api"  // 나중에 선언
)
```

**해결 2: dependsOn**:
```kotlin
// apps/commerce-api/build.gradle.kts
tasks.named("build") {
    dependsOn(":modules:jpa:build")
}
```

**Gradle의 자동 처리**: 보통은 Gradle이 의존성을 분석하여 자동으로 순서 결정

---

### Q10: 멀티모듈 환경에서 테스트는 어떻게 격리하나요?
**답변**:

**문제**: 모듈 간 테스트가 서로 영향을 줄 수 있음

**해결 방법**:

**1. 테스트용 프로필 분리**:
```yaml
# modules/jpa/src/test/resources/application-test.yml
spring:
  datasource:
    url: ${datasource.mysql-jpa.main.jdbc-url}  # Testcontainers
```

**2. DatabaseCleanUp 사용**:
```java
@AfterEach
void tearDown() {
    databaseCleanUp.truncateAllTables();
}
```

**3. Testcontainers 독립 실행**:
```java
// 각 모듈마다 별도의 Testcontainers 인스턴스
@SpringBootTest
@Import(MySqlTestContainersConfig.class)
class ProductServiceTest { }
```

**4. 병렬 실행 비활성화** (필요 시):
```kotlin
tasks.test {
    maxParallelForks = 1  // 순차 실행
}
```

---

## 실전 팁

### 면접에서 강조할 포인트

1. **구체적인 구조**
   - "3계층 구조: apps(실행), modules(인프라), supports(횡단관심사)"
   - "현재 2개 앱, 3개 모듈, 3개 support로 구성"

2. **의존성 관리 전략**
   - "순환 참조 방지를 위한 의존 방향 규칙"
   - "test-fixtures로 테스트 코드 재사용"

3. **빌드 최적화**
   - "Jar vs BootJar 설정으로 빌드 속도 개선"
   - "변경된 모듈만 재빌드"

4. **확장성**
   - "새로운 앱 추가 시 기존 모듈 재사용"
   - "팀 협업 시 모듈별 분담 가능"
