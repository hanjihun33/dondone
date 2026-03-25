import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import {
  AdminAdvanceRequestItemResponse,
  approveAdminAdvanceRequest,
  getAdminAdvanceRequests,
  rejectAdminAdvanceRequest
} from "../../../shared/api/admin";
import { ApiError } from "../../../shared/api/client";
import { clearStoredSession, getStoredAccessToken } from "../../../shared/auth/session";

type RequestFilter = "ALL" | "SUBMITTED" | "APPROVED" | "PAYING" | "PAID" | "PAYOUT_FAILED" | "REJECTED";

const ALL_COMPANIES_FILTER = "__ALL_COMPANIES__";
const UNKNOWN_COMPANY_FILTER = "__UNKNOWN_COMPANY__";

const REQUEST_FILTERS: Array<{
  value: RequestFilter;
  label: string;
}> = [
  { value: "ALL", label: "전체" },
  { value: "SUBMITTED", label: "승인 대기" },
  { value: "APPROVED", label: "승인됨" },
  { value: "PAYING", label: "지급중" },
  { value: "PAID", label: "지급완료" },
  { value: "PAYOUT_FAILED", label: "지급실패" },
  { value: "REJECTED", label: "반려" }
];

const STATUS_LABELS: Record<AdminAdvanceRequestItemResponse["status"], string> = {
  SUBMITTED: "승인 대기",
  APPROVED: "승인됨",
  PAYING: "지급중",
  PAID: "지급완료",
  PAYOUT_FAILED: "지급실패",
  REJECTED: "반려",
  NEEDS_REVIEW: "검토 필요"
};

const STATUS_CLASS_NAMES: Record<
  AdminAdvanceRequestItemResponse["status"],
  "pending" | "active" | "rejected"
> = {
  SUBMITTED: "pending",
  APPROVED: "active",
  PAYING: "active",
  PAID: "active",
  PAYOUT_FAILED: "rejected",
  REJECTED: "rejected",
  NEEDS_REVIEW: "pending"
};

function formatAssetAmount(
  amountAtomic: number | null,
  assetSymbol: string,
  assetDecimals: number
) {
  if (amountAtomic == null) {
    return "-";
  }

  const divisor = 10 ** assetDecimals;
  const amount = amountAtomic / divisor;
  const minimumFractionDigits = assetDecimals > 2 ? 2 : assetDecimals;

  return `${new Intl.NumberFormat("ko-KR", {
    minimumFractionDigits,
    maximumFractionDigits: assetDecimals
  }).format(amount)} ${assetSymbol}`;
}

function formatReferenceKrw(amount: number | null) {
  if (amount == null) {
    return "-";
  }

  return `약 ${new Intl.NumberFormat("ko-KR").format(amount)}원`;
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

function getStatusHint(request: AdminAdvanceRequestItemResponse) {
  switch (request.status) {
    case "SUBMITTED":
      return "관리자 검토 대기";
    case "APPROVED":
      return "지급 요청 생성 전";
    case "PAYING":
      return request.payoutStatus ? `지급 상태 ${request.payoutStatus}` : "지급 처리중";
    case "PAID":
      return request.payoutTxHash ? `tx ${request.payoutTxHash}` : "체인 반영 완료";
    case "PAYOUT_FAILED":
      return request.payoutFailureReason ?? "지급 처리 실패";
    case "REJECTED":
      return "관리자 반려";
    case "NEEDS_REVIEW":
      return "추가 검토 필요";
    default:
      return "-";
  }
}

function getStatusDetail(request: AdminAdvanceRequestItemResponse) {
  return getStatusHint(request);
}

function canApproveOrReject(status: AdminAdvanceRequestItemResponse["status"]) {
  return status === "SUBMITTED";
}

function getCompanyFilterValue(companyName: string | null) {
  return companyName?.trim() ? companyName : UNKNOWN_COMPANY_FILTER;
}

function getCompanyFilterLabel(companyName: string | null) {
  return companyName?.trim() ? companyName : "회사 미지정";
}

export function AdminAdvanceRequestSection() {
  const location = useLocation();
  const navigate = useNavigate();
  const [requests, setRequests] = useState<AdminAdvanceRequestItemResponse[]>([]);
  const [loadState, setLoadState] = useState<"loading" | "success" | "error">("loading");
  const [filter, setFilter] = useState<RequestFilter>("ALL");
  const [companyFilter, setCompanyFilter] = useState<string>(ALL_COMPANIES_FILTER);
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
    const params = new URLSearchParams(location.search);
    const company = params.get("company");
    if (!company) {
      setCompanyFilter(ALL_COMPANIES_FILTER);
      return;
    }

    setCompanyFilter(company);
  }, [location.search]);

  useEffect(() => {
    if (!feedbackMessage) {
      return undefined;
    }

    const timer = window.setTimeout(() => {
      setFeedbackMessage(null);
    }, 2400);

    return () => window.clearTimeout(timer);
  }, [feedbackMessage]);

  const companyScopedRequests = useMemo(() => {
    return requests.filter((request) =>
      companyFilter === ALL_COMPANIES_FILTER
        ? true
        : getCompanyFilterValue(request.companyName) === companyFilter
    );
  }, [companyFilter, requests]);

  const requestCountByStatus = useMemo(() => {
    const counts: Record<RequestFilter, number> = {
      ALL: companyScopedRequests.length,
      SUBMITTED: 0,
      APPROVED: 0,
      PAYING: 0,
      PAID: 0,
      PAYOUT_FAILED: 0,
      REJECTED: 0
    };

    companyScopedRequests.forEach((request) => {
      if (request.status in counts) {
        counts[request.status as RequestFilter] += 1;
      }
    });

    return counts;
  }, [companyScopedRequests]);

  const companyFilters = useMemo(() => {
    const values = Array.from(
      new Set(requests.map((request) => getCompanyFilterValue(request.companyName)))
    );

    return [
      { value: ALL_COMPANIES_FILTER, label: "전체 회사" },
      ...values.map((value) => ({
        value,
        label: value === UNKNOWN_COMPANY_FILTER ? "회사 미지정" : value
      }))
    ];
  }, [requests]);

  const summary = useMemo(() => {
    return companyScopedRequests.reduce(
      (acc, request) => {
        acc.totalRequestedAtomic += request.requestedAmountAtomic;

        if (request.status === "SUBMITTED") {
          acc.pendingRequestedAtomic += request.requestedAmountAtomic;
        }

        if (request.status === "APPROVED" || request.status === "PAYING" || request.status === "PAID") {
          acc.approvedAtomic += request.approvedAmountAtomic ?? request.requestedAmountAtomic;
        }

        if (request.status === "PAYOUT_FAILED") {
          acc.failedCount += 1;
        }

        if (request.status === "REJECTED") {
          acc.rejectedCount += 1;
        }

        return acc;
      },
      {
        totalRequestedAtomic: 0,
        pendingRequestedAtomic: 0,
        approvedAtomic: 0,
        failedCount: 0,
        rejectedCount: 0
      }
    );
  }, [companyScopedRequests]);

  const filteredRequests = useMemo(() => {
    return companyScopedRequests.filter((request) =>
      filter === "ALL" ? true : request.status === filter
    );
  }, [companyScopedRequests, filter]);

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
              requestStatus: nextStatus === "REJECTED" ? "REJECTED" : "APPROVED",
              payoutStatus: nextStatus === "PAYING" ? "REQUESTED" : request.payoutStatus,
              approvedAmountAtomic:
                nextStatus === "REJECTED" ? null : (request.approvedAmountAtomic ?? request.requestedAmountAtomic),
              approvedDisplayKrwAmount:
                nextStatus === "REJECTED"
                  ? null
                  : (request.approvedDisplayKrwAmount ?? request.requestedDisplayKrwAmount),
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
        updateRequestStatus(request.requestId, "PAYING");
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

  const summaryAssetSymbol = requests[0]?.assetSymbol ?? "dUSDC";
  const summaryAssetDecimals = requests[0]?.assetDecimals ?? 6;

  return (
    <section className="admin-section">
      {feedbackMessage ? (
        <div className="admin-toast" role="status" aria-live="polite">
          {feedbackMessage}
        </div>
      ) : null}

      <div className="admin-section-head with-sub">
        <div>
          <h3 className="admin-feature-title admin-feature-title-large">미리받기</h3>
        </div>
      </div>

      <div className="admin-request-summary">
        <article className="admin-request-summary-card">
          <span>전체 요청</span>
          <strong>{requestCountByStatus.ALL}건</strong>
          <p>{formatAssetAmount(summary.totalRequestedAtomic, summaryAssetSymbol, summaryAssetDecimals)}</p>
        </article>
        <article className="admin-request-summary-card">
          <span>승인 대기</span>
          <strong>{requestCountByStatus.SUBMITTED}건</strong>
          <p>{formatAssetAmount(summary.pendingRequestedAtomic, summaryAssetSymbol, summaryAssetDecimals)}</p>
        </article>
        <article className="admin-request-summary-card">
          <span>승인·지급 진행</span>
          <strong>
            {requestCountByStatus.APPROVED + requestCountByStatus.PAYING + requestCountByStatus.PAID}건
          </strong>
          <p>{formatAssetAmount(summary.approvedAtomic, summaryAssetSymbol, summaryAssetDecimals)}</p>
        </article>
        <article className="admin-request-summary-card">
          <span>지급 이슈</span>
          <strong>{summary.failedCount + summary.rejectedCount}건</strong>
          <p>실패 {summary.failedCount}건 · 반려 {summary.rejectedCount}건</p>
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

        <div className="admin-request-toolbar-right">
          <label className="admin-company-filter-select">
            <select value={companyFilter} onChange={(event) => setCompanyFilter(event.currentTarget.value)}>
              {companyFilters.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
        </div>
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
                <th>회사</th>
                <th>근로자</th>
                <th>금액</th>
                <th>근무 기준</th>
                <th>일정</th>
                <th>상태</th>
                <th>처리</th>
              </tr>
            </thead>
            <tbody>
              {filteredRequests.map((request) => {
                const statusDetail = getStatusDetail(request);

                return (
                  <tr key={request.requestId}>
                    <td>
                      <strong>{getCompanyFilterLabel(request.companyName)}</strong>
                      <p className="admin-cell-sub">{request.workplaceName}</p>
                    </td>
                    <td>
                      <strong>{request.workerName}</strong>
                      <p className="admin-cell-sub">{request.workerEmail}</p>
                    </td>
                    <td>
                      <div className="admin-metric-stack">
                        <strong>
                          {formatAssetAmount(
                            request.requestedAmountAtomic,
                            request.assetSymbol,
                            request.assetDecimals
                          )}
                        </strong>
                        <p className="admin-cell-sub">{formatReferenceKrw(request.requestedDisplayKrwAmount)}</p>
                      </div>
                    </td>
                    <td>
                      <div className="admin-metric-stack">
                        <strong>{request.reflectedWorkDays}일 반영</strong>
                        <p className="admin-cell-sub">반영 근무 {formatWorkMinutes(request.reflectedWorkMinutes)}</p>
                        <p className="admin-cell-sub">검토 필요 기록 {request.needsReviewRecordCount}건</p>
                      </div>
                    </td>
                    <td>
                      <div className="admin-metric-stack">
                        {request.reviewedAt ? (
                          <p className="admin-cell-sub">신청 {formatDateTime(request.requestedAt)}</p>
                        ) : (
                          <strong>신청 {formatDateTime(request.requestedAt)}</strong>
                        )}
                        <p className="admin-cell-sub">
                          급여 정산 예정 {formatDate(request.settlementDueDate ?? request.repaymentDueDate)}
                        </p>
                        {request.reviewedAt ? (
                          <strong>처리 {formatDateTime(request.reviewedAt)}</strong>
                        ) : (
                          <p className="admin-cell-sub">처리 -</p>
                        )}
                      </div>
                    </td>
                    <td className="admin-status-cell">
                      <div className="admin-metric-stack admin-status-stack">
                        <span className={`admin-status ${STATUS_CLASS_NAMES[request.status]}`}>
                          {STATUS_LABELS[request.status]}
                        </span>
                        <p className="admin-cell-sub admin-status-detail" title={statusDetail}>
                          {statusDetail}
                        </p>
                      </div>
                    </td>
                    <td>
                      {canApproveOrReject(request.status) ? (
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
                        <span className="admin-action-done">
                          {request.status === "PAYOUT_FAILED" ? "재처리 확인 필요" : "처리 완료"}
                        </span>
                      )}
                    </td>
                  </tr>
                );
              })}
              {filteredRequests.length === 0 ? (
                <tr>
                  <td colSpan={7} className="admin-empty-cell">
                    선택한 조건에 해당하는 미리받기 요청이 없습니다.
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
