import type {
  CorrectionRequestStatus,
  EmployerIssueItemType,
  EmployerIssueStatus
} from "../../../shared/api/employer";

export type IssueFilterKey = "all" | "pending" | "needs_review" | "approved" | "rejected";

export type IssueQueueDetail =
  | {
      kind: "correction";
      reasonCode: string | null;
      reviewReasonCode: string | null;
      recognizedCheckIn: string;
      recognizedCheckOut: string;
      requestMemo: string | null;
      decisionMemo: string | null;
      decisionByName: string | null;
      decisionAt: string | null;
      rejectReasonCode: string | null;
      attachments: Array<{
        type: string;
        fileName: string;
        downloadAvailable: boolean;
      }>;
    }
  | {
      kind: "review";
      reviewReason: string | null;
      reviewReasonCode: string;
      recognizedCheckIn: string;
      recognizedCheckOut: string;
      recordStatus: string;
      reflectionStatus: string;
      workedMinutes: number;
      memo: string | null;
      editReason: string | null;
      clockOutOutsideAllowedRadius: boolean;
      attachmentCount: number;
      workplaceName: string | null;
      workplaceAddress: string | null;
      checkInLabel: string | null;
      checkOutLabel: string | null;
    };

export type IssueQueueItem = {
  key: string;
  itemType: EmployerIssueItemType;
  issueStatus: EmployerIssueStatus | null;
  correctionStatus: CorrectionRequestStatus | null;
  requestId: number | null;
  workProofId: number;
  workerId: number;
  workerName: string;
  workerEmail: string;
  role: string;
  workDate: string;
  originalCheckIn: string;
  originalCheckOut: string;
  requestedCheckIn: string;
  requestedCheckOut: string;
  recognizedCheckIn: string;
  recognizedCheckOut: string;
  reasonCode: string | null;
  reviewReasonCode: string | null;
  reason: string;
  requestedAt: string;
  detailState: "idle" | "loading" | "loaded" | "error";
  detail: IssueQueueDetail | null;
  detailError: string | null;
};
