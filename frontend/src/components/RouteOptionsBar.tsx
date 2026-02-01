import React from "react";
import type { RouteMultiOptions, SortBy } from "../services/api";

type Props = {
  // Current options state (sort + toggles)
  options: RouteMultiOptions;

  // Parent passes a setter (we call it whenever user toggles something)
  onChange: (next: RouteMultiOptions) => void;
};

/**
 * A small “chip” button like Google Maps filters.
 */
function ChipButton({
  label,
  active,
  onClick,
}: {
  label: string;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      style={{
        padding: "8px 12px",
        borderRadius: 999,
        border: "1px solid rgba(0,0,0,0.15)",
        background: active ? "rgba(0,0,0,0.08)" : "white",
        fontSize: 14,
        cursor: "pointer",
        display: "inline-flex",
        alignItems: "center",
        gap: 8,
      }}
      aria-pressed={active}
    >
      {/* Small dot indicator */}
      <span
        style={{
          width: 10,
          height: 10,
          borderRadius: 999,
          background: active ? "black" : "rgba(0,0,0,0.25)",
          display: "inline-block",
        }}
      />
      {label}
    </button>
  );
}

/**
 * Google Maps style options bar:
 * - Bus toggle
 * - Tram toggle
 * - Route preference dropdown (Fastest / Fewest transfers)
 */
export default function RouteOptionsBar({ options, onChange }: Props) {
  // Default behaviour: include everything until UI is fully built
  const sortBy: SortBy = options.sortBy ?? "FASTEST";
  const includeBus = options.includeBus ?? true;
  const includeTram = options.includeTram ?? true;

  return (
    <div
      style={{
        display: "flex",
        gap: 12,
        alignItems: "center",
        flexWrap: "wrap",
        padding: "10px 12px",
        borderRadius: 14,
        border: "1px solid rgba(0,0,0,0.12)",
        background: "white",
      }}
    >
      {/* Left side: filter chips */}
      <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
        <ChipButton
          label="Bus"
          active={includeBus}
          onClick={() => onChange({ ...options, includeBus: !includeBus })}
        />
        <ChipButton
          label="Tram"
          active={includeTram}
          onClick={() => onChange({ ...options, includeTram: !includeTram })}
        />
      </div>

      {/* Right side: sort dropdown */}
      <div style={{ marginLeft: "auto", display: "flex", gap: 10, alignItems: "center" }}>
        <span style={{ fontSize: 13, color: "rgba(0,0,0,0.65)" }}>Route</span>

        <select
          value={sortBy}
          onChange={(e) => onChange({ ...options, sortBy: e.target.value as SortBy })}
          style={{
            padding: "8px 10px",
            borderRadius: 10,
            border: "1px solid rgba(0,0,0,0.15)",
            background: "white",
            fontSize: 14,
            cursor: "pointer",
          }}
        >
          <option value="FASTEST">Fastest</option>
          <option value="FEWEST_TRANSFERS">Fewest transfers</option>
        </select>
      </div>
    </div>
  );
}
