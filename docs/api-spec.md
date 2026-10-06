# API 명세

## 1. 공통 사항

### Base URL

```text
/api
```

### Content-Type

요청 및 응답은 JSON을 사용한다.

```text
Content-Type: application/json
```

### 금액 및 포인트

모든 금액과 포인트는 정수로 표현한다.

```text
1원 = 1포인트
```

---

# 2. 메뉴 목록 조회

## GET `/api/menus`

등록된 커피 메뉴 목록을 조회한다.

### Request

요청 본문 없음.

### Response

**200 OK**

```json
[
  {
    "menuId": 1,
    "name": "아메리카노",
    "price": 4500
  },
  {
    "menuId": 2,
    "name": "카페라떼",
    "price": 5000
  }
]
```

메뉴가 없는 경우 빈 배열을 반환한다.

```json
[]
```

---

# 3. 포인트 충전

## POST `/api/points/charge`

사용자의 포인트를 충전한다.

### Request

```json
{
  "userId": "user-1001",
  "amount": 10000
}
```

### 요청 조건

* `userId`는 필수이다.
* `amount`는 0보다 커야 한다.
* 사용자의 포인트 계정이 존재하지 않는 경우 최초 충전 시 생성한다.
* 기존 포인트 계정이 존재하는 경우 현재 잔액에 충전 금액을 더한다.

### 처리 과정

```text
1. userId에 해당하는 POINT_ACCOUNT 생성 또는 확보
2. POINT_ACCOUNT 행 잠금
3. 현재 잔액 조회
4. 충전 금액 증가
5. POINT_HISTORY 저장
6. COMMIT
```

포인트 잔액 변경과 충전 이력 저장은 하나의 트랜잭션으로 처리한다.

### Response

**200 OK**

```json
{
  "userId": "user-1001",
  "chargedAmount": 10000,
  "balance": 10000
}
```

### Error

| HTTP | Code      | 상황                  |
| ---- | --------- | ------------------- |
| 400  | POINT_001 | 충전 금액이 0 이하         |
| 409  | POINT_004 | 동시성 문제로 처리할 수 없는 경우 |

---

# 4. 커피 주문 및 결제

## POST `/api/orders`

포인트를 사용하여 커피를 주문하고 결제한다.

### Request Header

```text
X-Idempotency-Key: 7c4a-20261006-0001
```

`X-Idempotency-Key`는 동일한 주문 요청의 중복 처리를 방지하기 위한 값이다.

### Request Body

```json
{
  "userId": "user-1001",
  "menuId": 1
}
```

### 처리 조건

* `userId`는 필수이다.
* `menuId`는 필수이다.
* 존재하는 메뉴만 주문할 수 있다.
* 포인트 계정이 존재해야 한다.
* 포인트 잔액이 결제 금액보다 많거나 같아야 한다.
* 결제 금액은 주문 시점의 메뉴 가격을 사용한다.

### 처리 과정

```text
1. userId에 해당하는 POINT_ACCOUNT 조회
2. account_id + idempotency_key로 기존 주문 확인
3. 기존 주문이 있으면 해당 주문 반환
4. COFFEE_MENU 조회
5. POINT_ACCOUNT 행 잠금
6. 포인트 잔액 확인
7. 주문 당시 메뉴명/가격 저장
8. COFFEE_ORDER 저장
9. POINT_ACCOUNT 잔액 차감
10. POINT_HISTORY 저장
11. OUTBOX_EVENT 저장
12. COMMIT
```

모든 DB 변경사항은 하나의 트랜잭션으로 처리한다.

외부 데이터 수집 플랫폼에 대한 HTTP 요청은 해당 트랜잭션 안에서 수행하지 않는다.

### 신규 주문 Response

**201 Created**

```json
{
  "orderId": 1001,
  "userId": "user-1001",
  "menuId": 1,
  "menuName": "아메리카노",
  "paidAmount": 4500,
  "remainingPoint": 5500,
  "status": "PAID"
}
```

### 동일한 Idempotency-Key 재요청

이미 처리된 주문과 동일한 `X-Idempotency-Key`로 다시 요청하면 새로운 주문을 생성하지 않고 기존 주문을 반환한다.

**200 OK**

```json
{
  "orderId": 1001,
  "userId": "user-1001",
  "menuId": 1,
  "menuName": "아메리카노",
  "paidAmount": 4500,
  "remainingPoint": 5500,
  "status": "PAID"
}
```

동일한 키로 다른 메뉴를 요청하는 경우 요청 충돌로 판단한다.

**409 Conflict**

```json
{
  "code": "ORDER_002",
  "message": "동일한 Idempotency-Key로 다른 주문을 요청할 수 없습니다."
}
```

### Error

| HTTP | Code      | 상황                    |
| ---- | --------- | --------------------- |
| 400  | ORDER_001 | 주문 요청 정보가 올바르지 않음     |
| 404  | MENU_001  | 메뉴가 존재하지 않음           |
| 404  | POINT_002 | 포인트 계정이 존재하지 않음       |
| 409  | POINT_003 | 포인트 잔액 부족             |
| 409  | POINT_004 | 동시성 문제로 처리할 수 없음      |
| 409  | ORDER_002 | Idempotency-Key 요청 충돌 |

---

# 5. 인기 메뉴 조회

## GET `/api/menus/popular`

최근 7일 동안 가장 많이 주문된 메뉴 상위 3개를 조회한다.

### 집계 기준

* `COFFEE_ORDER.status = PAID`
* `paid_at >= 현재 시각 - 7일`
* `paid_at < 현재 시각`
* 메뉴별 주문 횟수를 집계
* 주문 횟수 내림차순
* 주문 횟수가 같은 경우 `menuId` 오름차순
* 상위 3개만 반환

### 예시 Response

**200 OK**

```json
[
  {
    "menuId": 1,
    "name": "아메리카노",
    "orderCount": 37
  },
  {
    "menuId": 2,
    "name": "카페라떼",
    "orderCount": 29
  },
  {
    "menuId": 4,
    "name": "바닐라라떼",
    "orderCount": 21
  }
]
```

최근 7일 동안 주문된 메뉴가 없다면 빈 배열을 반환한다.

```json
[]
```

### SQL 개념

```sql
SELECT
    menu_id,
    COUNT(*) AS order_count
FROM coffee_order
WHERE status = 'PAID'
  AND paid_at >= :sevenDaysAgo
  AND paid_at < :now
GROUP BY menu_id
ORDER BY order_count DESC, menu_id ASC
LIMIT 3;
```

메뉴명은 조회 결과의 `menu_id`를 기준으로 메뉴 정보를 조회하여 반환한다.

---

# 6. 외부 데이터 수집 플랫폼 전송

주문이 성공적으로 완료되면 외부 데이터 수집 플랫폼에 다음 정보를 전달한다.

### 전송 데이터

```json
{
  "eventId": "evt-10001",
  "userId": "user-1001",
  "menuId": 1,
  "paidAmount": 4500
}
```

### 처리 방식

주문 트랜잭션 안에서는 외부 API를 직접 호출하지 않는다.

대신 주문과 함께 `OUTBOX_EVENT`를 저장한다.

```text
주문 요청
  ↓
주문/포인트 변경
  ↓
POINT_HISTORY 저장
  ↓
OUTBOX_EVENT 저장
  ↓
COMMIT
  ↓
Outbox Worker
  ↓
외부 데이터 수집 플랫폼 HTTP 요청
```

외부 전송은 주문 트랜잭션과 분리되어 비동기적으로 처리된다.

따라서 외부 플랫폼에 장애가 발생하더라도 이미 완료된 주문 및 포인트 결제를 롤백하지 않는다.

전송 실패 시 재시도하고, 최대 재시도 횟수를 초과하면 `FAILED` 상태로 기록한다.

여러 서버 인스턴스가 동시에 Worker를 실행할 수 있으므로 이벤트 선점 과정에서 동시성 제어를 적용한다.

외부 플랫폼에서 중복 이벤트가 발생할 가능성을 고려하여 `eventId`를 멱등성 식별자로 사용할 수 있도록 한다.

---

# 7. 공통 Error Response

모든 API의 오류 응답은 다음 형식을 사용한다.

```json
{
  "code": "ERROR_CODE",
  "message": "오류 메시지"
}
```

## Error Code

| Code       | HTTP | Message                                 | 설명            |
| ---------- | ---: | --------------------------------------- | ------------- |
| COMMON_001 |  400 | 잘못된 요청입니다.                              | 공통 요청 오류      |
| COMMON_002 |  500 | 서버 내부 오류가 발생했습니다.                       | 처리되지 않은 서버 오류 |
| MENU_001   |  404 | 메뉴를 찾을 수 없습니다.                          | 존재하지 않는 메뉴    |
| POINT_001  |  400 | 충전 금액은 0보다 커야 합니다.                      | 잘못된 충전 금액     |
| POINT_002  |  404 | 포인트 계정을 찾을 수 없습니다.                      | 주문 시 계정 없음    |
| POINT_003  |  409 | 포인트 잔액이 부족합니다.                          | 결제 금액보다 잔액 부족 |
| POINT_004  |  409 | 동시성 문제로 요청을 처리하지 못했습니다.                 | 포인트 변경 동시성 문제 |
| ORDER_001  |  400 | 주문 요청 정보가 올바르지 않습니다.                    | 잘못된 주문 요청     |
| ORDER_002  |  409 | 동일한 Idempotency-Key로 다른 주문을 요청할 수 없습니다. | 중복 요청 키 충돌    |

---

# 8. 주요 테스트 시나리오

## 메뉴

* 메뉴 목록 정상 조회
* 메뉴가 없는 경우 빈 배열 반환

## 포인트 충전

* 정상 충전
* 0 또는 음수 충전 거부
* 최초 충전 시 계정 생성
* 기존 계정 충전
* 동시에 최초 충전 요청
* 동시에 여러 충전 요청

## 주문

* 정상 주문 및 결제
* 존재하지 않는 메뉴
* 존재하지 않는 포인트 계정
* 포인트 잔액 부족
* 동시에 여러 주문 요청
* 동일한 Idempotency-Key로 동일 주문 재요청
* 동일한 Idempotency-Key로 다른 주문 요청
* 주문 실패 시 포인트가 차감되지 않는지 확인
* 주문 실패 시 주문 이력이 생성되지 않는지 확인
* 주문 성공 시 Outbox 이벤트가 함께 생성되는지 확인

## 인기 메뉴

* 최근 7일 주문 집계
* 7일 이전 주문 제외
* `PAID` 주문만 집계
* 메뉴별 정확한 주문 횟수
* 주문 횟수 내림차순 정렬
* 동일한 주문 횟수일 때 `menuId` 오름차순 정렬
* 상위 3개만 반환
* 주문이 없는 경우 빈 배열 반환

## Outbox

* 주문 성공 시 Outbox 이벤트 생성
* 주문 롤백 시 Outbox 이벤트도 롤백
* 외부 API 전송 성공
* 외부 API 전송 실패 후 재시도
* 재시도 횟수 초과 시 FAILED 처리
* 여러 서버에서 동일 이벤트를 동시에 처리하지 않는지 확인
* 전송 성공 이벤트가 다시 전송되지 않는지 확인
