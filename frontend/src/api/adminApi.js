import httpClient from './httpClient'

export async function fetchAdminSummary() {
  const response = await httpClient.get('/api/admin/dashboard/summary')
  return response.data
}

export async function fetchDoctorLoad() {
  const response = await httpClient.get('/api/admin/dashboard/doctors')
  return response.data
}

export async function fetchServiceStats() {
  const response = await httpClient.get('/api/admin/dashboard/services')
  return response.data
}

export async function fetchCounters() {
  const response = await httpClient.get('/api/admin/counters')
  return response.data
}

export async function createCounter(payload) {
  // payload should conform to CounterRequest DTO
  const response = await httpClient.post('/api/admin/counters', payload)
  return response.data
}

export async function createService(payload) {
  // payload is a ServiceType entity (name, description, etc.)
  const response = await httpClient.post('/api/admin/services', payload)
  return response.data
}

export async function fetchPendingEmergencies() {
  const response = await httpClient.get('/api/admin/emergencies')
  return response.data
}

export async function approveEmergency(tokenId) {
  const response = await httpClient.put(`/api/admin/emergencies/${tokenId}/approve`)
  return response.data
}

export async function rejectEmergency(tokenId) {
  return httpClient.put(`/api/admin/emergencies/${tokenId}/reject`)
}

