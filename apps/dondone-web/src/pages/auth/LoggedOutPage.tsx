import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { resolveUserRoleByEmail, setStoredUserRole } from "../../shared/auth/session";
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
    placeholder: "이메일을 입력하세요",
    autoComplete: "email"
  },
  {
    key: "password",
    label: "비밀번호",
    type: "password",
    placeholder: "비밀번호를 입력하세요",
    autoComplete: "current-password"
  }
];

export function LoggedOutPage() {
  const navigate = useNavigate();
  const [formState, setFormState] = useState<LoggedOutFormState>({
    email: "admin@dondone.local",
    password: ""
  });

  const handleFieldChange = (key: keyof LoggedOutFormState, value: string) => {
    setFormState((prev) => ({
      ...prev,
      [key]: value
    }));
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const role = resolveUserRoleByEmail(formState.email);
    setStoredUserRole(role);
    navigate(role === "admin" ? "/admin" : "/dashboard");
  };

  return (
    <div className="logged-out-page">
      <div className="logged-out-shell">
        <section className="logged-out-brand-pane" aria-label="서비스 소개">
          <RemoteConnectivityPanel />
        </section>

        <section className="logged-out-form-pane" aria-label="로그인 폼">
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
              >
                로그인
              </button>
            </div>
            <p className="logged-out-signup-hint">
              계정이 없으신가요?{" "}
              <Link to="/signup" className="logged-out-signup-link">
                회원가입
              </Link>
            </p>
          </form>
        </section>
      </div>
    </div>
  );
}
