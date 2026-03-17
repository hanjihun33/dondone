import { createBrowserRouter } from "react-router-dom";
import { AppShell } from "./AppShell";
import { EmployerDashboardPage } from "../pages/dashboard/EmployerDashboardPage";
import { WorkerSummaryPage } from "../pages/workers/WorkerSummaryPage";
import { IssuesQueuePage } from "../pages/issues/IssuesQueuePage";

export const router = createBrowserRouter([
  {
    path: "/",
    element: <AppShell />,
    children: [
      {
        index: true,
        element: <EmployerDashboardPage />
      },
      {
        path: "workers",
        element: <WorkerSummaryPage />
      },
      {
        path: "issues",
        element: <IssuesQueuePage />
      }
    ]
  }
]);
