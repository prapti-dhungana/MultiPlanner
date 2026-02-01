import React from "react";
import { searchStations, Station } from "../services/stations";
import { useDebounce } from "../services/useDebounce";

type Props = {
  label: string;
  placeholder: string;
  value: Station | null;
  onChange: (station: Station | null) => void;
};

export function StationSearch({ label, placeholder, value, onChange }: Props) {
  const [query, setQuery] = React.useState(value?.name ?? "");
  const debouncedQuery = useDebounce(query, 300);

  const [results, setResults] = React.useState<Station[]>([]);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  const [suppressSearch, setSuppressSearch] = React.useState(false);

  // If parent changes selected value (e.g., after reorder), keep input text in sync
  React.useEffect(() => {
    setQuery(value?.name ?? "");
  }, [value?.code]); // use code so it doesn't reset while typing

  React.useEffect(() => {
    if (!debouncedQuery.trim()) {
      setResults([]);
      setError(null);
      setLoading(false);
      return;
    }

    if (suppressSearch) {
      setSuppressSearch(false);
      return;
    }

    setLoading(true);
    setError(null);

    searchStations(debouncedQuery)
      .then((stations) => setResults(stations))
      .catch((err) => setError(String(err)))
      .finally(() => setLoading(false));
  }, [debouncedQuery]); // keep your MVP behavior

  return (
    <div style={{ maxWidth: 520 }}>
      <label style={{ display: "block", marginBottom: 6 }}>{label}</label>

      <input
        value={query}
        onChange={(e) => {
          setQuery(e.target.value);

          // If user edits text after selecting a station, treat selection as cleared
          if (value) onChange(null);
        }}
        placeholder={placeholder}
        style={{ width: "100%", padding: 10, fontSize: 16 }}
      />

      {loading && <p style={{ marginTop: 8 }}>Loading…</p>}
      {error && <p style={{ marginTop: 8, color: "crimson" }}>Error: {error}</p>}

      {results.length > 0 && (
        <ul
          style={{
            marginTop: 8,
            padding: 0,
            listStyle: "none",
            border: "1px solid #ddd",
            borderRadius: 8,
            overflow: "hidden",
          }}
        >
          {results.map((s) => (
            <li key={s.code}>
              <button
                type="button"
                onClick={() => {
                  setSuppressSearch(true);
                  onChange(s);
                  setQuery(s.name);
                  setResults([]);
                }}
                style={{
                  width: "100%",
                  textAlign: "left",
                  padding: 10,
                  border: "none",
                  background: "white",
                  cursor: "pointer",
                }}
              >
                <strong>{s.name}</strong> <span>({s.code})</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
