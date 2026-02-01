import React from "react";
import ReactDOM from "react-dom/client";
import { StationSearch } from "./components/StationSearch";
import { Station } from "./services/stations";
import { routeLeg } from "./services/api";


type Stop = Station | null;

function App() {
  const [stops, setStops] = React.useState<Stop[]>([null, null]); // From, To

  const setStopAt = (idx: number, station: Station | null) => {
    setStops((prev) => {
      const next = [...prev];
      next[idx] = station;
      return next;
    });
  };

  const addStop = () => {
    setStops((prev) => {
      // insert before last (before "To")
      const next = [...prev];
      next.splice(next.length - 1, 0, null);
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

  const [routeResult, setRouteResult] = React.useState<any>(null);
  const [routeError, setRouteError] = React.useState<string | null>(null);
  const [routing, setRouting] = React.useState(false);

  const from = selection.from;
  const to = selection.to;

  return (
    <div style={{ padding: 16, fontFamily: "system-ui" }}>
      <h1>MultiPlanner</h1>
      <p>Station autocomplete (DB) + add/reorder stops</p>

      <div style={{ display: "grid", gap: 16 }}>
        <StationSearch
          label="From"
          placeholder="Type a station name"
          value={stops[0]}
          onChange={(s) => setStopAt(0, s)}
        />

        {stops.slice(1, -1).map((_, i) => {
          const idx = i + 1; // real index in stops array
          return (
            <div key={idx} style={{ display: "grid", gap: 8 }}>
              <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                <strong>Stop {i + 1}</strong>

                <button type="button" onClick={() => moveStop(idx, -1)}>
                  ↑
                </button>
                <button type="button" onClick={() => moveStop(idx, 1)}>
                  ↓
                </button>

                <button type="button" onClick={() => removeStop(idx)}>
                  Remove
                </button>
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

        <div style={{ display: "flex", gap: 8 }}>
          <button type="button" onClick={addStop}>
            Add stop
          </button>

          <button
            type="button"
            onClick={() => setStops([null, null])}
            style={{ marginLeft: 8 }}
          >
            Reset
          </button>

          <button
            type="button"
            disabled={!from || !to || routing}
            onClick={async () => {
              if (!from || !to) return;
              setRouting(true);
              setRouteError(null);
              try {
                const data = await routeLeg(from, to);
                setRouteResult(data);
              } catch (e) {
                setRouteError(String(e));
              } finally {
                setRouting(false);
              }
            }}
          >
            {routing ? "Routing..." : "Route"}
          </button>
        </div>
        
        {routeError && <p style={{ color: "crimson" }}>Error: {routeError}</p>}
        {routeResult && <pre>{JSON.stringify(routeResult, null, 2)}</pre>}
      
      </div>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
