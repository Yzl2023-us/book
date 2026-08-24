import api from './index'

export interface LoginParams {
  username: string
  password: string
}

export interface RegisterParams {
  username: string
  password: string
}

export interface UpdateUserParams {
  userName?: string
  password?: string
  isAdmin?: number
  avatar?: string
}

export interface UserInfo {
  userId: number
  userName: string
  isAdmin: number
  avatar?: string
}

export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export const userApi = {
  login(data: LoginParams) {
    return api.post('/api/user/login', data)
  },
  register(data: RegisterParams) {
    return api.post('/api/user/register', data)
  },
  getUserInfo(userId: number) {
    return api.get(`/api/user/info/${userId}`)
  },
  // admin
  listUsers(page = 0, size = 10) {
    return api.get('/api/user/admin/list', { params: { page, size } })
  },
  addUser(data: RegisterParams) {
    return api.post('/api/user/admin/add', data)
  },
  updateUser(userId: number, data: UpdateUserParams) {
    return api.put(`/api/user/admin/${userId}`, data)
  },
  deleteUser(userId: number) {
    return api.delete(`/api/user/admin/${userId}`)
  },
  uploadAvatar(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/api/user/upload-avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
  updateAvatar(userId: number, avatar: string) {
    return api.put(`/api/user/avatar/${userId}`, { avatar })
  },
}