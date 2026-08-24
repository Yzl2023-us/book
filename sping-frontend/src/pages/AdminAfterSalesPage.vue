<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { afterSaleApi, type AfterSaleOrder } from '@/api/afterSale'
import { useAuth } from '@/stores/auth'
import { RotateCcw, ArrowLeft, Check, X, Wallet, ChevronDown, ChevronUp } from 'lucide-vue-next'
import Layout from '@/components/Layout.vue'

const router = useRouter()
const { isAdmin } = useAuth()

const afterSales = ref<AfterSaleOrder[]>([])
const loading = ref(true)
const expandedId = ref<number | null>(null)
const rejectRemark = ref('')
const processing = ref(false)

const statusColor: Record<string, string> = {
  'PENDING_REVIEW': 'text-amber-600 bg-amber-50',
  'APPROVED': 'text-blue-600 bg-blue-50',
  'RETURNED': 'text-indigo-600 bg-indigo-50',
  'REJECTED': 'text-red-600 bg-red-50',
  'REFUNDED': 'text-green-600 bg-green-50',
  'CANCELED': 'text-gray-500 bg-gray-100',
}

async function fetchAfterSales() {
  loading.value = true
  try {
    const res = await afterSaleApi.getAllAfterSales()
    if (res.data.code === 200) {
      afterSales.value = res.data.data || []
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function toggleExpand(id: number) {
  expandedId.value = expandedId.value === id ? null : id
}

async function handleReview(id: number, action: string) {
  processing.value = true
  try {
    const remark = action === 'REJECT' ? rejectRemark.value : undefined
    const res = await afterSaleApi.review(id, action, remark)
    if (res.data.code === 200) {
      rejectRemark.value = ''
      expandedId.value = null
      await fetchAfterSales()
    } else {
      alert(res.data.message)
    }
  } catch {
    alert('操作失败')
  }
  finally { processing.value = false }
}

async function handleRefund(id: number) {
  if (!confirm('确认退款？')) return
  processing.value = true
  try {
    const res = await afterSaleApi.refund(id)
    if (res.data.code === 200) {
      await fetchAfterSales()
    } else {
      alert(res.data.message)
    }
  } catch {
    alert('操作失败')
  }
  finally { processing.value = false }
}

onMounted(() => {
  if (!isAdmin.value) {
    router.replace('/home')
    return
  }
  fetchAfterSales()
})
</script>

<template>
  <Layout>
    <button @click="router.back()" class="flex items-center gap-1 text-gray-500 hover:text-indigo-600 mb-6 transition-colors">
      <ArrowLeft class="w-4 h-4" />
      返回
    </button>

    <h1 class="text-2xl font-bold text-gray-900 mb-6">售后管理</h1>

    <div v-if="loading" class="text-center py-12 text-gray-500">加载中...</div>

    <div v-else-if="afterSales.length === 0" class="text-center py-16">
      <RotateCcw class="w-16 h-16 mx-auto mb-4 text-gray-300" />
      <p class="text-gray-400 text-lg">暂无售后记录</p>
    </div>

    <div v-else class="space-y-3">
      <div
        v-for="a in afterSales"
        :key="a.afterSaleId"
        class="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden"
      >
        <!-- Header -->
        <div
          class="p-4 cursor-pointer hover:bg-gray-50 transition-colors"
          @click="toggleExpand(a.afterSaleId)"
        >
          <div class="flex items-center justify-between">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <span class="text-xs text-gray-400">{{ a.orderNo }}</span>
                <span class="px-2 py-0.5 rounded-full text-xs font-medium" :class="statusColor[a.status] || 'text-gray-600 bg-gray-50'">
                  {{ a.statusText }}
                </span>
                <span class="px-2 py-0.5 rounded-full text-xs bg-gray-100 text-gray-500">{{ a.typeText }}</span>
              </div>
              <p class="text-sm text-gray-600">用户：{{ a.userName }} | 退款：<span class="text-red-600 font-semibold">¥{{ a.refundAmount }}</span></p>
            </div>
            <div class="flex items-center gap-3">
              <!-- Quick actions for PENDING_REVIEW -->
              <template v-if="a.status === 'PENDING_REVIEW'">
                <button
                  @click.stop="handleReview(a.afterSaleId, 'APPROVE')"
                  :disabled="processing"
                  class="px-3 py-1.5 bg-green-500 text-white rounded-lg text-sm hover:bg-green-600 transition-colors disabled:opacity-50"
                >
                  <Check class="w-4 h-4 inline" /> 通过
                </button>
                <button
                  @click.stop="toggleExpand(a.afterSaleId)"
                  class="px-3 py-1.5 bg-red-500 text-white rounded-lg text-sm hover:bg-red-600 transition-colors"
                >
                  <X class="w-4 h-4 inline" /> 拒绝
                </button>
              </template>
              <!-- Refund for RETURNED -->
              <button
                v-if="a.status === 'RETURNED'"
                @click.stop="handleRefund(a.afterSaleId)"
                :disabled="processing"
                class="px-3 py-1.5 bg-green-500 text-white rounded-lg text-sm hover:bg-green-600 transition-colors disabled:opacity-50"
              >
                <Wallet class="w-4 h-4 inline" /> 退款
              </button>
              <component :is="expandedId === a.afterSaleId ? ChevronUp : ChevronDown" class="w-5 h-5 text-gray-400" />
            </div>
          </div>
        </div>

        <!-- Expanded details -->
        <div v-if="expandedId === a.afterSaleId" class="border-t border-gray-100 p-4 bg-gray-50">
          <!-- Reject with remark -->
          <div v-if="a.status === 'PENDING_REVIEW'" class="mb-4">
            <label class="block text-sm text-gray-600 mb-1">拒绝原因</label>
            <div class="flex gap-2">
              <input
                v-model="rejectRemark"
                type="text"
                placeholder="请输入拒绝原因"
                class="flex-1 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-red-500"
              />
              <button
                @click="handleReview(a.afterSaleId, 'REJECT')"
                :disabled="processing || !rejectRemark.trim()"
                class="px-4 py-2 bg-red-500 text-white rounded-lg text-sm hover:bg-red-600 transition-colors disabled:opacity-50"
              >
                确认拒绝
              </button>
            </div>
          </div>

          <div class="text-sm text-gray-600 space-y-1">
            <p><span class="font-medium">售后类型：</span>{{ a.typeText }}</p>
            <p><span class="font-medium">退款金额：</span><span class="text-red-600 font-semibold">¥{{ a.refundAmount }}</span></p>
            <p v-if="a.reason"><span class="font-medium">申请原因：</span>{{ a.reason }}</p>
            <p><span class="font-medium">申请时间：</span>{{ a.createTime }}</p>
            <p v-if="a.reviewTime"><span class="font-medium">审核时间：</span>{{ a.reviewTime }}</p>
            <p v-if="a.returnTime"><span class="font-medium">退货时间：</span>{{ a.returnTime }}</p>
            <p v-if="a.refundTime"><span class="font-medium">退款时间：</span>{{ a.refundTime }}</p>
            <p v-if="a.cancelTime"><span class="font-medium">取消时间：</span>{{ a.cancelTime }}</p>
            <p v-if="a.adminRemark" class="text-red-600"><span class="font-medium">管理员备注：</span>{{ a.adminRemark }}</p>
          </div>
        </div>
      </div>
    </div>
  </Layout>
</template>
