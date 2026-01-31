import React from "react";
import ReactDOM from "react-dom/client";
import { fetchHealth } from "./services/api";
import { StationSearch } from "./components/StationSearch";
import { Station } from "./services/stations";

fetchHealth()
  .then((data) => console.log("Health:", data))
  .catch((err) => console.error("Health error:", err));

function App() {
  const [from, setFrom] = React.useState<Station | null>(null);
  const [to, setTo] = React.useState<Station | null>(null);

  return (
    <div style={{ padding: 16, fontFamily: "system-ui" }}>
      <h1>MultiPlanner</h1>
      <p>Station autocomplete (stub backend data for now)</p>

      <div style={{ display: "grid", gap: 16 }}>
        <StationSearch
          label="From"
          placeholder="Type a station name (e.g., lon)"
          onSelect={(station) => setFrom(station)}
        />

        <StationSearch
          label="To"
          placeholder="Type a station name (e.g., man)"
          onSelect={(station) => setTo(station)}
        />
      </div>

      <div style={{ marginTop: 20 }}>
        <h3>Selection</h3>
        <pre>{JSON.stringify({ from, to }, null, 2)}</pre>
      </div>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
