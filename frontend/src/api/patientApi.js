import httpClient from './httpClient'

export async function fetchPatientHistory(phone) {
  const response = await httpClient.get('/api/patients/history', {
    params: { phone }
  })
  return response.data
}

export async function fetchPatientHistoryByService(phone, serviceId) {
  const response = await httpClient.get(`/api/patients/history/service/${serviceId}`, {
    params: { phone }
  })
  return response.data
}

export async function fetchPatientHistoryByDoctor(phone, doctorId) {
  const response = await httpClient.get(`/api/patients/history/doctor/${doctorId}`, {
    params: { phone }
  })
  return response.data
}

