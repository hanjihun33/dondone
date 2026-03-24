import { useEffect, useMemo, useState } from "react";
import { useNavigate, useOutletContext } from "react-router-dom";
import { ApiError } from "../../shared/api/client";
import {
  approveEmployerCorrectionRequest,
  confirmEmployerReviewRecord,
  getEmployerCorrectionRequest,
  getEmployerCorrectionRequests,
  getEmployerIssues,
  getEmployerReviewRecord,
  rejectEmployerCorrectionRequest
} from "../../shared/api/employer";
import { clearStoredSession, getStoredAccessToken } from "../../shared/auth/session";
import { Badge } from "../../shared/ui/Badge";
import { SearchIcon } from "../../shared/ui/icons";
import { SectionCard } from "../../shared/ui/SectionCard";
import { IssueQueueList } from "./components/IssueQueueList";
import type { IssueFilterKey, IssueQueueItem } from "./model/issuesQueueData";

const requestFilters: { key: IssueFilterKey; label: string }[] = [
  { key: "all", label: "전체" },
  { key: "pending", label: "정정 요청" },
  { key: "needs_review", label: "검토 필요" },
  { key: "approved", label: "승인" },
  { key: "rejected", label: "반려" }
];

const rowsPerPageOptions = [5, 10, 20];
const roleFallback = "직무 정보 연동 대기";

const reviewReasonLabels: Record<string, string> = {
  CLOCK_OUT_OUTSIDE_ALLOWED_RADIUS: "반경 밖 퇴근",
  NEEDS_REVIEW: "검토 필요 기록"
};

function formatTime(value: string | null) {
  if (!value) {
    return "-";
  }

  return value.slice(11, 16);
}

function formatDateTime(value: string | null) {
  if (!value) {
    return "-";
  }

  return value.replace("T", " ").slice(0, 16);
}

function getReviewReasonLabel(reviewReasonCode: string | null) {
  if (!reviewReasonCode) {
    return null;
  }

  return reviewReasonLabels[reviewReasonCode] ?? reviewReasonCode;
}

function mapPendingIssues(
  items: Awaited<ReturnType<typeof getEmployerIssues>>["issues"]
): IssueQueueItem[] {
  return items.map((item) => ({
    key: item.itemType === "CORRECTION_REQUEST" ? `correction-${item.requestId}` : `review-${item.workProofId}`,
    itemType: item.itemType,
    issueStatus: item.issueStatus,
    correctionStatus: item.itemType === "CORRECTION_REQUEST" ? "PENDING" : null,
    requestId: item.requestId,
    workProofId: item.workProofId,
    workerId: item.workerId,
    workerName: item.workerName,
    workerEmail: item.workerEmail,
    role: item.role ?? roleFallback,
    workDate: item.workDate,
    originalCheckIn: formatTime(item.clockInAt),
    originalCheckOut: formatTime(item.clockOutAt),
    requestedCheckIn: formatTime(item.requestedClockInAt ?? item.clockInAt),
    requestedCheckOut: formatTime(item.requestedClockOutAt ?? item.clockOutAt),
    recognizedCheckIn: "-",
    recognizedCheckOut: "-",
    reasonCode: item.reasonCode,
    reviewReasonCode: item.reviewReasonCode,
    reason:
      item.itemType === "CORRECTION_REQUEST"
        ? item.reason ?? "요청 사유 없음"
        : item.reason ?? getReviewReasonLabel(item.reviewReasonCode) ?? "검토 사유 확인 필요",
    requestedAt: formatDateTime(item.raisedAt),
    detailState: "idle",
    detail: null,
    detailError: null
  }));
}

function mapClosedCorrections(
  items: Awaited<ReturnType<typeof getEmployerCorrectionRequests>>["requests"]
): IssueQueueItem[] {
  return items.map((item) => ({
    key: `correction-${item.requestId}`,
    itemType: "CORRECTION_REQUEST",
    issueStatus: null,
    correctionStatus: item.status,
    requestId: item.requestId,
    workProofId: item.workProofId,
    workerId: item.workerId,
    workerName: item.workerName,
    workerEmail: item.workerEmail,
    role: item.role ?? roleFallback,
    workDate: item.workDate,
    originalCheckIn: formatTime(item.originalClockInAt),
    originalCheckOut: formatTime(item.originalClockOutAt),
    requestedCheckIn: formatTime(item.requestedClockInAt),
    requestedCheckOut: formatTime(item.requestedClockOutAt),
    recognizedCheckIn: formatTime(item.recognizedClockInAt),
    recognizedCheckOut: formatTime(item.recognizedClockOutAt),
    reasonCode: item.reasonCode,
    reviewReasonCode: item.reviewReasonCode,
    reason: item.reason,
    requestedAt: formatDateTime(item.requestedAt),
    detailState: "idle",
    detail: null,
    detailError: null
  }));
}

function sortByRequestedAtDesc(items: IssueQueueItem[]) {
  return [...items].sort((a, b) => b.requestedAt.localeCompare(a.requestedAt));
}

function getPaginationItems(currentPage: number, totalPages: number) {
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, index) => index + 1);
  }

  const items: Array<number | "ellipsis"> = [1];
  const start = Math.max(2, currentPage - 1);
  const end = Math.min(totalPages - 1, currentPage + 1);

  if (start > 2) {
    items.push("ellipsis");
  }

  for (let page = start; page <= end; page += 1) {
    items.push(page);
  }

  if (end < totalPages - 1) {
    items.push("ellipsis");
  }

  items.push(totalPages);
  return items;
}

export function IssuesQueuePage() {
  const navigate = useNavigate();
  const { refreshTick } = useOutletContext<{ refreshTick: number }>();
  const [selectedFilter, setSelectedFilter] = useState<IssueFilterKey>("pending");
  const [searchQuery, setSearchQuery] = useState("");
  const [rowsPerPage, setRowsPerPage] = useState(5);
  const [currentPage, setCurrentPage] = useState(1);
  const [pendingIssues, setPendingIssues] = useState<IssueQueueItem[]>([]);
  const [closedCorrections, setClosedCorrections] = useState<IssueQueueItem[]>([]);
  const [loadState, setLoadState] = useState<"idle" | "loading" | "error">("idle");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [submittingReviewWorkProofId, setSubmittingReviewWorkProofId] = useState<number | null>(null);

  const reloadCollections = async () => {
    const token = getStoredAccessToken();
    if (!token) {
      clearStoredSession();
      navigate("/", { replace: true });
      return;
    }

    setLoadState("loading");
    setErrorMessage(null);

    try {
      const [issuesResponse, closedResponse] = await Promise.all([
        getEmployerIssues(token, {
          statuses: ["PENDING", "NEEDS_REVIEW"],
          page: 1,
          size: 100
        }),
        getEmployerCorrectionRequests(token, {
          statuses: ["APPROVED", "REJECTED"],
          page: 1,
          size: 100
        })
      ]);

      setPendingIssues(mapPendingIssues(issuesResponse.issues));
      setClosedCorrections(mapClosedCorrections(closedResponse.requests));
      setLoadState("idle");
    } catch (error: unknown) {
      if (error instanceof ApiError && error.status === 401) {
        clearStoredSession();
        navigate("/", { replace: true });
        return;
      }

      setLoadState("error");
      setErrorMessage(error instanceof Error ? error.message : "요청 목록을 불러오지 못했습니다.");
    }
  };

  useEffect(() => {
    void reloadCollections();
  }, [navigate, refreshTick]);

  const countByStatus = useMemo(() => {
    const pending = pendingIssues.filter((item) => item.correctionStatus === "PENDING").length;
    const needsReview = pendingIssues.filter((item) => item.issueStatus === "NEEDS_REVIEW").length;
    const approved = closedCorrections.filter((item) => item.correctionStatus === "APPROVED").length;
    const rejected = closedCorrections.filter((item) => item.correctionStatus === "REJECTED").length;

    return {
      pending,
      needs_review: needsReview,
      approved,
      rejected,
      all: pendingIssues.length + closedCorrections.length
    };
  }, [closedCorrections, pendingIssues]);

  const allRequests = useMemo(
    () => sortByRequestedAtDesc([...pendingIssues, ...closedCorrections]),
    [closedCorrections, pendingIssues]
  );

  const filteredRequests = useMemo(() => {
    const keyword = searchQuery.trim().toLowerCase();

    let baseList: IssueQueueItem[];
    switch (selectedFilter) {
      case "all":
        baseList = allRequests;
        break;
      case "pending":
        baseList = pendingIssues.filter((item) => item.correctionStatus === "PENDING");
        break;
      case "needs_review":
        baseList = pendingIssues.filter((item) => item.issueStatus === "NEEDS_REVIEW");
        break;
      case "approved":
        baseList = closedCorrections.filter((item) => item.correctionStatus === "APPROVED");
        break;
      case "rejected":
        baseList = closedCorrections.filter((item) => item.correctionStatus === "REJECTED");
        break;
    }

    return baseList.filter((request) => {
      if (keyword.length === 0) {
        return true;
      }

      return (
        request.workerName.toLowerCase().includes(keyword) ||
        request.reason.toLowerCase().includes(keyword) ||
        request.workerEmail.toLowerCase().includes(keyword)
      );
    });
  }, [allRequests, closedCorrections, pendingIssues, searchQuery, selectedFilter]);

  useEffect(() => {
    setCurrentPage(1);
  }, [rowsPerPage, searchQuery, selectedFilter]);

  const totalPages = Math.max(1, Math.ceil(filteredRequests.length / rowsPerPage));

  useEffect(() => {
    setCurrentPage((prev) => Math.min(prev, totalPages));
  }, [totalPages]);

  const pagedRequests = useMemo(() => {
    const startIndex = (currentPage - 1) * rowsPerPage;
    return filteredRequests.slice(startIndex, startIndex + rowsPerPage);
  }, [currentPage, filteredRequests, rowsPerPage]);

  const rangeStart = filteredRequests.length === 0 ? 0 : (currentPage - 1) * rowsPerPage + 1;
  const rangeEnd = filteredRequests.length === 0 ? 0 : Math.min(currentPage * rowsPerPage, filteredRequests.length);
  const pageItems = getPaginationItems(currentPage, totalPages);

  const updateIssueItem = (nextItem: IssueQueueItem) => {
    const updateList = (items: IssueQueueItem[]) =>
      items.map((item) => (item.key === nextItem.key ? nextItem : item));

    setPendingIssues((prev) => updateList(prev));
    setClosedCorrections((prev) => updateList(prev));
  };

  const moveCorrectionToClosed = (nextItem: IssueQueueItem) => {
    setPendingIssues((prev) => prev.filter((item) => item.key !== nextItem.key));
    setClosedCorrections((prev) =>
      sortByRequestedAtDesc([nextItem, ...prev.filter((item) => item.key !== nextItem.key)])
    );
  };

  const handleToggleDetail = async (item: IssueQueueItem) => {
    if (item.detailState === "loaded") {
      updateIssueItem({
        ...item,
        detailState: "idle",
        detail: null,
        detailError: null
      });
      return;
    }

    updateIssueItem({
      ...item,
      detailState: "loading",
      detail: null,
      detailError: null
    });

    const token = getStoredAccessToken();
    if (!token) {
      clearStoredSession();
      navigate("/", { replace: true });
      return;
    }

    try {
      if (item.itemType === "CORRECTION_REQUEST" && item.requestId) {
        const detail = await getEmployerCorrectionRequest(token, item.requestId);
        updateIssueItem({
          ...item,
          detailState: "loaded",
          detailError: null,
          detail: {
            kind: "correction",
            reasonCode: detail.reasonCode,
            reviewReasonCode: detail.reviewReasonCode,
            recognizedCheckIn: formatTime(detail.recognizedClockInAt),
            recognizedCheckOut: formatTime(detail.recognizedClockOutAt),
            requestMemo: detail.requestMemo,
            decisionMemo: detail.decisionMemo,
            decisionByName: detail.decisionByName,
            decisionAt: detail.decisionAt,
            rejectReasonCode: detail.rejectReasonCode,
            attachments: detail.attachments
          }
        });
        return;
      }

      const detail = await getEmployerReviewRecord(token, item.workProofId);
      updateIssueItem({
        ...item,
        detailState: "loaded",
        detailError: null,
        detail: {
          kind: "review",
          reviewReason: detail.reviewReason,
          reviewReasonCode: detail.reviewReasonCode,
          recognizedCheckIn: formatTime(detail.recognizedClockInAt),
          recognizedCheckOut: formatTime(detail.recognizedClockOutAt),
          recordStatus: detail.recordStatus,
          reflectionStatus: detail.reflectionStatus,
          workedMinutes: detail.workedMinutes,
          memo: detail.memo,
          editReason: detail.editReason,
          clockOutOutsideAllowedRadius: detail.clockOutOutsideAllowedRadius,
          attachmentCount: detail.attachmentCount,
          workplaceName: detail.workplace?.name ?? null,
          workplaceAddress: detail.workplace?.address ?? null,
          checkInLabel: detail.checkIn?.locationLabel ?? null,
          checkOutLabel: detail.checkOut?.locationLabel ?? null
        }
      });
    } catch (error: unknown) {
      if (error instanceof ApiError && error.status === 401) {
        clearStoredSession();
        navigate("/", { replace: true });
        return;
      }

      updateIssueItem({
        ...item,
        detailState: "error",
        detail: null,
        detailError: error instanceof Error ? error.message : "상세 정보를 불러오지 못했습니다."
      });
    }
  };

  const handleApprove = async (requestId: number) => {
    const token = getStoredAccessToken();
    if (!token) {
      clearStoredSession();
      navigate("/", { replace: true });
      return;
    }

    try {
      const detail = await approveEmployerCorrectionRequest(token, requestId, "");
      moveCorrectionToClosed({
        key: `correction-${detail.requestId}`,
        itemType: "CORRECTION_REQUEST",
        issueStatus: null,
        correctionStatus: detail.status,
        requestId: detail.requestId,
        workProofId: detail.workProofId,
        workerId: detail.workerId,
        workerName: detail.workerName,
        workerEmail: detail.workerEmail,
        role: detail.role ?? roleFallback,
        workDate: detail.workDate,
        originalCheckIn: formatTime(detail.originalClockInAt),
        originalCheckOut: formatTime(detail.originalClockOutAt),
        requestedCheckIn: formatTime(detail.requestedClockInAt),
        requestedCheckOut: formatTime(detail.requestedClockOutAt),
        recognizedCheckIn: formatTime(detail.recognizedClockInAt),
        recognizedCheckOut: formatTime(detail.recognizedClockOutAt),
        reasonCode: detail.reasonCode,
        reviewReasonCode: detail.reviewReasonCode,
        reason: detail.reason,
        requestedAt: formatDateTime(detail.requestedAt),
        detailState: "loaded",
        detailError: null,
        detail: {
          kind: "correction",
          reasonCode: detail.reasonCode,
          reviewReasonCode: detail.reviewReasonCode,
          recognizedCheckIn: formatTime(detail.recognizedClockInAt),
          recognizedCheckOut: formatTime(detail.recognizedClockOutAt),
          requestMemo: detail.requestMemo,
          decisionMemo: detail.decisionMemo,
          decisionByName: detail.decisionByName,
          decisionAt: detail.decisionAt,
          rejectReasonCode: detail.rejectReasonCode,
          attachments: detail.attachments
        }
      });
    } catch (error: unknown) {
      if (error instanceof ApiError && error.status === 401) {
        clearStoredSession();
        navigate("/", { replace: true });
        return;
      }

      setErrorMessage(error instanceof Error ? error.message : "정정 요청을 승인하지 못했습니다.");
    }
  };

  const handleReject = async (requestId: number) => {
    const token = getStoredAccessToken();
    if (!token) {
      clearStoredSession();
      navigate("/", { replace: true });
      return;
    }

    const promptResult = window.prompt("반려 사유를 입력해 주세요.", "");
    if (promptResult === null) {
      return;
    }

    try {
      const detail = await rejectEmployerCorrectionRequest(token, requestId, {
        decisionMemo: promptResult,
        rejectReasonCode: "EMPLOYER_REJECTED"
      });
      moveCorrectionToClosed({
        key: `correction-${detail.requestId}`,
        itemType: "CORRECTION_REQUEST",
        issueStatus: null,
        correctionStatus: detail.status,
        requestId: detail.requestId,
        workProofId: detail.workProofId,
        workerId: detail.workerId,
        workerName: detail.workerName,
        workerEmail: detail.workerEmail,
        role: detail.role ?? roleFallback,
        workDate: detail.workDate,
        originalCheckIn: formatTime(detail.originalClockInAt),
        originalCheckOut: formatTime(detail.originalClockOutAt),
        requestedCheckIn: formatTime(detail.requestedClockInAt),
        requestedCheckOut: formatTime(detail.requestedClockOutAt),
        recognizedCheckIn: formatTime(detail.recognizedClockInAt),
        recognizedCheckOut: formatTime(detail.recognizedClockOutAt),
        reasonCode: detail.reasonCode,
        reviewReasonCode: detail.reviewReasonCode,
        reason: detail.reason,
        requestedAt: formatDateTime(detail.requestedAt),
        detailState: "loaded",
        detailError: null,
        detail: {
          kind: "correction",
          reasonCode: detail.reasonCode,
          reviewReasonCode: detail.reviewReasonCode,
          recognizedCheckIn: formatTime(detail.recognizedClockInAt),
          recognizedCheckOut: formatTime(detail.recognizedClockOutAt),
          requestMemo: detail.requestMemo,
          decisionMemo: detail.decisionMemo,
          decisionByName: detail.decisionByName,
          decisionAt: detail.decisionAt,
          rejectReasonCode: detail.rejectReasonCode,
          attachments: detail.attachments
        }
      });
    } catch (error: unknown) {
      if (error instanceof ApiError && error.status === 401) {
        clearStoredSession();
        navigate("/", { replace: true });
        return;
      }

      setErrorMessage(error instanceof Error ? error.message : "정정 요청을 반려하지 못했습니다.");
    }
  };

  const handleConfirmReview = async (workProofId: number) => {
    const token = getStoredAccessToken();
    if (!token) {
      clearStoredSession();
      navigate("/", { replace: true });
      return;
    }

    setSubmittingReviewWorkProofId(workProofId);
    setErrorMessage(null);

    try {
      await confirmEmployerReviewRecord(token, workProofId);
      await reloadCollections();
    } catch (error: unknown) {
      if (error instanceof ApiError && error.status === 401) {
        clearStoredSession();
        navigate("/", { replace: true });
        return;
      }

      setErrorMessage(error instanceof Error ? error.message : "검토 필요 기록을 처리하지 못했습니다.");
    } finally {
      setSubmittingReviewWorkProofId(null);
    }
  };

  return (
    <div className="board issues-page">
      <header className="topbar issues-header">
        <div>
          <h2 className="issues-title">요청 관리</h2>
          <p className="issues-subtitle">정정 요청과 검토 필요 기록을 함께 확인합니다.</p>
          <p className="issues-subtitle">{`조회 범위 내 요청 ${filteredRequests.length}건`}</p>
          {loadState === "loading" ? <p className="issues-subtitle">요청 목록을 불러오는 중입니다...</p> : null}
          {errorMessage ? <p className="issues-subtitle">{errorMessage}</p> : null}
        </div>
        <div className="topbar-actions">
          <Badge tone="warn">{`정정 요청 ${countByStatus.pending}건`}</Badge>
        </div>
      </header>

      <SectionCard>
        <div className="issue-queue-toolbar">
          <div className="issue-filter-tabs">
            {requestFilters.map((filter) => (
              <button
                key={filter.key}
                type="button"
                className={`issue-filter-tab${selectedFilter === filter.key ? " active" : ""}`}
                onClick={() => setSelectedFilter(filter.key)}
              >
                {filter.label}
                <span>{countByStatus[filter.key]}</span>
              </button>
            ))}
          </div>

          <label className="issue-search-input">
            <span className="issue-search-leading-icon" aria-hidden="true">
              <SearchIcon />
            </span>
            <input
              type="text"
              aria-label="요청 검색"
              placeholder="근로자명, 이메일, 사유 검색"
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.currentTarget.value)}
            />
          </label>
        </div>

        <IssueQueueList
          requests={pagedRequests}
          onApprove={handleApprove}
          onConfirmReview={handleConfirmReview}
          onReject={handleReject}
          onToggleDetail={handleToggleDetail}
          submittingWorkProofId={submittingReviewWorkProofId}
        />

        <div className="worker-pagination">
          <div className="worker-pagination-left">
            <label htmlFor="issue-rows-per-page">페이지당 개수</label>
            <select
              id="issue-rows-per-page"
              value={rowsPerPage}
              onChange={(event) => setRowsPerPage(Number(event.currentTarget.value))}
            >
              {rowsPerPageOptions.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
            <span>
              {rangeStart}-{rangeEnd} / {filteredRequests.length}
            </span>
          </div>

          <div className="worker-pagination-right">
            <button
              type="button"
              className="worker-pagination-nav"
              onClick={() => setCurrentPage((prev) => Math.max(1, prev - 1))}
              disabled={currentPage <= 1}
              aria-label="이전 페이지"
            >
              이전
            </button>

            {pageItems.map((item, index) =>
              item === "ellipsis" ? (
                <span key={`ellipsis-${index}`} className="worker-pagination-ellipsis" aria-hidden="true">
                  ...
                </span>
              ) : (
                <button
                  type="button"
                  key={item}
                  className={`worker-pagination-page${item === currentPage ? " active" : ""}`}
                  onClick={() => setCurrentPage(item)}
                  aria-current={item === currentPage ? "page" : undefined}
                >
                  {item}
                </button>
              )
            )}

            <button
              type="button"
              className="worker-pagination-nav"
              onClick={() => setCurrentPage((prev) => Math.min(totalPages, prev + 1))}
              disabled={currentPage >= totalPages}
              aria-label="다음 페이지"
            >
              다음
            </button>
          </div>
        </div>
      </SectionCard>
    </div>
  );
}
