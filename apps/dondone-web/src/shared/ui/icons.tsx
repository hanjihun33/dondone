import { ReactNode } from "react";

function IconBase({ children }: { children: ReactNode }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">
      {children}
    </svg>
  );
}

export function DashboardIcon() {
  return (
    <IconBase>
      <rect x="3" y="3" width="8" height="8" rx="2" />
      <rect x="13" y="3" width="8" height="5" rx="2" />
      <rect x="13" y="10" width="8" height="11" rx="2" />
      <rect x="3" y="13" width="8" height="8" rx="2" />
    </IconBase>
  );
}

export function UsersIcon() {
  return (
    <IconBase>
      <circle cx="9" cy="8" r="3" />
      <path d="M4 19a5 5 0 0 1 10 0" />
      <path d="M16 11a3 3 0 1 0 0-6" />
      <path d="M20 19a4 4 0 0 0-4-4" />
    </IconBase>
  );
}

export function WalletIcon() {
  return (
    <IconBase>
      <path d="M4 8.5A2.5 2.5 0 0 1 6.5 6H19a2 2 0 0 1 2 2v2.5H6.5A2.5 2.5 0 0 0 4 13Z" />
      <path d="M4 13v4a2 2 0 0 0 2 2h15v-8.5H6.5A2.5 2.5 0 0 0 4 13Z" />
      <circle cx="17" cy="14.75" r="1" fill="currentColor" stroke="none" />
    </IconBase>
  );
}

export function AlertIcon() {
  return (
    <IconBase>
      <path d="M12 3 2.8 19h18.4L12 3Z" />
      <path d="M12 9v4.5" />
      <circle cx="12" cy="17" r="0.8" fill="currentColor" stroke="none" />
    </IconBase>
  );
}

export function RefreshIcon() {
  return (
    <IconBase>
      <path d="M20 6v5h-5" />
      <path d="M4 18v-5h5" />
      <path d="M6.7 9A7 7 0 0 1 18 11" />
      <path d="M17.3 15A7 7 0 0 1 6 13" />
    </IconBase>
  );
}

export function ShieldCheckIcon() {
  return (
    <IconBase>
      <path d="M12 3 6 5.5v5.3c0 4.1 2.5 7.9 6 9.2 3.5-1.3 6-5.1 6-9.2V5.5L12 3Z" />
      <path d="m9.5 12 1.8 1.8 3.4-3.8" />
    </IconBase>
  );
}
