import { ReactNode, useState } from "react";
import { Outlet, NavLink, useLocation } from "react-router-dom";

type NavItem = {
  icon: ReactNode;
  label: string;
  to?: string;
};

const navItems: NavItem[] = [
  { icon: <DashboardIcon />, label: "대시보드", to: "/" },
  { icon: <UsersIcon />, label: "근로자 목록", to: "/workers" },
  { icon: <AlertIcon />, label: "정산 이슈", to: "/issues" },
  { icon: <WalletIcon />, label: "선지급 확인" },
  { icon: <RefreshIcon />, label: "확인 요청 관리" },
  { icon: <ShieldCheckIcon />, label: "설정" },
];

function IconBase({ children }: { children: ReactNode }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">
      {children}
    </svg>
  );
}

function DashboardIcon() {
  return (
    <IconBase>
      <rect x="3" y="3" width="8" height="8" rx="2" />
      <rect x="13" y="3" width="8" height="5" rx="2" />
      <rect x="13" y="10" width="8" height="11" rx="2" />
      <rect x="3" y="13" width="8" height="8" rx="2" />
    </IconBase>
  );
}

function ClockIcon() {
  return (
    <IconBase>
      <circle cx="12" cy="12" r="8" />
      <path d="M12 7.5v5l3 2" />
    </IconBase>
  );
}

function InboxIcon() {
  return (
    <IconBase>
      <path d="M4 7h16v10a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7Z" />
      <path d="M4 13h4l2 3h4l2-3h4" />
    </IconBase>
  );
}

function UsersIcon() {
  return (
    <IconBase>
      <circle cx="9" cy="8" r="3" />
      <path d="M4 19a5 5 0 0 1 10 0" />
      <path d="M16 11a3 3 0 1 0 0-6" />
      <path d="M20 19a4 4 0 0 0-4-4" />
    </IconBase>
  );
}

function WalletIcon() {
  return (
    <IconBase>
      <path d="M4 8.5A2.5 2.5 0 0 1 6.5 6H19a2 2 0 0 1 2 2v2.5H6.5A2.5 2.5 0 0 0 4 13Z" />
      <path d="M4 13v4a2 2 0 0 0 2 2h15v-8.5H6.5A2.5 2.5 0 0 0 4 13Z" />
      <circle cx="17" cy="14.75" r="1" fill="currentColor" stroke="none" />
    </IconBase>
  );
}

function AlertIcon() {
  return (
    <IconBase>
      <path d="M12 3 2.8 19h18.4L12 3Z" />
      <path d="M12 9v4.5" />
      <circle cx="12" cy="17" r="0.8" fill="currentColor" stroke="none" />
    </IconBase>
  );
}

function RefreshIcon() {
  return (
    <IconBase>
      <path d="M20 6v5h-5" />
      <path d="M4 18v-5h5" />
      <path d="M6.7 9A7 7 0 0 1 18 11" />
      <path d="M17.3 15A7 7 0 0 1 6 13" />
    </IconBase>
  );
}

function SummaryIcon() {
  return (
    <IconBase>
      <path d="M6 7h12" />
      <path d="M6 12h12" />
      <path d="M6 17h8" />
      <circle cx="18" cy="17" r="1.2" fill="currentColor" stroke="none" />
    </IconBase>
  );
}

function ShieldCheckIcon() {
  return (
    <IconBase>
      <path d="M12 3 6 5.5v5.3c0 4.1 2.5 7.9 6 9.2 3.5-1.3 6-5.1 6-9.2V5.5L12 3Z" />
      <path d="m9.5 12 1.8 1.8 3.4-3.8" />
    </IconBase>
  );
}

export function AppShell() {
  const location = useLocation();
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(true);
  const [isHelpOpen, setIsHelpOpen] = useState(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [refreshTick, setRefreshTick] = useState(0);

  return (
    <div className={`page-shell${isSidebarCollapsed ? " sidebar-collapsed" : ""}`}>
      {/* ── Sidebar ── */}
      <aside className="sidebar">
        <div className="sidebar-brand">
          <button
            type="button"
            className="sidebar-toggle-btn"
            aria-label={isSidebarCollapsed ? "사이드바 펼치기" : "사이드바 접기"}
            onClick={() => setIsSidebarCollapsed((prev) => !prev)}
          >
            <span />
            <span />
            <span />
          </button>
          <div className="brand-wordmark" aria-label="DonDone">
            <span className="brand-wordmark-don">Don</span>
            <span className="brand-wordmark-done">Done</span>
          </div>
        </div>

        <nav className="nav-group">
          {navItems.map((item, idx) => {
            if (!item.to) {
              return (
                <button
                  key={`${item.label}-${idx}`}
                  type="button"
                  className="nav-item"
                  aria-label={item.label}
                >
                  <span className="nav-item-icon" aria-hidden="true">{item.icon}</span>
                  <span className="nav-item-label">{item.label}</span>
                </button>
              );
            }

            return (
              <NavLink
                key={`${item.label}-${idx}`}
                to={item.to}
                className={({ isActive }) => {
                  const active =
                    isActive &&
                    location.pathname === item.to;
                  return `nav-item${active ? " active" : ""}`;
                }}
                title={isSidebarCollapsed ? item.label : undefined}
              >
                <span className="nav-item-icon" aria-hidden="true">{item.icon}</span>
                <span className="nav-item-label">{item.label}</span>
              </NavLink>
            );
          })}
        </nav>

        <div className="sidebar-bottom">
          <section className="company-code-card">
            <div className="label">회사 확인 코드</div>
            <div className="company-code">DN-SEOUL-2914</div>
            <div className="company-code-meta">
              연결된 근로자 24명 · 마지막 확인 09:18
            </div>
          </section>
        </div>
      </aside>

      {/* ── Header Bar ── */}
      <header className="header-bar">
        <div className="header-left">
          <div className={`header-wordmark${isSidebarCollapsed ? " visible" : ""}`} aria-hidden={!isSidebarCollapsed}>
            <span className="brand-wordmark-don">Don</span>
            <span className="brand-wordmark-done">Done</span>
          </div>
        </div>
        <div className="header-right">
          <button
            className="header-icon-btn"
            type="button"
            aria-label="새로고침"
            onClick={() => setRefreshTick((prev) => prev + 1)}
          >
            ↻
          </button>
          <button
            className="header-help-btn"
            type="button"
            aria-label="도움말"
            onClick={() => {
              setIsHelpOpen((prev) => !prev);
              setIsSettingsOpen(false);
            }}
          >
            ?
          </button>
          <button
            className="header-icon-btn"
            type="button"
            aria-label="설정"
            onClick={() => {
              setIsSettingsOpen((prev) => !prev);
              setIsHelpOpen(false);
            }}
          >
            ⚙
          </button>
        </div>
      </header>

      {/* ── Main Content ── */}
      <main className="content">
        <Outlet context={{ refreshTick }} />
      </main>

      {(isHelpOpen || isSettingsOpen) ? (
        <div
          className="header-popover-backdrop"
          onClick={() => {
            setIsHelpOpen(false);
            setIsSettingsOpen(false);
          }}
        >
          <aside
            className="header-popover-panel"
            onClick={(event) => event.stopPropagation()}
          >
            {isHelpOpen ? (
              <>
                <div className="popover-kicker">도움말</div>
                <h3 className="popover-title">이 화면에서 할 수 있는 일</h3>
                <ul className="popover-list">
                  <li>오늘 출퇴근 현황과 요청 관리 상태를 한 번에 확인합니다.</li>
                  <li>근무 수정, 휴가 생성, 위치 불일치 같은 이슈를 검토합니다.</li>
                  <li>Verified Worker Summary와 회사 확인 코드는 운영 보조 신호로만 사용합니다.</li>
                </ul>
              </>
            ) : null}

            {isSettingsOpen ? (
              <>
                <div className="popover-kicker">설정</div>
                <h3 className="popover-title">회사 설정 요약</h3>
                <div className="popover-setting-card">
                  <span>회사 확인 코드</span>
                  <strong>DN-SEOUL-2914</strong>
                </div>
                <div className="popover-setting-card">
                  <span>연결 근로자</span>
                  <strong>24명</strong>
                </div>
                <div className="popover-setting-card">
                  <span>기본 사업장</span>
                  <strong>서울본사</strong>
                </div>
              </>
            ) : null}
          </aside>
        </div>
      ) : null}
    </div>
  );
}
