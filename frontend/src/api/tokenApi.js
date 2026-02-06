import httpClient from './httpClient'

// Create a token linked to a patient registration
export async function createPatientToken(payload) {
  // Payload should match CreatePatientTokenRequest DTO on backend
  // {
  //   name: string,
  //   age: number,
  //   phone: string,
  //   gender: 'MALE' | 'FEMALE' | 'OTHER',
  //   serviceId: number,
  //   priority: boolean
  // }
  const response = await httpClient.post('/api/tokens/patient', payload)
  return response.data
}

export async function getEtaForToken(tokenId) {
  const response = await httpClient.get(`/api/metrics/eta/${tokenId}`)
  // Backend returns a long representing ETA minutes
  return response.data
}

export async function getTokensByStatus(status) {
  const response = await httpClient.get(`/api/tokens/status/${status}`)
  return response.data
}

