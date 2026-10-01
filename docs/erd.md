# ERD 및 데이터 모델 설계

## 1. 설계 목표

커피 메뉴 조회, 포인트 충전, 포인트 결제, 인기 메뉴 집계,
외부 데이터 수집 플랫폼 이벤트 전송을 지원한다.

포인트 잔액과 변경 이력, 주문 정보, 외부 전송 이벤트를 분리하여
각 데이터의 목적을 명확하게 한다.

## 2. 테이블 구성

### 2.1 COFFEE_MENU

커피 메뉴 정보를 저장한다.

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGINT | PK, AUTO_INCREMENT | 메뉴 ID |
| name | VARCHAR(100) | NOT NULL | 메뉴명 |
| price | BIGINT | NOT NULL | 메뉴 가격(원) |

### 2.2 POINT_ACCOUNT

사용자별 포인트 계정과 현재 잔액을 저장한다.

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGINT | PK, AUTO_INCREMENT | 계정 ID |
| user_id | VARCHAR(100) | NOT NULL, UNIQUE | 사용자 식별자 |
| balance | BIGINT | NOT NULL | 현재 포인트 잔액 |
| created_at | DATETIME(6) | NOT NULL | 생성 시각 |
| updated_at | DATETIME(6) | NOT NULL | 수정 시각 |

최초 충전 시 계정을 생성하는 방식으로 설계한다.
user_id에 UNIQUE 제약조건을 두어 같은 사용자의 계정이
중복 생성되지 않도록 한다.

### 2.3 COFFEE_ORDER

주문 및 결제 정보를 저장한다.

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGINT | PK, AUTO_INCREMENT | 주문 ID |
| user_id | VARCHAR(100) | NOT NULL | 주문 사용자 ID |
| menu_id | BIGINT | FK, NOT NULL | 주문 메뉴 ID |
| price | BIGINT | NOT NULL | 주문 당시 가격(원) |
| status | VARCHAR(20) | NOT NULL | 주문 상태 |
| created_at | DATETIME(6) | NOT NULL | 주문 생성 시각 |
| paid_at | DATETIME(6) | NULL | 결제 완료 시각 |

주문 당시 가격을 별도로 저장하여 메뉴 가격이 변경되더라도
과거 주문의 결제 금액을 유지한다.

### 2.4 POINT_HISTORY

포인트 충전과 사용 내역을 저장한다.

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGINT | PK, AUTO_INCREMENT | 내역 ID |
| account_id | BIGINT | FK, NOT NULL | 포인트 계정 ID |
| order_id | BIGINT | FK, NULL | 관련 주문 ID |
| type | VARCHAR(20) | NOT NULL | CHARGE 또는 PAYMENT |
| amount | BIGINT | NOT NULL | 변경 금액 |
| balance_after | BIGINT | NOT NULL | 변경 후 잔액 |
| created_at | DATETIME(6) | NOT NULL | 내역 생성 시각 |

충전 내역에는 관련 주문이 없으므로 order_id는 NULL을 허용한다.
충전과 사용 내역은 포인트 잔액 변경과 같은 트랜잭션에서 저장한다.

### 2.5 OUTBOX_EVENT

외부 데이터 수집 플랫폼으로 전달할 이벤트를 저장한다.

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGINT | PK, AUTO_INCREMENT | 내부 ID |
| event_id | VARCHAR(100) | NOT NULL, UNIQUE | 이벤트 식별자 |
| event_type | VARCHAR(50) | NOT NULL | 이벤트 종류 |
| payload | JSON | NOT NULL | 전송할 데이터 |
| status | VARCHAR(20) | NOT NULL | 전송 상태 |
| attempt_count | INT | NOT NULL | 전송 시도 횟수 |
| created_at | DATETIME(6) | NOT NULL | 이벤트 생성 시각 |
| sent_at | DATETIME(6) | NULL | 전송 성공 시각 |

주문과 포인트 변경을 처리하는 트랜잭션에서 이벤트도 함께 저장한다.
외부 HTTP 요청은 해당 트랜잭션 안에서 직접 실행하지 않는다.
별도 처리기가 미전송 이벤트를 조회하여 전송하고 실패 시 재시도한다.

## 3. 테이블 관계

- COFFEE_MENU 1 : N COFFEE_ORDER
- POINT_ACCOUNT 1 : N POINT_HISTORY
- COFFEE_ORDER 1 : N POINT_HISTORY
  - 충전 내역은 주문과 연결되지 않을 수 있다.
- OUTBOX_EVENT는 외부 전송을 위한 이벤트 기록이다.

## 4. 데이터 정합성 및 동시성

- 포인트 잔액은 0 이상이어야 한다.
- 사용자별 포인트 계정은 하나만 존재해야 한다.
- 동시에 충전하거나 결제하는 상황에서 잔액이 잘못 변경되지 않도록
  데이터베이스 트랜잭션과 행 잠금을 검토한다.
- 주문 저장, 포인트 차감, 포인트 사용 내역 저장, 아웃박스 이벤트 저장은
  하나의 데이터베이스 트랜잭션으로 처리한다.
- 이벤트 전송은 재시도할 수 있으며 중복 전송 가능성도 고려한다.

## 5. 추가 검토 사항

- COFFEE_ORDER.user_id와 POINT_ACCOUNT.user_id의 관계를 검토한다.
- 동시에 최초 충전 요청이 들어올 때 계정 생성 경쟁을 처리한다.
- OUTBOX_EVENT를 여러 서버가 동시에 처리하지 않도록
  이벤트 선점 및 상태 변경 방식을 설계한다.
- 인기 메뉴 집계에 필요한 인덱스를 테스트 결과에 따라 결정한다.
- 실제 구현에 맞게 컬럼, 제약조건, 관계를 최종 확정한다.
