import { verifiedWorkerSummaries } from "../../mocks/employerConsoleData";
import { Badge } from "../../shared/ui/Badge";
import { SectionCard } from "../../shared/ui/SectionCard";

export function WorkerSummaryPage() {
  return (
    <div className="board">
      <header className="topbar">
        <div>
          <p className="headline-kicker">Verified Worker Summary</p>
          <h2 className="headline-title">
            점수 대신 확인 신호와 기록 일관성만 요약합니다.
          </h2>
          <p className="headline-sub">
            근로자를 줄 세우지 않고, 재직 확인과 최근 기록 신뢰도만 운영 보조
            신호로 보여줍니다.
          </p>
        </div>
        <div className="topbar-actions">
          <Badge tone="success">22명 확인 완료</Badge>
        </div>
      </header>

      <SectionCard
        title="요약 카드"
        description="근무 신뢰도, 정산 이슈 여부, 선지급 검토 상태를 내부 보조 카드로 표시합니다."
      >
        <div className="summary-list">
          {verifiedWorkerSummaries.map((summary) => (
            <div className="summary-card" key={summary.title}>
              <strong>{summary.title}</strong>
              <p>{summary.description}</p>
              <div className="summary-meta">
                {summary.chips.map((chip) => (
                  <span className="summary-chip" key={chip}>
                    {chip}
                  </span>
                ))}
              </div>
            </div>
          ))}
        </div>
      </SectionCard>
    </div>
  );
}
