import httpClient from './httpClient'

export async function callNextToken({ counterId, serviceTypeId }) {
  const response = await httpClient.post(
    `/api/counters/${counterId}/call-next/${serviceTypeId}`
  )
  return response.data
}

export async function completeToken(tokenId) {
  return httpClient.put(`/api/counters/tokens/${tokenId}/complete`)
}

export async function skipToken(tokenId) {
  return httpClient.put(`/api/counters/tokens/${tokenId}/skip`)
}

