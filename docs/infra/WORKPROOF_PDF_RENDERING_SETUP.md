# WorkProof PDF Rendering Setup

## 목적
- 현재 구현된 `WorkProof PDF` 렌더링의 로컬 실행 기준을 정리한다.
- 출퇴근 기록 정리 PDF의 현재 책임 범위와 후속 작업 범위를 구분한다.

## 현재 구현 범위
- 문서 요청 anchor
  - `DocumentGenerationRequest`
  - `DocumentType.PROOF_PACK`
- snapshot 조립
  - `WorkProofPdfSnapshot`
  - `WorkProofPdfSnapshotAssembler`
  - `DefaultWorkProofPdfSnapshotAssembler`
- 렌더링
  - `PdfRenderer`
  - `ThymeleafOpenHtmlPdfRenderer`
  - `templates/pdf/workproof-statement.html`
- 유스케이스 연결
  - `DocumentsService.generateProofPackPdf`
  - `GET /api/documents/{documentId}/download`

## 로컬 실행 흐름
1. 근무지, 계약, 출퇴근 기록, 급여 확인 결과를 먼저 만든다.
2. `POST /api/documents/proof-packs`로 proof pack 요청을 생성한다.
3. 생성된 `documentId`로 `GET /api/documents/{documentId}/download`를 호출한다.
4. 서버는 내부에서 아래 순서로 PDF를 생성한다.
   - document request 조회
   - `WorkProofPdfSnapshot` 조립
   - Thymeleaf HTML 렌더링
   - OpenHTMLtoPDF 변환
   - PDF 바이트 응답

## 관련 의존성
- `org.springframework.boot:spring-boot-starter-thymeleaf`
- `com.openhtmltopdf:openhtmltopdf-pdfbox:1.0.10`

## 현재 제약
- generated PDF는 object storage에 저장하지 않는다.
- presigned URL을 발급하지 않는다.
- 다운로드는 요청 시점에 on-demand로 다시 생성한다.
- 첨부 파일 원본은 PDF 본문에 포함하지 않고 개수/메타 수준만 반영한다.
- locale, timezone은 현재 `Locale.KOREA`, `Asia/Seoul` 고정 기본값을 사용한다.
- `PROOF_PACK`만 실제 렌더링을 제공한다.

## 로컬 검증 명령
- 컴파일
  - `./gradlew.bat compileJava --console=plain`
- unit 테스트
  - `./gradlew.bat test --tests com.workproofpay.backend.documents.DocumentsServiceTest --tests com.workproofpay.backend.documents.pdf.ThymeleafOpenHtmlPdfRendererTest --console=plain`
- integration 테스트
  - `./gradlew.bat integrationTest --tests com.workproofpay.backend.documents.pdf.workproof.WorkProofPdfSnapshotAssemblerIntegrationTest --console=plain`

## 테스트 해석 기준
- `DocumentsServiceTest`
  - proof pack PDF 생성 성공 시 `READY`
  - 렌더링 실패 시 `FAILED`
- `ThymeleafOpenHtmlPdfRendererTest`
  - 템플릿이 실제 PDF 바이트를 만든다
  - `contentType`, `fileName`, `sha256`가 비정상 값이 아니다
- `WorkProofPdfSnapshotAssemblerIntegrationTest`
  - 실제 Postgres fixture 기준으로 요약/상태/notice/audit 조립이 맞다

## 운영 전환 전 해야 할 일
- 생성된 PDF 저장 전략 결정
  - DB BLOB 저장 비권장
  - object storage key 저장 권장
- download endpoint를 on-demand 재생성에서 저장 기반 다운로드로 전환
- 비동기 worker 또는 job 분리
- HTML 템플릿 시각 검수
  - 페이지 나눔
  - 한글 글꼴
  - 표 너비
  - 긴 memo/edit reason 줄바꿈

## 후속 권장 순서
1. PDF 시각 품질 점검 및 템플릿 보정
2. object storage 업로드 연결
3. download URL/메타데이터 영속화
4. 비동기 생성 worker 분리
5. 거래 내역 PDF 추가

## 후속 범위에서 일부러 분리한 항목
- `TRANSACTION_STATEMENT` PDF
- Claim Kit 실제 렌더링
- 첨부 이미지 본문 삽입
- QR 검증 링크
- 다국어 템플릿
