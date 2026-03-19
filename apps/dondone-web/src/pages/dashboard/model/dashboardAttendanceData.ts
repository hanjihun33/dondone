export type AttendanceCellTone = "success" | "warn" | "leave" | "absent" | "active";

type AttendanceSummaryCard = {
  title: string;
  value: string;
  helper: string;
  tone: AttendanceCellTone;
};

type AttendanceDayCell = {
  date: string;
  label?: string;
  tone?: AttendanceCellTone;
  empty?: boolean;
};

type AttendanceRow = {
  id: string;
  name: string;
  role: string;
  avatarTone: "amber" | "sky" | "mint" | "violet" | "rose";
  avatarUrl?: string;
  days: AttendanceDayCell[];
};

export const attendanceSummaryCards: AttendanceSummaryCard[] = [
  {
    title: "오늘 출근",
    value: "40",
    helper: "124명 확인 대기",
    tone: "success"
  },
  {
    title: "지각",
    value: "26",
    helper: "정시 출근 12명",
    tone: "warn"
  },
  {
    title: "휴가",
    value: "04",
    helper: "승인된 휴가",
    tone: "leave"
  },
  {
    title: "결근",
    value: "01",
    helper: "사유 미입력",
    tone: "absent"
  }
];

export const attendanceFilters = ["휴가", "결근", "근무중"];

export const attendanceWeekDays = [
  "일요일",
  "월요일",
  "화요일",
  "수요일",
  "목요일",
  "금요일",
  "토요일"
];

export const attendanceRows: AttendanceRow[] = [
  {
    id: "dianne-russell",
    name: "Dianne Russell",
    role: "UI/UX 디자이너",
    avatarTone: "amber",
    days: [
      { date: "4", empty: true },
      { date: "5", label: "8시간", tone: "success" },
      { date: "6", label: "4시간 36분", tone: "warn" },
      { date: "7", label: "휴가", tone: "leave" },
      { date: "8", label: "8시간 39분", tone: "success" },
      { date: "9", label: "근무중", tone: "active" },
      { date: "10", empty: true }
    ]
  },
  {
    id: "bessie-cooper",
    name: "Bessie Cooper",
    role: "프로덕트 디자이너",
    avatarTone: "mint",
    days: [
      { date: "4", empty: true },
      { date: "5", label: "6시간 24분", tone: "warn" },
      { date: "6", label: "8시간", tone: "success" },
      { date: "7", label: "8시간", tone: "success" },
      { date: "8", label: "결근", tone: "absent" },
      { date: "9", label: "근무중", tone: "active" },
      { date: "10", empty: true }
    ]
  },
  {
    id: "brooklyn-jones",
    name: "Brooklyn Jones",
    role: "마케팅 담당",
    avatarTone: "rose",
    days: [
      { date: "4", empty: true },
      { date: "5", label: "8시간", tone: "success" },
      { date: "6", label: "8시간 12분", tone: "success" },
      { date: "7", label: "3시간 45분", tone: "warn" },
      { date: "8", label: "8시간", tone: "success" },
      { date: "9", label: "휴가", tone: "leave" },
      { date: "10", empty: true }
    ]
  },
  {
    id: "eleanor-pena",
    name: "Eleanor Pena",
    role: "콘텐츠 라이터",
    avatarTone: "sky",
    days: [
      { date: "4", empty: true },
      { date: "5", label: "8시간 15분", tone: "success" },
      { date: "6", label: "8시간", tone: "success" },
      { date: "7", label: "8시간 23분", tone: "success" },
      { date: "8", label: "7시간 24분", tone: "warn" },
      { date: "9", label: "근무중", tone: "active" },
      { date: "10", empty: true }
    ]
  },
  {
    id: "darlene-robert",
    name: "Darlene Robert",
    role: "UX 엔지니어",
    avatarTone: "violet",
    days: [
      { date: "4", empty: true },
      { date: "5", label: "8시간", tone: "success" },
      { date: "6", label: "5시간 17분", tone: "warn" },
      { date: "7", label: "4시간 13분", tone: "warn" },
      { date: "8", label: "휴가", tone: "leave" },
      { date: "9", label: "근무중", tone: "active" },
      { date: "10", empty: true }
    ]
  }
];
