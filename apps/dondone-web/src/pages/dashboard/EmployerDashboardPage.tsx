import { useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";
import {
  CalendarIcon,
  ChevronDownIcon,
  CheckCircleIcon,
  ClockIcon,
  CloseIcon,
  FilterIcon,
  LeaveIcon,
  MapPinIcon,
  SearchIcon,
  UserAvatarIcon
} from "../../shared/ui/icons";
import {
  attendanceFilters,
  attendanceRows,
  attendanceSummaryCards,
  attendanceWeekDays,
  type AttendanceCellTone
} from "./model/dashboardAttendanceData";

const DAY_MS = 24 * 60 * 60 * 1000;
const INITIAL_VISIBLE_ROWS = 5;
const ROW_BATCH_SIZE = 3;

function getSummaryIcon(tone: AttendanceCellTone) {
  if (tone === "warn") {
    return <ClockIcon />;
  }

  if (tone === "leave") {
    return <LeaveIcon />;
  }

  if (tone === "absent") {
    return <MapPinIcon />;
  }

  return <CheckCircleIcon />;
}

function getCellIcon(tone: AttendanceCellTone | undefined) {
  if (tone === "warn") {
    return <ClockIcon />;
  }

  if (tone === "leave") {
    return <LeaveIcon />;
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
  const weekNumber = Math.floor((weekStartDate.getTime() - firstWeekStart.getTime()) / (7 * DAY_MS)) + 1;
  return `${year}년 ${month + 1}월 ${weekNumber}주차`;
}

type AttendanceAvatarProps = {
  name: string;
  avatarTone: "amber" | "sky" | "mint" | "violet" | "rose";
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

export function EmployerDashboardPage() {
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedFilters, setSelectedFilters] = useState<string[]>([]);
  const [visibleRowCount, setVisibleRowCount] = useState(INITIAL_VISIBLE_ROWS);
  const [isFilterMenuOpen, setIsFilterMenuOpen] = useState(false);
  const [isWeekMenuOpen, setIsWeekMenuOpen] = useState(false);
  const [selectedWeekStartIso, setSelectedWeekStartIso] = useState(() =>
    toIsoDate(startOfWeek(new Date()))
  );
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

  const weekDates = useMemo(
    () =>
      Array.from({ length: attendanceWeekDays.length }, (_, index) => {
        const day = shiftDays(selectedWeekStartDate, index);
        return String(day.getDate());
      }),
    [selectedWeekStartDate]
  );

  const filterToneByLabel = useMemo(
    () =>
      ({
        휴가: "leave",
        결근: "absent",
        근무중: "active"
      }) as Record<string, AttendanceCellTone>,
    []
  );

  const selectedTones = useMemo(
    () =>
      new Set(
        selectedFilters
          .map((filter) => filterToneByLabel[filter])
          .filter((tone): tone is AttendanceCellTone => Boolean(tone))
      ),
    [filterToneByLabel, selectedFilters]
  );

  const filteredRows = useMemo(() => {
    const keyword = searchQuery.trim().toLowerCase();

    return attendanceRows.filter((row) => {
      const matchesKeyword =
        keyword.length === 0 ||
        row.name.toLowerCase().includes(keyword) ||
        row.role.toLowerCase().includes(keyword);

      const matchesTone =
        selectedTones.size === 0 ||
        row.days.some((day) => day.tone && selectedTones.has(day.tone));

      return matchesKeyword && matchesTone;
    });
  }, [searchQuery, selectedTones]);

  const toggleFilter = (filter: string) => {
    setSelectedFilters((prev) =>
      prev.includes(filter) ? prev.filter((item) => item !== filter) : [...prev, filter]
    );
  };

  const selectedWeekLabel = useMemo(
    () =>
      weekOptions.find((option) => option.value === selectedWeekStartIso)?.label ??
      formatWeekLabel(selectedWeekStartDate),
    [selectedWeekStartDate, selectedWeekStartIso, weekOptions]
  );

  const visibleRows = useMemo(
    () => filteredRows.slice(0, visibleRowCount),
    [filteredRows, visibleRowCount]
  );

  const hasMoreRows = filteredRows.length > visibleRowCount;
  const remainingRowCount = Math.max(filteredRows.length - visibleRowCount, 0);

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
  }, [searchQuery, selectedFilters]);

  return (
    <div className="console-page dashboard-page">
      <header className="attendance-dashboard-header">
        <div>
          <h2 className="attendance-dashboard-title">근무자 출근 현황</h2>
          <p className="attendance-dashboard-subtitle">근무자의 출근 기록을 한눈에 확인하세요</p>
        </div>
        <Link to="/issues" className="attendance-dashboard-link-button">
          정정 요청 관리
        </Link>
      </header>

      <section className="attendance-summary-grid">
        {attendanceSummaryCards.map((card) => (
          <article key={card.title} className={`attendance-summary-card ${card.tone}`}>
            <div className="attendance-summary-head">
              <span className={`attendance-summary-icon ${card.tone}`}>
                {getSummaryIcon(card.tone)}
              </span>
              <h3 className="attendance-summary-title">{card.title}</h3>
            </div>
            <p className="attendance-summary-value">{card.value}</p>
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
                {selectedFilters.length === 0
                  ? "필터 선택"
                  : `필터 ${selectedFilters.length}개`}
              </button>
              {isFilterMenuOpen && (
                <div className="attendance-filter-menu">
                  {attendanceFilters.map((filter) => (
                    <label key={filter} className="attendance-filter-option">
                      <input
                        type="checkbox"
                        checked={selectedFilters.includes(filter)}
                        onChange={() => toggleFilter(filter)}
                      />
                      <span>{filter}</span>
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

        {selectedFilters.length > 0 && (
          <div className="attendance-chip-row">
            {selectedFilters.map((filter) => (
              <button
                key={filter}
                type="button"
                className="attendance-chip"
                onClick={() => toggleFilter(filter)}
              >
                {filter}
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
              {visibleRows.map((row) => (
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
                    <td key={`${row.id}-${index}`} className={day.empty ? "is-empty" : undefined}>
                      <span className="attendance-date">{weekDates[index] || day.date}</span>
                      {day.label && (
                        <span className={`attendance-pill ${day.tone}`}>
                          <span className="attendance-pill-icon">{getCellIcon(day.tone)}</span>
                          {day.label}
                        </span>
                      )}
                    </td>
                  ))}
                </tr>
              ))}
              {filteredRows.length === 0 && (
                <tr>
                  <td className="attendance-empty-row" colSpan={attendanceWeekDays.length + 1}>
                    검색/필터 조건에 맞는 근무자가 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {hasMoreRows && (
          <div className="attendance-more-wrap">
            <button
              type="button"
              className="attendance-more-button"
              onClick={() => setVisibleRowCount((prev) => prev + ROW_BATCH_SIZE)}
            >
              더보기
              <span>{remainingRowCount}명 남음</span>
            </button>
          </div>
        )}
      </section>
    </div>
  );
}
