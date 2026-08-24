import api from './index'

export interface MessageInfo {
  id: number
  senderId: number
  receiverId: number
  bookId: number | null
  content: string
  isRead: number
  createTime: string
  senderName?: string
}

export const messageApi = {
  sendMessage(data: { receiverId: number; bookId?: number; content: string }) {
    return api.post('/api/message/send', data)
  },
  getConversation(receiverId: number) {
    return api.get(`/api/message/conversation/${receiverId}`)
  },
  getMyMessages() {
    return api.get('/api/message/my')
  },
  getBookMessages(bookId: number) {
    return api.get(`/api/message/book/${bookId}`)
  },
  markAsRead(messageId: number) {
    return api.put(`/api/message/${messageId}/read`)
  },
  getUnreadCount() {
    return api.get('/api/message/unread-count')
  },
  broadcastAnnouncement(content: string) {
    return api.post('/api/message/broadcast', { content })
  },
  getAnnouncements() {
    return api.get('/api/message/announcements')
  },
}