import { Navigate, createBrowserRouter } from "react-router-dom";
import { AppShell } from "./AppShell";
import { EmployerDashboardPage } from "../pages/dashboard/EmployerDashboardPage";
import { WorkerSummaryPage } from "../pages/workers/WorkerSummaryPage";
import { IssuesQueuePage } from "../pages/issues/IssuesQueuePage";
import { SettingsPage } from "../pages/settings/SettingsPage";
import { LoggedOutPage } from "../pages/auth/LoggedOutPage";
import { SignUpPage } from "../pages/auth/SignUpPage";
import { AdminPage } from "../pages/admin/AdminPage";
import { LegacyAdminPage } from "../pages/admin/LegacyAdminPage";
import { getStoredUserRole } from "../shared/auth/session";

function AdminOnlyRoute({ element }: { element: JSX.Element }) {
  const role = getStoredUserRole();
  if (role === null) {
    return <Navigate to="/" replace />;
  }

  if (role !== "admin") {
    return <Navigate to="/dashboard" replace />;
  }

  return element;
}

function ManagerOnlyRoute({ element }: { element: JSX.Element }) {
  const role = getStoredUserRole();
  if (role === null) {
    return <Navigate to="/" replace />;
  }

  if (role === "admin") {
    return <Navigate to="/admin" replace />;
  }

  return element;
}

export const router = createBrowserRouter([
  {
    path: "/",
    element: <LoggedOutPage />
  },
  {
    path: "/signup",
    element: <SignUpPage />
  },
  {
    path: "/",
    element: <AppShell />,
    children: [
      {
        path: "dashboard",
        element: <ManagerOnlyRoute element={<EmployerDashboardPage />} />
      },
      {
        path: "workers",
        element: <ManagerOnlyRoute element={<WorkerSummaryPage />} />
      },
      {
        path: "settings",
        element: <ManagerOnlyRoute element={<SettingsPage />} />
      },
      {
        path: "issues",
        element: <ManagerOnlyRoute element={<IssuesQueuePage />} />
      },
      {
        path: "admin",
        element: <AdminOnlyRoute element={<AdminPage />} />
      },
      {
        path: "admin/legacy",
        element: <AdminOnlyRoute element={<LegacyAdminPage />} />
      }
    ]
  },
  {
    path: "/logged-out",
    element: <Navigate to="/" replace />
  }
]);
