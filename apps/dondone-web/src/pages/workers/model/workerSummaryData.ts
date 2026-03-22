import type { EmployerWorkerAttendanceStatus } from "../../../shared/api/employer";

export type WorkerListRow = {
  id: number;
  name: string;
  employeeCode: string;
  appliedFor: string;
  team: string;
  email: string;
  phone: string;
  attendanceStatus: EmployerWorkerAttendanceStatus;
  latestWorkDate: string | null;
  avatarTone: "amber" | "sky" | "mint" | "violet" | "rose";
  avatarUrl?: string;
};

export type WorkerListColumn = {
  key: string;
  label: string;
};

const columns: WorkerListColumn[] = [
  { key: "candidate", label: "근로자" },
  { key: "appliedFor", label: "직무" },
  { key: "contact", label: "연락처" }
];

export const workerSummaryMeta = {
  title: "근로자 목록",
  subtitle: "근로자 기록을 추적하고 확인합니다.",
  columns
};
