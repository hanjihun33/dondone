import { ReactNode, useEffect, useState } from "react";
import { Navigate, NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { ApiError } from "../shared/api/client";
import {
  getEmployerProfile,
  getEmployerWorkerRegistrationCodes,
  getEmployerWorkplaceSettings,
  issueEmployerWorkerRegistrationCode
} from "../shared/api/employer";
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

const adminNavItems: NavItem[] = [
  { icon: <ClipboardCheckIcon />, label: "미리받기", to: "/admin#advance" },
  { icon: <AdminShieldIcon />, label: "회사 관리", to: "/admin#companies" }
];

type SettingsSummaryState = {
  activeMembershipCount: number | null;
  workerRegistrationCode: string | null;
  workerRegistrationIssuedAt: string | null;
  loading: boolean;
  issuing: boolean;
  errorMessage: string | null;
};

const INITIAL_SETTINGS_SUMMARY_STATE: SettingsSummaryState = {
  activeMembershipCount: null,
  workerRegistrationCode: null,
  workerRegistrationIssuedAt: null,
  loading: false,
  issuing: false,
  errorMessage: null
};

function formatDateTime(value: string | null) {
  if (!value) {
    return null;
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  }).format(date);
}

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
  const [settingsSummary, setSettingsSummary] = useState<SettingsSummaryState>(
    INITIAL_SETTINGS_SUMMARY_STATE
  );

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

  useEffect(() => {
    if (session?.role !== "manager") {
      setSettingsSummary(INITIAL_SETTINGS_SUMMARY_STATE);
      return;
    }

    const token = getStoredAccessToken();
    if (!token) {
      return;
    }

    let isDisposed = false;
    setSettingsSummary((prev) => ({
      ...prev,
      loading: true,
      errorMessage: null
    }));

    void Promise.all([
      getEmployerWorkplaceSettings(token),
      getEmployerWorkerRegistrationCodes(token)
    ])
      .then(([workplaceSettings, registrationCodes]) => {
        if (isDisposed) {
          return;
        }

        const activeCode = registrationCodes.codes.find((code) => code.active) ?? null;

        setSettingsSummary({
          activeMembershipCount: workplaceSettings.activeMembershipCount,
          workerRegistrationCode: activeCode?.registrationCode ?? null,
          workerRegistrationIssuedAt: activeCode?.issuedAt ?? null,
          loading: false,
          issuing: false,
          errorMessage: null
        });
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

        setSettingsSummary((prev) => ({
          ...prev,
          loading: false,
          issuing: false,
          errorMessage:
            error instanceof Error ? error.message : "회사 설정 요약을 불러오지 못했습니다."
        }));
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
  const activeMembershipLabel =
    settingsSummary.activeMembershipCount == null
      ? settingsSummary.loading
        ? "불러오는 중"
        : "-"
      : `${settingsSummary.activeMembershipCount}명`;
  const workerRegistrationCodeLabel =
    settingsSummary.workerRegistrationCode ??
    (settingsSummary.loading ? "불러오는 중" : "발급된 코드 없음");
  const workerRegistrationIssuedLabel = formatDateTime(settingsSummary.workerRegistrationIssuedAt);
  const workerRegistrationActionLabel = settingsSummary.workerRegistrationCode
    ? "코드 재발급"
    : "코드 발급";

  const handleLogoutClick = () => {
    clearStoredSession();
    setIsHelpOpen(false);
    setIsSettingsOpen(false);
    setIsProfileOpen(false);
    navigate("/", { replace: true });
  };

  const handleIssueWorkerRegistrationCode = async () => {
    const token = getStoredAccessToken();
    if (!token) {
      clearStoredSession();
      navigate("/", { replace: true });
      return;
    }

    setSettingsSummary((prev) => ({
      ...prev,
      issuing: true,
      errorMessage: null
    }));

    try {
      const response = await issueEmployerWorkerRegistrationCode(token);
      setSettingsSummary((prev) => ({
        ...prev,
        workerRegistrationCode: response.registrationCode,
        workerRegistrationIssuedAt: response.issuedAt,
        issuing: false,
        errorMessage: null
      }));
    } catch (error: unknown) {
      if (error instanceof ApiError && error.status === 401) {
        clearStoredSession();
        navigate("/", { replace: true });
        return;
      }

      setSettingsSummary((prev) => ({
        ...prev,
        issuing: false,
        errorMessage:
          error instanceof Error ? error.message : "근로자 등록 코드를 발급하지 못했습니다."
      }));
    }
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
                const active =
                  userRole === "admin"
                    ? location.pathname === "/admin" &&
                      (location.hash || "#advance") === item.to.slice(item.to.indexOf("#"))
                    : isActive && location.pathname === item.to;
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
                  <strong>{activeMembershipLabel}</strong>
                </div>
                <div className="popover-setting-card">
                  <span>기본 사업장</span>
                  <strong>{workplaceName}</strong>
                </div>
                <div className="popover-setting-card">
                  <div className="popover-setting-card-header">
                    <div>
                      <span>근로자 등록 코드</span>
                      <strong>{workerRegistrationCodeLabel}</strong>
                    </div>
                    <button
                      type="button"
                      className="popover-action-btn"
                      onClick={handleIssueWorkerRegistrationCode}
                      disabled={settingsSummary.loading || settingsSummary.issuing}
                    >
                      {settingsSummary.issuing ? "처리 중" : workerRegistrationActionLabel}
                    </button>
                  </div>
                  <p className="popover-setting-note">
                    {workerRegistrationIssuedLabel
                      ? `발급 시각 ${workerRegistrationIssuedLabel}`
                      : "활성 코드가 없습니다."}
                  </p>
                  <p className="popover-setting-note">
                    재발급 시 기존 코드는 즉시 사용할 수 없습니다.
                  </p>
                  {settingsSummary.errorMessage ? (
                    <p className="popover-setting-feedback error">{settingsSummary.errorMessage}</p>
                  ) : null}
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
