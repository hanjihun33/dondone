import type {
  EmployerAuthResponse,
  EmployerProfileResponse
} from "../api/employer";

export type UserRole = "admin" | "manager";

type AdminSession = {
  role: "admin";
};

export type EmployerSession = {
  role: "manager";
  accessToken: string;
  expiresAt: string;
  scope: EmployerAuthResponse;
  profile: EmployerProfileResponse | null;
};

type StoredSession = AdminSession | EmployerSession;

const SESSION_STORAGE_KEY = "dondone_session";
const SESSION_CHANGED_EVENT = "dondone:session-changed";

function dispatchSessionChange() {
  window.dispatchEvent(new Event(SESSION_CHANGED_EVENT));
}

function parseStoredSession(raw: string | null): StoredSession | null {
  if (!raw) {
    return null;
  }

  try {
    const parsed = JSON.parse(raw) as StoredSession;
    if (parsed.role === "admin") {
      return parsed;
    }

    if (parsed.role === "manager" && parsed.accessToken) {
      return parsed;
    }
  } catch {
    return null;
  }

  return null;
}

export function getStoredSession(): StoredSession | null {
  if (typeof window === "undefined") {
    return null;
  }

  return parseStoredSession(window.localStorage.getItem(SESSION_STORAGE_KEY));
}

export function getStoredEmployerSession(): EmployerSession | null {
  const session = getStoredSession();
  return session?.role === "manager" ? session : null;
}

export function getStoredUserRole(): UserRole | null {
  return getStoredSession()?.role ?? null;
}

export function getStoredAccessToken() {
  return getStoredEmployerSession()?.accessToken ?? null;
}

export function setStoredAdminSession() {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.setItem(
    SESSION_STORAGE_KEY,
    JSON.stringify({
      role: "admin"
    } satisfies AdminSession)
  );
  dispatchSessionChange();
}

export function setStoredEmployerSession(
  auth: EmployerAuthResponse,
  profile: EmployerProfileResponse | null = null
) {
  if (typeof window === "undefined") {
    return;
  }

  const expiresAt = new Date(Date.now() + auth.expiresIn * 1000).toISOString();
  const nextSession: EmployerSession = {
    role: "manager",
    accessToken: auth.accessToken,
    expiresAt,
    scope: auth,
    profile
  };

  window.localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(nextSession));
  dispatchSessionChange();
}

export function updateStoredEmployerProfile(profile: EmployerProfileResponse) {
  if (typeof window === "undefined") {
    return;
  }

  const current = getStoredEmployerSession();
  if (!current) {
    return;
  }

  const nextSession: EmployerSession = {
    ...current,
    profile
  };

  window.localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(nextSession));
  dispatchSessionChange();
}

export function clearStoredSession() {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.removeItem(SESSION_STORAGE_KEY);
  dispatchSessionChange();
}

export function resolveUserRoleByEmail(email: string): UserRole {
  const normalizedEmail = email.trim().toLowerCase();
  return normalizedEmail === "admin@dondone.local" ? "admin" : "manager";
}

export function subscribeUserRoleChange(listener: () => void): () => void {
  if (typeof window === "undefined") {
    return () => undefined;
  }

  const handleStorage = (event: StorageEvent) => {
    if (event.key === SESSION_STORAGE_KEY || event.key === null) {
      listener();
    }
  };

  window.addEventListener("storage", handleStorage);
  window.addEventListener(SESSION_CHANGED_EVENT, listener);

  return () => {
    window.removeEventListener("storage", handleStorage);
    window.removeEventListener(SESSION_CHANGED_EVENT, listener);
  };
}
