import api from './index'

export interface BookInfo {
  bookId: number
  bookName: string
  bookAuthor: string
  bookPrice: number
  bookTypeId: number
  bookDesc: string
  bookImg: string
  bookStock: number
  bookTypeName?: string
  sellerId?: number
  status?: string
}

export interface BookParams {
  bookName: string
  bookAuthor: string
  bookPrice: number
  bookTypeId: number
  bookDesc: string
  bookImg?: string
  bookStock?: number
}

export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface BookType {
  bookTypeId: number
  bookTypeName: string
  bookTypeDesc: string
}

export const bookApi = {
  addBook(data: BookParams) {
    return api.post('/api/book/add', data)
  },
  updateBook(bookId: number, data: BookParams) {
    return api.put(`/api/book/${bookId}`, data)
  },
  deleteBook(bookId: number) {
    return api.delete(`/api/book/${bookId}`)
  },
  getBookById(bookId: number) {
    return api.get(`/api/book/${bookId}`)
  },
  searchBooks(params: { keyword?: string; bookTypeId?: number; page?: number; size?: number }) {
    return api.get('/api/book/search', { params })
  },
  getHotBooks() {
    return api.get('/api/book/hot')
  },
  getBookTypes() {
    return api.get('/api/book/types')
  },
  uploadCover(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/api/book/upload-cover', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
  // 普通用户发布图书
  publishBook(data: BookParams) {
    return api.post('/api/book/publish', data)
  },
  // 查看自己发布的图书
  getMyPublished(page = 0, size = 10) {
    return api.get('/api/book/my-published', { params: { page, size } })
  },
  // 管理员查看所有图书（包括待审核）
  adminSearchBooks(params: { keyword?: string; bookTypeId?: number; page?: number; size?: number }) {
    return api.get('/api/book/admin/all', { params })
  },
  // 管理员查看待审核列表
  getPendingBooks(page = 0, size = 10) {
    return api.get('/api/book/pending', { params: { page, size } })
  },
  // 管理员审核图书
  reviewBook(bookId: number, status: 'APPROVED' | 'REJECTED') {
    return api.put(`/api/book/${bookId}/review`, { status })
  },
}