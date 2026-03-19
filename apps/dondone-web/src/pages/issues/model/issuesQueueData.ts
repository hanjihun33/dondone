export type CorrectionRequestStatus = "pending" | "approved" | "rejected";

export type CorrectionRequestItem = {
  id: string;
  workerName: string;
  role: string;
  workDate: string;
  originalCheckIn: string;
  originalCheckOut: string;
  requestedCheckIn: string;
  requestedCheckOut: string;
  reason: string;
  requestedAt: string;
  status: CorrectionRequestStatus;
};

const requests: CorrectionRequestItem[] = [
  {
    id: "req-001",
    workerName: "Dianne Russell",
    role: "UI/UX 디자이너",
    workDate: "2026-03-17",
    originalCheckIn: "09:42",
    originalCheckOut: "18:03",
    requestedCheckIn: "09:02",
    requestedCheckOut: "18:12",
    reason: "출근 태그가 지연 등록되어 실제 출근 시각으로 정정 요청",
    requestedAt: "2026-03-18 09:20",
    status: "pending"
  },
  {
    id: "req-002",
    workerName: "Bessie Cooper",
    role: "프로덕트 디자이너",
    workDate: "2026-03-18",
    originalCheckIn: "08:57",
    originalCheckOut: "17:11",
    requestedCheckIn: "08:57",
    requestedCheckOut: "18:05",
    reason: "야근 정산 누락으로 실제 퇴근 시각 반영 요청",
    requestedAt: "2026-03-18 18:30",
    status: "pending"
  },
  {
    id: "req-003",
    workerName: "Brooklyn Jones",
    role: "마케팅 담당",
    workDate: "2026-03-16",
    originalCheckIn: "10:05",
    originalCheckOut: "19:02",
    requestedCheckIn: "09:31",
    requestedCheckOut: "19:02",
    reason: "사내 네트워크 오류로 모바일 출근 기록이 늦게 저장됨",
    requestedAt: "2026-03-17 08:58",
    status: "pending"
  },
  {
    id: "req-004",
    workerName: "Eleanor Pena",
    role: "콘텐츠 라이터",
    workDate: "2026-03-15",
    originalCheckIn: "09:03",
    originalCheckOut: "17:48",
    requestedCheckIn: "09:03",
    requestedCheckOut: "18:11",
    reason: "고객사 미팅 후 복귀 기록이 늦게 업로드되어 퇴근 시각 수정 필요",
    requestedAt: "2026-03-15 18:44",
    status: "pending"
  }
];

export const issuesQueueData = {
  requests
};
