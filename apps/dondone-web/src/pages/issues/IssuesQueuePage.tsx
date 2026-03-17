import { issueQueue } from "../../mocks/employerConsoleData";
import { Badge } from "../../shared/ui/Badge";
import { SectionCard } from "../../shared/ui/SectionCard";

export function IssuesQueuePage() {
  return (
    <div className="board">
      <header className="topbar">
        <div>
          <p className="headline-kicker">정산 이슈 큐</p>
          <h2 className="headline-title">
            근로자 확인 요청 이후 필요한 설명과 정정만 확인합니다.
          </h2>
          <p className="headline-sub">
            회사는 연결된 경우에만, 운영 콘솔에서 해당 월의 확인 요청 상태와
            근거 요약을 보고 설명/정정에 참여하는 보조 축으로 동작합니다.
          </p>
        </div>
        <div className="topbar-actions">
          <Badge tone="warn">열린 이슈 4건</Badge>
        </div>
      </header>

      <SectionCard
        title="확인 필요 항목"
        description="Proof Pack, 변경 이력, 설명 메모가 함께 검토돼야 하는 항목만 남깁니다."
      >
        <div className="risk-board">
          {issueQueue.map((item) => (
            <div className="risk-item" key={item.title}>
              <div className="risk-topline">
                <strong>{item.title}</strong>
                <Badge tone={item.badgeTone}>{item.badgeText}</Badge>
              </div>
              <p>{item.description}</p>
            </div>
          ))}
        </div>
      </SectionCard>
    </div>
  );
}
