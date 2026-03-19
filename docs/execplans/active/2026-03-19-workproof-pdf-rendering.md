# Source Inputs
- 작업 가이드:
  - `AGENTS.md`
  - `apps/dondone-backend/AGENTS.md`
- PRD/계약:
  - `docs/DonDone_PRD_v1.5.md` 3.1, 4, 5, 11.4 PDF, 13 주차 계획
  - `docs/DonDone_P0_API_Contract_v0.md` 5.1~5.5 documents 계약
  - `docs/DonDone_P0_Functional_Spec_v0.md` documents 관련 흐름
- 기존 구현:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/documents/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/**`
  - `apps/dondone-backend/src/test/java/com/workproofpay/backend/documents/**`
- 현재 결정:
  - PDF 1차 구현은 `WorkProof` 기반 출퇴근 기록 정리 PDF부터 시작한다
  - 거래 내역 PDF, object storage, presigned URL, 비동기 worker 분리는 후속으로 미룬다

# Goal
- `WorkProof` 기반 출퇴근 기록 정리 PDF를 실제로 렌더링할 수 있는 최소 파이프라인을 설계하고 구현한다.
- 엔티티 직접 렌더링이 아니라 `문서 전용 snapshot -> HTML template -> PDF bytes` 흐름을 기준선으로 고정한다.

# In Scope
- `WorkProof PDF` v1 문서 유형 정의
- `WorkProofPdfSnapshot` 문서 전용 DTO 정의
- `WorkProofPdfSnapshotAssembler` 계약과 구현
- HTML/CSS 기반 PDF 템플릿 추가
- PDF 렌더러 인터페이스와 1차 어댑터 구현
- 문서 요청 anchor에서 `WorkProof` PDF 생성 흐름 연결
- snapshot 조립 및 PDF 렌더링 테스트 보강
- 실행/확장 기준 문서화

# Out of Scope
- 거래 내역 PDF (`TRANSACTION_STATEMENT`) 구현
- Claim Kit 실렌더링
- MinIO/S3 업로드
- presigned download URL 발급
- 비동기 jobs / worker 분리
- 모바일 다운로드/공유 연결
- 첨부 이미지 원본 본문 삽입
- 다국어 템플릿
- QR 검증 링크

# Affected Modules
## Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/documents/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/**`
- 필요 시 `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/**`

## Docs
- `docs/execplans/active/2026-03-19-workproof-pdf-rendering.md`
- 후속 필요 시 `docs/DonDone_P0_API_Contract_v0.md`

## Shared
- PDF 생성용 템플릿/렌더러 계약
- 문서 요청에서 PDF snapshot 조립까지의 책임 분리 규칙

# Contract Changes
- 외부 API 계약은 1차 단계에서 크게 변경하지 않는다
- 내부적으로는 `DocumentGenerationRequest`를 요청 메타/anchor로 유지하고, PDF 본문 데이터는 별도 snapshot DTO로 분리한다
- 문서 생성 파이프라인은 다음 내부 계약을 가진다
  - `WorkProofPdfAssembleCommand`
  - `WorkProofPdfSnapshot`
  - `WorkProofPdfSnapshotAssembler`
  - `PdfRenderer`
  - `RenderedPdf`

# Security Notes
- 문서 snapshot 조립 시 user ownership 검증을 유지한다
- 타인 문서 요청/타인 근무기록은 `404`로 숨긴다
- PDF에는 민감 원본 첨부 파일 내용 대신 메타/개수만 넣는다
- 로컬 파일 저장 또는 임시 파일 사용 시 운영 경로와 섞지 않는다

# Maintainability Notes
- 템플릿은 표시만 담당하고 계산/정렬/라벨 생성은 assembler에서 끝낸다
- 엔티티를 템플릿에 직접 넘기지 않고 snapshot DTO만 전달한다
- `DocumentGenerationRequest`는 request/status placeholder로 유지하고 문서 본문 source-of-truth로 재사용하지 않는다
- PDF 렌더러는 인터페이스 뒤에 두고 구현체 교체 가능하게 유지한다
- HTML/CSS 템플릿은 `documents/pdf/workproof`처럼 문서 유형별 디렉토리로 분리한다

# Implementation Steps
1. `WorkProof PDF` v1 범위와 snapshot 최소 필드를 고정한다
2. `WorkProofPdfSnapshot`, `WorkProofPdfAssembleCommand`, `PdfRenderer` 계약을 추가한다
3. `WorkProof`, `WorkProofAuditLog`, `Workplace`, `WorkContract`를 조립하는 assembler를 구현한다
4. HTML/CSS 템플릿과 렌더러 어댑터를 추가한다
5. 문서 서비스에서 `request -> snapshot -> pdf bytes` 흐름을 연결한다
6. snapshot 조립/렌더링 테스트를 추가한다
7. 로컬 실행 방법과 후속 확장 범위를 문서화한다

# Test Plan
- `./gradlew.bat compileJava`
- assembler 단위 테스트 또는 slice 테스트
- PDF 렌더링 smoke test
- 필요 시 생성된 PDF를 로컬 파일로 떨어뜨려 시각 확인
- 템플릿 회귀 확인 시 최소 1건의 representative snapshot 기준 렌더링 검증

# Review Focus
- 엔티티 직접 렌더링이 아니라 snapshot 기반 구조로 분리되었는가
- assembler가 조회/조립 책임만 가지고 렌더링/저장과 섞이지 않았는가
- 템플릿이 계산 로직 없이 표시용 값만 사용하는가
- `WorkProof` 요약, 기록 표, 수정 이력 표가 v1 범위에 맞게 최소/충분한가
- 추후 거래 내역 PDF나 storage 연동으로 확장 가능한 경계가 잡혔는가

# Worktree Split Decision
- Single lane

`WorkProof PDF`는 snapshot 모델, assembler, 템플릿, 렌더러, 서비스 연결이 같은 책임 경계 안에서 움직인다. 지금 단계에서는 contract와 구현 경계가 동시에 고정되어야 해서 병렬 분리가 안전하지 않다.

# Commit Plan
- `docs: workproof pdf rendering execution plan 추가`
- `feat: workproof pdf snapshot 모델과 렌더링 계약 추가`
- `feat: workproof pdf snapshot assembler 구현`
- `feat: workproof pdf html 템플릿과 렌더러 어댑터 추가`
- `feat: workproof pdf 생성 유스케이스 연결`
- `test: workproof pdf snapshot 및 렌더링 검증 추가`
- `docs: workproof pdf 렌더링 설정과 후속 작업 정리`

# Open Questions
- PDF 렌더링 엔진을 백엔드 내부 구현으로 시작할지, 별도 `pdf-service` 인터페이스를 먼저 둘지
- v1에서 계약 정보 노출 범위를 어디까지 둘지
- `WorkProofAuditLog`를 전부 포함할지, 기간 내 수정 이력만 보여줄지

# Assumptions
- v1 문서는 A4 세로, 한국어 텍스트, 표 중심 레이아웃을 사용한다
- 출력 문서는 `WorkProof Statement` 1종만 우선 구현한다
- 첨부 원본 파일은 본문 삽입 대신 개수/메타 수준으로만 반영한다
- `DocumentGenerationRequest`는 여전히 문서 요청 anchor와 상태 추적 역할을 유지한다
