import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { AlertCircle, Lock, Loader2 } from 'lucide-react'
import { useAuth } from '../context/AuthContext'

export default function Login() {
  const { login, hasRole } = useAuth()
  const [form, setForm] = useState({ username: '', password: '' })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)
  const navigate = useNavigate()
  const location = useLocation()

  const from = location.state?.from || '/staff'

  const handleChange = (e) => {
    const { name, value } = e.target
    setForm((prev) => ({ ...prev, [name]: value }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login(form)
      if (hasRole('ROLE_ADMIN')) {
        navigate('/admin', { replace: true })
      } else {
        navigate(from, { replace: true })
      }
    } catch (err) {
      setError(err.message || 'Invalid credentials')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen flex flex-col bg-gradient-to-br from-primary-50 to-slate-100">
      <div className="flex-1 flex items-center justify-center px-4">
        <div className="w-full max-w-md bg-white rounded-2xl shadow-lg border border-slate-100 p-8">
          <div className="flex items-center gap-2 mb  -mb-4">
            <Lock className="w-6 h-6 text-primary-500" />
            <h1 className="text-xl font-semibold text-primary-800">
              Staff & Admin Login
            </h1>
          </div>

          {error && (
            <div className="mt-4 mb-3 flex items-center gap-2 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700">
              <AlertCircle className="w-4 h-4" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="mt-6 space-y-4">
            <div className="space-y-1">
              <label className="block text-xs font-medium text-slate-700">
                Username
              </label>
              <input
                type="text"
                name="username"
                autoComplete="username"
                required
                value={form.username}
                onChange={handleChange}
                className="w-full rounded-md border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-400"
              />
            </div>
            <div className="space-y-1">
              <label className="block text-xs font-medium text-slate-700">
                Password
              </label>
              <input
                type="password"
                name="password"
                autoComplete="current-password"
                required
                value={form.password}
                onChange={handleChange}
                className="w-full rounded-md border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-400"
              />
            </div>
            <button
              type="submit"
              disabled={submitting}
              className="mt-2 w-full inline-flex justify-center items-center gap-2 rounded-md bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 disabled:opacity-60"
            >
              {submitting && (
                <Loader2 className="h-4 w-4 animate-spin" />
              )}
              Sign in
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}

