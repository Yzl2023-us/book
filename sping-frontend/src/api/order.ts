import api from './index'

export interface BorrowInfo {
  borrowId: number
  userId: number
  bookId: number
  borrowTime: string
  returnTime: string | null
  bookName?: string
  userName?: string
}

export const orderApi = {
  borrowBook(bookId: number) {
    return api.post('/api/borrow/borrow', { bookId })
  },
  returnBook(borrowId: number) {
    return api.put(`/api/borrow/${borrowId}/return`)
  },
  getBorrowById(borrowId: number) {
    return api.get(`/api/borrow/${borrowId}`)
  },
  getMyBorrows() {
    return api.get('/api/borrow/my')
  },
}