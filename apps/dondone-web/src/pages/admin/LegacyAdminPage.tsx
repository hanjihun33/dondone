import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";

type AdminAccount = {
  id: number;
  name: string;
  email: string;
  role: string;
  lastLogin: string;
  status: "활성" | "대기";
};

type AdminActivity = {
  id: number;
  time: string;
  actor: string;
  description: string;
};

type AdvanceRequestStatus = "대기" | "승인" | "반려";

type AdvanceRequest = {
  id: number;
  workerName: string;
  company: string;
  requestedAmount: string;
  monthlySettlement: string;
  requestedAt: string;
  reason: string;
  eligibilityNote: string;
  status: AdvanceRequestStatus;
};

type RequestFilter = "전체" | AdvanceRequestStatus;

const REQUEST_FILTERS: RequestFilter[] = ["전체", "대기", "승인", "반려"];

const REQUEST_STATUS_CLASSNAME: Record<AdvanceRequestStatus, "pending" | "active" | "rejected"> = {
  대기: "pending",
  승인: "active",
  반려: "rejected"
};

const ACCOUNT_STATUS_CLASSNAME: Record<AdminAccount["status"], "active" | "pending"> = {
  활성: "active",
  대기: "pending"
};

const adminSummary = [
  { label: "활성 회사", value: "12개", helper: "오늘 +1" },
  { label: "관리자 계정", value: "28명", helper: "활성 24명" },
  { label: "신규 가입 신청", value: "5건", helper: "승인 대기" },
  { label: "보안 알림", value: "2건", helper: "확인 필요" }
];

const adminAccounts: AdminAccount[] = [
  {
    id: 1,
    name: "김운영",
    email: "admin@dondone.local",
    role: "슈퍼 관리자",
    lastLogin: "오늘 09:12",
    status: "활성"
  },
  {
    id: 2,
    name: "박매니저",
    email: "ops@dondone.local",
    role: "운영 관리자",
    lastLogin: "오늘 08:43",
    status: "활성"
  },
  {
    id: 3,
    name: "이감사",
    email: "audit@dondone.local",
    role: "감사 담당",
    lastLogin: "어제 18:27",
    status: "대기"
  }
];

const adminActivities: AdminActivity[] = [
  {
    id: 1,
    time: "오늘 09:21",
    actor: "김운영",
    description: "회사 코드 DN-SEOUL-2914 설정 변경"
  },
  {
    id: 2,
    time: "오늘 09:05",
    actor: "박매니저",
    description: "정정 요청 3건 일괄 승인"
  },
  {
    id: 3,
    time: "오늘 08:41",
    actor: "시스템",
    description: "로그인 실패 4회 감지 및 보안 알림 생성"
  }
];

const initialAdvanceRequests: AdvanceRequest[] = [
  {
    id: 1,
    workerName: "최민수",
    company: "DN-SEOUL-2914",
    requestedAmount: "180,000원",
    monthlySettlement: "1,920,000원",
    requestedAt: "오늘 10:04",
    reason: "급여일 전 긴급 생활비",
    eligibilityNote: "최근 4주 출근률 98%, 결근 없음",
    status: "대기"
  },
  {
    id: 2,
    workerName: "한지우",
    company: "DN-SEOUL-2914",
    requestedAmount: "120,000원",
    monthlySettlement: "1,540,000원",
    requestedAt: "오늘 09:38",
    reason: "병원 진료비 선결제",
    eligibilityNote: "최근 4주 출근률 95%, 지각 1회",
    status: "대기"
  },
  {
    id: 3,
    workerName: "이수현",
    company: "DN-BUSAN-6602",
    requestedAmount: "90,000원",
    monthlySettlement: "1,210,000원",
    requestedAt: "어제 18:12",
    reason: "교통비 선지급 요청",
    eligibilityNote: "최근 4주 출근률 100%",
    status: "승인"
  },
  {
    id: 4,
    workerName: "오지훈",
    company: "DN-INCHEON-4301",
    requestedAmount: "210,000원",
    monthlySettlement: "1,350,000원",
    requestedAt: "어제 16:42",
    reason: "개인 사유",
    eligibilityNote: "최근 4주 결근 2회로 정책 기준 미달",
    status: "반려"
  }
];

export function LegacyAdminPage() {
  const [requestFilter, setRequestFilter] = useState<RequestFilter>("전체");
  const [advanceRequests, setAdvanceRequests] = useState<AdvanceRequest[]>(initialAdvanceRequests);
  const [feedbackMessage, setFeedbackMessage] = useState<string | null>(null);

  const requestCountByStatus = useMemo(() => {
    const counts: Record<RequestFilter, number> = {
      전체: advanceRequests.length,
      대기: 0,
      승인: 0,
      반려: 0
    };

    advanceRequests.forEach((request) => {
      counts[request.status] += 1;
    });

    return counts;
  }, [advanceRequests]);

  const filteredAdvanceRequests = useMemo(() => {
    return requestFilter === "전체"
      ? advanceRequests
      : advanceRequests.filter((request) => request.status === requestFilter);
  }, [advanceRequests, requestFilter]);

  const updateRequestStatus = (requestId: number, nextStatus: AdvanceRequestStatus) => {
    setAdvanceRequests((prev) =>
      prev.map((request) =>
        request.id === requestId
          ? {
              ...request,
              status: nextStatus
            }
          : request
      )
    );
  };

  const handleRequestAction = (request: AdvanceRequest, nextStatus: AdvanceRequestStatus) => {
    const actionLabel = nextStatus === "승인" ? "승인" : "반려";
    const confirmed = window.confirm(
      `${request.workerName}님의 미리받기 요청을 ${actionLabel}하시겠습니까?`
    );

    if (!confirmed) {
      return;
    }

    updateRequestStatus(request.id, nextStatus);
    setFeedbackMessage(
      `${request.workerName}님의 요청을 ${nextStatus === "승인" ? "승인" : "반려"} 처리했습니다.`
    );
  };

  useEffect(() => {
    if (!feedbackMessage) {
      return undefined;
    }

    const timer = window.setTimeout(() => {
      setFeedbackMessage(null);
    }, 2400);

    return () => window.clearTimeout(timer);
  }, [feedbackMessage]);

  return (
    <div className="console-page admin-page">
      {feedbackMessage ? (
        <div className="admin-toast" role="status" aria-live="polite">
          {feedbackMessage}
        </div>
      ) : null}

      <header className="admin-header">
        <div>
          <h2 className="admin-title">레거시 관리자 목업</h2>
          <p className="admin-subtitle">
            프론트에만 있던 기존 관리자 콘솔 화면입니다. 현재 운영 기능은{" "}
            <Link to="/admin" className="logged-out-signup-link">
              새 관리자 온보딩 화면
            </Link>
            에서 확인하세요.
          </p>
        </div>
        <Link to="/admin" className="admin-add-button">
          새 관리자 화면 보기
        </Link>
      </header>

      <section className="admin-summary-grid">
        {adminSummary.map((item) => (
          <article key={item.label} className="admin-summary-card">
            <p className="admin-summary-label">{item.label}</p>
            <p className="admin-summary-value">{item.value}</p>
            <p className="admin-summary-helper">{item.helper}</p>
          </article>
        ))}
      </section>

      <section className="admin-section">
        <div className="admin-section-head with-sub">
          <div>
            <h3>미리받기 승인 요청</h3>
            <p className="admin-section-sub">
              근로자의 미리받기 신청을 검토하고 승인 또는 반려 처리합니다.
            </p>
          </div>
        </div>

        <div className="admin-request-toolbar">
          <div className="admin-request-tabs">
            {REQUEST_FILTERS.map((filter) => (
              <button
                key={filter}
                type="button"
                className={`admin-request-tab ${requestFilter === filter ? "active" : ""}`}
                onClick={() => setRequestFilter(filter)}
              >
                {filter}
                <span>{requestCountByStatus[filter]}</span>
              </button>
            ))}
          </div>
          <p className="admin-request-count">총 {filteredAdvanceRequests.length}건</p>
        </div>

        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>근로자</th>
                <th>요청 금액</th>
                <th>예상 정산액</th>
                <th>신청 시각</th>
                <th>사유</th>
                <th>승인 기준</th>
                <th>상태</th>
                <th>처리</th>
              </tr>
            </thead>
            <tbody>
              {filteredAdvanceRequests.map((request) => (
                <tr key={request.id}>
                  <td>
                    <strong>{request.workerName}</strong>
                    <p className="admin-cell-sub">{request.company}</p>
                  </td>
                  <td>{request.requestedAmount}</td>
                  <td>{request.monthlySettlement}</td>
                  <td>{request.requestedAt}</td>
                  <td>{request.reason}</td>
                  <td>{request.eligibilityNote}</td>
                  <td>
                    <span className={`admin-status ${REQUEST_STATUS_CLASSNAME[request.status]}`}>
                      {request.status}
                    </span>
                  </td>
                  <td>
                    {request.status === "대기" ? (
                      <div className="admin-action-group">
                        <button
                          type="button"
                          className="admin-action-button approve"
                          onClick={() => handleRequestAction(request, "승인")}
                        >
                          승인
                        </button>
                        <button
                          type="button"
                          className="admin-action-button reject"
                          onClick={() => handleRequestAction(request, "반려")}
                        >
                          반려
                        </button>
                      </div>
                    ) : (
                      <span className="admin-action-done">처리 완료</span>
                    )}
                  </td>
                </tr>
              ))}
              {filteredAdvanceRequests.length === 0 && (
                <tr>
                  <td colSpan={8} className="admin-empty-cell">
                    해당 상태의 미리받기 요청이 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>

      <section className="admin-section">
        <div className="admin-section-head">
          <h3>관리자 계정</h3>
        </div>
        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>이름</th>
                <th>이메일</th>
                <th>권한</th>
                <th>최근 로그인</th>
                <th>상태</th>
                <th>관리</th>
              </tr>
            </thead>
            <tbody>
              {adminAccounts.map((account) => (
                <tr key={account.id}>
                  <td>{account.name}</td>
                  <td>{account.email}</td>
                  <td>{account.role}</td>
                  <td>{account.lastLogin}</td>
                  <td>
                    <span className={`admin-status ${ACCOUNT_STATUS_CLASSNAME[account.status]}`}>
                      {account.status}
                    </span>
                  </td>
                  <td>
                    <button type="button" className="admin-action-button">
                      권한 변경
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="admin-section">
        <div className="admin-section-head">
          <h3>최근 활동 로그</h3>
        </div>
        <ul className="admin-activity-list">
          {adminActivities.map((activity) => (
            <li key={activity.id} className="admin-activity-item">
              <span className="admin-activity-time">{activity.time}</span>
              <div>
                <strong>{activity.actor}</strong>
                <p>{activity.description}</p>
              </div>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
