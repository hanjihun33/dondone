import {
  requestFilters,
  requestRows,
  requestSummary
} from "../../mocks/employerConsoleData";
import { Badge } from "../../shared/ui/Badge";

export function EmployerDashboardPage() {
  return (
    <div className="console-page">
      {/* ── Page Title ── */}
      <div className="page-title-bar">
        <div className="page-title-left">
          <h2 className="page-title">요청 관리</h2>
          <span className="title-help-icon">?</span>
        </div>
        <button className="download-btn" type="button">
          <span className="dl-icon">↓</span>
          다운로드
        </button>
      </div>

      {/* ── Filter Bar ── */}
      <div className="filter-bar">
        <div className="filter-bar-left">
          <button className="filter-pill" type="button">
            <span className="pill-icon">📅</span>
            {requestFilters.dateRange}
          </button>
          <button className="filter-pill" type="button">
            {requestFilters.category} ▾
          </button>
        </div>
        <div className="request-total">
          총 요청 수: <strong>{requestSummary.total}</strong>
        </div>
      </div>

      {/* ── Summary Stats ── */}
      <section className="request-summary-grid">
        <article className="summary-stat-card">
          <span>대기중</span>
          <strong>{requestSummary.pending}</strong>
        </article>
        <article className="summary-stat-card">
          <span>승인됨</span>
          <strong>{requestSummary.approved}</strong>
        </article>
        <article className="summary-stat-card">
          <span>거절됨</span>
          <strong>{requestSummary.rejected}</strong>
        </article>
        <article className="summary-stat-card">
          <span>자동 처리</span>
          <strong>{requestSummary.autoResolved}</strong>
        </article>
      </section>

      {/* ── Request Table (Shiftee-style) ── */}
      <section className="request-board">
        {/* Column-level search inputs */}
        <div className="request-search-row">
          {["검색..", "검색..", "검색..", "", "검색..", "검색..", "검색.."].map(
            (ph, i) => (
              <div className="search-slot" key={i}>
                {ph && <input type="text" placeholder={ph} />}
              </div>
            )
          )}
        </div>

        {/* Table Header */}
        <div className="request-table-head">
          <span>요청 종류</span>
          <span>요청 보낸 사람</span>
          <span>요청사항</span>
          <span></span>
          <span>요청 사유</span>
          <span>상태</span>
          <span>관리</span>
        </div>

        {/* Table Body */}
        <div className="request-table">
          {requestRows.map((row, index) => (
            <div className="request-row" key={`${row.type}-${row.requester}-${index}`}>
              <div className="request-cell strong">{row.type}</div>
              <div className="request-cell">{row.requester}</div>
              <div className="request-cell">
                <div className="request-detail-main">{row.requestDetail}</div>
                {row.locationHint && (
                  <div className="request-detail-sub">📍 {row.locationHint}</div>
                )}
              </div>
              <div className="request-cell" />
              <div className="request-cell muted">{row.reason}</div>
              <div className="request-cell status-cell">
                <Badge tone={row.statusTone}>{row.statusText}</Badge>
                <div className="request-manager">{row.manager}</div>
              </div>
              <div className="request-cell action-cell">
                {row.actionPrimary && (
                  <button className="mini-action-btn primary" type="button">
                    {row.actionPrimary}
                  </button>
                )}
                {row.actionSecondary && (
                  <button className="mini-action-btn danger-outline" type="button">
                    {row.actionSecondary}
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* ── Insight Cards ── */}
      <section className="console-insight-grid">
        <article className="card insight-card">
          <div className="insight-header">
            <div>
              <p className="insight-kicker">Position Snapshot</p>
              <h3>위치 기반 요청 확인</h3>
            </div>
            <Badge tone="soft">현장 근거</Badge>
          </div>
          <p className="insight-copy">
            외부 미팅 후 복귀한 출근 요청은 위치 스냅샷, 근무지 반경, 수정 사유를
            함께 보고 승인합니다.
          </p>
          <div className="map-preview-card">
            <div className="map-grid" />
            <div className="map-pin major" style={{ left: "44%", top: "42%" }} />
            <div className="map-pin" style={{ left: "36%", top: "56%" }} />
            <div className="map-pin" style={{ left: "57%", top: "50%" }} />
            <div className="map-route" />
          </div>
        </article>

        <article className="card insight-card">
          <div className="insight-header">
            <div>
              <p className="insight-kicker">DonDone Rule</p>
              <h3>운영 메모</h3>
            </div>
            <Badge tone="warn">P0 안내</Badge>
          </div>
          <ul className="insight-list">
            <li>근로자를 점수화하지 않고 확인 요청이 들어온 건만 검토합니다.</li>
            <li>Proof Pack과 변경 이력은 설명 책임을 줄이는 보조 근거로만 사용합니다.</li>
            <li>실제 급여 확정이나 자금 운용 로직은 이 콘솔에서 처리하지 않습니다.</li>
          </ul>
        </article>
      </section>
    </div>
  );
}
