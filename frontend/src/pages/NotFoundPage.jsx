import { Link } from 'react-router-dom';
import { EmptyState } from '../components/States';

export default function NotFoundPage() {
  return (
    <div className="card">
      <EmptyState icon="file" title="Page not found">
        <Link to="/">Go to your dashboard</Link>
      </EmptyState>
    </div>
  );
}
