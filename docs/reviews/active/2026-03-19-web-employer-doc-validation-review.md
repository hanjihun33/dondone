# Web Employer Doc Validation Review

## Scope
- `docs/web/employer-web-direction.md`
- `docs/web/employer-worker-domain-map.md`
- `docs/web/auth-and-role-policy.md`
- `docs/web/shared-entity-validation.md`
- `docs/web/employer-web-api-map.md`
- `docs/web/workplace-settings-contract.md`
- `docs/web/correction-request-flow.md`
- `docs/web/implementation-slices.md`

## Findings
### Closed in doc update
- Employer auth 초대 계약에 `1회성`, `만료`, `폐기`, `companyId/role/email 바인딩`이 빠져 있었다.
- employer API 문서에 auth 표면이 빠져 있었고, read/write endpoint의 workplace/company scope 규칙이 충분히 강제되지 않았다.
- migration 공존 구간에서 레거시 `Workplace.user`, `WorkProof.user`를 employer 권한 판정에 쓰면 안 된다는 경계가 문서에 명시되지 않았다.

## Open Questions
- workplace switcher를 MVP에 넣지 않을 경우 `defaultWorkplaceId`를 어떤 엔티티가 canonical source로 가질지
- invitation token 저장 방식을 DB row로 둘지 signed token + server state 혼합으로 둘지

## Testing Gaps
- employer invitation token 재사용/만료/폐기 차단 테스트 계획이 아직 없다.
- employer endpoint의 cross-company, cross-workplace authorization 회귀 테스트 계획이 아직 없다.
- legacy owner 컬럼이 employer authorization source로 섞이지 않도록 막는 회귀 테스트 계획이 아직 없다.

## Residual Risks
- 다중 workplace 지원을 열면 현재 단일 기본 workplace 계약을 다시 손봐야 한다.
- worker/employer 겸용 계정을 열면 auth와 membership 정책을 재정의해야 한다.

## Result
- 현재 문서는 구현 시작 전 기준 문서로 사용할 수 있는 수준까지는 올라왔다.
- 다음 단계는 Slice 2 `Auth and profile foundation` 기준으로 구현 범위를 다시 자르는 것이다.
