<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'
import { messageApi } from '@/api/message'
import { Book, User, MessageSquare, ShoppingCart, LogOut, Menu, X, PlusCircle, PackageOpen, RotateCcw } from 'lucide-vue-next'

const router = useRouter()
const { user, isLoggedIn, isAdmin, logout } = useAuth()
const unreadCount = ref(0)
const mobileMenuOpen = ref(false)

async function fetchUnreadCount() {
  if (!isLoggedIn.value) return
  try {
    const res = await messageApi.getUnreadCount()
    if (res.data.code === 200) {
      unreadCount.value = res.data.data
    }
  } catch { /* ignore */ }
}

fetchUnreadCount()

function handleLogout() {
  logout()
  router.push('/')
  mobileMenuOpen.value = false
}

function navigateTo(path: string) {
  router.push(path)
  mobileMenuOpen.value = false
}
</script>

<template>
  <nav class="bg-white shadow-sm border-b border-gray-200 sticky top-0 z-50">
    <div class="max-w-7xl mx-auto px-4">
      <div class="flex items-center justify-between h-16">
        <div class="flex items-center gap-2 cursor-pointer" @click="navigateTo('/home')">
          <Book class="w-6 h-6 text-indigo-600" />
          <span class="text-xl font-bold text-gray-900">Sping 书城</span>
        </div>

        <!-- Desktop nav -->
        <div class="hidden md:flex items-center gap-6">
          <button @click="navigateTo('/home')" class="text-gray-700 hover:text-indigo-600 transition-colors">
            首页
          </button>

          <template v-if="isLoggedIn">
            <button @click="navigateTo('/messages')" class="text-gray-700 hover:text-indigo-600 transition-colors relative">
              <MessageSquare class="w-5 h-5 inline" />
              <span v-if="unreadCount > 0" class="absolute -top-2 -right-4 bg-red-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center">
                {{ unreadCount > 99 ? '99+' : unreadCount }}
              </span>
            </button>
            <button @click="navigateTo('/cart')" class="text-gray-700 hover:text-indigo-600 transition-colors">
              <ShoppingCart class="w-5 h-5 inline" />
            </button>
            <button @click="navigateTo('/orders')" class="text-gray-700 hover:text-indigo-600 transition-colors">
              <PackageOpen class="w-5 h-5 inline" />
            </button>
            <button @click="navigateTo('/after-sales')" class="text-gray-700 hover:text-indigo-600 transition-colors">
              <RotateCcw class="w-5 h-5 inline" />
            </button>
            <button @click="navigateTo('/publish')" class="text-indigo-600 hover:text-indigo-700 transition-colors flex items-center gap-1">
              <PlusCircle class="w-4 h-4" />
              发布图书
            </button>
            <button @click="navigateTo('/my-published')" class="text-gray-700 hover:text-indigo-600 transition-colors">
              我的发布
            </button>
            <button v-if="isAdmin" @click="navigateTo('/admin/books')" class="text-gray-700 hover:text-indigo-600 transition-colors">
              图书管理
            </button>
            <button v-if="isAdmin" @click="navigateTo('/admin/users')" class="text-gray-700 hover:text-indigo-600 transition-colors">
              用户管理
            </button>
            <button v-if="isAdmin" @click="navigateTo('/admin/orders')" class="text-gray-700 hover:text-indigo-600 transition-colors">
              订单管理
            </button>
            <button v-if="isAdmin" @click="navigateTo('/admin/after-sales')" class="text-gray-700 hover:text-indigo-600 transition-colors">
              售后管理
            </button>
            <div class="flex items-center gap-3 ml-4">
              <div class="flex items-center gap-2 cursor-pointer" @click="navigateTo('/profile')">
                <User class="w-5 h-5 text-gray-500" />
                <span class="text-gray-700">{{ user?.userName }}</span>
              </div>
              <button @click="handleLogout" class="text-gray-400 hover:text-red-500 transition-colors">
                <LogOut class="w-5 h-5" />
              </button>
            </div>
          </template>
          <template v-else>
            <button @click="navigateTo('/')" class="text-gray-700 hover:text-indigo-600 transition-colors">
              登录
            </button>
            <button @click="navigateTo('/register')" class="bg-indigo-600 text-white px-4 py-2 rounded-lg hover:bg-indigo-700 transition-colors">
              注册
            </button>
          </template>
        </div>

        <!-- Mobile toggle -->
        <button class="md:hidden" @click="mobileMenuOpen = !mobileMenuOpen">
          <X v-if="mobileMenuOpen" class="w-6 h-6" />
          <Menu v-else class="w-6 h-6" />
        </button>
      </div>

      <!-- Mobile menu -->
      <div v-if="mobileMenuOpen" class="md:hidden pb-4 border-t border-gray-100 pt-2">
        <div class="flex flex-col gap-2">
          <button @click="navigateTo('/home')" class="text-left px-2 py-2 text-gray-700 hover:bg-gray-50 rounded">首页</button>
          <template v-if="isLoggedIn">
            <button @click="navigateTo('/messages')" class="text-left px-2 py-2 text-gray-700 hover:bg-gray-50 rounded flex items-center gap-2">
              消息
              <span v-if="unreadCount > 0" class="bg-red-500 text-white text-xs rounded-full px-2 py-0.5">{{ unreadCount }}</span>
            </button>
            <button @click="navigateTo('/cart')" class="text-left px-2 py-2 text-gray-700 hover:bg-gray-50 rounded flex items-center gap-2">
              购物车
            </button>
            <button @click="navigateTo('/orders')" class="text-left px-2 py-2 text-gray-700 hover:bg-gray-50 rounded flex items-center gap-2">
              我的订单
            </button>
            <button @click="navigateTo('/after-sales')" class="text-left px-2 py-2 text-gray-700 hover:bg-gray-50 rounded flex items-center gap-2">
              我的售后
            </button>
            <button @click="navigateTo('/publish')" class="text-left px-2 py-2 text-indigo-600 hover:bg-indigo-50 rounded flex items-center gap-2">
              发布图书
            </button>
            <button @click="navigateTo('/my-published')" class="text-left px-2 py-2 text-gray-700 hover:bg-gray-50 rounded flex items-center gap-2">
              我的发布
            </button>
            <button v-if="isAdmin" @click="navigateTo('/admin/books')" class="text-left px-2 py-2 text-gray-700 hover:bg-gray-50 rounded">图书管理</button>
            <button v-if="isAdmin" @click="navigateTo('/admin/users')" class="text-left px-2 py-2 text-gray-700 hover:bg-gray-50 rounded">用户管理</button>
            <button v-if="isAdmin" @click="navigateTo('/admin/orders')" class="text-left px-2 py-2 text-gray-700 hover:bg-gray-50 rounded">订单管理</button>
            <button v-if="isAdmin" @click="navigateTo('/admin/after-sales')" class="text-left px-2 py-2 text-gray-700 hover:bg-gray-50 rounded">售后管理</button>
            <button @click="navigateTo('/profile')" class="text-left px-2 py-2 text-gray-700 hover:bg-gray-50 rounded">个人中心</button>
            <button @click="handleLogout" class="text-left px-2 py-2 text-red-500 hover:bg-red-50 rounded">退出登录</button>
          </template>
          <template v-else>
            <button @click="navigateTo('/')" class="text-left px-2 py-2 text-gray-700 hover:bg-gray-50 rounded">登录</button>
            <button @click="navigateTo('/register')" class="text-left px-2 py-2 text-indigo-600 hover:bg-indigo-50 rounded">注册</button>
          </template>
        </div>
      </div>
    </div>
  </nav>
</template>