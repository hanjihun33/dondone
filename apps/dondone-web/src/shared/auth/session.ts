export type UserRole = "admin" | "manager";

const ROLE_STORAGE_KEY = "dondone_user_role";

export function getStoredUserRole(): UserRole | null {
  if (typeof window === "undefined") {
    return null;
  }

  const raw = window.localStorage.getItem(ROLE_STORAGE_KEY);
  if (raw === "admin" || raw === "manager") {
    return raw;
  }

  return null;
}

export function setStoredUserRole(role: UserRole) {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.setItem(ROLE_STORAGE_KEY, role);
}

export function clearStoredUserRole() {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.removeItem(ROLE_STORAGE_KEY);
}

export function resolveUserRoleByEmail(email: string): UserRole {
  const normalizedEmail = email.trim().toLowerCase();
  return normalizedEmail === "admin@dondone.local" ? "admin" : "manager";
}

