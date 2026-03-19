import { Navigate, createBrowserRouter } from "react-router-dom";
import { AppShell } from "./AppShell";
import { EmployerDashboardPage } from "../pages/dashboard/EmployerDashboardPage";
import { WorkerSummaryPage } from "../pages/workers/WorkerSummaryPage";
import { IssuesQueuePage } from "../pages/issues/IssuesQueuePage";
import { SettingsPage } from "../pages/settings/SettingsPage";

export const router = createBrowserRouter([
  {
    path: "/",
    element: <AppShell />,
    children: [
      {
        index: true,
        element: <Navigate to="/dashboard" replace />
      },
      {
        path: "dashboard",
        element: <EmployerDashboardPage />
      },
      {
        path: "workers",
        element: <WorkerSummaryPage />
      },
      {
        path: "settings",
        element: <SettingsPage />
      },
      {
        path: "issues",
        element: <IssuesQueuePage />
      }
    ]
  }
]);
