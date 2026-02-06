import httpClient, { TOKEN_STORAGE_KEY } from './httpClient'

export async function login({ username, password }) {
  const response = await httpClient.post('/api/auth/login', { username, password })
  const { token } = response.data

  if (token) {
    localStorage.setItem(TOKEN_STORAGE_KEY, token)
  }

  return response.data
}

export function logout() {
  localStorage.removeItem(TOKEN_STORAGE_KEY)
}

export function getAuthToken() {
  return localStorage.getItem(TOKEN_STORAGE_KEY)
}

