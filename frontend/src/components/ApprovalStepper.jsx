import Icon from './Icon';

const stamp = new Intl.DateTimeFormat(undefined, { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
const when = (entry) => (entry ? stamp.format(new Date(entry.createdAt)) : null);

/**
 * Where a request sits in the Employee → Manager → HR workflow, derived from its status and audit history.
 * Purely presentational: the state machine lives in the backend.
 */
export function workflowSteps(status, history = []) {
  const find = (...actions) => [...history].reverse().find((h) => actions.includes(h.action));
  const created = find('CREATED');
  const managerApproved = find('MANAGER_APPROVED');
  const escalated = find('ESCALATED');
  const hrApproved = find('HR_APPROVED');
  const rejected = find('MANAGER_REJECTED', 'HR_REJECTED');
  const withdrawn = find('WITHDRAWN');

  const submitted = { key: 'submitted', label: 'Submitted', state: 'done', sub: when(created) };
  const manager = managerApproved
    ? { label: 'Manager approved', state: 'done', sub: when(managerApproved) }
    : escalated
      ? { label: 'Escalated to HR', state: 'escalated', sub: when(escalated) }
      : { label: 'Manager review', state: 'todo' };
  const hr = hrApproved
    ? { label: 'HR approved', state: 'done', sub: when(hrApproved) }
    : { label: 'HR review', state: 'todo' };
  const final = { label: 'Approved', state: hrApproved ? 'done' : 'todo', sub: when(hrApproved) };

  switch (status) {
    case 'PENDING_MANAGER':
      manager.state = 'current';
      manager.sub = 'Awaiting decision';
      break;
    case 'ESCALATED':
    case 'PENDING_HR':
      hr.state = 'current';
      hr.sub = 'Awaiting decision';
      break;
    case 'REJECTED': {
      const byManager = rejected?.action === 'MANAGER_REJECTED';
      const stage = byManager ? manager : hr;
      stage.label = byManager ? 'Manager rejected' : 'HR rejected';
      stage.state = 'failed';
      stage.sub = when(rejected);
      final.label = 'Rejected';
      break;
    }
    case 'CANCELLED':
      if (withdrawn) {
        const stage = managerApproved || escalated ? hr : manager;
        stage.label = 'Withdrawn';
        stage.state = 'failed';
        stage.sub = when(withdrawn);
        final.label = 'Cancelled';
      } else {
        return [submitted, manager, hr, final, {
          label: 'Cancelled', state: 'done', sub: when(find('CANCELLATION_APPROVED')),
        }];
      }
      break;
    case 'CANCEL_REQUESTED':
      return [submitted, manager, hr, final, { label: 'Cancellation review', state: 'current', sub: 'Awaiting decision' }];
    default:
      break;
  }
  return [submitted, manager, hr, final];
}

export default function ApprovalStepper({ status, history }) {
  const steps = workflowSteps(status, history);
  return (
    <ol className="stepper" aria-label="Approval progress">
      {steps.map((s, i) => (
        <li key={`${s.label}-${i}`} className={`step ${s.state}`} aria-current={s.state === 'current' ? 'step' : undefined}>
          <span className="step-dot" aria-hidden="true">
            {s.state === 'done' && <Icon name="check" size={14} />}
            {s.state === 'failed' && <Icon name="x" size={14} />}
            {s.state === 'todo' && i + 1}
          </span>
          <span className="step-label">{s.label}</span>
          {s.sub && <span className="step-sub">{s.sub}</span>}
        </li>
      ))}
    </ol>
  );
}
