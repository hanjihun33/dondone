import { FormEvent, useEffect, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { ApiError } from "../../shared/api/client";
import { acceptEmployerInvitation, getEmployerProfile } from "../../shared/api/employer";
import {
  getStoredUserRole,
  setStoredEmployerSession,
  updateStoredEmployerProfile
} from "../../shared/auth/session";
import { RemoteConnectivityPanel } from "./components/RemoteConnectivityPanel";

type SignUpFormState = {
  token: string;
  displayName: string;
  email: string;
  password: string;
  confirmPassword: string;
};

type SignUpField = {
  key: keyof SignUpFormState;
  label: string;
  type: "text" | "email" | "password";
  placeholder: string;
  autoComplete: string;
};

const SIGN_UP_FIELDS: SignUpField[] = [
  {
    key: "token",
    label: "초대 토큰",
    type: "text",
    placeholder: "초대 토큰을 입력하세요",
    autoComplete: "off"
  },
  {
    key: "displayName",
    label: "이름",
    type: "text",
    placeholder: "이름을 입력하세요",
    autoComplete: "name"
  },
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
    autoComplete: "new-password"
  },
  {
    key: "confirmPassword",
    label: "비밀번호 확인",
    type: "password",
    placeholder: "비밀번호를 다시 입력하세요",
    autoComplete: "new-password"
  }
];

export function SignUpPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [formState, setFormState] = useState<SignUpFormState>({
    token: searchParams.get("token") ?? "",
    displayName: "",
    email: "",
    password: "",
    confirmPassword: ""
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

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (formState.password !== formState.confirmPassword) {
      setErrorMessage("비밀번호가 일치하지 않습니다.");
      return;
    }

    setSubmitState("submitting");
    setErrorMessage(null);

    try {
      const auth = await acceptEmployerInvitation({
        token: formState.token.trim(),
        displayName: formState.displayName.trim(),
        email: formState.email.trim().toLowerCase(),
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
        setErrorMessage("회원가입에 실패했어요.");
      }
    } finally {
      setSubmitState("idle");
    }
  };

  const handleFieldChange = (key: keyof SignUpFormState, value: string) => {
    setFormState((prev) => ({
      ...prev,
      [key]: value
    }));
  };

  return (
    <div className="logged-out-page">
      <div className="logged-out-shell">
        <section className="logged-out-brand-pane" aria-label="서비스 소개">
          <RemoteConnectivityPanel />
        </section>

        <section className="logged-out-form-pane" aria-label="회원가입 폼">
          <h2 className="logged-out-form-logo" aria-label="DonDone">
            <span className="brand-wordmark-don">Don</span>
            <span className="brand-wordmark-done">Done</span>
          </h2>

          <form className="logged-out-form" onSubmit={handleSubmit}>
            {SIGN_UP_FIELDS.map((field) => (
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
                {submitState === "submitting" ? "가입 중..." : "가입하기"}
              </button>
            </div>
            {errorMessage ? (
              <p className="logged-out-signup-hint" role="alert">
                {errorMessage}
              </p>
            ) : null}
            <p className="logged-out-signup-hint">
              계정이 이미 있다면{" "}
              <Link to="/" className="logged-out-signup-link">
                로그인 하기
              </Link>
            </p>
          </form>
        </section>
      </div>
    </div>
  );
}
