import { ReactNode, useEffect, useState } from "react";
import { Navigate, NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { ApiError } from "../shared/api/client";
import { getEmployerProfile } from "../shared/api/employer";
import {
  clearStoredSession,
  getStoredAccessToken,
  getStoredSession,
  subscribeUserRoleChange,
  updateStoredEmployerProfile
} from "../shared/auth/session";
import {
  AdminShieldIcon,
  ClipboardCheckIcon,
  DashboardIcon,
  SettingsIcon,
  UserAvatarIcon,
  UsersIcon
} from "../shared/ui/icons";

type NavItem = {
  icon: ReactNode;
  label: string;
  to: string;
};

const managerNavItems: NavItem[] = [
  { icon: <DashboardIcon />, label: "대시보드", to: "/dashboard" },
  { icon: <UsersIcon />, label: "근로자 목록", to: "/workers" },
  { icon: <ClipboardCheckIcon />, label: "요청 관리", to: "/issues" },
  { icon: <SettingsIcon />, label: "설정", to: "/settings" }
];

const adminNavItems: NavItem[] = [{ icon: <AdminShieldIcon />, label: "관리자", to: "/admin" }];

export function AppShell() {
  const location = useLocation();
  const navigate = useNavigate();
  const [session, setSession] = useState(() => getStoredSession());
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(true);
  const [isHelpOpen, setIsHelpOpen] = useState(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [isProfileOpen, setIsProfileOpen] = useState(false);
  const [refreshTick, setRefreshTick] = useState(0);
  const [profileError, setProfileError] = useState<string | null>(null);

  useEffect(() => {
    return subscribeUserRoleChange(() => {
      setSession(getStoredSession());
    });
  }, []);

  useEffect(() => {
    if (session?.role !== "manager") {
      setProfileError(null);
      return;
    }

    const token = getStoredAccessToken();
    if (!token) {
      return;
    }

    let isDisposed = false;

    void getEmployerProfile(token)
      .then((profile) => {
        if (isDisposed) {
          return;
        }

        updateStoredEmployerProfile(profile);
        setProfileError(null);
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

        setProfileError(error instanceof Error ? error.message : "프로필을 불러오지 못했습니다.");
      });

    return () => {
      isDisposed = true;
    };
  }, [navigate, refreshTick, session?.role]);

  if (session === null) {
    return <Navigate to="/" replace />;
  }

  const userRole = session.role;
  const navItems = userRole === "admin" ? adminNavItems : managerNavItems;
  const employerSession = session.role === "manager" ? session : null;
  const companyName = employerSession?.profile?.companyName ?? employerSession?.scope.companyName ?? "-";
  const companyCode = employerSession?.profile?.companyCode ?? "확인 불가";
  const workplaceName =
    employerSession?.profile?.defaultWorkplaceName ??
    employerSession?.scope.defaultWorkplaceName ??
    "-";
  const displayName = employerSession?.profile?.displayName ?? "운영 담당자";
  const email = employerSession?.profile?.email ?? "-";
  const status = employerSession?.profile?.status ?? "ACTIVE";

  const handleLogoutClick = () => {
    clearStoredSession();
    setIsHelpOpen(false);
    setIsSettingsOpen(false);
    setIsProfileOpen(false);
    navigate("/", { replace: true });
  };

  return (
    <div className={`page-shell${isSidebarCollapsed ? " sidebar-collapsed" : ""}`}>
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
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => {
                const active = isActive && location.pathname === item.to;
                return `nav-item${active ? " active" : ""}`;
              }}
              title={isSidebarCollapsed ? item.label : undefined}
            >
              <span className="nav-item-icon" aria-hidden="true">
                {item.icon}
              </span>
              <span className="nav-item-label">{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-bottom">
          <section className="company-code-card">
            <div className="label">회사 확인 코드</div>
            <div className="company-code">{companyCode}</div>
            <div className="company-code-meta">
              {companyName} / {workplaceName}
            </div>
          </section>
        </div>
      </aside>

      <header className="header-bar">
        <div className="header-left">
          <div
            className={`header-wordmark${isSidebarCollapsed ? " visible" : ""}`}
            aria-hidden={!isSidebarCollapsed}
          >
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
              setIsProfileOpen(false);
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
              setIsProfileOpen(false);
            }}
          >
            ⚙
          </button>
          <button
            className="header-profile-btn"
            type="button"
            aria-label="내 프로필"
            aria-expanded={isProfileOpen}
            onClick={() => {
              setIsProfileOpen((prev) => !prev);
              setIsHelpOpen(false);
              setIsSettingsOpen(false);
            }}
          >
            <span className="header-profile-avatar" aria-hidden="true">
              <UserAvatarIcon />
            </span>
            <span className="header-profile-name">
              {userRole === "admin" ? "관리자" : displayName}
            </span>
          </button>
        </div>
      </header>

      <main className="content">
        <Outlet context={{ refreshTick }} />
      </main>

      {isHelpOpen || isSettingsOpen || isProfileOpen ? (
        <div
          className="header-popover-backdrop"
          onClick={() => {
            setIsHelpOpen(false);
            setIsSettingsOpen(false);
            setIsProfileOpen(false);
          }}
        >
          <aside className="header-popover-panel" onClick={(event) => event.stopPropagation()}>
            {isHelpOpen ? (
              <>
                <div className="popover-kicker">도움말</div>
                <h3 className="popover-title">이 화면에서 할 수 있는 일</h3>
                <ul className="popover-list">
                  <li>오늘 출퇴근 현황과 요청 관리 상태를 한 번에 확인합니다.</li>
                  <li>근무 수정과 검토 필요 이슈를 확인하고 처리합니다.</li>
                  <li>회사 확인 코드는 운영 보조 신호로만 사용합니다.</li>
                </ul>
              </>
            ) : null}

            {isSettingsOpen ? (
              <>
                <div className="popover-kicker">설정</div>
                <h3 className="popover-title">회사 설정 요약</h3>
                <div className="popover-setting-card">
                  <span>회사 확인 코드</span>
                  <strong>{companyCode}</strong>
                </div>
                <div className="popover-setting-card">
                  <span>연결 근로자</span>
                  <strong>-</strong>
                </div>
                <div className="popover-setting-card">
                  <span>기본 사업장</span>
                  <strong>{workplaceName}</strong>
                </div>
              </>
            ) : null}

            {isProfileOpen ? (
              <>
                <div className="popover-kicker">내 프로필</div>
                <h3 className="popover-title">계정 정보</h3>
                <div className="popover-setting-card">
                  <span>이름</span>
                  <strong>{userRole === "admin" ? "관리자" : displayName}</strong>
                </div>
                <div className="popover-setting-card">
                  <span>이메일</span>
                  <strong>{userRole === "admin" ? "admin@dondone.local" : email}</strong>
                </div>
                <div className="popover-setting-card">
                  <span>권한</span>
                  <strong>{userRole === "admin" ? "관리자" : "운영 담당자"}</strong>
                </div>
                {userRole === "manager" ? (
                  <div className="popover-setting-card">
                    <span>상태</span>
                    <strong>{status}</strong>
                  </div>
                ) : null}
                {profileError ? (
                  <div className="popover-setting-card">
                    <span>동기화 상태</span>
                    <strong>{profileError}</strong>
                  </div>
                ) : null}
                <button type="button" className="popover-logout-btn" onClick={handleLogoutClick}>
                  로그아웃
                </button>
              </>
            ) : null}
          </aside>
        </div>
      ) : null}
    </div>
  );
}
