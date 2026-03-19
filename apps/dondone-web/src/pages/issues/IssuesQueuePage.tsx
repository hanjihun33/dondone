import { useMemo, useState } from "react";
import { Badge } from "../../shared/ui/Badge";
import { SearchIcon } from "../../shared/ui/icons";
import { SectionCard } from "../../shared/ui/SectionCard";
import { IssueQueueList } from "./components/IssueQueueList";
import {
  issuesQueueData,
  type CorrectionRequestItem,
  type CorrectionRequestStatus
} from "./model/issuesQueueData";

type RequestFilterKey = "pending" | "approved" | "rejected" | "all";

const requestFilters: { key: RequestFilterKey; label: string }[] = [
  { key: "all", label: "전체" },
  { key: "pending", label: "대기" },
  { key: "approved", label: "수락" },
  { key: "rejected", label: "거절" }
];

export function IssuesQueuePage() {
  const [requests, setRequests] = useState<CorrectionRequestItem[]>(
    issuesQueueData.requests
  );
  const [selectedFilter, setSelectedFilter] =
    useState<RequestFilterKey>("pending");
  const [searchQuery, setSearchQuery] = useState("");

  const countByStatus = useMemo(() => {
    const pending = requests.filter((item) => item.status === "pending").length;
    const approved = requests.filter(
      (item) => item.status === "approved"
    ).length;
    const rejected = requests.filter(
      (item) => item.status === "rejected"
    ).length;

    return {
      pending,
      approved,
      rejected,
      all: requests.length
    };
  }, [requests]);

  const filteredRequests = useMemo(() => {
    const keyword = searchQuery.trim().toLowerCase();

    return requests.filter((request) => {
      const matchesStatus =
        selectedFilter === "all" || request.status === selectedFilter;

      const matchesKeyword =
        keyword.length === 0 ||
        request.workerName.toLowerCase().includes(keyword) ||
        request.reason.toLowerCase().includes(keyword);

      return matchesStatus && matchesKeyword;
    });
  }, [requests, searchQuery, selectedFilter]);

  const resolveRequest = (
    requestId: string,
    nextStatus: CorrectionRequestStatus
  ) => {
    setRequests((prev) =>
      prev.map((request) =>
        request.id === requestId ? { ...request, status: nextStatus } : request
      )
    );
  };

  return (
    <div className="board issues-page">
      <header className="topbar issues-header">
        <div>
          <h2 className="issues-title">요청 관리</h2>
          <p className="issues-subtitle">출퇴근 정정 요청을 확인하고 처리합니다.</p>
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
                className={`issue-filter-tab${
                  selectedFilter === filter.key ? " active" : ""
                }`}
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

        <IssueQueueList requests={filteredRequests} onResolve={resolveRequest} />
      </SectionCard>
    </div>
  );
}
