import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function ProtectedRoute({ allowedRoles }) {
  const { isAuthenticated, roles } = useAuth()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (allowedRoles && allowedRoles.length > 0) {
    const hasAllowedRole = roles.some((r) => allowedRoles.includes(r))
    if (!hasAllowedRole) {
      return <Navigate to="/kiosk" replace />
    }
  }

  return <Outlet />
}

