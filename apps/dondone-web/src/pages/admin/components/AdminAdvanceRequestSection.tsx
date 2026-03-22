import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  AdminAdvanceRequestItemResponse,
  approveAdminAdvanceRequest,
  getAdminAdvanceRequests,
  rejectAdminAdvanceRequest
} from "../../../shared/api/admin";
import { ApiError } from "../../../shared/api/client";
import { clearStoredSession, getStoredAccessToken } from "../../../shared/auth/session";

type RequestFilter = "ALL" | "SUBMITTED" | "APPROVED" | "REJECTED";

const REQUEST_FILTERS: Array<{
  value: RequestFilter;
  label: string;
}> = [
  { value: "ALL", label: "전체" },
  { value: "SUBMITTED", label: "승인 대기" },
  { value: "APPROVED", label: "승인 완료" },
  { value: "REJECTED", label: "반려" }
];

const STATUS_LABELS: Record<AdminAdvanceRequestItemResponse["status"], string> = {
  SUBMITTED: "승인 대기",
  APPROVED: "승인 완료",
  REJECTED: "반려",
  NEEDS_REVIEW: "검토 필요"
};

const STATUS_CLASS_NAMES: Record<
  AdminAdvanceRequestItemResponse["status"],
  "pending" | "active" | "rejected"
> = {
  SUBMITTED: "pending",
  APPROVED: "active",
  REJECTED: "rejected",
  NEEDS_REVIEW: "pending"
};

function formatKrw(amount: number | null) {
  if (amount == null) {
    return "-";
  }

  return `${new Intl.NumberFormat("ko-KR").format(amount)}원`;
}

function formatDate(value: string | null) {
  if (!value) {
    return "-";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit"
  }).format(date);
}

function formatDateTime(value: string | null) {
  if (!value) {
    return "-";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  }).format(date);
}

function formatWorkMinutes(minutes: number) {
  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;

  if (hours <= 0) {
    return `${remainingMinutes}분`;
  }

  if (remainingMinutes === 0) {
    return `${hours}시간`;
  }

  return `${hours}시간 ${remainingMinutes}분`;
}

export function AdminAdvanceRequestSection() {
  const navigate = useNavigate();
  const [requests, setRequests] = useState<AdminAdvanceRequestItemResponse[]>([]);
  const [loadState, setLoadState] = useState<"loading" | "success" | "error">("loading");
  const [filter, setFilter] = useState<RequestFilter>("ALL");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [feedbackMessage, setFeedbackMessage] = useState<string | null>(null);
  const [processingRequestId, setProcessingRequestId] = useState<number | null>(null);

  useEffect(() => {
    const token = getStoredAccessToken();
    if (!token) {
      clearStoredSession();
      navigate("/", { replace: true });
      return;
    }

    let disposed = false;

    void getAdminAdvanceRequests(token)
      .then((response) => {
        if (disposed) {
          return;
        }

        setRequests(response.requests);
        setLoadState("success");
        setErrorMessage(null);
      })
      .catch((error: unknown) => {
        if (disposed) {
          return;
        }

        if (error instanceof ApiError && error.status === 401) {
          clearStoredSession();
          navigate("/", { replace: true });
          return;
        }

        setLoadState("error");
        setErrorMessage(
          error instanceof Error ? error.message : "미리받기 요청 목록을 불러오지 못했습니다."
        );
      });

    return () => {
      disposed = true;
    };
  }, [navigate]);

  useEffect(() => {
    if (!feedbackMessage) {
      return undefined;
    }

    const timer = window.setTimeout(() => {
      setFeedbackMessage(null);
    }, 2400);

    return () => window.clearTimeout(timer);
  }, [feedbackMessage]);

  const requestCountByStatus = useMemo(() => {
    const counts: Record<RequestFilter, number> = {
      ALL: requests.length,
      SUBMITTED: 0,
      APPROVED: 0,
      REJECTED: 0
    };

    requests.forEach((request) => {
      if (request.status === "SUBMITTED" || request.status === "APPROVED" || request.status === "REJECTED") {
        counts[request.status] += 1;
      }
    });

    return counts;
  }, [requests]);

  const summary = useMemo(() => {
    return requests.reduce(
      (acc, request) => {
        acc.totalRequestedAmount += request.requestedAmount;

        if (request.status === "SUBMITTED") {
          acc.pendingRequestedAmount += request.requestedAmount;
        }

        if (request.status === "APPROVED") {
          acc.approvedAmount += request.approvedAmount ?? request.requestedAmount;
        }

        if (request.status === "REJECTED") {
          acc.rejectedCount += 1;
        }

        return acc;
      },
      {
        totalRequestedAmount: 0,
        pendingRequestedAmount: 0,
        approvedAmount: 0,
        rejectedCount: 0
      }
    );
  }, [requests]);

  const filteredRequests = useMemo(() => {
    return filter === "ALL" ? requests : requests.filter((request) => request.status === filter);
  }, [filter, requests]);

  const updateRequestStatus = (
    requestId: number,
    nextStatus: AdminAdvanceRequestItemResponse["status"]
  ) => {
    setRequests((prev) =>
      prev.map((request) =>
        request.requestId === requestId
          ? {
              ...request,
              status: nextStatus,
              approvedAmount: nextStatus === "APPROVED" ? request.requestedAmount : null,
              reviewedAt: new Date().toISOString()
            }
          : request
      )
    );
  };

  const handleAction = async (
    request: AdminAdvanceRequestItemResponse,
    action: "approve" | "reject"
  ) => {
    const token = getStoredAccessToken();
    if (!token) {
      clearStoredSession();
      navigate("/", { replace: true });
      return;
    }

    const actionLabel = action === "approve" ? "승인" : "반려";
    const confirmed = window.confirm(
      `${request.workerName} 근로자의 미리받기 요청을 ${actionLabel}할까요?`
    );
    if (!confirmed) {
      return;
    }

    setProcessingRequestId(request.requestId);
    setErrorMessage(null);

    try {
      if (action === "approve") {
        await approveAdminAdvanceRequest(token, request.requestId);
        updateRequestStatus(request.requestId, "APPROVED");
      } else {
        await rejectAdminAdvanceRequest(token, request.requestId);
        updateRequestStatus(request.requestId, "REJECTED");
      }

      setFeedbackMessage(`${request.workerName} 요청을 ${actionLabel}했습니다.`);
    } catch (error: unknown) {
      if (error instanceof ApiError && error.status === 401) {
        clearStoredSession();
        navigate("/", { replace: true });
        return;
      }

      setErrorMessage(error instanceof Error ? error.message : `요청 ${actionLabel}에 실패했습니다.`);
    } finally {
      setProcessingRequestId(null);
    }
  };

  return (
    <section className="admin-section">
      {feedbackMessage ? (
        <div className="admin-toast" role="status" aria-live="polite">
          {feedbackMessage}
        </div>
      ) : null}

      <div className="admin-section-head with-sub">
        <div>
          <h3>근로자 미리받기 요청</h3>
          <p className="admin-section-sub">
            앱에서 올라온 미리받기 요청을 현재 계약 범위 기준으로 검토하고 승인 또는 반려합니다.
          </p>
        </div>
      </div>

      <div className="admin-request-summary">
        <article className="admin-request-summary-card">
          <span>전체 요청</span>
          <strong>{requestCountByStatus.ALL}건</strong>
          <p>{formatKrw(summary.totalRequestedAmount)}</p>
        </article>
        <article className="admin-request-summary-card">
          <span>승인 대기</span>
          <strong>{requestCountByStatus.SUBMITTED}건</strong>
          <p>{formatKrw(summary.pendingRequestedAmount)}</p>
        </article>
        <article className="admin-request-summary-card">
          <span>승인 완료</span>
          <strong>{requestCountByStatus.APPROVED}건</strong>
          <p>{formatKrw(summary.approvedAmount)}</p>
        </article>
        <article className="admin-request-summary-card">
          <span>반려</span>
          <strong>{summary.rejectedCount}건</strong>
          <p>처리 이력 포함</p>
        </article>
      </div>

      <div className="admin-request-toolbar">
        <div className="admin-request-tabs">
          {REQUEST_FILTERS.map((option) => (
            <button
              key={option.value}
              type="button"
              className={`admin-request-tab ${filter === option.value ? "active" : ""}`}
              onClick={() => setFilter(option.value)}
            >
              {option.label}
              <span>{requestCountByStatus[option.value]}</span>
            </button>
          ))}
        </div>
        <p className="admin-request-count">현재 {filteredRequests.length}건</p>
      </div>

      {errorMessage ? (
        <div className="admin-empty-state" role="alert">
          {errorMessage}
        </div>
      ) : null}

      {loadState === "loading" ? (
        <div className="admin-empty-state">미리받기 요청 목록을 불러오는 중입니다.</div>
      ) : null}

      {loadState === "success" ? (
        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>근로자</th>
                <th>요청 금액</th>
                <th>근무 기준</th>
                <th>일정</th>
                <th>상태</th>
                <th>처리</th>
              </tr>
            </thead>
            <tbody>
              {filteredRequests.map((request) => (
                <tr key={request.requestId}>
                  <td>
                    <strong>{request.workerName}</strong>
                    <p className="admin-cell-sub">{request.workerEmail}</p>
                    <p className="admin-cell-sub">
                      {[request.companyName, request.workplaceName].filter(Boolean).join(" / ")}
                    </p>
                  </td>
                  <td>
                    <div className="admin-metric-stack">
                      <strong>{formatKrw(request.requestedAmount)}</strong>
                      <p className="admin-cell-sub">
                        승인 금액{" "}
                        {request.status === "SUBMITTED"
                          ? "대기"
                          : formatKrw(request.approvedAmount)}
                      </p>
                      <p className="admin-cell-sub">수수료 {formatKrw(request.feeAmount)}</p>
                    </div>
                  </td>
                  <td>
                    <div className="admin-metric-stack">
                      <strong>{request.reflectedWorkDays}일 반영</strong>
                      <p className="admin-cell-sub">
                        반영 근무 {formatWorkMinutes(request.reflectedWorkMinutes)}
                      </p>
                      <p className="admin-cell-sub">
                        검토 필요 기록 {request.needsReviewRecordCount}건
                      </p>
                    </div>
                  </td>
                  <td>
                    <div className="admin-metric-stack">
                      <strong>신청 {formatDateTime(request.requestedAt)}</strong>
                      <p className="admin-cell-sub">상환 예정 {formatDate(request.repaymentDueDate)}</p>
                      <p className="admin-cell-sub">처리 {formatDateTime(request.reviewedAt)}</p>
                    </div>
                  </td>
                  <td>
                    <span className={`admin-status ${STATUS_CLASS_NAMES[request.status]}`}>
                      {STATUS_LABELS[request.status]}
                    </span>
                  </td>
                  <td>
                    {request.status === "SUBMITTED" ? (
                      <div className="admin-action-group">
                        <button
                          type="button"
                          className="admin-action-button approve"
                          onClick={() => handleAction(request, "approve")}
                          disabled={processingRequestId === request.requestId}
                        >
                          승인
                        </button>
                        <button
                          type="button"
                          className="admin-action-button reject"
                          onClick={() => handleAction(request, "reject")}
                          disabled={processingRequestId === request.requestId}
                        >
                          반려
                        </button>
                      </div>
                    ) : (
                      <span className="admin-action-done">처리 완료</span>
                    )}
                  </td>
                </tr>
              ))}
              {filteredRequests.length === 0 ? (
                <tr>
                  <td colSpan={6} className="admin-empty-cell">
                    선택한 상태에 해당하는 미리받기 요청이 없습니다.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      ) : null}
    </section>
  );
}
