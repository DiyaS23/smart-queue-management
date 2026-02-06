import httpClient from './httpClient'

export async function fetchServices() {
  const response = await httpClient.get('/api/admin/services')
  return response.data
}

