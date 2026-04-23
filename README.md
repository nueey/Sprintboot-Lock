# Spring Lock Starter

애노테이션 하나로 **비관적 락 / 낙관적 락 / Redis 분산 락**을 교체할 수 있는 Spring Boot Starter입니다.

## 배경

실무에서 동시성 문제를 해결할 때마다 락 종류마다 제각각인 구현 코드를 작성해야 했습니다.  
Redisson 코드가 서비스 레이어에 직접 노출되거나, 재시도 로직이 중복되는 문제를 해결하기 위해  
**락 전략을 애노테이션으로 추상화**한 스타터를 직접 설계했습니다.

## 지원 애노테이션

| 애노테이션 | 전략 | 적합한 상황 |
|---|---|---|
| `@DistributedLock` | Redis (Redisson) | 다중 인스턴스, 높은 충돌 빈도 |
| `@PessimisticLock` | JPA `SELECT FOR UPDATE` | 단일 DB, 충돌 빈도 높음 |
| `@OptimisticLock` | JPA 버전 필드 + 재시도 | 충돌이 드문 시나리오 |

## 빠른 시작

### 의존성 추가

```gradle
dependencies {
    implementation 'io.github.mye19:spring-lock-starter:1.0.0'

    // 분산 락 사용 시
    implementation 'org.redisson:redisson-spring-boot-starter:3.27.2'
}
```

### 분산 락

```java
// SpEL로 동적 키 생성 지원
@DistributedLock(key = "'order:' + #orderId", waitTime = 3, leaseTime = 5)
public void processOrder(Long orderId) {
    // 다중 서버 환경에서 동시 실행 차단
}
```

### 비관적 락

```java
@PessimisticLock(timeoutMillis = 3000)
public void decreaseStock(Long productId, int quantity) {
    // SELECT FOR UPDATE 로 행 단위 잠금
    Stock stock = stockRepository.findByProductIdWithLock(productId);
    stock.decrease(quantity);
}
```

### 낙관적 락

```java
@OptimisticLock(maxRetry = 3, retryDelayMillis = 100)
public void increaseLikeCount(Long postId) {
    // 버전 충돌 시 최대 3회 자동 재시도
    Post post = postRepository.findById(postId).orElseThrow();
    post.like();
}
```

## 설정

```yaml
spring:
  lock:
    distributed:
      enabled: true   # RedissonClient 빈 없으면 자동 비활성화
    pessimistic:
      enabled: true   # JPA 없으면 자동 비활성화
    optimistic:
      enabled: true
```

## 설계 결정

### 트랜잭션 AOP 순서
`@DistributedLock`은 `@Order(1)`로 트랜잭션 AOP(`@Order(2)`)보다 **먼저 실행**됩니다.  
락 해제가 트랜잭션 커밋 이후에 일어나도록 보장해, "커밋 전 락 해제 → 다른 스레드가 커밋 전 데이터 읽음" 문제를 방지합니다.

```
[DistributedLockAspect] → [TX begin] → [method] → [TX commit] → [lock.unlock()]
```

### SpEL 키 파싱
`@DistributedLock(key = "'coupon:' + #userId + ':' + #couponId")` 처럼  
파라미터를 조합한 동적 키를 지원합니다. 객체 필드 접근(`#user.id`)도 가능합니다.

### 조건부 자동 구성
- RedissonClient 빈이 없으면 `DistributedLockAspect`는 등록되지 않습니다.
- JPA가 클래스패스에 없으면 `PessimisticLockAspect`는 등록되지 않습니다.
- 불필요한 의존성 없이 필요한 락만 선택해서 사용할 수 있습니다.

## 프로젝트 구조

```
src/main/java/io/github/mye19/lock/
├── annotation/
│   ├── DistributedLock.java       # 분산 락 애노테이션
│   ├── PessimisticLock.java       # 비관적 락 애노테이션
│   └── OptimisticLock.java        # 낙관적 락 재시도 애노테이션
├── aspect/
│   ├── DistributedLockAspect.java # Redisson 락 획득/해제
│   ├── PessimisticLockAspect.java # JPA 락 힌트 전파
│   └── OptimisticLockAspect.java  # 버전 충돌 재시도
├── config/
│   └── LockAutoConfiguration.java # 조건부 자동 구성
├── exception/
│   ├── LockAcquisitionException.java
│   └── OptimisticLockRetryExhaustedException.java
├── properties/
│   └── LockProperties.java        # application.yml 프로퍼티
└── strategy/
    └── SpelKeyParser.java         # SpEL 키 파싱
```

## 기술 스택

- Java 17
- Spring Boot 3.2
- Redisson 3.27 (분산 락)
- Spring AOP (AspectJ)
- Gradle
