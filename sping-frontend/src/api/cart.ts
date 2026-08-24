import api from './index'

export interface CartItem {
  cartItemId: number
  cartId: number
  bookId: number
  quantity: number
  addTime: string
  bookName?: string
  bookAuthor?: string
  bookPrice?: number
  bookImg?: string
}

export const cartApi = {
  addToCart(bookId: number, quantity: number = 1) {
    return api.post('/api/cart/add', { bookId, quantity })
  },
  getMyCart() {
    return api.get('/api/cart/my')
  },
  updateQuantity(cartItemId: number, quantity: number) {
    return api.put(`/api/cart/${cartItemId}`, null, { params: { quantity } })
  },
  removeItem(cartItemId: number) {
    return api.delete(`/api/cart/${cartItemId}`)
  },
  clearCart() {
    return api.delete('/api/cart/clear')
  },
}
