import { Badge } from "../../../shared/ui/Badge";
import type { IssueQueueItem } from "../model/issuesQueueData";

type IssueQueueListProps = {
  requests: IssueQueueItem[];
  onApprove: (requestId: number) => void;
  onReject: (requestId: number) => void;
  onConfirmReview: (workProofId: number) => void;
  onToggleDetail: (item: IssueQueueItem) => void;
  submittingWorkProofId: number | null;
};

const attachmentTypeLabels: Record<string, string> = {
  PHOTO: "사진",
  MEMO: "메모",
  DOCUMENT: "문서",
  OTHER: "기타"
};

const recordStatusLabels: Record<string, string> = {
  CHECKED_IN: "출근",
  CHECKED_OUT: "퇴근 완료",
  NEEDS_REVIEW: "검토 필요"
};

const reflectionStatusLabels: Record<string, string> = {
  PENDING: "반영 대기",
  REFLECTED: "반영 완료",
  NEEDS_REVIEW: "검토 필요",
  EXCLUDED: "제외"
};

const reviewReasonLabels: Record<string, string> = {
  CLOCK_OUT_OUTSIDE_ALLOWED_RADIUS: "반경 밖 퇴근",
  NEEDS_REVIEW: "검토 필요 기록"
};

function getBadgeTone(item: IssueQueueItem) {
  if (item.correctionStatus === "APPROVED") {
    return "success" as const;
  }

  if (item.correctionStatus === "REJECTED") {
    return "danger" as const;
  }

  if (item.issueStatus === "NEEDS_REVIEW") {
    return "warn" as const;
  }

  return "warn" as const;
}

function getBadgeText(item: IssueQueueItem) {
  if (item.correctionStatus === "APPROVED") {
    return "승인";
  }

  if (item.correctionStatus === "REJECTED") {
    return "반려";
  }

  if (item.issueStatus === "NEEDS_REVIEW") {
    return "기록 검토";
  }

  return "대기";
}

function getIssueTypeLabel(item: IssueQueueItem) {
  return item.itemType === "CORRECTION_REQUEST" ? "정정 요청" : "검토 필요 기록";
}

function parseTimeToMinutes(value: string) {
  const [hour, minute] = value.split(":").map((unit) => Number(unit));
  if (
    Number.isNaN(hour) ||
    Number.isNaN(minute) ||
    hour < 0 ||
    hour > 23 ||
    minute < 0 ||
    minute > 59
  ) {
    return null;
  }

  return hour * 60 + minute;
}

function getTimeDiffInfo(original: string, requested: string) {
  const originalMinutes = parseTimeToMinutes(original);
  const requestedMinutes = parseTimeToMinutes(requested);

  if (originalMinutes === null || requestedMinutes === null) {
    return { text: "차이 없음", tone: "same" as const };
  }

  const diff = requestedMinutes - originalMinutes;
  if (diff === 0) {
    return { text: "차이 없음", tone: "same" as const };
  }

  return {
    text: `변경 ${diff > 0 ? `+${diff}` : diff}분`,
    tone: diff > 0 ? ("up" as const) : ("down" as const)
  };
}

function getAttachmentTypeLabel(type: string) {
  return attachmentTypeLabels[type] ?? type;
}

function getRecordStatusLabel(status: string) {
  return recordStatusLabels[status] ?? status;
}

function getReflectionStatusLabel(status: string) {
  return reflectionStatusLabels[status] ?? status;
}

function getReviewReasonLabel(reviewReason: string | null, reviewReasonCode: string) {
  return reviewReason ?? reviewReasonLabels[reviewReasonCode] ?? reviewReasonCode;
}

function renderDetail(item: IssueQueueItem) {
  if (item.detailState === "loading") {
    return <div className="issue-detail-card">상세 정보를 불러오는 중입니다...</div>;
  }

  if (item.detailState === "error") {
    return <div className="issue-detail-card">{item.detailError ?? "상세 정보를 불러오지 못했습니다."}</div>;
  }

  if (item.detailState !== "loaded" || !item.detail) {
    return null;
  }

  if (item.detail.kind === "correction") {
    return (
      <div className="issue-detail-card">
        <p>요청 메모: {item.detail.requestMemo ?? "없음"}</p>
        <p>
          처리 결과:{" "}
          {item.detail.decisionAt
            ? `${item.detail.decisionByName ?? "처리자 미상"} / ${item.detail.decisionAt}`
            : "아직 처리되지 않았습니다."}
        </p>
        {item.detail.decisionMemo ? <p>처리 메모: {item.detail.decisionMemo}</p> : null}
        {item.detail.rejectReasonCode ? <p>반려 코드: {item.detail.rejectReasonCode}</p> : null}
        <div className="issue-detail-list">
          {item.detail.attachments.length > 0 ? (
            item.detail.attachments.map((attachment) => (
              <div key={`${attachment.type}-${attachment.fileName}`} className="issue-detail-row">
                <span>
                  {getAttachmentTypeLabel(attachment.type)} / {attachment.fileName}
                </span>
                <span>{attachment.downloadAvailable ? "다운로드 가능" : "메타데이터만 표시"}</span>
              </div>
            ))
          ) : (
            <div className="issue-detail-row">
              <span>첨부 증빙이 없습니다.</span>
            </div>
          )}
        </div>
      </div>
    );
  }

  return (
    <div className="issue-detail-card">
      <p>검토 사유: {getReviewReasonLabel(item.detail.reviewReason, item.detail.reviewReasonCode)}</p>
      <p>
        기록 상태: {getRecordStatusLabel(item.detail.recordStatus)} / 반영 상태:{" "}
        {getReflectionStatusLabel(item.detail.reflectionStatus)}
      </p>
      <p>근무 시간: {item.detail.workedMinutes}분</p>
      {item.detail.memo ? <p>메모: {item.detail.memo}</p> : null}
      {item.detail.editReason ? <p>수정 사유: {item.detail.editReason}</p> : null}
      {item.detail.clockOutOutsideAllowedRadius ? (
        <p>사업장 반경 밖에서 퇴근해 사업주 확인이 필요합니다.</p>
      ) : null}
      <p>첨부 증빙: {item.detail.attachmentCount}개</p>
      {item.detail.workplaceName ? (
        <p>
          사업장: {item.detail.workplaceName}
          {item.detail.workplaceAddress ? ` / ${item.detail.workplaceAddress}` : ""}
        </p>
      ) : null}
      {item.detail.checkInLabel ? <p>출근 위치: {item.detail.checkInLabel}</p> : null}
      {item.detail.checkOutLabel ? <p>퇴근 위치: {item.detail.checkOutLabel}</p> : null}
    </div>
  );
}

export function IssueQueueList({
  requests,
  onApprove,
  onReject,
  onConfirmReview,
  onToggleDetail,
  submittingWorkProofId
}: IssueQueueListProps) {
  if (requests.length === 0) {
    return (
      <div className="issue-empty-state">
        <p>검색 또는 필터 조건에 맞는 요청이 없습니다.</p>
      </div>
    );
  }

  return (
    <div className="issue-request-list">
      {requests.map((request) => {
        const checkInDiff = getTimeDiffInfo(request.originalCheckIn, request.requestedCheckIn);
        const checkOutDiff = getTimeDiffInfo(request.originalCheckOut, request.requestedCheckOut);
        const isPendingCorrection = request.itemType === "CORRECTION_REQUEST" && request.correctionStatus === "PENDING";
        const isPendingReview =
          request.itemType === "REVIEW_REQUIRED_RECORD" && request.issueStatus === "NEEDS_REVIEW";
        const isSubmittingReview = submittingWorkProofId === request.workProofId;

        return (
          <article className="issue-request-item" key={request.key}>
            <header className="issue-request-head">
              <div>
                <strong>{request.workerName}</strong>
                <p>{getIssueTypeLabel(request)}</p>
                <p>
                  {request.role} / {request.workDate}
                </p>
              </div>
              <Badge tone={getBadgeTone(request)}>{getBadgeText(request)}</Badge>
            </header>

            <div className="issue-time-grid">
              <div className="issue-time-group">
                <span className="issue-time-group-label">출근</span>
                <div className="issue-time-row">
                  <span>원본</span>
                  <strong>{request.originalCheckIn}</strong>
                </div>
                <div className="issue-time-row">
                  <span>요청</span>
                  <strong>{request.requestedCheckIn}</strong>
                </div>
                <p className={`issue-time-diff ${checkInDiff.tone}`}>{checkInDiff.text}</p>
              </div>

              <div className="issue-time-group">
                <span className="issue-time-group-label">퇴근</span>
                <div className="issue-time-row">
                  <span>원본</span>
                  <strong>{request.originalCheckOut}</strong>
                </div>
                <div className="issue-time-row">
                  <span>요청</span>
                  <strong>{request.requestedCheckOut}</strong>
                </div>
                <p className={`issue-time-diff ${checkOutDiff.tone}`}>{checkOutDiff.text}</p>
              </div>
            </div>

            <p className="issue-request-reason">{request.reason}</p>

            <footer className="issue-request-foot">
              <span className="issue-request-meta">
                {request.itemType === "CORRECTION_REQUEST" ? "요청 시각" : "발생 시각"} {request.requestedAt}
              </span>

              <div className="issue-action-group">
                <button
                  type="button"
                  className="issue-action-button"
                  onClick={() => onToggleDetail(request)}
                >
                  {request.detailState === "loaded" ? "상세 닫기" : "상세 보기"}
                </button>
                {isPendingCorrection ? (
                  <>
                    <button
                      type="button"
                      className="issue-action-button reject"
                      onClick={() => request.requestId && onReject(request.requestId)}
                    >
                      반려
                    </button>
                    <button
                      type="button"
                      className="issue-action-button approve"
                      onClick={() => request.requestId && onApprove(request.requestId)}
                    >
                      승인
                    </button>
                  </>
                ) : null}
                {isPendingReview ? (
                  <button
                    type="button"
                    className="issue-action-button approve"
                    onClick={() => onConfirmReview(request.workProofId)}
                    disabled={isSubmittingReview}
                  >
                    {isSubmittingReview ? "처리 중" : "검토 완료"}
                  </button>
                ) : null}
              </div>
            </footer>

            {renderDetail(request)}
          </article>
        );
      })}
    </div>
  );
}
