type CityPoint = {
  id: number;
  top: number;
  left: number;
};

type Connection = {
  source: number;
  target: number;
};

type ConnectivityLine = {
  key: string;
  path: string;
  delay: string;
};

const cities: CityPoint[] = [
  { id: 1, top: 11, left: 12 },
  { id: 2, top: 24, left: 22 },
  { id: 3, top: 62, left: 32 },
  { id: 4, top: 20, left: 52 },
  { id: 5, top: 41, left: 73 },
  { id: 6, top: 28, left: 79 },
  { id: 7, top: 11, left: 91 },
  { id: 8, top: 65, left: 81 }
];

const connections: Connection[] = [
  { source: 6, target: 1 },
  { source: 6, target: 2 },
  { source: 6, target: 3 },
  { source: 6, target: 4 },
  { source: 6, target: 5 },
  { source: 6, target: 7 },
  { source: 6, target: 8 }
];

function calculatePath(start: CityPoint, end: CityPoint) {
  const midX = (start.left + end.left) / 2;
  const midY = (start.top + end.top) / 2;
  const horizontalDistance = Math.abs(end.left - start.left);
  const verticalDistance = Math.abs(end.top - start.top);
  const controlY = midY - Math.max(8, Math.min(20, horizontalDistance * 0.3 + verticalDistance * 0.15));

  return `M ${start.left} ${start.top} Q ${midX} ${controlY} ${end.left} ${end.top}`;
}

function buildConnectivityLines(cityPoints: CityPoint[], cityConnections: Connection[]): ConnectivityLine[] {
  const cityById = new Map(cityPoints.map((city) => [city.id, city]));

  return cityConnections.flatMap((connection, index) => {
    const start = cityById.get(connection.source);
    const end = cityById.get(connection.target);

    if (!start || !end) {
      return [];
    }

    return [
      {
        key: `${connection.source}-${connection.target}`,
        path: calculatePath(start, end),
        delay: `${index * 0.34}s`
      }
    ];
  });
}

const connectivityLines = buildConnectivityLines(cities, connections);

export function RemoteConnectivityPanel() {
  return (
    <section className="connectivity-panel" aria-label="DonDone 서비스 소개">
      <div className="connectivity-main">
        <h1 className="connectivity-title">
          <span className="title-remote">DonDone</span>
          <span className="title-connectivity">송금을 쉽고 간편하게</span>
        </h1>

        <div className="connectivity-map-wrapper">
          <div className="connectivity-map-background" />

          <svg className="connectivity-svg-overlay" viewBox="0 0 100 100" preserveAspectRatio="none">
            {connectivityLines.map((line) => (
              <path
                key={line.key}
                d={line.path}
                className="connectivity-line"
                style={{ animationDelay: line.delay }}
              />
            ))}
          </svg>

          {cities.map((city) => (
            <div
              key={city.id}
              className="connectivity-city"
              style={{ top: `${city.top}%`, left: `${city.left}%` }}
              aria-hidden="true"
            >
              <span className="connectivity-city-point" />
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
