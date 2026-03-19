# Web Docs

이 디렉터리는 `dondone-web` 구현과 웹 전용 백엔드 연동을 진행할 때 자주 확인하는 기준 문서를 모아두는 공간이다. 실행계획이나 리뷰 이력보다 더 오래 유지할 설계 기준과 검증 규칙을 여기에 둔다.

## 문서 역할 구분
- `docs/web/`
  - 장기 기준 문서
  - 방향성, 도메인 규칙, API 매핑, 권한 원칙, 검증 체크리스트
- `docs/execplans/active/`
  - 현재 작업 실행 계획
  - 누가 무엇을 어떤 순서로 구현할지 관리
- `docs/reviews/active/`
  - 구현 중 검증 결과, 리스크, 후속 조치

## 중요한 원칙
- 이 문서는 인덱스다. 진행 상태판으로 쓰지 않는다.
- 현재 어디까지 진행했는지는 `implementation-slices.md`에서 본다.
- 현재 세션의 실제 작업 범위는 최신 `docs/execplans/active/` 문서에서 본다.
- 구현 중 발견한 리스크와 검증 결과는 `docs/reviews/active/`에 남긴다.

## 문서 목록
| 문서 | 역할 | 먼저 볼 시점 |
| --- | --- | --- |
| `employer-web-direction.md` | 웹 전용 API 분리 이유와 전체 방향 | 처음 |
| `employer-worker-domain-map.md` | 고용주-회사-사업장-근로자 관계와 소유권 규칙 | 엔티티/권한 설계 전 |
| `auth-and-role-policy.md` | 웹 로그인/회원가입과 역할 분리 원칙 | auth 설계 전 |
| `shared-entity-validation.md` | 공유 엔티티 변경 시 검증 게이트 | 구현 전/중간 |
| `employer-web-api-map.md` | 화면과 API를 1:1로 매핑 | endpoint 설계 전 |
| `workplace-settings-contract.md` | 사업장 설정과 출퇴근 판정 연결 규칙 | settings 구현 전 |
| `correction-request-flow.md` | 정정 요청 상태 전이와 반영 규칙 | correction 구현 전 |
| `implementation-slices.md` | 구현 순서와 slice별 완료 조건 | 실제 작업 시작 전 |

## 권장 읽기 순서
1. `employer-web-direction.md`
2. `employer-worker-domain-map.md`
3. `auth-and-role-policy.md`
4. `shared-entity-validation.md`
5. `employer-web-api-map.md`
6. `workplace-settings-contract.md`
7. `correction-request-flow.md`
8. `implementation-slices.md`

## 새 대화 시작 가이드
1. 이 문서를 읽고 필요한 기준 문서를 찾는다.
2. `implementation-slices.md`에서 현재 `in_progress` 또는 첫 `not_started` slice를 확인한다.
3. 최신 `docs/execplans/active/` 문서에서 현재 세션 범위를 확인한다.
4. 최근 검증 이슈가 있으면 `docs/reviews/active/`의 최신 review note를 본다.

## 작업 단계별 추천 참조
### 계획 고정 전
- `employer-web-direction.md`
- `employer-worker-domain-map.md`
- `implementation-slices.md`

### auth/권한 작업 전
- `auth-and-role-policy.md`
- `shared-entity-validation.md`

### dashboard/workers/settings 작업 전
- `employer-web-api-map.md`
- `workplace-settings-contract.md`

### correction request 작업 전
- `correction-request-flow.md`
- `shared-entity-validation.md`

## 사용 원칙
- 웹 전용 API는 기존 앱 API와 분리해서 본다.
- 데이터 소유권과 권한은 근로자/고용주 공유 도메인 관점에서 함께 검증한다.
- DB/엔티티 영향이 있는 변경은 `shared-entity-validation.md`를 먼저 갱신한 뒤 진행한다.
- 실제 작업 순서와 담당 범위는 `docs/execplans/active/`에서 관리한다.
- 구현 중 발견한 리스크와 검증 결과는 `docs/reviews/active/`에 남긴다.
- 웹 구현 중 기존 앱 API 변경이 필요해지면 바로 범위를 다시 검토한다.

## 갱신 규칙
- 새로운 웹 화면이 추가되면 `employer-web-api-map.md`를 먼저 갱신한다.
- 새 엔티티 또는 소속 관계가 추가되면 `employer-worker-domain-map.md`와 `shared-entity-validation.md`를 같이 갱신한다.
- 권한 규칙이나 가입 흐름이 바뀌면 `auth-and-role-policy.md`를 갱신한다.
- 상태 전이 규칙이 바뀌면 `correction-request-flow.md` 또는 `workplace-settings-contract.md`를 갱신한다.
