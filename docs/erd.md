# ERD 설계

## 1. 데이터베이스 개요

커피 주문 시스템은 다음 5개의 테이블로 구성한다.

* `COFFEE_MENU`: 커피 메뉴 정보
* `POINT_ACCOUNT`: 사용자별 포인트 계정 및 잔액
* `COFFEE_ORDER`: 커피 주문 및 결제 정보
* `POINT_HISTORY`: 포인트 충전/결제 이력
* `OUTBOX_EVENT`: 외부 데이터 수집 플랫폼 전송을 위한 Outbox 이벤트

---

## 2. 테이블 설계

### 2.1 COFFEE_MENU

커피 메뉴의 기본 정보를 저장한다.

| 컬럼         | 타입           | 제약조건               | 설명    |
| ---------- | ------------ | ------------------ | ----- |
| id         | BIGINT       | PK, AUTO_INCREMENT | 메뉴 ID |
| name       | VARCHAR(100) | NOT NULL           | 메뉴명   |
| price      | BIGINT       | NOT NULL, > 0      | 메뉴 가격 |
| created_at | DATETIME(6)  | NOT NULL           | 생성 시각 |
| updated_at | DATETIME(6)  | NOT NULL           | 수정 시각 |

### 설계 의도

메뉴 가격은 원화 기준 정수로 저장한다.

금액 계산에서 부동소수점 오차가 발생하지 않도록 `BIGINT`를 사용한다.

---

### 2.2 POINT_ACCOUNT

사용자별 포인트 계정과 현재 잔액을 저장한다.

| 컬럼         | 타입           | 제약조건               | 설명        |
| ---------- | ------------ | ------------------ | --------- |
| id         | BIGINT       | PK, AUTO_INCREMENT | 포인트 계정 ID |
| user_id    | VARCHAR(100) | NOT NULL, UNIQUE   | 사용자 식별자   |
| balance    | BIGINT       | NOT NULL, >= 0     | 현재 포인트 잔액 |
| created_at | DATETIME(6)  | NOT NULL           | 생성 시각     |
| updated_at | DATETIME(6)  | NOT NULL           | 수정 시각     |

### 설계 의도

`user_id`를 UNIQUE로 설정하여 한 사용자당 하나의 포인트 계정만 존재하도록 한다.

포인트 잔액은 `POINT_ACCOUNT.balance`에서 관리하며, 충전 및 결제 시 해당 계정 행을 비관적 잠금으로 잠근다.

이를 통해 여러 서버 인스턴스에서 동시에 포인트를 변경하더라도 잔액 갱신 과정에서 Lost Update가 발생하지 않도록 한다.

---

### 2.3 COFFEE_ORDER

커피 주문 및 결제 정보를 저장한다.

| 컬럼              | 타입           | 제약조건               | 설명            |
| --------------- | ------------ | ------------------ | ------------- |
| id              | BIGINT       | PK, AUTO_INCREMENT | 주문 ID         |
| account_id      | BIGINT       | NOT NULL, FK       | 결제한 포인트 계정 ID |
| menu_id         | BIGINT       | NOT NULL, FK       | 주문한 메뉴 ID     |
| menu_name       | VARCHAR(100) | NOT NULL           | 주문 당시 메뉴명     |
| paid_amount     | BIGINT       | NOT NULL, > 0      | 실제 결제 금액      |
| status          | VARCHAR(20)  | NOT NULL           | 주문 상태         |
| idempotency_key | VARCHAR(100) | NOT NULL           | 중복 주문 방지 키    |
| created_at      | DATETIME(6)  | NOT NULL           | 주문 생성 시각      |
| paid_at         | DATETIME(6)  | NOT NULL           | 결제 완료 시각      |

### 제약조건

```text
FK account_id → POINT_ACCOUNT.id
FK menu_id → COFFEE_MENU.id
UNIQUE(account_id, idempotency_key)
```

### 설계 의도

API에서는 `userId`를 전달받지만 주문 테이블에는 `user_id`를 직접 저장하지 않고 `account_id`를 저장한다.

이를 통해 주문과 포인트 계정의 관계를 데이터베이스에서도 명확하게 보장한다.

`menu_name`과 `paid_amount`는 주문 당시의 값을 저장한다.

메뉴 가격이나 이름이 이후 변경되더라도 과거 주문의 결제 금액과 주문 당시 메뉴명을 보존할 수 있다.

`idempotency_key`는 동일한 주문 요청의 중복 처리를 방지하기 위해 사용한다.

동일한 사용자가 동일한 키를 사용하여 요청을 다시 보내면 기존 주문을 반환한다.

동일한 키로 다른 메뉴를 요청한 경우에는 요청 충돌로 판단하여 처리하지 않는다.

---

### 2.4 POINT_HISTORY

포인트 충전 및 결제 이력을 저장한다.

| 컬럼            | 타입          | 제약조건               | 설명               |
| ------------- | ----------- | ------------------ | ---------------- |
| id            | BIGINT      | PK, AUTO_INCREMENT | 이력 ID            |
| account_id    | BIGINT      | NOT NULL, FK       | 포인트 계정 ID        |
| order_id      | BIGINT      | NULL, FK, UNIQUE   | 결제 주문 ID         |
| type          | VARCHAR(20) | NOT NULL           | CHARGE / PAYMENT |
| amount        | BIGINT      | NOT NULL, > 0      | 변동 포인트           |
| balance_after | BIGINT      | NOT NULL, >= 0     | 변동 후 잔액          |
| created_at    | DATETIME(6) | NOT NULL           | 이력 생성 시각         |

### 제약조건

```text
FK account_id → POINT_ACCOUNT.id
FK order_id → COFFEE_ORDER.id
UNIQUE(order_id)
```

### 설계 의도

포인트 충전과 결제를 모두 이력으로 남긴다.

`balance_after`를 함께 저장하여 해당 거래가 완료된 직후의 잔액을 추적할 수 있도록 한다.

결제 이력의 `order_id`에 UNIQUE 제약을 적용하여 하나의 주문에 대해 결제 이력이 중복 생성되는 것을 방지한다.

MySQL에서는 UNIQUE 컬럼에 여러 개의 NULL을 허용하므로 충전 이력처럼 `order_id`가 없는 데이터도 저장할 수 있다.

---

### 2.5 OUTBOX_EVENT

주문 완료 후 외부 데이터 수집 플랫폼으로 전달해야 하는 이벤트를 저장한다.

| 컬럼              | 타입           | 제약조건               | 설명                                   |
| --------------- | ------------ | ------------------ | ------------------------------------ |
| id              | BIGINT       | PK, AUTO_INCREMENT | 내부 ID                                |
| event_id        | VARCHAR(100) | NOT NULL, UNIQUE   | 이벤트 식별자                              |
| event_type      | VARCHAR(100) | NOT NULL           | 이벤트 종류                               |
| payload         | JSON         | NOT NULL           | 외부 전송 데이터                            |
| status          | VARCHAR(20)  | NOT NULL           | PENDING / PROCESSING / SENT / FAILED |
| attempt_count   | INT          | NOT NULL           | 전송 시도 횟수                             |
| next_attempt_at | DATETIME(6)  | NULL               | 다음 전송 가능 시각                          |
| locked_until    | DATETIME(6)  | NULL               | 작업 잠금 만료 시각                          |
| last_error      | TEXT         | NULL               | 마지막 전송 오류                            |
| created_at      | DATETIME(6)  | NOT NULL           | 이벤트 생성 시각                            |
| sent_at         | DATETIME(6)  | NULL               | 전송 완료 시각                             |

### 설계 의도

주문 트랜잭션과 외부 API 호출을 분리하기 위해 Transactional Outbox 패턴을 사용한다.

주문 트랜잭션 안에서는 외부 API를 호출하지 않고 `OUTBOX_EVENT`만 저장한다.

트랜잭션이 성공적으로 커밋된 이후 별도의 Worker가 Outbox 이벤트를 조회하여 외부 플랫폼으로 전송한다.

이를 통해 외부 플랫폼 장애가 주문 및 포인트 결제 트랜잭션에 영향을 주지 않도록 한다.

여러 서버 인스턴스가 동시에 Worker를 실행할 수 있으므로 이벤트를 가져오는 과정에서 원자적인 선점 처리가 필요하다.

전송 실패 시 재시도하며, 최대 재시도 횟수를 초과한 이벤트는 `FAILED` 상태로 관리한다.

`event_id`는 외부 플랫폼에서 중복 이벤트를 방지하는 데 사용할 수 있다.

Transactional Outbox만으로 외부 시스템에 대한 정확히 한 번의 전달(Exactly Once)을 보장할 수는 없으므로 외부 시스템의 멱등 처리를 고려한다.

---

## 3. 테이블 관계

```text
POINT_ACCOUNT
    │
    ├── 1 : N ── COFFEE_ORDER
    │                 │
    │                 └── N : 1 ── COFFEE_MENU
    │
    └── 1 : N ── POINT_HISTORY
                         │
                         └── 0..1 : 1 ── COFFEE_ORDER

OUTBOX_EVENT
    └── 주문과 직접적인 FK 관계를 두지 않고
        payload에 외부 전송에 필요한 주문 정보를 저장
```

관계를 정리하면 다음과 같다.

* 하나의 `POINT_ACCOUNT`는 여러 주문을 가질 수 있다.
* 하나의 `COFFEE_MENU`는 여러 주문에 사용될 수 있다.
* 하나의 `POINT_ACCOUNT`는 여러 포인트 이력을 가진다.
* 하나의 주문은 결제 포인트 이력을 최대 하나 가진다.
* `OUTBOX_EVENT`는 주문과 DB FK로 직접 연결하지 않는다.

---

## 4. 주문 처리 트랜잭션

주문 요청이 들어오면 다음 과정을 하나의 DB 트랜잭션으로 처리한다.

```text
1. userId로 POINT_ACCOUNT 조회
2. account_id + idempotency_key로 기존 주문 확인
3. COFFEE_MENU 조회
4. POINT_ACCOUNT 행 잠금
5. 포인트 잔액 확인
6. 주문 당시 메뉴명/가격 확인
7. COFFEE_ORDER 저장
8. POINT_ACCOUNT 잔액 차감
9. POINT_HISTORY 저장
10. OUTBOX_EVENT 저장
11. COMMIT
```

포인트 차감, 주문 생성, 포인트 이력 생성, Outbox 이벤트 생성은 모두 하나의 트랜잭션으로 처리한다.

따라서 중간 단계에서 오류가 발생하면 전체 변경사항을 롤백한다.

---

## 5. 포인트 충전 트랜잭션

포인트 충전은 다음 과정을 하나의 트랜잭션으로 처리한다.

```text
1. userId에 해당하는 POINT_ACCOUNT 생성 또는 확보
2. POINT_ACCOUNT 행 잠금
3. 현재 balance 조회
4. 충전 금액 증가
5. POINT_HISTORY 저장
6. COMMIT
```

최초 충전 요청이 동시에 여러 서버에서 들어오는 경우에도 `user_id UNIQUE` 제약과 원자적인 계정 생성 처리를 사용하여 하나의 계정만 생성되도록 한다.

---

## 6. 동시성 제어

### 포인트 잔액

포인트 충전과 주문 결제 시 `POINT_ACCOUNT` 행에 비관적 잠금(Pessimistic Lock)을 적용한다.

예를 들어 잔액이 10,000포인트이고 동시에 7,000포인트 주문이 두 번 요청된 경우:

```text
요청 A → POINT_ACCOUNT 잠금 → 잔액 10,000 확인
       → 7,000 차감 → 잔액 3,000 → COMMIT

요청 B → A가 잠금을 해제할 때까지 대기
       → 잔액 3,000 확인
       → 7,000 부족
       → 결제 실패
```

이를 통해 두 요청이 동시에 기존 잔액을 읽고 잘못 차감하는 문제를 방지한다.

### 중복 요청

동시성 제어와 중복 요청 방지는 서로 다른 문제이므로 `idempotency_key`를 별도로 사용한다.

```text
동시성 제어
→ 여러 요청이 동시에 잔액을 변경하는 문제 해결

Idempotency
→ 동일한 요청이 여러 번 처리되는 문제 해결
```

---

## 7. 인기 메뉴 조회 기준

인기 메뉴는 최근 7일 동안 성공적으로 결제된 주문을 대상으로 계산한다.

기준:

```text
status = PAID
paid_at >= 현재 시각 - 7일
paid_at < 현재 시각
```

메뉴별 주문 수를 집계한 후 다음 순서로 정렬한다.

```text
1. 주문 수 DESC
2. menu_id ASC
3. 상위 3개
```

주문 수가 한 건도 없는 메뉴는 결과에 포함하지 않는다.

후보 인덱스는 다음과 같다.

```text
COFFEE_ORDER(status, paid_at, menu_id)
```

실제 인덱스 효과는 데이터 분포와 실행 계획을 확인하여 검증한다.

---

## 8. 시간 및 금액 기준

* 금액과 포인트는 정수로 저장한다.
* 포인트와 금액은 1원 = 1포인트로 처리한다.
* 서버와 DB의 시간은 UTC 기준으로 일관되게 관리한다.
* API에서 사용자에게 시간을 표시할 필요가 있다면 필요한 시간대에 맞게 변환한다.
* 주문의 `paid_at`을 인기 메뉴 집계의 기준 시간으로 사용한다.
