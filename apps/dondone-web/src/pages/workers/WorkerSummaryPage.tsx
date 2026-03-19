import { useEffect, useMemo, useState } from "react";
import { SearchIcon } from "../../shared/ui/icons";
import { WorkerSummaryList } from "./components/WorkerSummaryList";
import {
  type WorkerAttendanceStatus,
  workerSummaryData
} from "./model/workerSummaryData";

const quickStatusFilters: { key: "all" | WorkerAttendanceStatus; label: string }[] = [
  { key: "all", label: "전체" },
  { key: "present", label: "오늘 출근" },
  { key: "late", label: "지각" },
  { key: "leave", label: "휴가" },
  { key: "absent", label: "결근" }
];
const rowsPerPageOptions = [5, 10, 20];

export function WorkerSummaryPage() {
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedStatus, setSelectedStatus] = useState<"all" | WorkerAttendanceStatus>("all");
  const [rowsPerPage, setRowsPerPage] = useState(5);
  const [currentPage, setCurrentPage] = useState(1);

  const filteredRows = useMemo(() => {
    const keyword = searchQuery.trim().toLowerCase();

    return workerSummaryData.rows.filter((row) => {
      const matchesKeyword =
        keyword.length === 0 ||
        row.name.toLowerCase().includes(keyword) ||
        row.employeeCode.toLowerCase().includes(keyword) ||
        row.appliedFor.toLowerCase().includes(keyword) ||
        row.team.toLowerCase().includes(keyword) ||
        row.email.toLowerCase().includes(keyword);

      const matchesStatus =
        selectedStatus === "all" || row.attendanceStatus === selectedStatus;

      return matchesKeyword && matchesStatus;
    });
  }, [searchQuery, selectedStatus]);

  const totalRows = filteredRows.length;
  const totalPages = Math.max(1, Math.ceil(totalRows / rowsPerPage));

  useEffect(() => {
    setCurrentPage(1);
  }, [searchQuery, selectedStatus, rowsPerPage]);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  const pagedRows = useMemo(() => {
    const startIndex = (currentPage - 1) * rowsPerPage;
    const endIndex = startIndex + rowsPerPage;
    return filteredRows.slice(startIndex, endIndex);
  }, [currentPage, filteredRows, rowsPerPage]);

  const rangeStart = totalRows === 0 ? 0 : (currentPage - 1) * rowsPerPage + 1;
  const rangeEnd = totalRows === 0 ? 0 : Math.min(currentPage * rowsPerPage, totalRows);

  return (
    <div className="console-page worker-list-page">
      <header className="worker-list-header">
        <div>
          <h2 className="worker-list-title">{workerSummaryData.title}</h2>
          <p className="worker-list-subtitle">{workerSummaryData.subtitle}</p>
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
          columns={workerSummaryData.columns}
          rows={pagedRows}
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
