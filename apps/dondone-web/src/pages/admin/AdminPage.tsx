import { FormEvent, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  AdminEmployerCompanyEmployerSummaryResponse,
  AdminEmployerCompanyCreatedResponse,
  AdminEmployerCompanySummaryResponse,
  createAdminEmployerCompany,
  getAdminEmployerCompanyEmployers,
  getAdminEmployerCompanies,
  getAdminEmployerSignupCode
} from "../../shared/api/admin";
import { ApiError } from "../../shared/api/client";
import { clearStoredSession, getStoredAccessToken } from "../../shared/auth/session";

type AdminCompanyFormState = {
  companyName: string;
  companyCode: string;
};

const INITIAL_FORM_STATE: AdminCompanyFormState = {
  companyName: "",
  companyCode: ""
};

const CREATE_FIELDS: Array<{
  key: keyof AdminCompanyFormState;
  label: string;
  placeholder: string;
}> = [
  {
    key: "companyName",
    label: "회사명",
    placeholder: "예: 돈던 물류"
  },
  {
    key: "companyCode",
    label: "회사 코드",
    placeholder: "예: DN-SEOUL-2914"
  }
];

function formatDateTime(value: string | null) {
  if (!value) {
    return "-";
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

export function AdminPage() {
  const navigate = useNavigate();
  const [formState, setFormState] = useState<AdminCompanyFormState>(INITIAL_FORM_STATE);
  const [companies, setCompanies] = useState<AdminEmployerCompanySummaryResponse[]>([]);
  const [loadState, setLoadState] = useState<"loading" | "success" | "error">("loading");
  const [submitState, setSubmitState] = useState<"idle" | "submitting">("idle");
  const [pageError, setPageError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [lastIssuedCode, setLastIssuedCode] =
    useState<AdminEmployerCompanyCreatedResponse | null>(null);
  const [revealedCode, setRevealedCode] = useState<{
    companyId: number;
    companyName: string;
    employerSignupCode: string;
    issuedAt: string;
  } | null>(null);
  const [revealingCompanyId, setRevealingCompanyId] = useState<number | null>(null);
  const [selectedCompany, setSelectedCompany] = useState<{
    companyId: number;
    companyName: string;
  } | null>(null);
  const [companyEmployers, setCompanyEmployers] = useState<
    AdminEmployerCompanyEmployerSummaryResponse[]
  >([]);
  const [detailState, setDetailState] = useState<"idle" | "loading" | "success" | "error">("idle");
  const [detailError, setDetailError] = useState<string | null>(null);

  useEffect(() => {
    const token = getStoredAccessToken();
    if (!token) {
      clearStoredSession();
      navigate("/", { replace: true });
      return;
    }

    let disposed = false;

    void getAdminEmployerCompanies(token)
      .then((response) => {
        if (disposed) {
          return;
        }

        setCompanies(response.companies);
        setLoadState("success");
        setPageError(null);
      })
      .catch((error: unknown) => {
        if (disposed) {
          return;
        }

        if (error instanceof ApiError && error.status === 401) {
          clearStoredSession();
          navigate("/", { replace: true });
          return;
        }

        setPageError(error instanceof Error ? error.message : "회사 목록을 불러오지 못했어요.");
        setLoadState("error");
      });

    return () => {
      disposed = true;
    };
  }, [navigate]);

  const handleFieldChange = (key: keyof AdminCompanyFormState, value: string) => {
    setFormState((prev) => ({
      ...prev,
      [key]: value
    }));
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const token = getStoredAccessToken();
    if (!token) {
      clearStoredSession();
      navigate("/", { replace: true });
      return;
    }

    setSubmitState("submitting");
    setFormError(null);

    try {
      const created = await createAdminEmployerCompany(token, {
        companyName: formState.companyName.trim(),
        companyCode: formState.companyCode.trim()
      });

      setLastIssuedCode(created);
      setCompanies((prev) => [
        {
          companyId: created.companyId,
          companyName: created.companyName,
          companyCode: created.companyCode,
          defaultWorkplaceId: created.defaultWorkplaceId,
          defaultWorkplaceName: created.defaultWorkplaceName,
          address: created.address,
          detailAddress: created.detailAddress,
          latitude: created.latitude,
          longitude: created.longitude,
          allowedRadiusMeters: created.allowedRadiusMeters,
          workplaceSettingsConfigured: created.workplaceSettingsConfigured,
          hasJoinedEmployer: created.hasJoinedEmployer,
          employerCount: created.employerCount,
          latestEmployerJoinedAt: created.latestEmployerJoinedAt,
          hasActiveEmployerSignupCode: true,
          latestEmployerSignupCodeIssuedAt: created.employerSignupCodeIssuedAt,
          createdAt: created.createdAt
        },
        ...prev
      ]);
      setFormState(INITIAL_FORM_STATE);
      setLoadState("success");
    } catch (error: unknown) {
      if (error instanceof ApiError && error.status === 401) {
        clearStoredSession();
        navigate("/", { replace: true });
        return;
      }

      if (error instanceof ApiError) {
        setFormError(error.message || error.code);
      } else if (error instanceof Error) {
        setFormError(error.message);
      } else {
        setFormError("회사 생성에 실패했어요.");
      }
    } finally {
      setSubmitState("idle");
    }
  };

  const handleRevealCode = async (company: AdminEmployerCompanySummaryResponse) => {
    const token = getStoredAccessToken();
    if (!token) {
      clearStoredSession();
      navigate("/", { replace: true });
      return;
    }

    setRevealingCompanyId(company.companyId);
    setFormError(null);

    try {
      const response = await getAdminEmployerSignupCode(token, company.companyId);
      setRevealedCode({
        companyId: response.companyId,
        companyName: response.companyName,
        employerSignupCode: response.employerSignupCode,
        issuedAt: response.issuedAt
      });
    } catch (error: unknown) {
      if (error instanceof ApiError && error.status === 401) {
        clearStoredSession();
        navigate("/", { replace: true });
        return;
      }

      if (error instanceof ApiError) {
        setFormError(error.message || error.code);
      } else if (error instanceof Error) {
        setFormError(error.message);
      } else {
        setFormError("고용주 가입 코드를 불러오지 못했어요.");
      }
    } finally {
      setRevealingCompanyId(null);
    }
  };

  const handleToggleEmployerDetail = async (company: AdminEmployerCompanySummaryResponse) => {
    if (selectedCompany?.companyId === company.companyId) {
      setSelectedCompany(null);
      setCompanyEmployers([]);
      setDetailError(null);
      setDetailState("idle");
      return;
    }

    const token = getStoredAccessToken();
    if (!token) {
      clearStoredSession();
      navigate("/", { replace: true });
      return;
    }

    setSelectedCompany({
      companyId: company.companyId,
      companyName: company.companyName
    });
    setCompanyEmployers([]);
    setDetailError(null);
    setDetailState("loading");

    try {
      const response = await getAdminEmployerCompanyEmployers(token, company.companyId);
      setSelectedCompany({
        companyId: response.companyId,
        companyName: response.companyName
      });
      setCompanyEmployers(response.employers);
      setDetailState("success");
    } catch (error: unknown) {
      if (error instanceof ApiError && error.status === 401) {
        clearStoredSession();
        navigate("/", { replace: true });
        return;
      }

      if (error instanceof ApiError) {
        setDetailError(error.message || error.code);
      } else if (error instanceof Error) {
        setDetailError(error.message);
      } else {
        setDetailError("고용주 상세를 불러오지 못했어요.");
      }
      setDetailState("error");
    }
  };

  return (
    <div className="admin-page">
      <header className="admin-header">
        <div>
          <h2 className="admin-title">회사 온보딩 관리</h2>
          <p className="admin-subtitle">
            서비스 관리자가 회사를 등록하고, 고용주 가입 코드 발급과 가입/설정 진행 상태를 함께
            확인합니다.
          </p>
        </div>
      </header>

      {lastIssuedCode ? (
        <section className="admin-section">
          <div className="admin-section-head with-sub">
            <div>
              <h3>최근 발급된 고용주 가입 코드</h3>
              <p className="admin-section-sub">
                raw 코드는 발급 직후에만 다시 볼 수 있습니다. 필요한 담당자에게 지금 전달하고,
                고용주는 가입 후 설정에서 사업장 위치와 허용 반경을 직접 완료해야 합니다.
              </p>
            </div>
          </div>
          <div className="admin-code-banner">
            <div>
              <strong>{lastIssuedCode.companyName}</strong>
              <p>
                기본 사업장: {lastIssuedCode.defaultWorkplaceName} / 회사 코드:{" "}
                {lastIssuedCode.companyCode}
              </p>
            </div>
            <div className="admin-code-banner-value">{lastIssuedCode.employerSignupCode}</div>
          </div>
        </section>
      ) : null}

      {revealedCode ? (
        <section className="admin-section">
          <div className="admin-section-head with-sub">
            <div>
              <h3>현재 고용주 가입 코드</h3>
              <p className="admin-section-sub">
                {revealedCode.companyName}의 현재 active 코드를 다시 확인한 결과입니다.
              </p>
            </div>
          </div>
          <div className="admin-code-banner">
            <div>
              <strong>{revealedCode.companyName}</strong>
              <p>최근 발급: {formatDateTime(revealedCode.issuedAt)}</p>
            </div>
            <div className="admin-code-banner-value">{revealedCode.employerSignupCode}</div>
          </div>
        </section>
      ) : null}

      <section className="admin-section">
        <div className="admin-section-head with-sub">
          <div>
            <h3>회사 등록</h3>
            <p className="admin-section-sub">
              서비스 관리자는 회사명과 회사 코드만 등록합니다. 기본 사업장은 placeholder로 자동
              생성되고, 실제 위치와 허용 반경은 고용주가 가입 후 설정에서 직접 입력합니다.
            </p>
          </div>
        </div>
        <form className="admin-company-form" onSubmit={handleSubmit}>
          <div className="admin-company-form-grid">
            {CREATE_FIELDS.map((field) => (
              <label key={field.key} className="admin-company-field">
                <span>{field.label}</span>
                <input
                  type="text"
                  value={formState[field.key]}
                  onChange={(event) => handleFieldChange(field.key, event.currentTarget.value)}
                  placeholder={field.placeholder}
                />
              </label>
            ))}
          </div>

          <div className="admin-form-note">
            생성되는 기본 사업장명은 <strong>{formState.companyName || "회사명"} 기본 사업장</strong>
            입니다.
          </div>

          {formError ? (
            <p className="admin-form-feedback" role="alert">
              {formError}
            </p>
          ) : null}

          <div className="admin-company-form-actions">
            <button type="submit" className="admin-add-button" disabled={submitState === "submitting"}>
              {submitState === "submitting" ? "회사 생성 중..." : "회사 등록 및 코드 발급"}
            </button>
          </div>
        </form>
      </section>

      <section className="admin-section">
        <div className="admin-section-head with-sub">
          <div>
            <h3>등록된 회사</h3>
            <p className="admin-section-sub">
              회사 생성 이후 고용주 가입 여부와 설정 완료 여부를 함께 확인합니다.
            </p>
          </div>
        </div>

        {loadState === "loading" ? (
          <div className="admin-empty-state">회사 목록을 불러오는 중입니다.</div>
        ) : null}

        {loadState === "error" ? (
          <div className="admin-empty-state" role="alert">
            {pageError ?? "회사 목록을 불러오지 못했어요."}
          </div>
        ) : null}

        {loadState === "success" && companies.length === 0 ? (
          <div className="admin-empty-state">등록된 회사가 없습니다.</div>
        ) : null}

        {loadState === "success" && companies.length > 0 ? (
          <div className="admin-table-wrap">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>회사</th>
                  <th>기본 사업장</th>
                  <th>고용주 가입 상태</th>
                  <th>고용주 설정 상태</th>
                  <th>고용주 코드 상태</th>
                  <th>코드 조회</th>
                  <th>고용주 상세</th>
                  <th>등록일</th>
                </tr>
              </thead>
              <tbody>
                {companies.map((company) => (
                  <tr key={company.companyId}>
                    <td>
                      <strong>{company.companyName}</strong>
                      <p className="admin-cell-sub">{company.companyCode}</p>
                    </td>
                    <td>
                      <strong>{company.defaultWorkplaceName}</strong>
                      <p className="admin-cell-sub">#{company.defaultWorkplaceId}</p>
                    </td>
                    <td>
                      <span
                        className={`admin-status ${company.hasJoinedEmployer ? "active" : "pending"}`}
                      >
                        {company.hasJoinedEmployer ? "가입 완료" : "가입 대기"}
                      </span>
                      <p className="admin-cell-sub">
                        {company.hasJoinedEmployer
                          ? `가입 고용주 ${company.employerCount}명 / 최근 ${formatDateTime(company.latestEmployerJoinedAt)}`
                          : "발급된 코드로 아직 가입한 고용주가 없습니다."}
                      </p>
                    </td>
                    <td>
                      <span
                        className={`admin-status ${
                          company.workplaceSettingsConfigured ? "active" : "pending"
                        }`}
                      >
                        {company.workplaceSettingsConfigured ? "설정 완료" : "설정 필요"}
                      </span>
                      <p className="admin-cell-sub">
                        {company.workplaceSettingsConfigured
                          ? `${company.address} / ${company.allowedRadiusMeters}m`
                          : "고용주가 위치와 허용 반경을 입력해야 합니다."}
                      </p>
                    </td>
                    <td>
                      <span
                        className={`admin-status ${
                          company.hasActiveEmployerSignupCode ? "active" : "pending"
                        }`}
                      >
                        {company.hasActiveEmployerSignupCode ? "발급 완료" : "발급 필요"}
                      </span>
                      <p className="admin-cell-sub">
                        최근 발급: {formatDateTime(company.latestEmployerSignupCodeIssuedAt)}
                      </p>
                    </td>
                    <td>
                      {company.hasActiveEmployerSignupCode ? (
                        <button
                          type="button"
                          className="admin-action-button"
                          onClick={() => handleRevealCode(company)}
                          disabled={revealingCompanyId === company.companyId}
                        >
                          {revealingCompanyId === company.companyId ? "조회 중..." : "코드 보기"}
                        </button>
                      ) : (
                        <span className="admin-action-done">조회 불가</span>
                      )}
                    </td>
                    <td>
                      <button
                        type="button"
                        className="admin-action-button"
                        onClick={() => handleToggleEmployerDetail(company)}
                        disabled={
                          detailState === "loading" && selectedCompany?.companyId === company.companyId
                        }
                      >
                        {selectedCompany?.companyId === company.companyId
                          ? detailState === "loading"
                            ? "불러오는 중.."
                            : "닫기"
                          : "상세 보기"}
                      </button>
                    </td>
                    <td>{formatDateTime(company.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </section>

      {selectedCompany ? (
        <section className="admin-section">
          <div className="admin-section-head with-sub">
            <div>
              <h3>{selectedCompany.companyName} 고용주 상세</h3>
              <p className="admin-section-sub">
                어떤 담당자가 가입했는지, 언제 가입했는지, 기본 사업장 설정이 끝났는지 확인합니다.
              </p>
            </div>
          </div>

          {detailState === "loading" ? (
            <div className="admin-empty-state">고용주 상세를 불러오는 중입니다.</div>
          ) : null}

          {detailState === "error" ? (
            <div className="admin-empty-state" role="alert">
              {detailError ?? "고용주 상세를 불러오지 못했어요."}
            </div>
          ) : null}

          {detailState === "success" && companyEmployers.length === 0 ? (
            <div className="admin-empty-state">아직 가입한 고용주가 없습니다.</div>
          ) : null}

          {detailState === "success" && companyEmployers.length > 0 ? (
            <div className="admin-table-wrap">
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>담당자</th>
                    <th>이메일</th>
                    <th>가입 상태</th>
                    <th>기본 사업장</th>
                    <th>사업장 설정</th>
                    <th>가입일</th>
                  </tr>
                </thead>
                <tbody>
                  {companyEmployers.map((employer) => (
                    <tr key={employer.employerProfileId}>
                      <td>
                        <strong>{employer.displayName}</strong>
                        <p className="admin-cell-sub">계정 #{employer.accountId}</p>
                      </td>
                      <td>{employer.email ?? "-"}</td>
                      <td>
                        <span className="admin-status active">
                          {employer.profileStatus === "ACTIVE" ? "가입 완료" : employer.profileStatus}
                        </span>
                      </td>
                      <td>
                        <strong>{employer.defaultWorkplaceName ?? "-"}</strong>
                        <p className="admin-cell-sub">#{employer.defaultWorkplaceId}</p>
                      </td>
                      <td>
                        <span
                          className={`admin-status ${
                            employer.workplaceSettingsConfigured ? "active" : "pending"
                          }`}
                        >
                          {employer.workplaceSettingsConfigured ? "설정 완료" : "설정 필요"}
                        </span>
                      </td>
                      <td>{formatDateTime(employer.joinedAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </section>
      ) : null}
    </div>
  );
}
