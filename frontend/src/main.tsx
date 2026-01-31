import React from "react";
import ReactDOM from "react-dom/client";
import { fetchHealth } from "./services/api";

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
