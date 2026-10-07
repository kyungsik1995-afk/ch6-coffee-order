# K사 커피 주문 시스템

K사 서버 개발 과제로 구현한 커피 주문 시스템입니다.

## 프로젝트 목표

Spring Boot와 JPA를 활용하여 커피 메뉴 조회 및 포인트 충전 기능을 구현하고, 이후 주문/결제와 동시성 제어 등의 기능을 확장하는 것을 목표로 합니다.

## 기술 스택

- Java 17
- Spring Boot 4.0.8
- Spring Data JPA
- Spring Web MVC
- MySQL
- Gradle
- JUnit 5
- Mockito

## 주요 기능

### 1. 커피 메뉴 조회

`GET /api/menus`

커피 메뉴의 ID, 이름, 가격을 조회합니다.

응답 예시:

```json
[
  {
    "menuId": 1,
    "name": "아메리카노",
    "price": 3000
  }
]

Entity를 API 응답으로 직접 반환하지 않고 CoffeeMenuResponse DTO로 변환하여 반환하도록 구현했습니다.

이를 통해 데이터베이스 Entity와 외부 API의 응답 구조를 분리했습니다.

2. 포인트 충전

POST /api/points/charge

요청:

{
  "userId": "user-1001",
  "amount": 5000
}

응답:

{
  "userId": "user-1001",
  "chargedAmount": 5000,
  "balance": 5000
}

포인트 계정과 포인트 변경 이력을 분리하여 관리합니다.

PointAccount: 현재 포인트 잔액 관리
PointHistory: 포인트 충전 및 변경 이력 관리

userId에는 DB UNIQUE 제약조건을 적용하여 하나의 사용자에게 하나의 포인트 계정만 존재하도록 설계했습니다.

3. 요청 데이터 검증

Bean Validation을 사용하여 포인트 충전 요청을 검증합니다.

userId 필수
amount 필수
amount는 0보다 커야 함

잘못된 요청은 HTTP 400으로 처리합니다.

4. 예외 처리

@RestControllerAdvice를 이용하여 Validation 예외를 공통 응답 형식으로 처리했습니다.

{
  "code": "POINT_001",
  "message": "충전 요청이 올바르지 않습니다."
}
5. 트랜잭션

포인트 충전 시 계정 잔액 변경과 포인트 이력 저장을 하나의 트랜잭션으로 처리하도록 @Transactional을 적용했습니다.

포인트 계정 조회/생성
        ↓
포인트 잔액 변경
        ↓
포인트 이력 저장
        ↓
      COMMIT
6. 동시성 제어 설계

포인트 잔액에 대한 동시 수정 문제를 고려하여 PESSIMISTIC_WRITE 락을 적용했습니다.

@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<PointAccount> findByUserId(String userId);

또한 userId에 DB UNIQUE 제약조건을 적용하여 계정 중복 생성을 방지하도록 설계했습니다.

테스트

다음 테스트를 작성했습니다.

메뉴 목록 조회 Controller 테스트
포인트 충전 Controller 테스트
포인트 입력값 Validation 테스트
포인트 충전 Service 단위 테스트
실제 MySQL을 사용하는 포인트 통합 테스트
포인트 동시성 테스트 작성

테스트 환경은 별도의 MySQL 데이터베이스를 사용하도록 구성했습니다.

ch6_coffee_order_test
데이터베이스 설계

현재 주요 테이블:

COFFEE_MENU
POINT_ACCOUNT
POINT_HISTORY

향후 주문 및 외부 이벤트 처리를 위해 다음 테이블도 설계했습니다.

COFFEE_ORDER
OUTBOX_EVENT

자세한 설계는 다음 문서를 참고합니다.

docs/erd.md
docs/api-spec.md
향후 구현 계획

제출 이후 다음 기능을 추가로 구현할 예정입니다.

커피 주문 및 포인트 결제
주문 동시성 제어
Idempotency Key를 이용한 중복 주문 방지
Transactional Outbox를 이용한 외부 이벤트 처리
최근 7일 인기 메뉴 TOP 3 조회
다중 서버 환경에서의 동시성 및 데이터 정합성 검증
프로젝트 진행 방식

설계 → Entity/Repository → Service → Controller/DTO → 테스트 순서로 기능을 구현하고 있습니다.
