# exec 디렉터리 안내

이 디렉터리는 DonDone 제출/시연용 문서를 모아두는 공간입니다.

## 파일 구성

- `porting-manual.md`
  - 제출용 포팅 매뉴얼 (런타임/빌드/배포/DB 항목 포함)

- `deployment-operations-guide.md.md`
  - 운영/배포 런북 성격 문서

- `external-services-onboarding.md`
  - 외부 서비스 가입/키 주입/검증 포인트 정리

- `demo-scenario-script.md`
  - 발표용 시연 시나리오 대본

- `Image/`
  - 시연 문서에서 참조하는 스크린샷/이미지 파일

- `db-dumps/`
  - 데이터베이스 덤프 파일

## 작성/관리 원칙

1. 민감정보(실제 비밀번호, API Secret, Private Key)는 문서/덤프에 포함하지 않습니다.
2. 시연 문서 이미지 파일명은 문서 내 참조명과 동일하게 유지합니다.
3. DB 덤프는 생성 일시가 드러나는 파일명(`YYYYMMDD_HHMMSS`)을 권장합니다.
4. 파일명 변경 시 문서 내부 링크와 이미지 참조를 함께 수정합니다.
