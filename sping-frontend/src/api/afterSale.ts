import api from './index'

export interface AfterSaleOrder {
  afterSaleId: number
  orderId: number
  orderNo: string
  userId?: number
  userName?: string
  type: string
  typeText: string
  reason?: string
  refundAmount: number
  status: string
  statusText: string
  adminRemark?: string
  createTime?: string
  reviewTime?: string
  returnTime?: string
  refundTime?: string
  cancelTime?: string
}

export interface ApplyAfterSaleRequest {
  orderId: number
  type: string
  reason?: string
  refundAmount: number
}

export const afterSaleApi = {
  apply(data: ApplyAfterSaleRequest) {
    return api.post('/api/after-sale/apply', data)
  },
  getMyAfterSales() {
    return api.get('/api/after-sale/my')
  },
  getDetail(afterSaleId: number) {
    return api.get(`/api/after-sale/${afterSaleId}`)
  },
  cancel(afterSaleId: number) {
    return api.put(`/api/after-sale/${afterSaleId}/cancel`)
  },
  getAllAfterSales() {
    return api.get('/api/after-sale/admin/list')
  },
  review(afterSaleId: number, action: string, remark?: string) {
    return api.put(`/api/after-sale/admin/${afterSaleId}/review`, { action, remark })
  },
  confirmReturn(afterSaleId: number) {
    return api.put(`/api/after-sale/${afterSaleId}/return`)
  },
  refund(afterSaleId: number) {
    return api.put(`/api/after-sale/admin/${afterSaleId}/refund`)
  },
}
