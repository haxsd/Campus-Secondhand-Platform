import { get, post } from '@/utils/request'
import { mockCreateDispute } from '@/mock/dispute'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'

export function createDispute(data) {
  if (useMock) return mockCreateDispute(data)
  return post('/disputes', data)
}

export function getDispute(id) {
  if (useMock) return Promise.resolve(null)
  return get(`/disputes/${id}`)
}

export function getDisputeByOrder(orderId) {
  if (useMock) return Promise.resolve(null)
  return get(`/disputes/by-order/${orderId}`)
}

export function appendDisputeEvidence(id, data) {
  if (useMock) return Promise.resolve(null)
  return post(`/disputes/${id}/evidence`, data)
}
