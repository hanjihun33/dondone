import { useEffect, useMemo, useState } from "react";
import { useNavigate, useOutletContext } from "react-router-dom";
import { ApiError } from "../../shared/api/client";
import {
  getEmployerWorkplaceSettings,
  updateEmployerWorkplaceSettings
} from "../../shared/api/employer";
import { clearStoredSession, getStoredAccessToken } from "../../shared/auth/session";
import { KakaoWorkplaceMap } from "./components/KakaoWorkplaceMap";

type SaveState = "idle" | "saving" | "success" | "error";
type SearchState = "idle" | "searching" | "success" | "error";

type SettingsFormState = {
  address: string;
  detailAddress: string;
  center: { lat: number; lng: number };
  radiusMeters: number;
};

const DEFAULT_CENTER = { lat: 37.501274, lng: 127.039585 };
const radiusOptions = [100, 200, 300, 500, 1000];

export function SettingsPage() {
  const navigate = useNavigate();
  const { refreshTick } = useOutletContext<{ refreshTick: number }>();
  const [formState, setFormState] = useState<SettingsFormState>({
    address: "",
    detailAddress: "",
    center: DEFAULT_CENTER,
    radiusMeters: 300
  });
  const [initialState, setInitialState] = useState<SettingsFormState | null>(null);
  const [saveState, setSaveState] = useState<SaveState>("idle");
  const [searchQuery, setSearchQuery] = useState("");
  const [searchState, setSearchState] = useState<SearchState>("idle");
  const [searchMessage, setSearchMessage] = useState("");
  const [loadMessage, setLoadMessage] = useState<string | null>(null);
  const [searchRequest, setSearchRequest] = useState<{ id: number; query: string } | null>(null);

  const kakaoMapAppKey = import.meta.env.VITE_KAKAO_MAP_APP_KEY;
  const radiusMeters = useMemo(() => formState.radiusMeters, [formState.radiusMeters]);

  useEffect(() => {
    const token = getStoredAccessToken();
    if (!token) {
      clearStoredSession();
      navigate("/", { replace: true });
      return;
    }

    let isDisposed = false;
    setLoadMessage("근무 위치 정보를 불러오는 중입니다...");

    void getEmployerWorkplaceSettings(token)
      .then((response) => {
        if (isDisposed) {
          return;
        }

        const nextState: SettingsFormState = {
          address: response.address,
          detailAddress: response.detailAddress ?? "",
          center: {
            lat: response.latitude,
            lng: response.longitude
          },
          radiusMeters: response.allowedRadiusMeters
        };

        setFormState(nextState);
        setInitialState(nextState);
        setSearchQuery(response.address);
        setLoadMessage(`기본 사업장 ${response.workplaceName} / 연결 근로자 ${response.activeMembershipCount}명`);
      })
      .catch((error: unknown) => {
        if (isDisposed) {
          return;
        }

        if (error instanceof ApiError && error.status === 401) {
          clearStoredSession();
          navigate("/", { replace: true });
          return;
        }

        setLoadMessage(error instanceof Error ? error.message : "근무 위치 정보를 불러오지 못했습니다.");
      });

    return () => {
      isDisposed = true;
    };
  }, [navigate, refreshTick]);

  const handleCenterChange = (next: { lat: number; lng: number }) => {
    setFormState((prev) => ({
      ...prev,
      center: next
    }));
    setSaveState("idle");
  };

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
    if (!initialState) {
      return;
    }

    setFormState(initialState);
    setSearchQuery(initialState.address);
    setSaveState("idle");
    setSearchState("idle");
    setSearchMessage("");
  };

  const handleSave = async () => {
    const token = getStoredAccessToken();
    if (!token) {
      clearStoredSession();
      navigate("/", { replace: true });
      return;
    }

    if (!formState.address.trim()) {
      setSaveState("error");
      setLoadMessage("주소를 입력한 뒤 저장해 주세요.");
      return;
    }

    setSaveState("saving");
    setLoadMessage(null);

    try {
      const response = await updateEmployerWorkplaceSettings(token, {
        address: formState.address.trim(),
        detailAddress: formState.detailAddress.trim(),
        latitude: formState.center.lat,
        longitude: formState.center.lng,
        allowedRadiusMeters: formState.radiusMeters
      });

      const nextState: SettingsFormState = {
        address: response.address,
        detailAddress: response.detailAddress ?? "",
        center: {
          lat: response.latitude,
          lng: response.longitude
        },
        radiusMeters: response.allowedRadiusMeters
      };

      setInitialState(nextState);
      setFormState(nextState);
      setSearchQuery(response.address);
      setSaveState("success");
      setLoadMessage(`설정이 저장되었습니다. ${response.effectiveFrom ?? "즉시"} 반영됩니다.`);
    } catch (error: unknown) {
      if (error instanceof ApiError && error.status === 401) {
        clearStoredSession();
        navigate("/", { replace: true });
        return;
      }

      setSaveState("error");
      setLoadMessage(error instanceof Error ? error.message : "설정을 저장하지 못했습니다.");
    }
  };

  const saveLabel =
    saveState === "saving"
      ? "저장 중..."
      : saveState === "success"
        ? "저장 완료"
        : saveState === "error"
          ? "저장 실패"
          : "";

  return (
    <div className="console-page location-settings-page">
      <header className="location-settings-header">
        <div>
          <h2 className="location-settings-title">근무 위치 설정</h2>
          <p className="location-settings-subtitle">
            기준 위치와 반경을 설정해 출근 위치 판정에 사용합니다.
          </p>
          {loadMessage ? <p className="location-settings-subtitle">{loadMessage}</p> : null}
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
          center={formState.center}
          radiusMeters={radiusMeters}
          onCenterChange={handleCenterChange}
          searchRequest={searchRequest}
          onSearchSuccess={({ addressLabel, center }) => {
            setFormState((prev) => ({
              ...prev,
              address: addressLabel,
              center
            }));
            setSearchQuery(addressLabel);
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
              저장 주소
              <em aria-hidden="true">*</em>
            </span>
            <input
              type="text"
              value={formState.address}
              onChange={(event) => {
                setFormState((prev) => ({
                  ...prev,
                  address: event.currentTarget.value
                }));
                setSaveState("idle");
              }}
            />
            <small>사업장 주소로 저장됩니다.</small>
          </label>

          <label className="location-field">
            <span>
              반경 설정
              <em aria-hidden="true">*</em>
            </span>
            <select
              value={formState.radiusMeters}
              onChange={(event) => {
                setFormState((prev) => ({
                  ...prev,
                  radiusMeters: Number(event.currentTarget.value)
                }));
                setSaveState("idle");
              }}
            >
              {radiusOptions.map((option) => (
                <option key={option} value={option}>
                  {option}m
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
              value={formState.detailAddress}
              onChange={(event) => {
                setFormState((prev) => ({
                  ...prev,
                  detailAddress: event.currentTarget.value
                }));
                setSaveState("idle");
              }}
            />
            <small>출근 위치를 설명하는 메모입니다.</small>
          </label>
        </div>
      </section>
    </div>
  );
}
