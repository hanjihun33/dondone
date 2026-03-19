import { useEffect, useRef, useState } from "react";

type MapCenter = {
  lat: number;
  lng: number;
};

type KakaoWorkplaceMapProps = {
  appKey?: string;
  center: MapCenter;
  radiusMeters: number;
  onCenterChange: (next: MapCenter) => void;
  searchRequest?: {
    id: number;
    query: string;
  } | null;
  onSearchSuccess?: (result: {
    center: MapCenter;
    addressLabel: string;
  }) => void;
  onSearchError?: (message: string) => void;
};

type MapState = "loading" | "ready" | "missing-key" | "error";

let kakaoSdkLoader: Promise<void> | null = null;

function getKakao() {
  return (window as Window & { kakao?: any }).kakao;
}

function buildRadiusBounds(kakao: any, center: MapCenter, radiusMeters: number) {
  const paddedRadius = Math.max(radiusMeters * 1.25, radiusMeters + 80);
  const latDelta = paddedRadius / 111_320;
  const lngDivisor = Math.max(Math.cos((center.lat * Math.PI) / 180), 0.12);
  const lngDelta = paddedRadius / (111_320 * lngDivisor);

  const southWest = new kakao.maps.LatLng(center.lat - latDelta, center.lng - lngDelta);
  const northEast = new kakao.maps.LatLng(center.lat + latDelta, center.lng + lngDelta);
  return new kakao.maps.LatLngBounds(southWest, northEast);
}

function fitMapToRadius(kakao: any, map: any, center: MapCenter, radiusMeters: number) {
  map.setBounds(buildRadiusBounds(kakao, center, radiusMeters));
}

function loadKakaoMapsSdk(appKey: string) {
  const kakao = getKakao();
  if (kakao?.maps) {
    return Promise.resolve();
  }

  if (kakaoSdkLoader) {
    return kakaoSdkLoader;
  }

  kakaoSdkLoader = new Promise<void>((resolve, reject) => {
    const src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${appKey}&autoload=false&libraries=services`;
    const existing = document.querySelector<HTMLScriptElement>("script[data-kakao-maps-sdk='true']");

    const onScriptLoaded = () => {
      const loadedKakao = getKakao();
      if (!loadedKakao?.maps) {
        reject(new Error("Kakao Maps SDK is unavailable after loading script."));
        return;
      }

      loadedKakao.maps.load(() => resolve());
    };

    if (existing) {
      if (existing.getAttribute("src") !== src) {
        reject(new Error("Kakao Maps SDK script already exists with different app key."));
        return;
      }

      if (getKakao()?.maps) {
        onScriptLoaded();
        return;
      }

      existing.addEventListener("load", onScriptLoaded, { once: true });
      existing.addEventListener("error", () => reject(new Error("Failed to load Kakao Maps SDK.")), {
        once: true
      });
      return;
    }

    const script = document.createElement("script");
    script.src = src;
    script.async = true;
    script.dataset.kakaoMapsSdk = "true";
    script.addEventListener("load", onScriptLoaded, { once: true });
    script.addEventListener("error", () => reject(new Error("Failed to load Kakao Maps SDK.")), {
      once: true
    });
    document.head.appendChild(script);
  });

  return kakaoSdkLoader;
}

export function KakaoWorkplaceMap({
  appKey,
  center,
  radiusMeters,
  onCenterChange,
  searchRequest,
  onSearchSuccess,
  onSearchError
}: KakaoWorkplaceMapProps) {
  const mapContainerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<any>(null);
  const markerRef = useRef<any>(null);
  const circleRef = useRef<any>(null);
  const geocoderRef = useRef<any>(null);
  const placesRef = useRef<any>(null);
  const lastHandledSearchIdRef = useRef<number | null>(null);
  const [mapState, setMapState] = useState<MapState>(appKey ? "loading" : "missing-key");

  useEffect(() => {
    if (!appKey) {
      setMapState("missing-key");
      return;
    }

    if (!mapContainerRef.current) {
      return;
    }

    let isCancelled = false;
    setMapState("loading");

    loadKakaoMapsSdk(appKey)
      .then(() => {
        if (isCancelled || !mapContainerRef.current) {
          return;
        }

        const kakao = getKakao();
        if (!kakao?.maps) {
          setMapState("error");
          return;
        }

        if (!mapRef.current) {
          const centerLatLng = new kakao.maps.LatLng(center.lat, center.lng);
          const map = new kakao.maps.Map(mapContainerRef.current, {
            center: centerLatLng,
            level: 3
          });

          const zoomControl = new kakao.maps.ZoomControl();
          map.addControl(zoomControl, kakao.maps.ControlPosition.RIGHT);

          const marker = new kakao.maps.Marker({
            position: centerLatLng,
            draggable: true
          });
          marker.setMap(map);

          const circle = new kakao.maps.Circle({
            center: centerLatLng,
            radius: radiusMeters,
            strokeWeight: 1,
            strokeColor: "#6A78E5",
            strokeOpacity: 0.65,
            strokeStyle: "solid",
            fillColor: "#6A78E5",
            fillOpacity: 0.18
          });
          circle.setMap(map);

          kakao.maps.event.addListener(marker, "dragend", () => {
            const position = marker.getPosition();
            onCenterChange({
              lat: position.getLat(),
              lng: position.getLng()
            });
          });

          kakao.maps.event.addListener(map, "click", (mouseEvent: any) => {
            const position = mouseEvent.latLng;
            marker.setPosition(position);
            onCenterChange({
              lat: position.getLat(),
              lng: position.getLng()
            });
          });

          mapRef.current = map;
          markerRef.current = marker;
          circleRef.current = circle;
          geocoderRef.current =
            kakao.maps.services && kakao.maps.services.Geocoder
              ? new kakao.maps.services.Geocoder()
              : null;
          placesRef.current =
            kakao.maps.services && kakao.maps.services.Places
              ? new kakao.maps.services.Places()
              : null;
          fitMapToRadius(kakao, map, center, radiusMeters);
        }

        setMapState("ready");
      })
      .catch(() => {
        if (!isCancelled) {
          setMapState("error");
        }
      });

    return () => {
      isCancelled = true;
    };
  }, [appKey, onCenterChange]);

  useEffect(() => {
    const kakao = getKakao();
    if (!kakao?.maps || !mapRef.current || !markerRef.current || !circleRef.current) {
      return;
    }

    const centerLatLng = new kakao.maps.LatLng(center.lat, center.lng);
    markerRef.current.setPosition(centerLatLng);
    circleRef.current.setPosition(centerLatLng);
    circleRef.current.setRadius(radiusMeters);
    fitMapToRadius(kakao, mapRef.current, center, radiusMeters);
  }, [center.lat, center.lng, radiusMeters]);

  useEffect(() => {
    if (!searchRequest) {
      return;
    }

    if (lastHandledSearchIdRef.current === searchRequest.id) {
      return;
    }

    if (mapState !== "ready") {
      return;
    }

    const kakao = getKakao();
    if (
      !kakao?.maps?.services ||
      !geocoderRef.current ||
      !placesRef.current ||
      !mapRef.current ||
      !markerRef.current
    ) {
      lastHandledSearchIdRef.current = searchRequest.id;
      onSearchError?.("주소 검색 서비스를 사용할 수 없습니다.");
      return;
    }

    lastHandledSearchIdRef.current = searchRequest.id;

    const applySearchResult = (resultItem: any, addressLabel: string) => {
      const resolvedCenter = {
        lat: Number(resultItem.y),
        lng: Number(resultItem.x)
      };

      const centerLatLng = new kakao.maps.LatLng(resolvedCenter.lat, resolvedCenter.lng);
      markerRef.current.setPosition(centerLatLng);
      circleRef.current?.setPosition(centerLatLng);
      fitMapToRadius(kakao, mapRef.current, resolvedCenter, radiusMeters);
      onCenterChange(resolvedCenter);
      onSearchSuccess?.({
        center: resolvedCenter,
        addressLabel
      });
    };

    const runKeywordSearch = () => {
      placesRef.current.keywordSearch(searchRequest.query, (placeResults: any[], placeStatus: string) => {
        if (placeStatus !== kakao.maps.services.Status.OK || !placeResults.length) {
          onSearchError?.("검색 결과가 없어요. 주소 또는 장소명을 다시 확인해 주세요.");
          return;
        }

        const firstPlace = placeResults[0];
        const label = firstPlace.place_name ?? firstPlace.road_address_name ?? firstPlace.address_name;
        applySearchResult(firstPlace, label);
      });
    };

    geocoderRef.current.addressSearch(searchRequest.query, (result: any[], status: string) => {
      if (status === kakao.maps.services.Status.OK && result.length) {
        const first = result[0];
        const label =
          first.road_address?.address_name ?? first.address?.address_name ?? first.address_name;
        applySearchResult(first, label);
        return;
      }

      runKeywordSearch();
    });
  }, [mapState, onCenterChange, onSearchError, onSearchSuccess, radiusMeters, searchRequest]);

  return (
    <div className="location-map-shell">
      <div className="location-map-canvas" ref={mapContainerRef} />
      <div className="location-map-credit">지도 데이터 © Kakao</div>

      {mapState !== "ready" ? (
        <div className="location-map-overlay" role="status">
          {mapState === "loading" ? <strong>지도를 불러오는 중입니다.</strong> : null}
          {mapState === "missing-key" ? (
            <>
              <strong>카카오맵 키가 필요합니다.</strong>
              <p>`VITE_KAKAO_MAP_APP_KEY`를 설정해 주세요.</p>
            </>
          ) : null}
          {mapState === "error" ? (
            <>
              <strong>지도를 불러오지 못했습니다.</strong>
              <p>카카오맵 키와 도메인 등록 상태를 확인해 주세요.</p>
            </>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}
