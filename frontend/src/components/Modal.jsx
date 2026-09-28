import { useEffect, useId, useRef, useState } from 'react';
import { errorMessage } from '../api/client';
import { Alert } from './States';

export function Modal({ title, onClose, children, footer }) {
  const titleId = useId();
  const ref = useRef(null);

  useEffect(() => {
    const previous = document.activeElement;
    ref.current?.querySelector('textarea, input, select, button')?.focus();
    const onKey = (e) => e.key === 'Escape' && onClose();
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('keydown', onKey);
      previous?.focus?.();
    };
  }, [onClose]);

  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby={titleId} ref={ref}>
        <div className="modal-header">
          <h2 id={titleId}>{title}</h2>
        </div>
        <div className="modal-body">{children}</div>
        {footer && <div className="modal-footer">{footer}</div>}
      </div>
    </div>
  );
}

/**
 * Confirmation step for workflow actions, with an optional/required comment.
 * `onConfirm(comment)` should return a promise; errors are shown inline.
 */
export function ConfirmDialog({
  title,
  message,
  confirmLabel = 'Confirm',
  tone = 'primary',
  commentLabel,
  commentRequired = false,
  onConfirm,
  onClose,
}) {
  const [comment, setComment] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const missingComment = commentRequired && !comment.trim();

  const submit = async (e) => {
    e.preventDefault();
    if (missingComment) {
      setError('Please add a comment explaining the decision.');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await onConfirm(comment.trim());
    } catch (err) {
      setError(errorMessage(err));
      setBusy(false);
    }
  };

  const buttonClass = { primary: 'btn-primary', good: 'btn-good', danger: 'btn-danger-solid' }[tone];

  return (
    <Modal title={title} onClose={busy ? () => {} : onClose}>
      <form onSubmit={submit} className="stack" style={{ gap: 14 }}>
        {message && <p>{message}</p>}
        {commentLabel && (
          <div className="field">
            <label htmlFor="decision-comment">
              {commentLabel} {commentRequired ? <span className="muted">(required)</span> : <span className="muted">(optional)</span>}
            </label>
            <textarea
              id="decision-comment"
              className={`textarea ${error && missingComment ? 'invalid' : ''}`}
              maxLength={1000}
              value={comment}
              onChange={(e) => setComment(e.target.value)}
            />
          </div>
        )}
        {error && <Alert tone="error">{error}</Alert>}
        <div className="form-actions">
          <button type="button" className="btn" onClick={onClose} disabled={busy}>
            Cancel
          </button>
          <button type="submit" className={`btn ${buttonClass}`} disabled={busy}>
            {busy ? 'Working…' : confirmLabel}
          </button>
        </div>
      </form>
    </Modal>
  );
}
