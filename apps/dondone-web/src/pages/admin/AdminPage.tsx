import { FormEvent, useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import {
  AdminAdvanceRequestItemResponse,
  AdminEmployerCompanyEmployerSummaryResponse,
  AdminEmployerCompanySummaryResponse,
  createAdminEmployerCompany,
  getAdminAdvanceRequests,
  getAdminEmployerCompanies,
  getAdminEmployerCompanyEmployers,
  getAdminEmployerSignupCode
} from "../../shared/api/admin";
import { ApiError } from "../../shared/api/client";
import { clearStoredSession, getStoredAccessToken } from "../../shared/auth/session";
import { AdminAdvanceRequestSection } from "./components/AdminAdvanceRequestSection";

type AdminCompanyFormState = {
  companyName: string;
  companyCode: string;
};

type RevealedCodeState = {
  employerSignupCode: string;
  issuedAt: string;
};

type EmployerDetailState = {
  employers: AdminEmployerCompanyEmployerSummaryResponse[];
  errorMessage: string | null;
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

function getCompanyDefaultWorkplaceLabel(companyName: string) {
  return `${companyName || "회사명"} 기본 사업장`;
}

export function AdminPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const activeSection = location.hash === "#companies" ? "companies" : "advance";
  const [formState, setFormState] = useState<AdminCompanyFormState>(INITIAL_FORM_STATE);
  const [companies, setCompanies] = useState<AdminEmployerCompanySummaryResponse[]>([]);
  const [advanceRequests, setAdvanceRequests] = useState<AdminAdvanceRequestItemResponse[]>([]);
  const [revealedCodesByCompanyId, setRevealedCodesByCompanyId] = useState<
    Record<number, RevealedCodeState>
  >({});
  const [employerDetailsByCompanyId, setEmployerDetailsByCompanyId] = useState<
    Record<number, EmployerDetailState>
  >({});
  const [loadingCompanyMeta, setLoadingCompanyMeta] = useState(false);
  const [loadState, setLoadState] = useState<"loading" | "success" | "error">("loading");
  const [submitState, setSubmitState] = useState<"idle" | "submitting">("idle");
  const [pageError, setPageError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

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
        void getAdminAdvanceRequests(token)
          .then((advanceResponse) => {
            if (disposed) {
              return;
            }
            setAdvanceRequests(advanceResponse.requests);
          })
          .catch(() => {
            if (!disposed) {
              setAdvanceRequests([]);
            }
          });
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

  useEffect(() => {
    if (activeSection !== "companies" || loadState !== "success" || companies.length === 0) {
      return;
    }

    const token = getStoredAccessToken();
    if (!token) {
      clearStoredSession();
      navigate("/", { replace: true });
      return;
    }

    const companiesNeedingCode = companies.filter(
      (company) => company.hasActiveEmployerSignupCode && !revealedCodesByCompanyId[company.companyId]
    );
    const companiesNeedingDetails = companies.filter(
      (company) => !employerDetailsByCompanyId[company.companyId]
    );

    if (companiesNeedingCode.length === 0 && companiesNeedingDetails.length === 0) {
      return;
    }

    let disposed = false;
    setLoadingCompanyMeta(true);

    void Promise.all([
      Promise.all(
        companiesNeedingCode.map(async (company) => {
          const response = await getAdminEmployerSignupCode(token, company.companyId);
          return {
            companyId: company.companyId,
            data: {
              employerSignupCode: response.employerSignupCode,
              issuedAt: response.issuedAt
            }
          };
        })
      ),
      Promise.all(
        companiesNeedingDetails.map(async (company) => {
          try {
            const response = await getAdminEmployerCompanyEmployers(token, company.companyId);
            return {
              companyId: company.companyId,
              data: {
                employers: response.employers,
                errorMessage: null
              }
            };
          } catch (error: unknown) {
            if (error instanceof ApiError && error.status === 401) {
              throw error;
            }

            return {
              companyId: company.companyId,
              data: {
                employers: [],
                errorMessage:
                  error instanceof Error ? error.message : "고용주 상세를 불러오지 못했어요."
              }
            };
          }
        })
      )
    ])
      .then(([codeEntries, detailEntries]) => {
        if (disposed) {
          return;
        }

        if (codeEntries.length > 0) {
          setRevealedCodesByCompanyId((prev) => {
            const next = { ...prev };
            codeEntries.forEach((entry) => {
              next[entry.companyId] = entry.data;
            });
            return next;
          });
        }

        if (detailEntries.length > 0) {
          setEmployerDetailsByCompanyId((prev) => {
            const next = { ...prev };
            detailEntries.forEach((entry) => {
              next[entry.companyId] = entry.data;
            });
            return next;
          });
        }
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

        setPageError(error instanceof Error ? error.message : "회사 상세 데이터를 불러오지 못했어요.");
      })
      .finally(() => {
        if (!disposed) {
          setLoadingCompanyMeta(false);
        }
      });

    return () => {
      disposed = true;
    };
  }, [
    activeSection,
    companies,
    employerDetailsByCompanyId,
    loadState,
    navigate,
    revealedCodesByCompanyId
  ]);

  const handleFieldChange = (key: keyof AdminCompanyFormState, value: string) => {
    setFormState((prev) => ({
      ...prev,
      [key]: value
    }));
  };

  const handleCloseCreateModal = () => {
    if (submitState === "submitting") {
      return;
    }

    setIsCreateModalOpen(false);
    setFormError(null);
    setFormState(INITIAL_FORM_STATE);
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
      setRevealedCodesByCompanyId((prev) => ({
        ...prev,
        [created.companyId]: {
          employerSignupCode: created.employerSignupCode,
          issuedAt: created.employerSignupCodeIssuedAt
        }
      }));
      setEmployerDetailsByCompanyId((prev) => ({
        ...prev,
        [created.companyId]: {
          employers: [],
          errorMessage: null
        }
      }));
      setFormState(INITIAL_FORM_STATE);
      setLoadState("success");
      setIsCreateModalOpen(false);
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

  const companyRows = useMemo(() => {
    return companies.map((company) => {
      const revealedCode = revealedCodesByCompanyId[company.companyId];
      const employerDetail = employerDetailsByCompanyId[company.companyId];
      const advanceRequestCount = advanceRequests.filter(
        (request) => request.companyName === company.companyName
      ).length;

      return {
        company,
        revealedCode,
        employerDetail,
        advanceRequestCount
      };
    });
  }, [advanceRequests, companies, employerDetailsByCompanyId, revealedCodesByCompanyId]);

  const companySummary = useMemo(
    () => ({
      companyCount: companies.length,
      companyWithEmployerCount: companies.filter((company) => company.hasJoinedEmployer).length,
      activeCodeCount: companies.filter((company) => company.hasActiveEmployerSignupCode).length,
      totalAdvanceRequestCount: advanceRequests.length
    }),
    [advanceRequests.length, companies]
  );

  return (
    <div className="admin-page">
      <header className="admin-header">
        <div>
          <h2 className="admin-title">관리자 운영</h2>
        </div>
      </header>

      {activeSection === "advance" ? (
        <section id="advance" className="admin-anchor-section">
          <AdminAdvanceRequestSection />
        </section>
      ) : null}

      {activeSection === "companies" ? (
        <section id="companies" className="admin-section admin-anchor-section">
          <div className="admin-section-head with-sub">
            <div>
              <h3 className="admin-company-title">등록된 회사</h3>
            </div>
            <button type="button" className="admin-add-button" onClick={() => setIsCreateModalOpen(true)}>
              회사 추가
            </button>
          </div>

          <div className="admin-request-summary admin-request-summary-top">
            <article className="admin-request-summary-card">
              <span>등록 회사</span>
              <strong>{companySummary.companyCount}개</strong>
              <p>관리 중인 전체 회사 수</p>
            </article>
            <article className="admin-request-summary-card">
              <span>가입 완료 회사</span>
              <strong>{companySummary.companyWithEmployerCount}개</strong>
              <p>고용주가 가입한 회사 수</p>
            </article>
            <article className="admin-request-summary-card">
              <span>활성 코드</span>
              <strong>{companySummary.activeCodeCount}개</strong>
              <p>즉시 사용 가능한 가입 코드 수</p>
            </article>
            <article className="admin-request-summary-card">
              <span>미리받기 요청</span>
              <strong>{companySummary.totalAdvanceRequestCount}건</strong>
              <p>전체 회사 기준 누적 요청 수</p>
            </article>
          </div>

          {pageError ? (
            <div className="admin-empty-state" role="alert">
              {pageError}
            </div>
          ) : null}

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
                    <th>고용주 코드</th>
                    <th>담당자</th>
                    <th>이메일</th>
                    <th>등록일</th>
                    <th>미리받기</th>
                  </tr>
                </thead>
                <tbody>
                  {companyRows.map(({ company, revealedCode, employerDetail, advanceRequestCount }) => (
                    <tr key={company.companyId}>
                      <td>
                        <strong>{company.companyName}</strong>
                        <p className="admin-cell-sub">{company.companyCode}</p>
                        <p className="admin-cell-sub">
                          고용주 {company.employerCount}명 / 최근 {formatDateTime(company.latestEmployerJoinedAt)}
                        </p>
                      </td>
                      <td>
                        <strong>{company.defaultWorkplaceName}</strong>
                        <p className="admin-cell-sub">#{company.defaultWorkplaceId}</p>
                        <p className="admin-cell-sub">
                          {company.workplaceSettingsConfigured
                            ? `${company.address} / 허용 반경 ${company.allowedRadiusMeters}m`
                            : "위치 설정 필요"}
                        </p>
                      </td>
                      <td>
                        <div className="admin-code-inline">
                          <strong>
                            {company.hasActiveEmployerSignupCode
                              ? revealedCode?.employerSignupCode ?? (loadingCompanyMeta ? "불러오는 중..." : "-")
                              : "-"}
                          </strong>
                          <p className="admin-cell-sub">
                            {company.hasActiveEmployerSignupCode
                              ? `발급 ${formatDateTime(revealedCode?.issuedAt ?? company.latestEmployerSignupCodeIssuedAt)}`
                              : "활성 코드 없음"}
                          </p>
                        </div>
                      </td>
                      <td>
                        {!employerDetail && loadingCompanyMeta ? (
                          <p className="admin-cell-sub">불러오는 중...</p>
                        ) : null}
                        {employerDetail?.employers.length ? (
                          <div className="admin-inline-text-list">
                            {employerDetail.employers.map((employer) => (
                              <p key={employer.employerProfileId} className="admin-inline-text-item">
                                {employer.displayName}
                              </p>
                            ))}
                          </div>
                        ) : null}
                        {employerDetail?.errorMessage ? (
                          <p className="admin-cell-sub">{employerDetail.errorMessage}</p>
                        ) : null}
                        {employerDetail && employerDetail.employers.length === 0 && !employerDetail.errorMessage ? (
                          <p className="admin-cell-sub">가입한 고용주가 없습니다.</p>
                        ) : null}
                      </td>
                      <td>
                        {!employerDetail && loadingCompanyMeta ? (
                          <p className="admin-cell-sub">불러오는 중...</p>
                        ) : null}
                        {employerDetail?.employers.length ? (
                          <div className="admin-inline-text-list">
                            {employerDetail.employers.map((employer) => (
                              <p key={employer.employerProfileId} className="admin-inline-text-item">
                                {employer.email ?? "-"}
                              </p>
                            ))}
                          </div>
                        ) : null}
                        {employerDetail?.errorMessage ? <p className="admin-cell-sub">-</p> : null}
                        {employerDetail && employerDetail.employers.length === 0 && !employerDetail.errorMessage ? (
                          <p className="admin-cell-sub">-</p>
                        ) : null}
                      </td>
                      <td>{formatDateTime(company.createdAt)}</td>
                      <td>
                        <div className="admin-company-detail-cell">
                          <Link
                            className="admin-inline-link-button admin-inline-count-link"
                            to={`/admin?company=${encodeURIComponent(company.companyName)}#advance`}
                          >
                            {advanceRequestCount}건
                          </Link>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </section>
      ) : null}

      {isCreateModalOpen ? (
        <div className="admin-modal-backdrop" onClick={handleCloseCreateModal}>
          <div className="admin-modal" onClick={(event) => event.stopPropagation()}>
            <div className="admin-modal-head">
              <div>
                <h3>회사등록</h3>
                <p>회사명과 회사 코드만 입력하면 기본 사업장과 가입 코드가 함께 준비됩니다.</p>
              </div>
              <button type="button" className="admin-modal-close" onClick={handleCloseCreateModal}>
                닫기
              </button>
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
                생성되는 기본 사업장명은{" "}
                <strong>{getCompanyDefaultWorkplaceLabel(formState.companyName || "회사명")}</strong> 입니다.
              </div>

              {formError ? (
                <p className="admin-form-feedback" role="alert">
                  {formError}
                </p>
              ) : null}

              <div className="admin-company-form-actions">
                <button type="button" className="admin-ghost-button" onClick={handleCloseCreateModal}>
                  취소
                </button>
                <button type="submit" className="admin-add-button" disabled={submitState === "submitting"}>
                  {submitState === "submitting" ? "회사 생성 중..." : "회사 등록"}
                </button>
              </div>
            </form>
          </div>
        </div>
      ) : null}
    </div>
  );
}
