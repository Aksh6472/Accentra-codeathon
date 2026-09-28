import { Navigate, useLocation } from 'react-router-dom';
import { homePathFor, useAuth } from './AuthContext';

/**
 * Route guard. Hides screens the user's role cannot use; the backend enforces the same rules
 * independently, so this is a convenience, not the security boundary.
 */
export default function RequireAuth({ roles, children }) {
  const { user } = useAuth();
  const location = useLocation();
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (roles && !roles.includes(user.role)) {
    return <Navigate to={homePathFor(user.role)} replace />;
  }
  return children;
}
