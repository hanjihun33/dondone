export type WorkerAttendanceStatus = "present" | "late" | "leave" | "absent";

export type WorkerListRow = {
  name: string;
  employeeCode: string;
  appliedFor: string;
  team: string;
  email: string;
  phone: string;
  attendanceStatus: WorkerAttendanceStatus;
  avatarTone: "amber" | "sky" | "mint" | "violet" | "rose";
  avatarUrl?: string;
};

export type WorkerListColumn = {
  key: string;
  label: string;
  sortable?: boolean;
};

const columns: WorkerListColumn[] = [
  { key: "candidate", label: "근로자", sortable: true },
  { key: "appliedFor", label: "직무", sortable: true },
  { key: "contact", label: "연락처", sortable: true }
];

const rows: WorkerListRow[] = [
  {
    name: "김민수",
    employeeCode: "52936567",
    appliedFor: "UI/UX 디자이너",
    team: "디자인팀",
    email: "minsukim@gmail.com",
    phone: "010-2841-0109",
    attendanceStatus: "present",
    avatarTone: "amber",
    avatarUrl: "https://i.pravatar.cc/96?img=12"
  },
  {
    name: "이서연",
    employeeCode: "76031847",
    appliedFor: "프로덕트 디자이너",
    team: "디자인팀",
    email: "seoyeon.lee@gmail.com",
    phone: "010-7263-0123",
    attendanceStatus: "late",
    avatarUrl: "https://i.pravatar.cc/96?img=47",
    avatarTone: "rose"
  },
  {
    name: "박준호",
    employeeCode: "58276066",
    appliedFor: "프론트엔드 개발자",
    team: "개발팀",
    email: "junho.park@gmail.com",
    phone: "010-2295-0118",
    attendanceStatus: "leave",
    avatarTone: "sky"
  },
  {
    name: "정하늘",
    employeeCode: "11081197",
    appliedFor: "인사 매니저",
    team: "인사팀",
    email: "haneul.jeong@gmail.com",
    phone: "010-4707-0101",
    attendanceStatus: "absent",
    avatarUrl: "https://i.pravatar.cc/96?img=5",
    avatarTone: "mint"
  },
  {
    name: "최지원",
    employeeCode: "38766940",
    appliedFor: "마케팅 리드",
    team: "마케팅팀",
    email: "jiwon.choi@gmail.com",
    phone: "010-7032-0122",
    attendanceStatus: "present",
    avatarTone: "violet"
  },
  {
    name: "오유진",
    employeeCode: "01906912",
    appliedFor: "iOS 엔지니어",
    team: "개발팀",
    email: "yujin.oh@gmail.com",
    phone: "010-9320-0107",
    attendanceStatus: "late",
    avatarUrl: "https://i.pravatar.cc/96?img=32",
    avatarTone: "rose"
  },
  {
    name: "강다은",
    employeeCode: "34034474",
    appliedFor: "UX 라이터",
    team: "디자인팀",
    email: "daeun.kang@gmail.com",
    phone: "010-5170-0117",
    attendanceStatus: "present",
    avatarTone: "amber"
  },
  {
    name: "송지후",
    employeeCode: "29103050",
    appliedFor: "UI 디자이너",
    team: "디자인팀",
    email: "jihoo.song@gmail.com",
    phone: "010-1671-0110",
    attendanceStatus: "present",
    avatarTone: "sky"
  },
  {
    name: "윤세아",
    employeeCode: "55700223",
    appliedFor: "UX 엔지니어",
    team: "디자인팀",
    email: "seah.yoon@gmail.com",
    phone: "010-9629-0129",
    attendanceStatus: "leave",
    avatarTone: "mint"
  }
];

export const workerSummaryData = {
  title: "근로자 목록",
  subtitle: "근로자 기록을 추적하고 확인합니다.",
  columns,
  rows
};
