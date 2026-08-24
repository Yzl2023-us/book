<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'
import { messageApi, type MessageInfo } from '@/api/message'
import { MessageSquare, Megaphone, Send, X, ChevronDown, ChevronUp } from 'lucide-vue-next'
import Layout from '@/components/Layout.vue'

const router = useRouter()
const { isLoggedIn, isAdmin } = useAuth()
const messages = ref<MessageInfo[]>([])
const announcements = ref<MessageInfo[]>([])
const loading = ref(true)
const showBroadcastForm = ref(false)
const broadcastContent = ref('')
const broadcasting = ref(false)
const announceExpanded = ref(true)

async function fetchMessages() {
  loading.value = true
  try {
    const res = await messageApi.getMyMessages()
    if (res.data.code === 200) {
      messages.value = res.data.data || []
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function fetchAnnouncements() {
  try {
    const res = await messageApi.getAnnouncements()
    if (res.data.code === 200) {
      announcements.value = res.data.data || []
    }
  } catch { /* ignore */ }
}

async function handleMarkRead(id: number) {
  try {
    await messageApi.markAsRead(id)
    await fetchMessages()
  } catch { /* ignore */ }
}

async function handleBroadcast() {
  if (!broadcastContent.value.trim()) {
    alert('请输入公告内容')
    return
  }
  broadcasting.value = true
  try {
    const res = await messageApi.broadcastAnnouncement(broadcastContent.value.trim())
    if (res.data.code === 200) {
      broadcastContent.value = ''
      showBroadcastForm.value = false
      await fetchAnnouncements()
    } else {
      alert(res.data.message || '发布失败')
    }
  } catch {
    alert('发布失败，请重试')
  }
  finally { broadcasting.value = false }
}

onMounted(() => {
  fetchMessages()
  fetchAnnouncements()
})
</script>

<template>
  <Layout>
    <div v-if="!isLoggedIn" class="text-center py-12 text-gray-400">
      <MessageSquare class="w-12 h-12 mx-auto mb-3 opacity-30" />
      <p>请先登录</p>
    </div>

    <div v-else>
      <div class="flex items-center justify-between mb-6">
        <div class="flex items-center gap-2">
          <MessageSquare class="w-5 h-5 text-indigo-600" />
          <h1 class="text-2xl font-bold text-gray-900">消息中心</h1>
        </div>
        <button
          v-if="isAdmin"
          @click="showBroadcastForm = !showBroadcastForm"
          class="flex items-center gap-1.5 px-4 py-2 bg-orange-500 text-white rounded-lg text-sm font-medium hover:bg-orange-600 transition-colors"
        >
          <Megaphone class="w-4 h-4" />
          发布公告
        </button>
      </div>

      <!-- Broadcast form -->
      <div v-if="showBroadcastForm" class="bg-orange-50 rounded-xl border border-orange-200 p-4 mb-6">
        <div class="flex items-center justify-between mb-3">
          <div class="flex items-center gap-2">
            <Megaphone class="w-5 h-5 text-orange-600" />
            <span class="font-semibold text-orange-800">发布系统公告</span>
          </div>
          <button @click="showBroadcastForm = false" class="text-orange-400 hover:text-orange-600">
            <X class="w-5 h-5" />
          </button>
        </div>
        <textarea
          v-model="broadcastContent"
          rows="3"
          placeholder="请输入公告内容，所有用户都将看到..."
          class="w-full border border-orange-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-orange-500 resize-none"
        ></textarea>
        <div class="flex justify-end mt-3">
          <button
            @click="handleBroadcast"
            :disabled="broadcasting"
            class="flex items-center gap-1.5 px-4 py-2 bg-orange-500 text-white rounded-lg text-sm font-medium hover:bg-orange-600 transition-colors disabled:opacity-50"
          >
            <Send class="w-4 h-4" />
            {{ broadcasting ? '发布中...' : '发布公告' }}
          </button>
        </div>
      </div>

      <!-- Announcements section -->
      <div v-if="announcements.length > 0" class="mb-8">
        <div
          class="flex items-center gap-2 mb-3 cursor-pointer select-none"
          @click="announceExpanded = !announceExpanded"
        >
          <Megaphone class="w-5 h-5 text-orange-500" />
          <h2 class="text-lg font-semibold text-gray-900">系统公告</h2>
          <span class="text-xs text-gray-400 ml-1">({{ announcements.length }})</span>
          <component :is="announceExpanded ? ChevronUp : ChevronDown" class="w-4 h-4 text-gray-400 ml-auto" />
        </div>
        <div v-if="announceExpanded" class="space-y-2">
          <div
            v-for="ann in announcements"
            :key="ann.id"
            class="bg-gradient-to-r from-orange-50 to-amber-50 rounded-lg border border-orange-100 p-4"
          >
            <div class="flex items-center gap-2 mb-1">
              <span class="px-2 py-0.5 rounded-full text-xs font-medium bg-orange-100 text-orange-700">系统公告</span>
              <span class="text-xs text-gray-400">{{ new Date(ann.createTime).toLocaleString() }}</span>
            </div>
            <p class="text-sm text-gray-700">{{ ann.content }}</p>
          </div>
        </div>
      </div>

      <!-- Private messages -->
      <div v-if="loading" class="text-center py-12 text-gray-500">加载中...</div>

      <div v-else-if="messages.length === 0 && announcements.length === 0" class="text-center py-12 text-gray-400">
        <MessageSquare class="w-12 h-12 mx-auto mb-3 opacity-30" />
        <p>暂无消息</p>
      </div>

      <div v-else-if="messages.length > 0">
        <h2 class="text-lg font-semibold text-gray-900 mb-3">我的消息</h2>
        <div class="space-y-3">
          <div
            v-for="msg in messages"
            :key="msg.id"
            :class="[
              'bg-white rounded-lg shadow-sm border p-5 cursor-pointer transition-all',
              msg.isRead === 0 ? 'border-indigo-200 bg-indigo-50/50' : 'border-gray-100'
            ]"
            @click="handleMarkRead(msg.id)"
          >
            <div class="flex items-center justify-between">
              <div>
                <div class="flex items-center gap-2">
                  <span class="font-medium text-gray-900">{{ msg.senderName || '用户' + msg.senderId }}</span>
                  <span v-if="msg.isRead === 0" class="w-2 h-2 bg-indigo-500 rounded-full"></span>
                </div>
                <p class="text-gray-600 mt-1">{{ msg.content }}</p>
                <p class="text-xs text-gray-400 mt-2">{{ new Date(msg.createTime).toLocaleString() }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Layout>
</template>
