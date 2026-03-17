export type MetricItem = {
  title: string;
  value: string;
  description: string;
};

export type WorkerSnapshot = {
  name: string;
  meta: string;
  leftLabel: string;
  leftValue: string;
  rightLabel: string;
  rightValue: string;
  badgeTone: "success" | "warn" | "danger" | "soft";
  badgeText: string;
};

export type SummaryCard = {
  title: string;
  description: string;
  chips: string[];
};

export type IssueItem = {
  title: string;
  description: string;
  badgeTone: "warn" | "danger" | "soft";
  badgeText: string;
};

export type RequestRow = {
  type: string;
  requester: string;
  requestDetail: string;
  reason: string;
  statusTone: "success" | "warn" | "danger" | "soft";
  statusText: string;
  manager: string;
  actionPrimary?: string;
  actionSecondary?: string;
  locationHint?: string;
};

export const dashboardMetrics: MetricItem[] = [
  {
    title: "오늘 출근 완료",
    value: "19명",
    description: "연결 근로자 24명 중 79%가 출근 기록을 남겼습니다."
  },
  {
    title: "확인 필요 차이",
    value: "4건",
    description: "실입금 확인 이후 근거 요약이 필요한 항목만 표시합니다."
  },
  {
    title: "선지급 가능 대상",
    value: "11명",
    description: "근무 반영률과 재직 확인 신호가 충족된 데모 기준입니다."
  },
  {
    title: "누락/수정 요청",
    value: "3건",
    description: "근무 수정 사유와 첨부 여부를 함께 확인할 수 있습니다."
  }
];

export const todaySnapshots: WorkerSnapshot[] = [
  {
    name: "Nguyen Thi Linh",
    meta: "주방 보조 · 재직 확인 완료 · 최근 14일 기록 일관성 높음",
    leftLabel: "출근",
    leftValue: "08:57",
    rightLabel: "수정 요청",
    rightValue: "없음",
    badgeTone: "success",
    badgeText: "정상 반영"
  },
  {
    name: "Pham Van Tuan",
    meta: "홀 스태프 · 재직 확인 완료 · 어제 퇴근 사유 메모 추가",
    leftLabel: "출근",
    leftValue: "09:11",
    rightLabel: "수정 요청",
    rightValue: "1건",
    badgeTone: "warn",
    badgeText: "확인 필요"
  },
  {
    name: "Tran Minh Chau",
    meta: "세척 파트 · 위치 확인 실패 후 재기록 대기",
    leftLabel: "출근",
    leftValue: "미기록",
    rightLabel: "수정 요청",
    rightValue: "대기",
    badgeTone: "danger",
    badgeText: "누락 가능성"
  }
];

export const verifiedWorkerSummaries: SummaryCard[] = [
  {
    title: "Nguyen Thi Linh",
    description:
      "최근 4주 연속 기록 누락 없음. 수정 사유 1건은 첨부 포함으로 확인 완료됐습니다.",
    chips: ["재직 확인", "정산 이슈 없음", "선지급 가능"]
  },
  {
    title: "Pham Van Tuan",
    description:
      "이번 달 수정 기록 2건. 차이 확인 요청 1건이 열려 있어 설명 메모 검토가 필요합니다.",
    chips: ["재직 확인", "설명 요청 진행 중", "선지급 보류"]
  }
];

export const issueQueue: IssueItem[] = [
  {
    title: "3월 1주 실입금 차이 확인 요청",
    description:
      "야간 6시간 반영 여부와 수정 기록 1건이 함께 표시됩니다. Proof Pack과 변경 이력 카드 확인 후 설명 메모를 남길 수 있습니다.",
    badgeTone: "warn",
    badgeText: "추가 확인 필요"
  },
  {
    title: "퇴근 누락 후 수정 요청",
    description:
      "퇴근 시각 누락 이후 같은 날 22:41 수정. 첨부 1건 포함. Verified Worker Summary에서는 최근 기록 일관성 저하로만 표시됩니다.",
    badgeTone: "danger",
    badgeText: "사유 검토 필요"
  },
  {
    title: "선지급 가능 대상 재확인",
    description:
      "이번 주 근무 반영률은 충족됐지만 회사 확인이 아직 완료되지 않은 근로자 2명이 남아 있습니다.",
    badgeTone: "soft",
    badgeText: "운영 검토"
  }
];

export const eligibleWorkers: WorkerSnapshot[] = [
  {
    name: "Le Quoc Bao",
    meta: "반영 근무 17일 · 이번 달 수정 기록 없음",
    leftLabel: "가능 한도",
    leftValue: "₩180,000",
    rightLabel: "확인 신호",
    rightValue: "정상",
    badgeTone: "success",
    badgeText: "가능"
  },
  {
    name: "Vo Thu Trang",
    meta: "반영 근무 13일 · 최근 1건 수정 사유 확인 완료",
    leftLabel: "가능 한도",
    leftValue: "₩120,000",
    rightLabel: "확인 신호",
    rightValue: "주의",
    badgeTone: "warn",
    badgeText: "검토 후 가능"
  }
];

export const ctaCards = [
  {
    title: "회사 확인 코드 연결 안내",
    description:
      "신규 사업장 연결 시 근로자가 입력할 코드와 연결 상태만 간단히 보여줍니다.",
    linkLabel: "코드 공유 가이드 보기"
  },
  {
    title: "Proof Pack 연동 확인",
    description:
      "근로자가 문서 생성 후 확인 요청을 보냈을 때, 운영 콘솔에서 근거 요약만 확인하는 흐름입니다.",
    linkLabel: "문서 상태 확인"
  },
  {
    title: "설명 메모 초안",
    description:
      "정산 차이 확인 요청에 대해 회사가 남길 설명 메모 초안 영역을 후속 범위로 연결합니다.",
    linkLabel: "후속 화면 정의"
  }
];

export const requestFilters = {
  dateRange: "2026.03.01 - 2026.03.17",
  category: "모든 요청",
  workplace: "서울본사",
  searchPlaceholders: ["요청 종류", "요청 보낸 사람", "요청사항", "요청 사유"]
};

export const requestRows: RequestRow[] = [
  {
    type: "출근 요청",
    requester: "김철수",
    requestDetail: "출근 09:27 · 서울본사 / 마케팅",
    reason: "외부 미팅 후 사무실 이동",
    statusTone: "warn",
    statusText: "대기중",
    manager: "김관리 (1/1)",
    actionPrimary: "승인",
    actionSecondary: "거절",
    locationHint: "위치 스냅샷 확인 가능"
  },
  {
    type: "근무일정 삭제",
    requester: "김민지",
    requestDetail: "[10월 4일] 09:00 - 18:00 / 서울본사 · 운영",
    reason: "중복 등록된 일정 삭제 요청",
    statusTone: "soft",
    statusText: "자동 승인됨",
    manager: "시스템 규칙",
    locationHint: "동일한 일정이 이미 정리됨"
  },
  {
    type: "휴가 삭제",
    requester: "강예린",
    requestDetail: "반차 13:00 - 18:00 (4h, 0.5일)",
    reason: "휴가 취소합니다.",
    statusTone: "danger",
    statusText: "거절됨",
    manager: "김관리 (X) (1/1)"
  },
  {
    type: "휴가 생성",
    requester: "김지수",
    requestDetail: "연차 09:00 - 18:00 (8h, 1일)",
    reason: "휴가 재생성 요청",
    statusTone: "success",
    statusText: "승인됨",
    manager: "김관리 (O) (1/1)"
  },
  {
    type: "근무일정 수정",
    requester: "김지수",
    requestDetail: "기존 09:00 - 18:00 → 신규 10:00 - 19:00",
    reason: "재택근무 8h 신청",
    statusTone: "danger",
    statusText: "거절됨",
    manager: "김관리 (X) (1/1)"
  },
  {
    type: "근무일정 생성",
    requester: "이영희",
    requestDetail: "10월 1일, 2일, 3일 / 서울본사 · 마케팅",
    reason: "다음달 근무일정 생성 요청",
    statusTone: "success",
    statusText: "승인됨",
    manager: "김관리 (O) (1/1)"
  }
];

export const requestSummary = {
  total: 11,
  pending: 1,
  approved: 3,
  rejected: 2,
  autoResolved: 1
};
