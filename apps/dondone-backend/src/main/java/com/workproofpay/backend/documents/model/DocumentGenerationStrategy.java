package com.workproofpay.backend.documents.model;

/**
 * 문서 타입별로 생성 요청 이후 어떤 실행 방식을 따르는지 구분한다.
 * ON_DEMAND_DOWNLOAD 는 요청 메타만 저장하고 실제 렌더링은 다운로드 시점에 수행한다.
 * REQUEST_STATUS_WORKFLOW 는 요청 상태 추적과 후속 worker/패키지 확장을 전제로 둔다.
 */
public enum DocumentGenerationStrategy {
    ON_DEMAND_DOWNLOAD,
    REQUEST_STATUS_WORKFLOW
}
