import React from "react";
import { searchStations, Station } from "../services/stations";
import { useDebounce } from "../services/useDebounce";

type Props = {
  label: string;
  placeholder: string;
  onSelect: (station: Station) => void;
};

export function StationSearch({ label, placeholder, onSelect }: Props) {
  const [query, setQuery] = React.useState("");
  const debouncedQuery = useDebounce(query, 300);

  const [results, setResults] = React.useState<Station[]>([]);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  const [suppressSearch, setSuppressSearch] = React.useState(false);


  React.useEffect(() => {
    // Clear results if empty
    if (!debouncedQuery.trim()) {
      setResults([]);
      setError(null);
      setLoading(false);
      return;
    }

    //dropdown disappers if search selected
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
  }, [debouncedQuery]);

  return (
    <div style={{ maxWidth: 520 }}>
      <label style={{ display: "block", marginBottom: 6 }}>{label}</label>

      <input
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder={placeholder}
        style={{ width: "100%", padding: 10, fontSize: 16 }}
      />

      {loading && <p style={{ marginTop: 8 }}>Loading…</p>}
      {error && (
        <p style={{ marginTop: 8, color: "crimson" }}>
          Error: {error}
        </p>
      )}

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
                    setSuppressSearch(true)
                    onSelect(s);
                    setQuery(s.name); //update input box text
                    setResults([]) //close dropdown
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
