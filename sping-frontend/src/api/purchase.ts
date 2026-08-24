import api from './index'

export interface PurchaseOrder {
  orderId: number
  orderNo: string
  totalAmount: number
  recipientName: string
  recipientPhone: string
  recipientAddress: string
  status: string
  statusText: string
  reviewRemark?: string
  createTime: string
  payTime?: string
  reviewTime?: string
  shipTime?: string
  cancelTime?: string
  items?: OrderItem[]
  payment?: PaymentInfo
  userId?: number
  userName?: string
}

export interface OrderItem {
  itemId: number
  bookId: number
  bookName: string
  bookPrice: number
  quantity: number
  subtotal: number
}

export interface PaymentInfo {
  paymentId: number
  transactionId: string
  payAmount: number
  payMethod: string
  status: string
  payTime: string
}

export interface CheckoutRequest {
  recipientName: string
  recipientPhone: string
  recipientAddress: string
}

export const purchaseApi = {
  checkout(data: CheckoutRequest) {
    return api.post('/api/purchase/checkout', data)
  },
  pay(orderId: number, payMethod: string = 'BALANCE') {
    return api.post(`/api/purchase/${orderId}/pay`, { payMethod })
  },
  getOrderDetail(orderId: number) {
    return api.get(`/api/purchase/${orderId}`)
  },
  getMyOrders() {
    return api.get('/api/purchase/my')
  },
  getAllOrders() {
    return api.get('/api/purchase/admin/list')
  },
  reviewOrder(orderId: number, action: string, remark?: string) {
    return api.put(`/api/purchase/admin/${orderId}/review`, { action, remark })
  },
  shipOrder(orderId: number) {
    return api.put(`/api/purchase/admin/${orderId}/ship`)
  },
  cancelOrder(orderId: number) {
    return api.put(`/api/purchase/${orderId}/cancel`)
  },
}
