import { useCallback, useMemo, useState } from "react";
import { KakaoWorkplaceMap } from "./components/KakaoWorkplaceMap";

type SaveState = "idle" | "saving" | "success";
type SearchState = "idle" | "searching" | "success" | "error";

const DEFAULT_RADIUS = "300m";
const DEFAULT_DETAIL_ADDRESS = "";
const DEFAULT_CENTER = { lat: 37.501274, lng: 127.039585 };

const radiusOptions = ["100m", "200m", "300m", "500m", "1km"];

const radiusMeterMap: Record<string, number> = {
  "100m": 100,
  "200m": 200,
  "300m": 300,
  "500m": 500,
  "1km": 1000
};

export function SettingsPage() {
  const [radius, setRadius] = useState(DEFAULT_RADIUS);
  const [detailAddress, setDetailAddress] = useState(DEFAULT_DETAIL_ADDRESS);
  const [center, setCenter] = useState(DEFAULT_CENTER);
  const [saveState, setSaveState] = useState<SaveState>("idle");
  const [searchQuery, setSearchQuery] = useState("");
  const [searchState, setSearchState] = useState<SearchState>("idle");
  const [searchMessage, setSearchMessage] = useState("");
  const [searchRequest, setSearchRequest] = useState<{ id: number; query: string } | null>(null);

  const kakaoMapAppKey = import.meta.env.VITE_KAKAO_MAP_APP_KEY;
  const radiusMeters = useMemo(() => radiusMeterMap[radius] ?? 300, [radius]);

  const handleCenterChange = useCallback((next: { lat: number; lng: number }) => {
    setCenter(next);
    setSaveState("idle");
  }, []);

  const handleSearchSubmit = () => {
    const trimmed = searchQuery.trim();
    if (!trimmed) {
      setSearchState("error");
      setSearchMessage("검색할 주소를 입력해 주세요.");
      return;
    }

    setSearchState("searching");
    setSearchMessage("주소를 검색 중입니다...");
    setSearchRequest((prev) => ({
      id: (prev?.id ?? 0) + 1,
      query: trimmed
    }));
  };

  const handleReset = () => {
    setRadius(DEFAULT_RADIUS);
    setDetailAddress(DEFAULT_DETAIL_ADDRESS);
    setCenter(DEFAULT_CENTER);
    setSaveState("idle");
  };

  const handleSave = () => {
    setSaveState("saving");
    window.setTimeout(() => {
      setSaveState("success");
    }, 600);
  };

  const saveLabel = saveState === "saving" ? "저장 중..." : "저장 완료";

  return (
    <div className="console-page location-settings-page">
      <header className="location-settings-header">
        <div>
          <h2 className="location-settings-title">근무 위치 설정</h2>
          <p className="location-settings-subtitle">
            기준 위치와 반경을 설정해 출근 위치 판정에 사용합니다.
          </p>
        </div>
        <div className="location-settings-actions">
          {saveState !== "idle" ? (
            <span className={`location-save-status ${saveState}`}>{saveLabel}</span>
          ) : null}
          <button type="button" className="location-ghost-button" onClick={handleReset}>
            초기화
          </button>
          <button
            type="button"
            className="location-primary-button"
            onClick={handleSave}
            disabled={saveState === "saving"}
          >
            설정 저장
          </button>
        </div>
      </header>

      <section className="location-settings-card">
        <KakaoWorkplaceMap
          appKey={kakaoMapAppKey}
          center={center}
          radiusMeters={radiusMeters}
          onCenterChange={handleCenterChange}
          searchRequest={searchRequest}
          onSearchSuccess={({ addressLabel }) => {
            setSearchState("success");
            setSearchMessage(`"${addressLabel}" 위치로 지도를 이동했어요.`);
          }}
          onSearchError={(message) => {
            setSearchState("error");
            setSearchMessage(message);
          }}
        />

        <p className="location-hint">핀을 옮겨 정확한 근무 위치를 설정할 수 있어요.</p>

        <label className="location-search-field">
            <span>주소/장소 검색</span>
            <div className="location-search-input-wrap">
              <input
                type="text"
                placeholder="예: 강남구 테헤란로 212 또는 멀티캠퍼스 역삼"
                value={searchQuery}
              onChange={(event) => {
                setSearchQuery(event.currentTarget.value);
                if (searchState !== "idle") {
                  setSearchState("idle");
                  setSearchMessage("");
                }
              }}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.preventDefault();
                  handleSearchSubmit();
                }
              }}
            />
            <button
              type="button"
              className="location-search-button"
              onClick={handleSearchSubmit}
              disabled={searchState === "searching"}
            >
              {searchState === "searching" ? "검색 중..." : "검색"}
            </button>
          </div>
        </label>

        {searchMessage ? (
          <p className={`location-search-feedback ${searchState}`}>{searchMessage}</p>
        ) : null}

        <div className="location-form">
          <label className="location-field">
            <span>
              반경 설정
              <em aria-hidden="true">*</em>
            </span>
            <select
              value={radius}
              onChange={(event) => {
                setRadius(event.currentTarget.value);
                setSaveState("idle");
              }}
            >
              {radiusOptions.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
            <small>지도의 파란 원으로 반경을 확인할 수 있어요.</small>
          </label>

          <label className="location-field">
            <span>상세 위치 메모 (선택)</span>
            <input
              type="text"
              placeholder="예: 멀티캠퍼스 역삼 1층 정문 앞"
              value={detailAddress}
              onChange={(event) => {
                setDetailAddress(event.currentTarget.value);
                setSaveState("idle");
              }}
            />
            <small>주소 검색용이 아니라, 출근 위치를 설명하는 메모입니다.</small>
          </label>
        </div>
      </section>
    </div>
  );
}
