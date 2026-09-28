import { useCallback, useEffect, useRef, useState } from 'react';
import { errorMessage } from '../api/client';

/** Runs an async loader on mount / when deps change; exposes loading, error and reload. */
export default function useAsync(loader, deps = []) {
  const [state, setState] = useState({ data: null, loading: true, error: null });
  const latest = useRef(0);

  const run = useCallback(async () => {
    const id = ++latest.current;
    setState((s) => ({ ...s, loading: true, error: null }));
    try {
      const data = await loader();
      if (id === latest.current) setState({ data, loading: false, error: null });
    } catch (err) {
      if (id === latest.current) setState({ data: null, loading: false, error: errorMessage(err) });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => {
    run();
  }, [run]);

  return { ...state, reload: run };
}
