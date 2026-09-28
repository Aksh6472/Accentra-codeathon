import { Navigate, Route, Routes } from 'react-router-dom';
import { homePathFor, useAuth } from './auth/AuthContext';
import RequireAuth from './auth/RequireAuth';
import Layout from './components/Layout';
import LoginPage from './pages/LoginPage';
import NotificationsPage from './pages/NotificationsPage';
import LeaveDetailPage from './pages/LeaveDetailPage';
import TeamCalendarPage from './pages/TeamCalendarPage';
import NotFoundPage from './pages/NotFoundPage';
import EmployeeDashboard from './pages/employee/EmployeeDashboard';
import ApplyLeavePage from './pages/employee/ApplyLeavePage';
import MyLeavesPage from './pages/employee/MyLeavesPage';
import ManagerDashboard from './pages/manager/ManagerDashboard';
import ManagerApprovalsPage from './pages/manager/ManagerApprovalsPage';
import TeamMembersPage from './pages/manager/TeamMembersPage';
import HrDashboard from './pages/hr/HrDashboard';
import HrApprovalsPage from './pages/hr/HrApprovalsPage';
import EscalationsPage from './pages/hr/EscalationsPage';
import AllLeavesPage from './pages/hr/AllLeavesPage';
import PoliciesPage from './pages/hr/PoliciesPage';
import HolidaysPage from './pages/hr/HolidaysPage';
import AnalyticsPage from './pages/hr/AnalyticsPage';
import AuditLogPage from './pages/hr/AuditLogPage';
import EmployeesPage from './pages/hr/EmployeesPage';
import EmployeeDetailPage from './pages/hr/EmployeeDetailPage';

function HomeRedirect() {
  const { user } = useAuth();
  return <Navigate to={user ? homePathFor(user.role) : '/login'} replace />;
}

const guard = (roles, element) => <RequireAuth roles={roles}>{element}</RequireAuth>;

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RequireAuth><Layout /></RequireAuth>}>
        <Route index element={<HomeRedirect />} />

        <Route path="dashboard" element={guard(['EMPLOYEE'], <EmployeeDashboard />)} />
        <Route path="apply" element={guard(['EMPLOYEE'], <ApplyLeavePage />)} />
        <Route path="my-leaves" element={guard(['EMPLOYEE'], <MyLeavesPage />)} />

        <Route path="manager" element={guard(['MANAGER'], <ManagerDashboard />)} />
        <Route path="manager/approvals" element={guard(['MANAGER'], <ManagerApprovalsPage />)} />
        <Route path="manager/team" element={guard(['MANAGER'], <TeamMembersPage />)} />

        <Route path="hr" element={guard(['HR'], <HrDashboard />)} />
        <Route path="hr/approvals" element={guard(['HR'], <HrApprovalsPage />)} />
        <Route path="hr/escalations" element={guard(['HR'], <EscalationsPage />)} />
        <Route path="hr/leaves" element={guard(['HR'], <AllLeavesPage />)} />
        <Route path="hr/policies" element={guard(['HR'], <PoliciesPage />)} />
        <Route path="hr/holidays" element={guard(['HR'], <HolidaysPage />)} />
        <Route path="hr/analytics" element={guard(['HR'], <AnalyticsPage />)} />
        <Route path="hr/audit" element={guard(['HR'], <AuditLogPage />)} />
        <Route path="hr/employees" element={guard(['HR'], <EmployeesPage />)} />
        <Route path="hr/employees/:id" element={guard(['HR'], <EmployeeDetailPage />)} />

        <Route path="team-calendar" element={guard(['MANAGER', 'HR'], <TeamCalendarPage />)} />
        <Route path="leaves/:id" element={<LeaveDetailPage />} />
        <Route path="notifications" element={<NotificationsPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
