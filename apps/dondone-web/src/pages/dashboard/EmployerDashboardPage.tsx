import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useNavigate, useOutletContext } from "react-router-dom";
import { ApiError } from "../../shared/api/client";
import {
  type EmployerAttendanceBoardRowResponse,
  type EmployerDashboardSummaryResponse,
  type EmployerWorkerAttendanceStatus,
  getEmployerAttendanceBoard,
  getEmployerDashboardSummary
} from "../../shared/api/employer";
import { clearStoredSession, getStoredAccessToken } from "../../shared/auth/session";
import {
  CalendarIcon,
  ChevronDownIcon,
  CheckCircleIcon,
  ClockIcon,
  CloseIcon,
  FilterIcon,
  MapPinIcon,
  SearchIcon,
  UserAvatarIcon
} from "../../shared/ui/icons";

const DAY_MS = 24 * 60 * 60 * 1000;
const INITIAL_VISIBLE_ROWS = 5;
const ROW_BATCH_SIZE = 3;
const attendanceWeekDays = ["일", "월", "화", "수", "목", "금", "토"];
const dashboardStatusFilters: { label: string; status: EmployerWorkerAttendanceStatus }[] = [
  { label: "근무중", status: "WORKING" },
  { label: "근무 완료", status: "COMPLETED" },
  { label: "검토 필요", status: "NEEDS_REVIEW" },
  { label: "기록 없음", status: "NO_RECORD" }
];
const avatarTones = ["amber", "sky", "mint", "violet", "rose"] as const;

type SummaryTone = "success" | "warn" | "absent" | "active";
type AttendanceCellTone = SummaryTone;

type AttendanceDayCell = {
  date: string;
  label: string;
  tone: AttendanceCellTone;
};

type AttendanceRow = {
  id: number;
  name: string;
  role: string;
  avatarTone: (typeof avatarTones)[number];
  avatarUrl?: string;
  days: AttendanceDayCell[];
};

type AttendanceAvatarProps = {
  name: string;
  avatarTone: (typeof avatarTones)[number];
  avatarUrl?: string;
};

function AttendanceAvatar({ name, avatarTone, avatarUrl }: AttendanceAvatarProps) {
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

function getSummaryIcon(tone: AttendanceCellTone) {
  if (tone === "warn") {
    return <ClockIcon />;
  }

  if (tone === "absent") {
    return <MapPinIcon />;
  }

  return <CheckCircleIcon />;
}

function getCellIcon(tone: AttendanceCellTone) {
  if (tone === "warn") {
    return <ClockIcon />;
  }

  if (tone === "absent") {
    return <MapPinIcon />;
  }

  return <CheckCircleIcon />;
}

function startOfWeek(date: Date) {
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  d.setDate(d.getDate() - d.getDay());
  return d;
}

function shiftDays(date: Date, diff: number) {
  const d = new Date(date);
  d.setDate(d.getDate() + diff);
  return d;
}

function toIsoDate(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function parseIsoDate(value: string) {
  const [year, month, day] = value.split("-").map((unit) => Number(unit));
  if (!year || !month || !day) {
    return null;
  }

  const date = new Date(year, month - 1, day);
  return Number.isNaN(date.getTime()) ? null : date;
}

function formatWeekLabel(weekStartDate: Date) {
  const anchorDate = shiftDays(weekStartDate, 3);
  const year = anchorDate.getFullYear();
  const month = anchorDate.getMonth();
  const firstDayOfMonth = new Date(year, month, 1);
  const firstWeekStart = startOfWeek(firstDayOfMonth);
  const weekNumber =
    Math.floor((weekStartDate.getTime() - firstWeekStart.getTime()) / (7 * DAY_MS)) + 1;
  return `${year}년 ${month + 1}월 ${weekNumber}주차`;
}

function formatWorkedMinutes(minutes: number | null) {
  if (minutes == null) {
    return "근무 완료";
  }

  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;
  return `${hours}시간 ${String(remainingMinutes).padStart(2, "0")}분`;
}

function getDayTone(status: EmployerWorkerAttendanceStatus): AttendanceCellTone {
  if (status === "COMPLETED") {
    return "success";
  }

  if (status === "NEEDS_REVIEW") {
    return "warn";
  }

  if (status === "NO_RECORD") {
    return "absent";
  }

  return "active";
}

function getDayLabel(day: EmployerAttendanceBoardRowResponse["days"][number]) {
  if (day.attendanceStatus === "COMPLETED") {
    return formatWorkedMinutes(day.workedMinutes);
  }

  if (day.attendanceStatus === "NEEDS_REVIEW") {
    return "검토 필요";
  }

  if (day.attendanceStatus === "NO_RECORD") {
    return "기록 없음";
  }

  return "근무중";
}

function toAttendanceRow(row: EmployerAttendanceBoardRowResponse): AttendanceRow {
  return {
    id: row.workerId,
    name: row.name,
    role: row.role ?? "직무 기준 데이터 정리 예정",
    avatarTone: avatarTones[row.workerId % avatarTones.length],
    avatarUrl: row.avatarUrl ?? undefined,
    days: row.days.map((day) => ({
      date: day.date,
      label: getDayLabel(day),
      tone: getDayTone(day.attendanceStatus)
    }))
  };
}

function buildSummaryCards(summary: EmployerDashboardSummaryResponse | null) {
  if (!summary) {
    return [
      { title: "근무중", value: "-", helper: "요약을 불러오는 중", tone: "active" as const },
      { title: "근무 완료", value: "-", helper: "요약을 불러오는 중", tone: "success" as const },
      { title: "검토 필요", value: "-", helper: "요약을 불러오는 중", tone: "warn" as const },
      { title: "기록 없음", value: "-", helper: "요약을 불러오는 중", tone: "absent" as const }
    ];
  }

  return [
    {
      title: "근무중",
      value: String(summary.workingCount),
      helper: `활성 근로자 ${summary.activeWorkerCount}명`,
      tone: "active" as const
    },
    {
      title: "근무 완료",
      value: String(summary.completedCount),
      helper: `기준일 ${summary.asOf}`,
      tone: "success" as const
    },
    {
      title: "검토 필요",
      value: String(summary.needsReviewCount),
      helper: "검토할 기록",
      tone: "warn" as const
    },
    {
      title: "기록 없음",
      value: String(summary.noRecordCount),
      helper: "오늘 기록이 없는 근로자",
      tone: "absent" as const
    }
  ];
}

export function EmployerDashboardPage() {
  const navigate = useNavigate();
  const { refreshTick } = useOutletContext<{ refreshTick: number }>();
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedFilters, setSelectedFilters] = useState<EmployerWorkerAttendanceStatus[]>([]);
  const [visibleRowCount, setVisibleRowCount] = useState(INITIAL_VISIBLE_ROWS);
  const [isFilterMenuOpen, setIsFilterMenuOpen] = useState(false);
  const [isWeekMenuOpen, setIsWeekMenuOpen] = useState(false);
  const [selectedWeekStartIso, setSelectedWeekStartIso] = useState(() =>
    toIsoDate(startOfWeek(new Date()))
  );
  const [summary, setSummary] = useState<EmployerDashboardSummaryResponse | null>(null);
  const [rows, setRows] = useState<AttendanceRow[]>([]);
  const [totalRows, setTotalRows] = useState(0);
  const [hasMoreRows, setHasMoreRows] = useState(false);
  const [boardWeekStartIso, setBoardWeekStartIso] = useState(selectedWeekStartIso);
  const [summaryError, setSummaryError] = useState<string | null>(null);
  const [boardError, setBoardError] = useState<string | null>(null);
  const [summaryLoading, setSummaryLoading] = useState(false);
  const [boardLoading, setBoardLoading] = useState(false);
  const filterMenuRef = useRef<HTMLDivElement | null>(null);
  const weekMenuRef = useRef<HTMLDivElement | null>(null);

  const selectedWeekStartDate = useMemo(
    () => parseIsoDate(selectedWeekStartIso) ?? startOfWeek(new Date()),
    [selectedWeekStartIso]
  );

  const weekOptions = useMemo(
    () =>
      Array.from({ length: 17 }, (_, index) => {
        const weekStart = shiftDays(selectedWeekStartDate, (index - 8) * 7);
        return {
          value: toIsoDate(weekStart),
          label: formatWeekLabel(weekStart)
        };
      }),
    [selectedWeekStartDate]
  );

  const weekDates = useMemo(() => {
    const base = parseIsoDate(boardWeekStartIso) ?? selectedWeekStartDate;
    return Array.from({ length: attendanceWeekDays.length }, (_, index) => {
      const day = shiftDays(base, index);
      return String(day.getDate());
    });
  }, [boardWeekStartIso, selectedWeekStartDate]);

  const selectedWeekLabel = useMemo(
    () =>
      weekOptions.find((option) => option.value === selectedWeekStartIso)?.label ??
      formatWeekLabel(selectedWeekStartDate),
    [selectedWeekStartDate, selectedWeekStartIso, weekOptions]
  );

  useEffect(() => {
    const handleOutsideClick = (event: MouseEvent) => {
      const target = event.target;
      if (!(target instanceof Node)) {
        return;
      }

      if (filterMenuRef.current && !filterMenuRef.current.contains(target)) {
        setIsFilterMenuOpen(false);
      }

      if (weekMenuRef.current && !weekMenuRef.current.contains(target)) {
        setIsWeekMenuOpen(false);
      }
    };

    document.addEventListener("mousedown", handleOutsideClick);
    return () => {
      document.removeEventListener("mousedown", handleOutsideClick);
    };
  }, []);

  useEffect(() => {
    setVisibleRowCount(INITIAL_VISIBLE_ROWS);
  }, [searchQuery, selectedFilters, selectedWeekStartIso]);

  useEffect(() => {
    const token = getStoredAccessToken();
    if (!token) {
      clearStoredSession();
      navigate("/", { replace: true });
      return;
    }

    let isDisposed = false;
    setSummaryLoading(true);
    setSummaryError(null);

    void getEmployerDashboardSummary(token)
      .then((response) => {
        if (isDisposed) {
          return;
        }

        setSummary(response);
        setSummaryLoading(false);
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

        setSummaryLoading(false);
        setSummaryError(error instanceof Error ? error.message : "대시보드 요약을 불러오지 못했습니다.");
      });

    return () => {
      isDisposed = true;
    };
  }, [navigate, refreshTick]);

  useEffect(() => {
    const token = getStoredAccessToken();
    if (!token) {
      clearStoredSession();
      navigate("/", { replace: true });
      return;
    }

    let isDisposed = false;
    setBoardLoading(true);
    setBoardError(null);

    void getEmployerAttendanceBoard(token, {
      weekStart: selectedWeekStartIso,
      query: searchQuery.trim() || undefined,
      statuses: selectedFilters.length > 0 ? selectedFilters : undefined,
      page: 1,
      size: visibleRowCount
    })
      .then((response) => {
        if (isDisposed) {
          return;
        }

        setRows(response.rows.map(toAttendanceRow));
        setTotalRows(response.totalElements);
        setHasMoreRows(response.hasNext);
        setBoardWeekStartIso(response.weekStart);
        setBoardLoading(false);
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

        setBoardLoading(false);
        setBoardError(error instanceof Error ? error.message : "근무 현황을 불러오지 못했습니다.");
      });

    return () => {
      isDisposed = true;
    };
  }, [navigate, refreshTick, searchQuery, selectedFilters, selectedWeekStartIso, visibleRowCount]);

  const toggleFilter = (status: EmployerWorkerAttendanceStatus) => {
    setSelectedFilters((prev) =>
      prev.includes(status) ? prev.filter((item) => item !== status) : [...prev, status]
    );
  };

  const selectedFilterLabels = selectedFilters.map(
    (status) => dashboardStatusFilters.find((filter) => filter.status === status)?.label ?? status
  );
  const summaryCards = buildSummaryCards(summary);

  return (
    <div className="console-page dashboard-page">
      <header className="attendance-dashboard-header">
        <div>
          <h2 className="attendance-dashboard-title">근무자 출근 현황</h2>
          <p className="attendance-dashboard-subtitle">근무자의 출근 기록을 한눈에 확인하세요</p>
          {summaryError ? <p className="attendance-dashboard-subtitle">{summaryError}</p> : null}
          {boardError ? <p className="attendance-dashboard-subtitle">{boardError}</p> : null}
        </div>
        <Link to="/issues" className="attendance-dashboard-link-button">
          정정 요청 관리
        </Link>
      </header>

      <section className="attendance-summary-grid">
        {summaryCards.map((card) => (
          <article key={card.title} className={`attendance-summary-card ${card.tone}`}>
            <div className="attendance-summary-head">
              <span className={`attendance-summary-icon ${card.tone}`}>
                {getSummaryIcon(card.tone)}
              </span>
              <h3 className="attendance-summary-title">{card.title}</h3>
            </div>
            <p className="attendance-summary-value">
              {summaryLoading && card.value === "-" ? "..." : card.value}
            </p>
            <p className="attendance-summary-helper">{card.helper}</p>
          </article>
        ))}
      </section>

      <section className="attendance-board">
        <div className="attendance-toolbar">
          <label className="attendance-search">
            <span className="attendance-search-icon">
              <SearchIcon />
            </span>
            <input
              type="text"
              placeholder="검색어를 입력하세요..."
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.currentTarget.value)}
            />
          </label>
          <div className="attendance-toolbar-actions">
            <div className="attendance-filter-wrap" ref={filterMenuRef}>
              <button
                type="button"
                className="attendance-toolbar-button"
                onClick={() => {
                  setIsFilterMenuOpen((prev) => !prev);
                  setIsWeekMenuOpen(false);
                }}
              >
                <FilterIcon />
                {selectedFilters.length === 0 ? "필터 선택" : `필터 ${selectedFilters.length}개`}
              </button>
              {isFilterMenuOpen && (
                <div className="attendance-filter-menu">
                  {dashboardStatusFilters.map((filter) => (
                    <label key={filter.status} className="attendance-filter-option">
                      <input
                        type="checkbox"
                        checked={selectedFilters.includes(filter.status)}
                        onChange={() => toggleFilter(filter.status)}
                      />
                      <span>{filter.label}</span>
                    </label>
                  ))}
                </div>
              )}
            </div>

            <div className="attendance-week-selector" ref={weekMenuRef}>
              <button
                type="button"
                className={`attendance-week-trigger ${isWeekMenuOpen ? "is-open" : ""}`}
                aria-haspopup="listbox"
                aria-expanded={isWeekMenuOpen}
                onClick={() => {
                  setIsWeekMenuOpen((prev) => !prev);
                  setIsFilterMenuOpen(false);
                }}
              >
                <span className="attendance-week-trigger-icon">
                  <CalendarIcon />
                </span>
                <span>{selectedWeekLabel}</span>
                <span className="attendance-week-trigger-caret" aria-hidden="true">
                  <ChevronDownIcon />
                </span>
              </button>
              {isWeekMenuOpen && (
                <div className="attendance-week-menu" role="listbox">
                  {weekOptions.map((option) => {
                    const isSelected = option.value === selectedWeekStartIso;
                    return (
                      <button
                        key={option.value}
                        type="button"
                        role="option"
                        aria-selected={isSelected}
                        className={`attendance-week-option ${isSelected ? "is-selected" : ""}`}
                        onClick={() => {
                          setSelectedWeekStartIso(option.value);
                          setIsWeekMenuOpen(false);
                        }}
                      >
                        {option.label}
                      </button>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        </div>

        {selectedFilterLabels.length > 0 && (
          <div className="attendance-chip-row">
            {selectedFilters.map((filter, index) => (
              <button
                key={filter}
                type="button"
                className="attendance-chip"
                onClick={() => toggleFilter(filter)}
              >
                {selectedFilterLabels[index]}
                <span className="attendance-chip-close-icon" aria-hidden="true">
                  <CloseIcon />
                </span>
              </button>
            ))}
          </div>
        )}

        <div className="attendance-table-wrap">
          <table className="attendance-table">
            <thead>
              <tr>
                <th className="employee-col">근무자</th>
                {attendanceWeekDays.map((day) => (
                  <th key={day}>{day}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id}>
                  <td className="employee-cell">
                    <div className="employee-info">
                      <AttendanceAvatar
                        name={row.name}
                        avatarTone={row.avatarTone}
                        avatarUrl={row.avatarUrl}
                      />
                      <div>
                        <strong>{row.name}</strong>
                        <p>{row.role}</p>
                      </div>
                    </div>
                  </td>
                  {row.days.map((day, index) => (
                    <td key={`${row.id}-${day.date}`}>
                      <span className="attendance-date">{weekDates[index] ?? day.date}</span>
                      <span className={`attendance-pill ${day.tone}`}>
                        <span className="attendance-pill-icon">{getCellIcon(day.tone)}</span>
                        {day.label}
                      </span>
                    </td>
                  ))}
                </tr>
              ))}
              {!boardLoading && rows.length === 0 ? (
                <tr>
                  <td className="attendance-empty-row" colSpan={attendanceWeekDays.length + 1}>
                    검색/필터 조건에 맞는 근무자가 없습니다.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>

        {boardLoading ? <p className="attendance-dashboard-subtitle">근무 현황을 불러오는 중입니다...</p> : null}

        {hasMoreRows && (
          <div className="attendance-more-wrap">
            <button
              type="button"
              className="attendance-more-button"
              onClick={() => setVisibleRowCount((prev) => prev + ROW_BATCH_SIZE)}
            >
              더보기
              <span>{Math.max(totalRows - rows.length, 0)}명 남음</span>
            </button>
          </div>
        )}
      </section>
    </div>
  );
}
