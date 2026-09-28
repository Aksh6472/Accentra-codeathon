import client from './client';

const data = (promise) => promise.then((res) => res.data);

export const api = {
  login: (email, password) => data(client.post('/auth/login', { email, password })),

  me: () => data(client.get('/employees/me')),
  myBalances: (year) => data(client.get('/employees/me/balance', { params: { year } })),

  policies: () => data(client.get('/policies')),
  createPolicy: (body) => data(client.post('/policies', body)),
  updatePolicy: (id, body) => data(client.put(`/policies/${id}`, body)),
  settings: () => data(client.get('/settings')),
  updateSettings: (body) => data(client.put('/settings', body)),

  holidays: (year) => data(client.get('/holidays', { params: { year } })),
  createHoliday: (body) => data(client.post('/holidays', body)),
  deleteHoliday: (id) => data(client.delete(`/holidays/${id}`)),

  previewLeave: (body) => data(client.post('/leaves/preview', body)),
  applyLeave: (body) => data(client.post('/leaves', body)),
  myLeaves: () => data(client.get('/leaves/my')),
  leave: (id) => data(client.get(`/leaves/${id}`)),
  cancelLeave: (id, comment) => data(client.post(`/leaves/${id}/cancel`, { comment })),

  managerPending: () => data(client.get('/manager/leaves/pending')),
  managerEscalated: () => data(client.get('/manager/leaves/escalated')),
  managerHistory: () => data(client.get('/manager/leaves/history')),

  hrPending: () => data(client.get('/hr/leaves/pending')),
  hrEscalated: () => data(client.get('/hr/leaves/escalated')),
  hrLeaves: (params) => data(client.get('/hr/leaves', { params })),
  audit: (params) => data(client.get('/hr/audit', { params })),
  hrEmployees: (year) => data(client.get('/hr/employees', { params: { year } })),
  hrEmployeeBalances: (id, year) => data(client.get(`/hr/employees/${id}/balances`, { params: { year } })),

  teams: () => data(client.get('/teams')),
  teamCalendar: (id, from, to) => data(client.get(`/teams/${id}/calendar`, { params: { from, to } })),
  teamConflicts: (id, from, to) => data(client.get(`/teams/${id}/conflicts`, { params: { from, to } })),

  notifications: () => data(client.get('/notifications')),
  unreadCount: () => data(client.get('/notifications/unread-count')),
  markNotificationRead: (id) => data(client.post(`/notifications/${id}/read`)),
  markAllNotificationsRead: () => data(client.post('/notifications/read-all')),

  analytics: (year) => data(client.get('/analytics', { params: { year } })),
};

/** Maps a workflow action (from a request's availableActions) to the endpoint that performs it. */
export function performLeaveAction(action, leaveId, role, comment) {
  const body = { comment: comment || null };
  const stage = role === 'HR' ? 'hr' : 'manager';
  const routes = {
    MANAGER_APPROVE: `/manager/leaves/${leaveId}/approve`,
    MANAGER_REJECT: `/manager/leaves/${leaveId}/reject`,
    HR_APPROVE: `/hr/leaves/${leaveId}/approve`,
    HR_REJECT: `/hr/leaves/${leaveId}/reject`,
    WITHDRAW: `/leaves/${leaveId}/cancel`,
    REQUEST_CANCELLATION: `/leaves/${leaveId}/cancel`,
    APPROVE_CANCELLATION: `/${stage}/leaves/${leaveId}/cancellation/approve`,
    REJECT_CANCELLATION: `/${stage}/leaves/${leaveId}/cancellation/reject`,
  };
  return data(client.post(routes[action], body));
}
