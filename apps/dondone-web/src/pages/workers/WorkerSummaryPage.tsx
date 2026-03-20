import { useEffect, useMemo, useState } from "react";
import { useNavigate, useOutletContext } from "react-router-dom";
import { ApiError } from "../../shared/api/client";
import {
  type EmployerWorkerAttendanceStatus,
  getEmployerWorkers
} from "../../shared/api/employer";
import { clearStoredSession, getStoredAccessToken } from "../../shared/auth/session";
import { SearchIcon } from "../../shared/ui/icons";
import { WorkerSummaryList } from "./components/WorkerSummaryList";
import { type WorkerListRow, workerSummaryMeta } from "./model/workerSummaryData";

const quickStatusFilters: { key: "all" | EmployerWorkerAttendanceStatus; label: string }[] = [
  { key: "all", label: "전체" },
  { key: "WORKING", label: "근무중" },
  { key: "COMPLETED", label: "근무 완료" },
  { key: "NEEDS_REVIEW", label: "검토 필요" },
  { key: "NO_RECORD", label: "기록 없음" }
];

const rowsPerPageOptions = [5, 10, 20];
const avatarTones: WorkerListRow["avatarTone"][] = ["amber", "sky", "mint", "violet", "rose"];

function toWorkerRow(
  row: Awaited<ReturnType<typeof getEmployerWorkers>>["workers"][number]
): WorkerListRow {
  return {
    id: row.workerId,
    name: row.name,
    employeeCode: row.employeeCode ?? `근로자-${row.workerId}`,
    appliedFor: row.role ?? "직무 기준 데이터 정리 예정",
    team: row.team ?? "소속 팀 데이터 정리 예정",
    email: row.email,
    phone: row.phone ?? "-",
    attendanceStatus: row.attendanceStatus,
    latestWorkDate: row.latestWorkDate,
    avatarTone: avatarTones[row.workerId % avatarTones.length],
    avatarUrl: row.avatarUrl ?? undefined
  };
}

export function WorkerSummaryPage() {
  const navigate = useNavigate();
  const { refreshTick } = useOutletContext<{ refreshTick: number }>();
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedStatus, setSelectedStatus] = useState<"all" | EmployerWorkerAttendanceStatus>("all");
  const [rowsPerPage, setRowsPerPage] = useState(5);
  const [currentPage, setCurrentPage] = useState(1);
  const [rows, setRows] = useState<WorkerListRow[]>([]);
  const [totalRows, setTotalRows] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loadState, setLoadState] = useState<"idle" | "loading" | "error">("idle");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchQuery, selectedStatus, rowsPerPage]);

  useEffect(() => {
    const token = getStoredAccessToken();
    if (!token) {
      clearStoredSession();
      navigate("/", { replace: true });
      return;
    }

    let isDisposed = false;
    setLoadState("loading");
    setErrorMessage(null);

    void getEmployerWorkers(token, {
      query: searchQuery.trim() || undefined,
      statuses: selectedStatus === "all" ? undefined : [selectedStatus],
      page: currentPage,
      size: rowsPerPage
    })
      .then((response) => {
        if (isDisposed) {
          return;
        }

        setRows(response.workers.map(toWorkerRow));
        setTotalRows(response.totalElements);
        setTotalPages(Math.max(response.totalPages, 1));
        setLoadState("idle");
      })
      .catch((error: unknown) => {
        if (isDisposed) {
          return;
        }

        if (error instanceof ApiError && error.status === 401) {
          clearStoredSession();
          navigate("/", { replace: true });
          return;
        }

        setLoadState("error");
        setErrorMessage(error instanceof Error ? error.message : "근로자 목록을 불러오지 못했습니다.");
      });

    return () => {
      isDisposed = true;
    };
  }, [currentPage, navigate, refreshTick, rowsPerPage, searchQuery, selectedStatus]);

  const rangeStart = totalRows === 0 ? 0 : (currentPage - 1) * rowsPerPage + 1;
  const rangeEnd = totalRows === 0 ? 0 : Math.min(currentPage * rowsPerPage, totalRows);
  const helperText = useMemo(() => {
    if (loadState === "loading") {
      return "근로자 목록을 불러오는 중입니다...";
    }

    if (loadState === "error") {
      return errorMessage ?? "근로자 목록을 불러오지 못했습니다.";
    }

    return `조회 범위 내 근로자 ${totalRows}명`;
  }, [errorMessage, loadState, totalRows]);

  return (
    <div className="console-page worker-list-page">
      <header className="worker-list-header">
        <div>
          <h2 className="worker-list-title">{workerSummaryMeta.title}</h2>
          <p className="worker-list-subtitle">{workerSummaryMeta.subtitle}</p>
          <p className="worker-list-subtitle">{helperText}</p>
        </div>
      </header>

      <section className="worker-list-panel attendance-board">
        <div className="attendance-toolbar">
          <label className="attendance-search">
            <span className="attendance-search-icon">
              <SearchIcon />
            </span>
            <input
              type="text"
              placeholder="근로자/직무/연락처 검색"
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.currentTarget.value)}
            />
          </label>

          <div className="attendance-toolbar-actions">
            <div className="worker-quick-filter">
              {quickStatusFilters.map((filter) => (
                <button
                  key={filter.key}
                  type="button"
                  className={`worker-quick-filter-button${selectedStatus === filter.key ? " active" : ""}`}
                  onClick={() => setSelectedStatus(filter.key)}
                >
                  {filter.label}
                </button>
              ))}
            </div>
          </div>
        </div>

        <WorkerSummaryList
          columns={workerSummaryMeta.columns}
          rows={rows}
          pagination={{
            rowsPerPage,
            rowsPerPageOptions,
            currentPage,
            totalPages,
            totalRows,
            rangeStart,
            rangeEnd,
            onRowsPerPageChange: setRowsPerPage,
            onPageChange: setCurrentPage
          }}
        />
      </section>
    </div>
  );
}
