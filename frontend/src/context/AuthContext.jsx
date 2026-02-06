import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { TOKEN_STORAGE_KEY } from '../api/httpClient'
import { login as loginRequest, logout as logoutRequest } from '../api/authApi'

const AuthContext = createContext(null)

function decodeJwt(token) {
  try {
    const payload = token.split('.')[1]
    const decoded = JSON.parse(
      atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
        .split('')
        .reduce((acc, c, i) => {
          acc += c
          return acc
        }, '')
    )
    return decoded
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(null)
  const [user, setUser] = useState(null)

  useEffect(() => {
    const stored = localStorage.getItem(TOKEN_STORAGE_KEY)
    if (stored) {
      const payload = decodeJwt(stored)
      if (payload && payload.exp && Date.now() / 1000 < payload.exp) {
        setToken(stored)
        setUser({
          username: payload.sub,
          // Map single 'role' to 'roles' array
          roles: payload.role ? [payload.role] : [] 
        })
      } else {
        localStorage.removeItem(TOKEN_STORAGE_KEY)
      }
    }
  }, [])

const handleLogin = async (credentials) => {
    const res = await loginRequest(credentials);
    const newToken = res.token;

    // Save the token immediately
    localStorage.setItem(TOKEN_STORAGE_KEY, newToken);

    const payload = decodeJwt(newToken);
    setToken(newToken);
    setUser(payload && {
      username: payload.sub,
      roles: payload.role ? [payload.role] : [] // Correctly maps the single role
    });
    return res;
};
  const handleLogout = () => {
    setToken(null)
    setUser(null)
    localStorage.removeItem(TOKEN_STORAGE_KEY)
    logoutRequest()
  }

  const value = useMemo(
    () => ({
      token,
      user,
      isAuthenticated: !!token,
      roles: user?.roles || [],
      login: handleLogin,
      logout: handleLogout,
      hasRole: (role) => (user?.roles || []).includes(role)
    }),
    [token, user]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return ctx
}

