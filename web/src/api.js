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
  funds: (firmId) => http.get('/api/funds', { params: { firmId } }),
  positions: (firmId) => http.get('/api/positions', { params: { firmId } }),
  orders: (firmId) => http.get('/api/orders', { params: { firmId } }),
  myTrades: (firmId) => http.get('/api/trades', { params: { firmId } }),
  flows: (firmId) => http.get('/api/flows', { params: { firmId } }),
  place: (firmId, payload) =>
    http.post('/api/orders', payload, { headers: { 'X-Firm-Id': firmId } }),
  cancel: (firmId, orderId) =>
    http.post(`/api/orders/${orderId}/cancel`, null, { headers: { 'X-Firm-Id': firmId } }),
  reset: () => http.post('/api/demo/reset'),
  status: () => http.get('/api/demo/status'),
}

export default http
