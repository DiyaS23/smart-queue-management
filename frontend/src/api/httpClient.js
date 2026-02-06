import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export const TOKEN_STORAGE_KEY = 'hospital_jwt'

const httpClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
})

httpClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

httpClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status

    // If the token is expired/invalid, force logout + redirect
    if (status === 401) {
      localStorage.removeItem(TOKEN_STORAGE_KEY)
      // Hard redirect ensures we fully reset protected state.
      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
    }

    // Normalize backend error for UI
    const message =
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.response?.data ||
      error.message ||
      'Unexpected error'

    return Promise.reject({
      ...error,
      message,
      status
    })
  }
)

export default httpClient

