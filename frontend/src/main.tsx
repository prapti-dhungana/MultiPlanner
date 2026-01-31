import React from "react";
import ReactDOM from "react-dom/client";
import { fetchHealth } from "./services/api";
import { searchStations } from "./services/stations";

fetchHealth()
  .then((data) => console.log("Health:", data))
  .catch((err) => console.error("Health error:", err));

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <div style={{ padding: 16, fontFamily: "system-ui" }}>
      <h1>MultiPlanner</h1>
      <p>Open the console to see the backend health check.</p>
    </div>
  </React.StrictMode>
);

//testing
searchStations("lon")
  .then((stations) => console.log("Stations:", stations))
  .catch((err) => console.error("Station search error:", err));
