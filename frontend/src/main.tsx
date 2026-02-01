import React from "react";
import ReactDOM from "react-dom/client";

import RouteOptionsBar from "./components/RouteOptionsBar";
import { StationSearch } from "./components/StationSearch";

import type { Station } from "./services/stations";
import { routeMulti } from "./services/api";
import type { MultiRouteResponse, LegSummary, Segment, RouteMultiOptions } from "./services/api";

type Stop = Station | null;

//Format minutes nicely (e.g., 14 min, 1h, 1h 10m)
function formatMinutes(mins: number) {
  const h = Math.floor(mins / 60);
  const m = mins % 60;
  if (h <= 0) return `${m} min`;
  if (m === 0) return `${h}h`;
  return `${h}h ${m}m`;
}

//Convert TfL mode ids into nicer labels.
function prettyMode(mode: string | null | undefined) {
  if (!mode) return "Travel";
  const m = mode.toLowerCase();
  if (m === "walking") return "Walk";
  if (m === "tube") return "Tube";
  if (m === "bus") return "Bus";
  if (m === "dlr") return "DLR";
  if (m === "overground") return "Overground";
  if (m === "national-rail" || m === "nationalrail") return "National Rail";
  if (m === "elizabeth-line") return "Elizabeth line";
  return mode;
}

function SegmentRow({ s }: { s: Segment }) {
  return (
    <div
      style={{
        display: "grid",
        gap: 4,
        padding: "10px 12px",
        border: "1px solid #eee",
        borderRadius: 10,
        background: "white",
      }}
    >
      <div style={{ display: "flex", justifyContent: "space-between", gap: 12 }}>
        <div style={{ fontWeight: 600 }}>
          {prettyMode(s.mode)}
          {s.line ? ` • ${s.line}` : ""}
        </div>
        <div style={{ color: "#555" }}>{formatMinutes(s.durationMinutes)}</div>
      </div>

      {(s.from || s.to) && (
        <div style={{ color: "#333" }}>
          {s.from ? s.from : "Start"} → {s.to ? s.to : "End"}
        </div>
      )}

      {s.direction && <div style={{ color: "#666", fontSize: 13 }}>{s.direction}</div>}
    </div>
  );
}

function LegCard({ leg, index }: { leg: LegSummary; index: number }) {
  return (
    <div
      style={{
        border: "1px solid #e6e6e6",
        borderRadius: 14,
        padding: 14,
        background: "white",
        boxShadow: "0 1px 6px rgba(0,0,0,0.06)",
      }}
    >
      <div style={{ display: "flex", justifyContent: "space-between", gap: 12, alignItems: "baseline" }}>
        <div style={{ fontWeight: 700, fontSize: 16 }}>
          Leg {index + 1}: {leg.fromName} → {leg.toName}
        </div>
        <div style={{ color: "#333", fontWeight: 600 }}>{formatMinutes(leg.durationMinutes)}</div>
      </div>

      <div style={{ marginTop: 6, color: "#444" }}>
        <span style={{ fontWeight: 600 }}>{leg.summary}</span>
        <span style={{ color: "#666" }}> • {leg.interchanges} change{leg.interchanges === 1 ? "" : "s"}</span>
      </div>

      <div style={{ marginTop: 12, display: "grid", gap: 10 }}>
        {leg.segments.map((s, i) => (
          <SegmentRow key={i} s={s} />
        ))}
      </div>
    </div>
  );
}

function App() {
  // Stops list: (From, Stops, To)
  const [stops, setStops] = React.useState<Stop[]>([null, null]);

  //default = include everything 
  const [options, setOptions] = React.useState<RouteMultiOptions>({
    includeBus: true,
    includeTram: true,
    sortBy: "FASTEST",
  });

  const setStopAt = (idx: number, station: Station | null) => {
    setStops((prev) => {
      const next = [...prev];
      next[idx] = station;
      return next;
    });
  };

  const addStop = () => {
    setStops((prev) => {
      const next = [...prev];
      next.splice(next.length - 1, 0, null); // insert before last ("To")
      return next;
    });
  };

  const removeStop = (idx: number) => {
    setStops((prev) => prev.filter((_, i) => i !== idx));
  };

  const moveStop = (idx: number, dir: -1 | 1) => {
    setStops((prev) => {
      const next = [...prev];
      const j = idx + dir;
      if (j < 1 || j > next.length - 2) return prev; // only move intermediate stops
      const tmp = next[idx];
      next[idx] = next[j];
      next[j] = tmp;
      return next;
    });
  };

  const selection = {
    from: stops[0],
    via: stops.slice(1, -1),
    to: stops[stops.length - 1],
  };

  const [routeResult, setRouteResult] = React.useState<MultiRouteResponse | null>(null);
  const [routeError, setRouteError] = React.useState<string | null>(null);
  const [routing, setRouting] = React.useState(false);

  const from = selection.from;
  const to = selection.to;

  return (
    <div style={{ padding: 18, fontFamily: "system-ui", background: "#f7f7f8", minHeight: "100vh" }}>
      <div style={{ maxWidth: 900, margin: "0 auto", display: "grid", gap: 16 }}>
        <div>
          <h1 style={{ margin: 0 }}>MultiPlanner</h1>
          <p style={{ marginTop: 6, color: "#444" }}>
            Station autocomplete (DB) + add/reorder stops + TfL routing
          </p>
        </div>

        {/* Search panel */}
        <div
          style={{
            background: "white",
            border: "1px solid #e6e6e6",
            borderRadius: 16,
            padding: 16,
            boxShadow: "0 1px 10px rgba(0,0,0,0.06)",
          }}
        >
          <div style={{ display: "grid", gap: 14 }}>
            <StationSearch
              label="From"
              placeholder="Type a station name"
              value={stops[0]}
              onChange={(s) => setStopAt(0, s)}
            />

            {stops.slice(1, -1).map((_, i) => {
              const idx = i + 1;
              return (
                <div key={idx} style={{ display: "grid", gap: 8 }}>
                  <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                    <strong>Stop {i + 1}</strong>
                    <button type="button" onClick={() => moveStop(idx, -1)}>↑</button>
                    <button type="button" onClick={() => moveStop(idx, 1)}>↓</button>
                    <button type="button" onClick={() => removeStop(idx)}>Remove</button>
                  </div>

                  <StationSearch
                    label=""
                    placeholder="Type a station name"
                    value={stops[idx]}
                    onChange={(s) => setStopAt(idx, s)}
                  />
                </div>
              );
            })}

            <StationSearch
              label="To"
              placeholder="Type a station name"
              value={stops[stops.length - 1]}
              onChange={(s) => setStopAt(stops.length - 1, s)}
            />

            {/* options bar with bus/tram toggles + sort preference) */}
            <RouteOptionsBar options={options} onChange={setOptions} />

            {/* Buttons row */}
            <div style={{ display: "flex", gap: 10, alignItems: "center", flexWrap: "wrap" }}>
              <button type="button" onClick={addStop}>Add stop</button>

              <button
                type="button"
                onClick={() => {
                  setStops([null, null]);
                  setRouteResult(null);
                  setRouteError(null);

                  // Reset options back to defaults as well
                  setOptions({
                    includeBus: true,
                    includeTram: true,
                    sortBy: "FASTEST",
                  });
                }}
              >
                Reset
              </button>

              <button
                type="button"
                disabled={!from || !to || routing}
                onClick={async () => {
                  if (!from || !to) return;

                  // Build full stop list: [from, ...via, to]
                  const all: Station[] = [];
                  all.push(from);

                  for (const s of selection.via) {
                    if (s) all.push(s);
                  }

                  all.push(to);

                  // Guard: no nulls
                  if (all.some((s) => !s || !s.name)) return;

                  setRouting(true);
                  setRouteError(null);
                  setRouteResult(null);

                  try {
                    //  Send options to backend 
                    const data = await routeMulti(all, options);
                    setRouteResult(data);
                  } catch (e) {
                    const msg = e instanceof Error ? e.message : String(e);
                    setRouteError(msg);
                  }finally {
                    setRouting(false);
                  }
                }}
                style={{
                  padding: "8px 14px",
                  borderRadius: 10,
                  border: "1px solid #111",
                  background: routing ? "#eee" : "#111",
                  color: routing ? "#111" : "white",
                  fontWeight: 700,
                }}
              >
                {routing ? "Routing..." : "Plan route"}
              </button>

              {routeError && <span style={{ color: "crimson", fontWeight: 600 }}>Error: {routeError}</span>}
            </div>
          </div>
        </div>

        {/* Results */}
        {routeResult && (
          <div style={{ display: "grid", gap: 12 }}>
            <div
              style={{
                background: "white",
                border: "1px solid #e6e6e6",
                borderRadius: 16,
                padding: 14,
                boxShadow: "0 1px 10px rgba(0,0,0,0.06)",
              }}
            >
              <div style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
                <div style={{ fontWeight: 800, fontSize: 18 }}>Trip summary</div>

                <div style={{ display: "flex", gap: 16, color: "#333", fontWeight: 700 }}>
                  <div>Total: {formatMinutes(routeResult.totalDurationMinutes)}</div>
                  <div>Changes: {routeResult.totalInterchanges}</div>
                </div>
              </div>

              <div style={{ marginTop: 6, color: "#666" }}>
                {routeResult.legs} leg{routeResult.legs === 1 ? "" : "s"}
              </div>

              {/* Tiny “Maps style” line showing current options */}
              <div style={{ marginTop: 8, fontSize: 13, color: "#666" }}>
                {options.sortBy === "FASTEST" ? "Fastest route" : "Fewest transfers"} •{" "}
                {options.includeBus ? "Bus" : "No bus"} •{" "}
                {options.includeTram ? "Tram" : "No tram"}
              </div>
            </div>

            <div style={{ display: "grid", gap: 12 }}>
              {routeResult.results.map((leg, i) => (
                <LegCard key={i} leg={leg} index={i} />
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
