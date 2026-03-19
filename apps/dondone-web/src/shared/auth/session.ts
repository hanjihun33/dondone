export type UserRole = "admin" | "manager";

const ROLE_STORAGE_KEY = "dondone_user_role";
const ROLE_CHANGED_EVENT = "dondone:user-role-changed";

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
  window.dispatchEvent(new Event(ROLE_CHANGED_EVENT));
}

export function clearStoredUserRole() {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.removeItem(ROLE_STORAGE_KEY);
  window.dispatchEvent(new Event(ROLE_CHANGED_EVENT));
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
    if (event.key === ROLE_STORAGE_KEY || event.key === null) {
      listener();
    }
  };

  window.addEventListener("storage", handleStorage);
  window.addEventListener(ROLE_CHANGED_EVENT, listener);

  return () => {
    window.removeEventListener("storage", handleStorage);
    window.removeEventListener(ROLE_CHANGED_EVENT, listener);
  };
}
