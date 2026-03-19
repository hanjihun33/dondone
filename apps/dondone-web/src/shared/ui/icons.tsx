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
      <path d="M4.5 15a7.5 7.5 0 1 1 15 0" />
      <path d="M12 15 16.2 10.8" />
      <circle cx="12" cy="15" r="1.1" fill="currentColor" stroke="none" />
      <path d="M6.8 15h-.3" />
      <path d="M17.2 15h.3" />
      <path d="M12 8.2v-.3" />
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

export function ClipboardCheckIcon() {
  return (
    <IconBase>
      <rect x="6" y="4.5" width="12" height="16" rx="2.2" />
      <rect x="9" y="2.5" width="6" height="3.5" rx="1.2" />
      <path d="m9 13 2.2 2.2 3.8-4.2" />
    </IconBase>
  );
}

export function SettingsIcon() {
  return (
    <IconBase>
      <circle cx="12" cy="12" r="2.8" />
      <path d="M19.4 15a1 1 0 0 0 .2 1.1l.1.1a1 1 0 0 1 0 1.4l-1.1 1.1a1 1 0 0 1-1.4 0l-.1-.1a1 1 0 0 0-1.1-.2 1 1 0 0 0-.6.9V20a1 1 0 0 1-1 1h-1.6a1 1 0 0 1-1-1v-.2a1 1 0 0 0-.6-.9 1 1 0 0 0-1.1.2l-.1.1a1 1 0 0 1-1.4 0l-1.1-1.1a1 1 0 0 1 0-1.4l.1-.1a1 1 0 0 0 .2-1.1 1 1 0 0 0-.9-.6H4a1 1 0 0 1-1-1v-1.6a1 1 0 0 1 1-1h.2a1 1 0 0 0 .9-.6 1 1 0 0 0-.2-1.1l-.1-.1a1 1 0 0 1 0-1.4l1.1-1.1a1 1 0 0 1 1.4 0l.1.1a1 1 0 0 0 1.1.2 1 1 0 0 0 .6-.9V4a1 1 0 0 1 1-1h1.6a1 1 0 0 1 1 1v.2a1 1 0 0 0 .6.9 1 1 0 0 0 1.1-.2l.1-.1a1 1 0 0 1 1.4 0l1.1 1.1a1 1 0 0 1 0 1.4l-.1.1a1 1 0 0 0-.2 1.1 1 1 0 0 0 .9.6h.2a1 1 0 0 1 1 1v1.6a1 1 0 0 1-1 1h-.2a1 1 0 0 0-.9.6Z" />
    </IconBase>
  );
}

export function AdminShieldIcon() {
  return (
    <IconBase>
      <path d="M12 3.5 5.5 6v6.5c0 3.9 2.6 7.4 6.5 8.5 3.9-1.1 6.5-4.6 6.5-8.5V6L12 3.5Z" />
      <path d="M9.6 12.1 11.2 13.7 14.5 10.4" />
    </IconBase>
  );
}

export function CalendarIcon() {
  return (
    <IconBase>
      <rect x="3" y="4.5" width="18" height="16.5" rx="2.5" />
      <path d="M3 9.5h18" />
      <path d="M8 3v3" />
      <path d="M16 3v3" />
    </IconBase>
  );
}

export function ChevronDownIcon() {
  return (
    <IconBase>
      <path d="m6.5 9 5.5 6 5.5-6" />
    </IconBase>
  );
}

export function MapPinIcon() {
  return (
    <IconBase>
      <path d="M12 21s6-5.8 6-10a6 6 0 1 0-12 0c0 4.2 6 10 6 10Z" />
      <circle cx="12" cy="11" r="2.2" />
    </IconBase>
  );
}

export function SearchIcon() {
  return (
    <IconBase>
      <circle cx="11" cy="11" r="6.5" />
      <path d="m20 20-3.7-3.7" />
    </IconBase>
  );
}

export function FilterIcon() {
  return (
    <IconBase>
      <path d="M4 5h16l-6 7v5l-4 2v-7Z" />
    </IconBase>
  );
}

export function CheckCircleIcon() {
  return (
    <IconBase>
      <circle cx="12" cy="12" r="8.5" />
      <path d="m8.5 12 2.2 2.2 4.8-5" />
    </IconBase>
  );
}

export function ClockIcon() {
  return (
    <IconBase>
      <circle cx="12" cy="12" r="8.5" />
      <path d="M12 7.5v5l3.2 1.8" />
    </IconBase>
  );
}

export function LeaveIcon() {
  return (
    <IconBase>
      <circle cx="12" cy="12" r="8.5" />
      <path d="M8 12h8" />
    </IconBase>
  );
}

export function UserAvatarIcon() {
  return (
    <IconBase>
      <circle cx="12" cy="9" r="3.2" />
      <path d="M5.8 18a6.2 6.2 0 0 1 12.4 0" />
    </IconBase>
  );
}

export function CloseIcon() {
  return (
    <IconBase>
      <path d="m7 7 10 10" />
      <path d="m17 7-10 10" />
    </IconBase>
  );
}
