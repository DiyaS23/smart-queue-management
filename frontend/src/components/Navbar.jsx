import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Activity, Hospital, LogOut, History, UserCircle2 } from 'lucide-react'
import { useAuth } from '../context/AuthContext'

export default function Navbar() {
  const { isAuthenticated, user, hasRole, logout } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const linkClass = (path) =>
    `px-3 py-1.5 text-sm rounded-md ${
      location.pathname === path
        ? 'bg-primary-600 text-white'
        : 'text-slate-700 hover:bg-primary-50 hover:text-primary-700'
    }`

  return (
    <header className="w-full border-b border-slate-200 bg-white">
      <div className="mx-auto max-w-6xl flex items-center justify-between px-4 py-3">
        <div className="flex items-center gap-2">
          <div className="flex h-9 w-9 items-center justify-center rounded-full bg-primary-100 text-primary-700">
            <Hospital className="w-5 h-5" />
          </div>
          <div>
            <div className="text-sm font-semibold text-primary-800">
              Hospital Token System
            </div>
            <div className="text-xs text-slate-500 flex items-center gap-1">
              <Activity className="w-3 h-3" />
              Live Queue Management
            </div>
          </div>
        </div>

        <nav className="flex items-center gap-2">
          <Link to="/kiosk" className={linkClass('/kiosk')}>
            Kiosk
          </Link>
          <Link to="/display" className={linkClass('/display')}>
            Display
          </Link>
          <Link to="/history" className={linkClass('/history')}>
            <span className="inline-flex items-center gap-1">
              <History className="w-4 h-4" />
              History
            </span>
          </Link>
          {isAuthenticated && (hasRole('ROLE_STAFF') || hasRole('ROLE_ADMIN')) && (
            <Link to="/staff" className={linkClass('/staff')}>
              Staff
            </Link>
          )}
          {isAuthenticated && hasRole('ROLE_ADMIN') && (
            <Link to="/admin" className={linkClass('/admin')}>
              Admin
            </Link>
          )}
        </nav>

        <div className="flex items-center gap-3">
          {isAuthenticated ? (
            <>
              <div className="flex items-center gap-1 text-xs text-slate-600">
                <UserCircle2 className="w-4 h-4 text-primary-500" />
                <span className="font-medium">{user?.username}</span>
              </div>
              <button
                type="button"
                onClick={handleLogout}
                className="inline-flex items-center gap-1 rounded-md border border-slate-200 px-2.5 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-50"
              >
                <LogOut className="w-3.5 h-3.5" />
                Logout
              </button>
            </>
          ) : (
            <Link
              to="/login"
              className="inline-flex items-center gap-1 rounded-md bg-primary-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-primary-700"
            >
              <UserCircle2 className="w-3.5 h-3.5" />
              Login
            </Link>
          )}
        </div>
      </div>
    </header>
  )
}

