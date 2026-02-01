import { useEffect, useState } from "react";

/**
 * Returns a debounced version of a value.
 * So if delay=300ms, it updates only when the user stops typing for 300ms.
 */
export function useDebounce<T>(value: T, delayMs: number): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedValue(value), delayMs);
    return () => clearTimeout(timer);
  }, [value, delayMs]);

  return debouncedValue;
}
