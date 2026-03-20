import { FormEvent, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { loginUser } from "../../shared/api/auth";
import { ApiError } from "../../shared/api/client";
import { getEmployerProfile, loginEmployer } from "../../shared/api/employer";
import {
  getStoredUserRole,
  resolveUserRoleByEmail,
  setStoredAdminSession,
  setStoredEmployerSession,
  updateStoredEmployerProfile
} from "../../shared/auth/session";
import { RemoteConnectivityPanel } from "./components/RemoteConnectivityPanel";

type LoggedOutFormState = {
  email: string;
  password: string;
};

type LoggedOutField = {
  key: keyof LoggedOutFormState;
  label: string;
  type: "email" | "password";
  placeholder: string;
  autoComplete: string;
};

const LOGGED_OUT_FIELDS: LoggedOutField[] = [
  {
    key: "email",
    label: "이메일",
    type: "email",
    placeholder: "이메일을 입력해주세요.",
    autoComplete: "email"
  },
  {
    key: "password",
    label: "비밀번호",
    type: "password",
    placeholder: "비밀번호를 입력해주세요.",
    autoComplete: "current-password"
  }
];

export function LoggedOutPage() {
  const navigate = useNavigate();
  const [formState, setFormState] = useState<LoggedOutFormState>({
    email: "manager@gmail.com",
    password: "qweqwe123"
  });
  const [submitState, setSubmitState] = useState<"idle" | "submitting">("idle");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    const role = getStoredUserRole();
    if (!role) {
      return;
    }

    navigate(role === "admin" ? "/admin" : "/dashboard", { replace: true });
  }, [navigate]);

  const handleFieldChange = (key: keyof LoggedOutFormState, value: string) => {
    setFormState((prev) => ({
      ...prev,
      [key]: value
    }));
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitState("submitting");
    setErrorMessage(null);

    const normalizedEmail = formState.email.trim().toLowerCase();
    const role = resolveUserRoleByEmail(normalizedEmail);

    try {
      if (role === "admin") {
        const auth = await loginUser({
          email: normalizedEmail,
          password: formState.password
        });
        setStoredAdminSession(auth);
        navigate("/admin", { replace: true });
        return;
      }

      const auth = await loginEmployer({
        email: normalizedEmail,
        password: formState.password
      });

      setStoredEmployerSession(auth);
      const profile = await getEmployerProfile(auth.accessToken);
      updateStoredEmployerProfile(profile);
      navigate("/dashboard", { replace: true });
    } catch (error: unknown) {
      if (error instanceof ApiError) {
        setErrorMessage(error.message || error.code);
      } else if (error instanceof Error) {
        setErrorMessage(error.message);
      } else {
        setErrorMessage("로그인에 실패했어요.");
      }
    } finally {
      setSubmitState("idle");
    }
  };

  return (
    <div className="logged-out-page">
      <div className="logged-out-shell">
        <section className="logged-out-brand-pane" aria-label="서비스 소개">
          <RemoteConnectivityPanel />
        </section>

        <section className="logged-out-form-pane" aria-label="로그인">
          <h2 className="logged-out-form-logo" aria-label="DonDone">
            <span className="brand-wordmark-don">Don</span>
            <span className="brand-wordmark-done">Done</span>
          </h2>

          <form className="logged-out-form" onSubmit={handleSubmit}>
            {LOGGED_OUT_FIELDS.map((field) => (
              <label key={field.key} className="logged-out-field">
                <span>{field.label}</span>
                <input
                  type={field.type}
                  value={formState[field.key]}
                  onChange={(event) => handleFieldChange(field.key, event.currentTarget.value)}
                  placeholder={field.placeholder}
                  autoComplete={field.autoComplete}
                />
              </label>
            ))}

            <div className="logged-out-actions login-actions">
              <button
                type="submit"
                className="logged-out-primary-button logged-out-primary-button-large"
                disabled={submitState === "submitting"}
              >
                {submitState === "submitting" ? "로그인 중..." : "로그인"}
              </button>
            </div>
            <p className="logged-out-signup-hint">
              계정이 없으신가요?{" "}
              <Link to="/signup" className="logged-out-signup-link">
                회원가입
              </Link>
            </p>
            <p className="logged-out-signup-hint">
              개발용 고용주 계정: <strong>manager@gmail.com</strong> / <strong>qweqwe123</strong>
            </p>
            <p className="logged-out-signup-hint">
              개발용 회사 코드: <strong>EMPLOYER-SEOUL-2026</strong>
            </p>
            <p className="logged-out-signup-hint">
              개발용 관리자 계정: <strong>admin@dondone.local</strong> / <strong>qweqwe123</strong>
            </p>
            {errorMessage ? (
              <p className="logged-out-signup-hint" role="alert">
                {errorMessage}
              </p>
            ) : null}
          </form>
        </section>
      </div>
    </div>
  );
}
