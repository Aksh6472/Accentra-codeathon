// Colour follows the leave type, never its position in a list: known types have fixed slots,
// custom types get a stable slot derived from their code.
const FIXED = { CASUAL: 1, EARNED: 2, SICK: 3 };

export function leaveTypeColor(code) {
  if (FIXED[code]) return `var(--series-${FIXED[code]})`;
  const hash = [...(code || '')].reduce((sum, c) => sum + c.charCodeAt(0), 0);
  return `var(--series-${4 + (hash % 5)})`;
}

export default function LeaveTypeChip({ code, name }) {
  return (
    <span className="type-chip">
      <i style={{ background: leaveTypeColor(code) }} aria-hidden="true" />
      {name}
    </span>
  );
}
