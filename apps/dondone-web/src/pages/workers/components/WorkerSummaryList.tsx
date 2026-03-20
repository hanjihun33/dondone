import { useState } from "react";
import { Badge } from "../../../shared/ui/Badge";
import { UserAvatarIcon } from "../../../shared/ui/icons";
import type { WorkerListColumn, WorkerListRow } from "../model/workerSummaryData";

type WorkerSummaryListProps = {
  columns: WorkerListColumn[];
  rows: WorkerListRow[];
  pagination: {
    rowsPerPage: number;
    rowsPerPageOptions: number[];
    currentPage: number;
    totalPages: number;
    totalRows: number;
    rangeStart: number;
    rangeEnd: number;
    onRowsPerPageChange: (next: number) => void;
    onPageChange: (next: number) => void;
  };
};

type WorkerAvatarProps = {
  name: string;
  avatarTone: "amber" | "sky" | "mint" | "violet" | "rose";
  avatarUrl?: string;
};

function WorkerAvatar({ name, avatarTone, avatarUrl }: WorkerAvatarProps) {
  const [imageError, setImageError] = useState(false);
  const hasImage = Boolean(avatarUrl) && !imageError;

  return (
    <span className={`employee-avatar ${hasImage ? avatarTone : "fallback"}`}>
      {hasImage ? (
        <img
          src={avatarUrl}
          alt={`${name} 프로필 사진`}
          loading="lazy"
          onError={() => setImageError(true)}
        />
      ) : (
        <UserAvatarIcon />
      )}
    </span>
  );
}

function getStatusTone(status: WorkerListRow["attendanceStatus"]) {
  if (status === "COMPLETED") {
    return "success" as const;
  }

  if (status === "NEEDS_REVIEW") {
    return "warn" as const;
  }

  if (status === "NO_RECORD") {
    return "danger" as const;
  }

  return "soft" as const;
}

function getStatusLabel(status: WorkerListRow["attendanceStatus"]) {
  if (status === "WORKING") {
    return "근무중";
  }

  if (status === "COMPLETED") {
    return "근무 완료";
  }

  if (status === "NEEDS_REVIEW") {
    return "검토 필요";
  }

  return "기록 없음";
}

export function WorkerSummaryList({ columns, rows, pagination }: WorkerSummaryListProps) {
  const pageItems =
    pagination.totalPages <= 7
      ? Array.from({ length: pagination.totalPages }, (_, index) => index + 1)
      : (() => {
          const items: Array<number | "ellipsis"> = [1];
          const start = Math.max(2, pagination.currentPage - 1);
          const end = Math.min(pagination.totalPages - 1, pagination.currentPage + 1);

          if (start > 2) {
            items.push("ellipsis");
          }

          for (let page = start; page <= end; page += 1) {
            items.push(page);
          }

          if (end < pagination.totalPages - 1) {
            items.push("ellipsis");
          }

          items.push(pagination.totalPages);
          return items;
        })();

  return (
    <>
      <div className="attendance-table-wrap worker-list-table-wrap">
        <table className="attendance-table">
          <thead>
            <tr>
              {columns.map((column) => (
                <th
                  key={column.key}
                  className={column.key === "candidate" ? "employee-col" : undefined}
                >
                  <span className="worker-head-cell">{column.label}</span>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 ? (
              <tr>
                <td className="attendance-empty-row" colSpan={columns.length}>
                  검색/필터 조건에 맞는 근로자가 없습니다.
                </td>
              </tr>
            ) : (
              rows.map((row) => (
                <tr key={row.id}>
                  <td className="employee-cell">
                    <div className="employee-info">
                      <WorkerAvatar
                        name={row.name}
                        avatarTone={row.avatarTone}
                        avatarUrl={row.avatarUrl}
                      />
                      <div>
                        <strong>{row.name}</strong>
                        <p>{row.employeeCode}</p>
                        <Badge tone={getStatusTone(row.attendanceStatus)}>
                          {getStatusLabel(row.attendanceStatus)}
                        </Badge>
                      </div>
                    </div>
                  </td>
                  <td>
                    <div className="worker-role">
                      <strong>{row.appliedFor}</strong>
                      <p>{row.team}</p>
                      <p>{row.latestWorkDate ? `최근 근무일: ${row.latestWorkDate}` : "최근 근무일 없음"}</p>
                    </div>
                  </td>
                  <td>
                    <div className="worker-contact">
                      <strong>{row.email}</strong>
                      {row.phone === "-" ? <span>{row.phone}</span> : <a href={`tel:${row.phone}`}>{row.phone}</a>}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="worker-pagination">
        <div className="worker-pagination-left">
          <label htmlFor="worker-rows-per-page">페이지당 행</label>
          <select
            id="worker-rows-per-page"
            value={pagination.rowsPerPage}
            onChange={(event) => pagination.onRowsPerPageChange(Number(event.currentTarget.value))}
          >
            {pagination.rowsPerPageOptions.map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
          <span>
            {pagination.rangeStart}-{pagination.rangeEnd} / {pagination.totalRows}
          </span>
        </div>

        <div className="worker-pagination-right">
          <button
            type="button"
            className="worker-pagination-nav"
            onClick={() => pagination.onPageChange(Math.max(1, pagination.currentPage - 1))}
            disabled={pagination.currentPage <= 1}
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
                className={`worker-pagination-page${item === pagination.currentPage ? " active" : ""}`}
                onClick={() => pagination.onPageChange(item)}
                aria-current={item === pagination.currentPage ? "page" : undefined}
              >
                {item}
              </button>
            )
          )}

          <button
            type="button"
            className="worker-pagination-nav"
            onClick={() =>
              pagination.onPageChange(Math.min(pagination.totalPages, pagination.currentPage + 1))
            }
            disabled={pagination.currentPage >= pagination.totalPages}
            aria-label="다음 페이지"
          >
            다음
          </button>
        </div>
      </div>
    </>
  );
}
