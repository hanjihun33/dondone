import { apiRequest } from "./client";

export type EmployerWorkerAttendanceStatus =
  | "WORKING"
  | "COMPLETED"
  | "NEEDS_REVIEW"
  | "NO_RECORD";

export type WorkProofRecordStatus = "CHECKED_IN" | "CHECKED_OUT" | "NEEDS_REVIEW";

export type WorkProofReflectionStatus =
  | "PENDING"
  | "REFLECTED"
  | "NEEDS_REVIEW"
  | "EXCLUDED";

export type AttendanceOvertimeRoundingUnit =
  | "FIFTEEN_MINUTES"
  | "THIRTY_MINUTES"
  | "ONE_HOUR";

export type EmployerIssueItemType = "CORRECTION_REQUEST" | "REVIEW_REQUIRED_RECORD";
export type EmployerIssueStatus = "PENDING" | "NEEDS_REVIEW";
export type CorrectionRequestStatus = "PENDING" | "APPROVED" | "REJECTED";

export type EmployerAuthResponse = {
  accessToken: string;
  expiresIn: number;
  employerId: number;
  companyId: number;
  companyName: string;
  defaultWorkplaceId: number;
  defaultWorkplaceName: string;
};

export type EmployerProfileResponse = {
  employerId: number;
  displayName: string;
  email: string;
  companyId: number;
  companyName: string;
  companyCode: string;
  defaultWorkplaceId: number;
  defaultWorkplaceName: string;
  status: string;
};

export type EmployerWorkplaceSettingsResponse = {
  workplaceId: number;
  workplaceName: string;
  companyId: number;
  companyName: string;
  address: string;
  detailAddress: string | null;
  latitude: number;
  longitude: number;
  allowedRadiusMeters: number;
  scheduledClockInTime: string | null;
  scheduledClockOutTime: string | null;
  overtimeRoundingUnit: AttendanceOvertimeRoundingUnit | null;
  effectiveFrom: string | null;
  updatedAt: string | null;
  updatedByAccountId: number | null;
  activeMembershipCount: number;
};

export type EmployerWorkerRegistrationCodeResponse = {
  codeId: number;
  registrationCode: string;
  active: boolean;
  issuedAt: string;
  revokedAt: string | null;
};

export type EmployerWorkerRegistrationCodesResponse = {
  companyId: number;
  companyName: string;
  workplaceId: number;
  workplaceName: string;
  codes: EmployerWorkerRegistrationCodeResponse[];
};

export type EmployerDashboardSummaryResponse = {
  activeWorkerCount: number;
  workingCount: number;
  completedCount: number;
  needsReviewCount: number;
  noRecordCount: number;
  asOf: string;
};

export type EmployerAttendanceBoardDayResponse = {
  date: string;
  recordStatus: WorkProofRecordStatus | null;
  reflectionStatus: WorkProofReflectionStatus | null;
  attendanceStatus: EmployerWorkerAttendanceStatus;
  workedMinutes: number | null;
};

export type EmployerAttendanceBoardRowResponse = {
  workerId: number;
  name: string;
  role: string | null;
  avatarUrl: string | null;
  days: EmployerAttendanceBoardDayResponse[];
};

export type EmployerAttendanceBoardResponse = {
  weekStart: string;
  weekEnd: string;
  rows: EmployerAttendanceBoardRowResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
};

export type EmployerWorkerSummaryResponse = {
  workerId: number;
  employeeCode: string | null;
  name: string;
  team: string | null;
  role: string | null;
  email: string;
  phone: string | null;
  avatarUrl: string | null;
  recordStatus: WorkProofRecordStatus | null;
  reflectionStatus: WorkProofReflectionStatus | null;
  attendanceStatus: EmployerWorkerAttendanceStatus;
  latestWorkDate: string | null;
};

export type EmployerWorkersResponse = {
  workers: EmployerWorkerSummaryResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type EmployerIssueSummaryResponse = {
  itemType: EmployerIssueItemType;
  issueStatus: EmployerIssueStatus;
  requestId: number | null;
  workProofId: number;
  workerId: number;
  workerName: string;
  workerEmail: string;
  role: string | null;
  workDate: string;
  clockInAt: string | null;
  clockOutAt: string | null;
  requestedClockInAt: string | null;
  requestedClockOutAt: string | null;
  reasonCode: string | null;
  reason: string | null;
  reviewReasonCode: string | null;
  raisedAt: string;
};

export type EmployerIssuesResponse = {
  issues: EmployerIssueSummaryResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type EmployerReviewRequiredRecordDetailResponse = {
  workProofId: number;
  workerId: number;
  workerName: string;
  workerEmail: string;
  workDate: string;
  recordStatus: WorkProofRecordStatus;
  reflectionStatus: WorkProofReflectionStatus;
  reviewReasonCode: string;
  reviewReason: string | null;
  recognizedClockInAt: string | null;
  recognizedClockOutAt: string | null;
  workedMinutes: number;
  clockOutOutsideAllowedRadius: boolean;
  edited: boolean;
  editReason: string | null;
  memo: string | null;
  attachmentCount: number;
  workplace: {
    workplaceId: number;
    name: string;
    address: string;
    mapLabel: string | null;
    latitude: number | null;
    longitude: number | null;
  } | null;
  checkIn: {
    deviceAt: string | null;
    serverAt: string | null;
    latitude: number | null;
    longitude: number | null;
    locationLabel: string | null;
  } | null;
  checkOut: {
    deviceAt: string | null;
    serverAt: string | null;
    latitude: number | null;
    longitude: number | null;
    locationLabel: string | null;
  } | null;
};

export type EmployerReviewRecordConfirmResponse = {
  workProofId: number;
  reflectionStatus: WorkProofReflectionStatus;
  confirmedAt: string;
};

export type EmployerCorrectionRequestSummaryResponse = {
  requestId: number;
  workProofId: number;
  workerId: number;
  workerName: string;
  workerEmail: string;
  role: string | null;
  workDate: string;
  originalClockInAt: string | null;
  originalClockOutAt: string | null;
  requestedClockInAt: string | null;
  requestedClockOutAt: string | null;
  recognizedClockInAt: string | null;
  recognizedClockOutAt: string | null;
  reasonCode: string | null;
  reviewReasonCode: string | null;
  reason: string;
  requestedAt: string;
  status: CorrectionRequestStatus;
};

export type EmployerCorrectionRequestsResponse = {
  requests: EmployerCorrectionRequestSummaryResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type EmployerCorrectionRequestDetailResponse = {
  requestId: number;
  workProofId: number;
  workerId: number;
  workerName: string;
  workerEmail: string;
  role: string | null;
  workDate: string;
  originalClockInAt: string | null;
  originalClockOutAt: string | null;
  requestedClockInAt: string | null;
  requestedClockOutAt: string | null;
  recognizedClockInAt: string | null;
  recognizedClockOutAt: string | null;
  reasonCode: string | null;
  reviewReasonCode: string | null;
  reason: string;
  requestMemo: string | null;
  attachmentCount: number;
  attachments: Array<{
    type: string;
    fileName: string;
    downloadAvailable: boolean;
  }>;
  requestedAt: string;
  status: CorrectionRequestStatus;
  decisionByAccountId: number | null;
  decisionByName: string | null;
  decisionAt: string | null;
  decisionMemo: string | null;
  rejectReasonCode: string | null;
};

type EmployerLoginRequest = {
  email: string;
  password: string;
};

type EmployerInvitationAcceptRequest = {
  token: string;
  email: string;
  password: string;
  displayName: string;
};

type EmployerSignupRequest = {
  companyCode: string;
  displayName: string;
  email: string;
  password: string;
};

type UpdateEmployerWorkplaceSettingsRequest = {
  address: string;
  detailAddress: string;
  latitude: number;
  longitude: number;
  allowedRadiusMeters: number;
  scheduledClockInTime: string | null;
  scheduledClockOutTime: string | null;
  overtimeRoundingUnit: AttendanceOvertimeRoundingUnit | null;
};

export async function loginEmployer(request: EmployerLoginRequest) {
  return apiRequest<EmployerAuthResponse>("/api/employer-auth/login", {
    method: "POST",
    body: request
  });
}

export async function acceptEmployerInvitation(request: EmployerInvitationAcceptRequest) {
  return apiRequest<EmployerAuthResponse>("/api/employer-auth/invitations/accept", {
    method: "POST",
    body: request
  });
}

export async function signupEmployer(request: EmployerSignupRequest) {
  return apiRequest<EmployerAuthResponse>("/api/employer-auth/signup", {
    method: "POST",
    body: request
  });
}

export async function getEmployerProfile(token: string) {
  return apiRequest<EmployerProfileResponse>("/api/employer/profile", { token });
}

export async function getEmployerWorkplaceSettings(token: string) {
  return apiRequest<EmployerWorkplaceSettingsResponse>("/api/employer/workplace-settings", {
    token
  });
}

export async function updateEmployerWorkplaceSettings(
  token: string,
  request: UpdateEmployerWorkplaceSettingsRequest
) {
  return apiRequest<EmployerWorkplaceSettingsResponse>("/api/employer/workplace-settings", {
    method: "PUT",
    body: request,
    token
  });
}

export async function getEmployerWorkerRegistrationCodes(token: string) {
  return apiRequest<EmployerWorkerRegistrationCodesResponse>(
    "/api/employer/worker-registration-codes",
    {
      token
    }
  );
}

export async function issueEmployerWorkerRegistrationCode(token: string) {
  return apiRequest<EmployerWorkerRegistrationCodeResponse>(
    "/api/employer/worker-registration-codes",
    {
      method: "POST",
      token
    }
  );
}

export async function getEmployerDashboardSummary(token: string) {
  return apiRequest<EmployerDashboardSummaryResponse>("/api/employer/dashboard/summary", {
    token
  });
}

export async function getEmployerAttendanceBoard(
  token: string,
  query: {
    weekStart?: string;
    query?: string;
    statuses?: EmployerWorkerAttendanceStatus[];
    page?: number;
    size?: number;
  }
) {
  return apiRequest<EmployerAttendanceBoardResponse>("/api/employer/dashboard/attendance-board", {
    token,
    query
  });
}

export async function getEmployerWorkers(
  token: string,
  query: {
    query?: string;
    statuses?: EmployerWorkerAttendanceStatus[];
    page?: number;
    size?: number;
  }
) {
  return apiRequest<EmployerWorkersResponse>("/api/employer/workers", {
    token,
    query
  });
}

export async function getEmployerIssues(
  token: string,
  query: {
    query?: string;
    statuses?: EmployerIssueStatus[];
    page?: number;
    size?: number;
  }
) {
  return apiRequest<EmployerIssuesResponse>("/api/employer/issues", {
    token,
    query
  });
}

export async function getEmployerReviewRecord(token: string, workProofId: number) {
  return apiRequest<EmployerReviewRequiredRecordDetailResponse>(
    `/api/employer/issues/review-records/${workProofId}`,
    { token }
  );
}

export async function confirmEmployerReviewRecord(token: string, workProofId: number) {
  return apiRequest<EmployerReviewRecordConfirmResponse>(
    `/api/employer/issues/review-records/${workProofId}/confirm`,
    {
      method: "POST",
      token
    }
  );
}

export async function getEmployerCorrectionRequests(
  token: string,
  query: {
    query?: string;
    statuses?: CorrectionRequestStatus[];
    page?: number;
    size?: number;
  }
) {
  return apiRequest<EmployerCorrectionRequestsResponse>("/api/employer/correction-requests", {
    token,
    query
  });
}

export async function getEmployerCorrectionRequest(token: string, requestId: number) {
  return apiRequest<EmployerCorrectionRequestDetailResponse>(
    `/api/employer/correction-requests/${requestId}`,
    { token }
  );
}

export async function approveEmployerCorrectionRequest(
  token: string,
  requestId: number,
  decisionMemo: string
) {
  return apiRequest<EmployerCorrectionRequestDetailResponse>(
    `/api/employer/correction-requests/${requestId}/approve`,
    {
      method: "POST",
      token,
      body: {
        decisionMemo
      }
    }
  );
}

export async function rejectEmployerCorrectionRequest(
  token: string,
  requestId: number,
  request: {
    decisionMemo: string;
    rejectReasonCode: string;
  }
) {
  return apiRequest<EmployerCorrectionRequestDetailResponse>(
    `/api/employer/correction-requests/${requestId}/reject`,
    {
      method: "POST",
      token,
      body: request
    }
  );
}
