import { useEffect, useMemo, useState } from "react";
import { useNavigate, useOutletContext } from "react-router-dom";
import { ApiError } from "../../shared/api/client";
import {
  approveEmployerCorrectionRequest,
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
  { key: "pending", label: "대기" },
  { key: "needs_review", label: "검토 필요" },
  { key: "approved", label: "수락" },
  { key: "rejected", label: "거절" }
];

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
    role: item.role ?? "직무 기준 데이터 정리 예정",
    workDate: item.workDate,
    originalCheckIn: formatTime(item.clockInAt),
    originalCheckOut: formatTime(item.clockOutAt),
    requestedCheckIn: formatTime(item.requestedClockInAt ?? item.clockInAt),
    requestedCheckOut: formatTime(item.requestedClockOutAt ?? item.clockOutAt),
    reason:
      item.itemType === "CORRECTION_REQUEST"
        ? item.reason ?? "정정 요청"
        : item.reason ?? "검토 필요 기록",
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
    role: item.role ?? "직무 기준 데이터 정리 예정",
    workDate: item.workDate,
    originalCheckIn: formatTime(item.originalClockInAt),
    originalCheckOut: formatTime(item.originalClockOutAt),
    requestedCheckIn: formatTime(item.requestedClockInAt),
    requestedCheckOut: formatTime(item.requestedClockOutAt),
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

export function IssuesQueuePage() {
  const navigate = useNavigate();
  const { refreshTick } = useOutletContext<{ refreshTick: number }>();
  const [selectedFilter, setSelectedFilter] = useState<IssueFilterKey>("pending");
  const [searchQuery, setSearchQuery] = useState("");
  const [pendingIssues, setPendingIssues] = useState<IssueQueueItem[]>([]);
  const [closedCorrections, setClosedCorrections] = useState<IssueQueueItem[]>([]);
  const [loadState, setLoadState] = useState<"idle" | "loading" | "error">("idle");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

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

    const baseList =
      selectedFilter === "all"
        ? allRequests
        : selectedFilter === "pending"
          ? pendingIssues.filter((item) => item.correctionStatus === "PENDING")
          : selectedFilter === "needs_review"
            ? pendingIssues.filter((item) => item.issueStatus === "NEEDS_REVIEW")
            : closedCorrections.filter((item) =>
                selectedFilter === "approved"
                  ? item.correctionStatus === "APPROVED"
                  : item.correctionStatus === "REJECTED"
              );

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
        role: detail.role ?? "직무 기준 데이터 정리 예정",
        workDate: detail.workDate,
        originalCheckIn: formatTime(detail.originalClockInAt),
        originalCheckOut: formatTime(detail.originalClockOutAt),
        requestedCheckIn: formatTime(detail.requestedClockInAt),
        requestedCheckOut: formatTime(detail.requestedClockOutAt),
        reason: detail.reason,
        requestedAt: formatDateTime(detail.requestedAt),
        detailState: "loaded",
        detailError: null,
        detail: {
          kind: "correction",
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

      setErrorMessage(error instanceof Error ? error.message : "요청을 수락하지 못했습니다.");
    }
  };

  const handleReject = async (requestId: number) => {
    const token = getStoredAccessToken();
    if (!token) {
      clearStoredSession();
      navigate("/", { replace: true });
      return;
    }

    const promptResult = window.prompt("거절 사유를 입력하세요.", "");
    if (promptResult === null) {
      return;
    }
    const decisionMemo = promptResult;

    try {
      const detail = await rejectEmployerCorrectionRequest(token, requestId, {
        decisionMemo,
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
        role: detail.role ?? "직무 기준 데이터 정리 예정",
        workDate: detail.workDate,
        originalCheckIn: formatTime(detail.originalClockInAt),
        originalCheckOut: formatTime(detail.originalClockOutAt),
        requestedCheckIn: formatTime(detail.requestedClockInAt),
        requestedCheckOut: formatTime(detail.requestedClockOutAt),
        reason: detail.reason,
        requestedAt: formatDateTime(detail.requestedAt),
        detailState: "loaded",
        detailError: null,
        detail: {
          kind: "correction",
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

      setErrorMessage(error instanceof Error ? error.message : "요청을 거절하지 못했습니다.");
    }
  };

  return (
    <div className="board issues-page">
      <header className="topbar issues-header">
        <div>
          <h2 className="issues-title">요청 관리</h2>
          <p className="issues-subtitle">출퇴근 정정 요청을 확인하고 처리합니다.</p>
          {loadState === "loading" ? <p className="issues-subtitle">요청 목록을 불러오는 중입니다...</p> : null}
          {errorMessage ? <p className="issues-subtitle">{errorMessage}</p> : null}
        </div>
        <div className="topbar-actions">
          <Badge tone="warn">{`대기 ${countByStatus.pending}건`}</Badge>
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
              placeholder="근로자명 또는 사유로 검색"
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.currentTarget.value)}
            />
          </label>
        </div>

        <IssueQueueList
          requests={filteredRequests}
          onApprove={handleApprove}
          onReject={handleReject}
          onToggleDetail={handleToggleDetail}
        />
      </SectionCard>
    </div>
  );
}
