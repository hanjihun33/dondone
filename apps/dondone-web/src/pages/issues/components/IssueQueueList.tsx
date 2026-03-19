import { Badge } from "../../../shared/ui/Badge";
import type {
  CorrectionRequestItem,
  CorrectionRequestStatus
} from "../model/issuesQueueData";

type IssueQueueListProps = {
  requests: CorrectionRequestItem[];
  onResolve: (requestId: string, nextStatus: CorrectionRequestStatus) => void;
};

function getBadgeTone(status: CorrectionRequestStatus) {
  if (status === "approved") {
    return "success" as const;
  }

  if (status === "rejected") {
    return "danger" as const;
  }

  return "warn" as const;
}

function getBadgeText(status: CorrectionRequestStatus) {
  if (status === "approved") {
    return "수락";
  }

  if (status === "rejected") {
    return "거절";
  }

  return "대기";
}

function getResolutionText(status: CorrectionRequestStatus) {
  if (status === "approved") {
    return "수락 완료";
  }

  if (status === "rejected") {
    return "거절 완료";
  }

  return "";
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
    return { text: "변경 계산 불가", tone: "same" as const };
  }

  const diff = requestedMinutes - originalMinutes;
  if (diff === 0) {
    return { text: "변경 없음", tone: "same" as const };
  }

  return {
    text: `변경 ${diff > 0 ? `+${diff}` : diff}분`,
    tone: diff > 0 ? ("up" as const) : ("down" as const)
  };
}

export function IssueQueueList({ requests, onResolve }: IssueQueueListProps) {
  if (requests.length === 0) {
    return (
      <div className="issue-empty-state">
        <p>조건에 맞는 정정 요청이 없습니다.</p>
      </div>
    );
  }

  return (
    <div className="issue-request-list">
      {requests.map((request) => (
        <article className="issue-request-item" key={request.id}>
          {(() => {
            const checkInDiff = getTimeDiffInfo(
              request.originalCheckIn,
              request.requestedCheckIn
            );
            const checkOutDiff = getTimeDiffInfo(
              request.originalCheckOut,
              request.requestedCheckOut
            );

            return (
              <>
          <header className="issue-request-head">
            <div>
              <strong>{request.workerName}</strong>
              <p>
                {request.role} · 근무일 {request.workDate}
              </p>
            </div>
            <Badge tone={getBadgeTone(request.status)}>
              {getBadgeText(request.status)}
            </Badge>
          </header>

          <div className="issue-time-grid">
            <div className="issue-time-group">
              <span className="issue-time-group-label">출근</span>
              <div className="issue-time-row">
                <span>기존</span>
                <strong>{request.originalCheckIn}</strong>
              </div>
              <div className="issue-time-row">
                <span>요청</span>
                <strong>{request.requestedCheckIn}</strong>
              </div>
              <p className={`issue-time-diff ${checkInDiff.tone}`}>
                {checkInDiff.text}
              </p>
            </div>

            <div className="issue-time-group">
              <span className="issue-time-group-label">퇴근</span>
              <div className="issue-time-row">
                <span>기존</span>
                <strong>{request.originalCheckOut}</strong>
              </div>
              <div className="issue-time-row">
                <span>요청</span>
                <strong>{request.requestedCheckOut}</strong>
              </div>
              <p className={`issue-time-diff ${checkOutDiff.tone}`}>
                {checkOutDiff.text}
              </p>
            </div>
          </div>

          <p className="issue-request-reason">{request.reason}</p>

          <footer className="issue-request-foot">
            <span className="issue-request-meta">
              요청 시각 {request.requestedAt}
            </span>

            {request.status === "pending" ? (
              <div className="issue-action-group">
                <button
                  type="button"
                  className="issue-action-button reject"
                  onClick={() => onResolve(request.id, "rejected")}
                >
                  거절
                </button>
                <button
                  type="button"
                  className="issue-action-button approve"
                  onClick={() => onResolve(request.id, "approved")}
                >
                  수락
                </button>
              </div>
            ) : (
              <span className={`issue-resolution ${request.status}`}>
                {getResolutionText(request.status)}
              </span>
            )}
          </footer>
              </>
            );
          })()}
        </article>
      ))}
    </div>
  );
}
