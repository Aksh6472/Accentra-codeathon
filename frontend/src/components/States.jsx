import Icon from './Icon';

export function Loading({ label = 'Loading…' }) {
  return (
    <div className="state" role="status" aria-live="polite">
      <div className="spinner" />
      <span>{label}</span>
    </div>
  );
}

export function EmptyState({ icon = 'inbox', title, children }) {
  return (
    <div className="state">
      <Icon name={icon} size={32} />
      <h3>{title}</h3>
      {children && <p>{children}</p>}
    </div>
  );
}

export function ErrorState({ message, onRetry }) {
  return (
    <div className="state" role="alert">
      <Icon name="alert" size={32} />
      <h3>Could not load this data</h3>
      <p>{message}</p>
      {onRetry && (
        <button type="button" className="btn btn-sm" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}

export function Alert({ tone = 'info', title, children, icon }) {
  const icons = { warn: 'alert', serious: 'alert', error: 'alert', success: 'check', info: 'info' };
  return (
    <div className={`alert alert-${tone}`} role={tone === 'error' ? 'alert' : undefined}>
      <Icon name={icon || icons[tone]} />
      <div>
        {title && <strong>{title}</strong>}
        {children}
      </div>
    </div>
  );
}

/** Renders loading / error / children depending on an async state. */
export function AsyncBoundary({ state, children, loadingLabel }) {
  if (state.loading && !state.data) return <Loading label={loadingLabel} />;
  if (state.error) return <ErrorState message={state.error} onRetry={state.reload} />;
  return children(state.data);
}
