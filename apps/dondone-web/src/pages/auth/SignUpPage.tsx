import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { RemoteConnectivityPanel } from "./components/RemoteConnectivityPanel";

type SignUpFormState = {
  companyName: string;
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
    key: "companyName",
    label: "회사명",
    type: "text",
    placeholder: "회사명을 입력하세요",
    autoComplete: "organization"
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
  const [formState, setFormState] = useState<SignUpFormState>({
    companyName: "",
    email: "",
    password: "",
    confirmPassword: ""
  });

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    navigate("/");
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
              >
                가입하기
              </button>
            </div>
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
