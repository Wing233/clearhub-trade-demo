import axios from 'axios'

const http = axios.create({
  baseURL: '/',
  timeout: 12000,
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code !== 'undefined' && body.code !== 0) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body ? body.data : body
  },
  (error) => {
    const message = error?.response?.data?.message || error.message || '网络异常'
    return Promise.reject(new Error(message))
  },
)

export const api = {
  firms: () => http.get('/api/firms'),
  commodities: () => http.get('/api/commodities'),
  book: (commodityId, depth = 10) =>
    http.get(`/api/market/${commodityId}/orderbook`, { params: { depth } }),
  marketTrades: (commodityId, limit = 50) =>
    http.get(`/api/market/${commodityId}/trades`, { params: { limit } }),
  candles: (commodityId, limit = 60) =>
    http.get(`/api/market/${commodityId}/candles`, { params: { limit } }),
  funds: (firmId) => http.get('/api/funds', { params: { firmId } }),
  positions: (firmId) => http.get('/api/positions', { params: { firmId } }),
  orders: (firmId) => http.get('/api/orders', { params: { firmId } }),
  myTrades: (firmId) => http.get('/api/trades', { params: { firmId } }),
  flows: (firmId) => http.get('/api/flows', { params: { firmId } }),
  place: (firmId, payload) =>
    http.post('/api/orders', payload, { headers: { 'X-Firm-Id': firmId } }),
  cancel: (firmId, orderId) =>
    http.post(`/api/orders/${orderId}/cancel`, null, { headers: { 'X-Firm-Id': firmId } }),
  clearingSummary: () => http.get('/api/clearing/summary'),
  clearingRun: () => http.post('/api/clearing/run'),
  clearingBatches: (limit = 20) => http.get('/api/clearing/batches', { params: { limit } }),
  clearingBatch: (batchId) => http.get(`/api/clearing/batches/${batchId}`),
  clearingReport: (batchId) => http.post(`/api/clearing/batches/${batchId}/report`),
  integrationStatus: () => http.get('/api/integration/status'),
  bankAccounts: () => http.get('/api/bank/accounts'),
  bankTransactions: (firmId = null, limit = 100) =>
    http.get('/api/bank/transactions', { params: { firmId, limit } }),
  bankDeposit: (payload) => http.post('/api/bank/deposit', payload),
  bankWithdraw: (payload) => http.post('/api/bank/withdraw', payload),
  reset: () => http.post('/api/demo/reset'),
  status: () => http.get('/api/demo/status'),
}

export default http
