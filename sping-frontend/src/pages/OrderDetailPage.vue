<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { purchaseApi, type PurchaseOrder } from '@/api/purchase'
import { afterSaleApi } from '@/api/afterSale'
import { useAuth } from '@/stores/auth'
import { ArrowLeft, Package, CreditCard, CheckCircle, XCircle, Truck, Clock, RotateCcw } from 'lucide-vue-next'
import Layout from '@/components/Layout.vue'

const route = useRoute()
const router = useRouter()
const { isLoggedIn } = useAuth()

const orderId = Number(route.params.id)
const order = ref<PurchaseOrder | null>(null)
const loading = ref(true)
const paying = ref(false)
const canceling = ref(false)
const showAfterSaleForm = ref(false)
const applying = ref(false)
const afterSaleForm = ref({ type: 'RETURN_REFUND', reason: '', refundAmount: 0 })

async function fetchOrder() {
  loading.value = true
  try {
    const res = await purchaseApi.getOrderDetail(orderId)
    if (res.data.code === 200) {
      order.value = res.data.data
    } else {
      alert(res.data.message)
      router.replace('/orders')
    }
  } catch {
    alert('加载订单失败')
  }
  finally { loading.value = false }
}

async function handlePay() {
  paying.value = true
  try {
    const res = await purchaseApi.pay(orderId)
    if (res.data.code === 200) {
      await fetchOrder()
    } else {
      alert(res.data.message || '支付失败')
    }
  } catch {
    alert('支付失败，请重试')
  }
  finally { paying.value = false }
}

async function handleCancel() {
  if (!confirm('确认取消该订单？')) return
  canceling.value = true
  try {
    const res = await purchaseApi.cancelOrder(orderId)
    if (res.data.code === 200) {
      await fetchOrder()
    } else {
      alert(res.data.message || '取消订单失败')
    }
  } catch {
    alert('取消订单失败，请重试')
  }
  finally { canceling.value = false }
}

function openAfterSaleForm() {
  afterSaleForm.value = {
    type: 'RETURN_REFUND',
    reason: '',
    refundAmount: order.value?.totalAmount || 0,
  }
  showAfterSaleForm.value = true
}

async function submitAfterSale() {
  if (!afterSaleForm.value.reason.trim()) {
    alert('请填写申请原因')
    return
  }
  if (afterSaleForm.value.refundAmount <= 0) {
    alert('退款金额必须大于0')
    return
  }
  if (afterSaleForm.value.refundAmount > (order.value?.totalAmount || 0)) {
    alert('退款金额不能超过订单总额')
    return
  }
  applying.value = true
  try {
    const res = await afterSaleApi.apply({
      orderId,
      type: afterSaleForm.value.type,
      reason: afterSaleForm.value.reason,
      refundAmount: afterSaleForm.value.refundAmount,
    })
    if (res.data.code === 200) {
      alert('售后申请已提交！')
      showAfterSaleForm.value = false
    } else {
      alert(res.data.message || '申请失败')
    }
  } catch {
    alert('申请失败，请重试')
  }
  finally { applying.value = false }
}

const statusConfig: Record<string, { icon: any; color: string; bg: string }> = {
  'PENDING_PAYMENT': { icon: Clock, color: 'text-amber-600', bg: 'bg-amber-50' },
  'PENDING_REVIEW': { icon: Clock, color: 'text-blue-600', bg: 'bg-blue-50' },
  'APPROVED': { icon: CheckCircle, color: 'text-green-600', bg: 'bg-green-50' },
  'REJECTED': { icon: XCircle, color: 'text-red-600', bg: 'bg-red-50' },
  'SHIPPED': { icon: Truck, color: 'text-indigo-600', bg: 'bg-indigo-50' },
  'COMPLETED': { icon: CheckCircle, color: 'text-green-600', bg: 'bg-green-50' },
  'CANCELED': { icon: XCircle, color: 'text-gray-500', bg: 'bg-gray-100' },
}

onMounted(() => {
  if (!isLoggedIn.value) {
    router.replace('/')
    return
  }
  fetchOrder()
})
</script>

<template>
  <Layout>
    <button @click="router.back()" class="flex items-center gap-1 text-gray-500 hover:text-indigo-600 mb-6 transition-colors">
      <ArrowLeft class="w-4 h-4" />
      返回
    </button>

    <div v-if="loading" class="text-center py-12 text-gray-500">加载中...</div>

    <div v-else-if="order" class="max-w-3xl mx-auto space-y-6">
      <!-- Order header -->
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-3">
            <Package class="w-6 h-6 text-indigo-600" />
            <h1 class="text-xl font-bold text-gray-900">订单详情</h1>
          </div>
          <span
            class="px-3 py-1 rounded-full text-sm font-medium"
            :class="[statusConfig[order.status]?.bg || 'bg-gray-50', statusConfig[order.status]?.color || 'text-gray-600']"
          >
            {{ order.statusText }}
          </span>
        </div>
        <div class="text-sm text-gray-500 space-y-1">
          <p>订单编号：{{ order.orderNo }}</p>
          <p>下单时间：{{ order.createTime }}</p>
          <p v-if="order.payTime">支付时间：{{ order.payTime }}</p>
          <p v-if="order.reviewTime">审核时间：{{ order.reviewTime }}</p>
          <p v-if="order.shipTime">发货时间：{{ order.shipTime }}</p>
          <p v-if="order.cancelTime">取消时间：{{ order.cancelTime }}</p>
        </div>
      </div>

      <!-- Payment info -->
      <div v-if="order.payment" class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
        <div class="flex items-center gap-2 mb-3">
          <CreditCard class="w-5 h-5 text-indigo-600" />
          <h2 class="text-lg font-semibold text-gray-900">支付信息</h2>
        </div>
        <div class="text-sm text-gray-600 space-y-1">
          <p>交易流水号：{{ order.payment.transactionId }}</p>
          <p>支付金额：<span class="text-indigo-600 font-semibold">¥{{ order.payment.payAmount }}</span></p>
          <p>支付方式：{{ order.payment.payMethod === 'BALANCE' ? '余额支付' : order.payment.payMethod }}</p>
          <p>支付时间：{{ order.payment.payTime }}</p>
        </div>
      </div>

      <!-- Review remark if rejected -->
      <div v-if="order.status === 'REJECTED' && order.reviewRemark" class="bg-red-50 rounded-xl border border-red-100 p-4">
        <div class="flex items-center gap-2 mb-1">
          <XCircle class="w-5 h-5 text-red-500" />
          <span class="font-semibold text-red-700">审核拒绝</span>
        </div>
        <p class="text-sm text-red-600">{{ order.reviewRemark }}</p>
      </div>

      <!-- Recipient info -->
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-3">收货信息</h2>
        <div class="text-sm text-gray-600 space-y-1">
          <p>收件人：{{ order.recipientName }}</p>
          <p>联系电话：{{ order.recipientPhone }}</p>
          <p>收货地址：{{ order.recipientAddress }}</p>
        </div>
      </div>

      <!-- Items -->
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-3">商品清单</h2>
        <div class="space-y-3">
          <div
            v-for="item in order.items"
            :key="item.itemId"
            class="flex items-center justify-between py-2 border-b border-gray-50 last:border-0"
          >
            <div class="flex-1 min-w-0">
              <p class="text-sm font-medium text-gray-900 truncate">{{ item.bookName }}</p>
              <p class="text-xs text-gray-500">¥{{ item.bookPrice }} x {{ item.quantity }}</p>
            </div>
            <p class="text-sm font-semibold text-gray-900">¥{{ item.subtotal?.toFixed(2) || (item.bookPrice * item.quantity).toFixed(2) }}</p>
          </div>
        </div>
        <div class="flex justify-end mt-4 pt-4 border-t border-gray-100">
          <p class="text-lg font-bold text-indigo-600">合计：¥{{ order.totalAmount }}</p>
        </div>
      </div>

      <!-- Action buttons -->
      <div v-if="order.status === 'PENDING_PAYMENT'" class="flex gap-3">
        <button
          @click="handlePay"
          :disabled="paying"
          class="flex-1 bg-indigo-600 text-white py-3 rounded-lg hover:bg-indigo-700 transition-colors font-medium disabled:opacity-50"
        >
          {{ paying ? '支付中...' : '立即支付（模拟）' }}
        </button>
        <button
          @click="handleCancel"
          :disabled="canceling"
          class="px-6 py-3 bg-white text-red-600 border border-red-300 rounded-lg hover:bg-red-50 transition-colors font-medium disabled:opacity-50"
        >
          {{ canceling ? '取消中...' : '取消订单' }}
        </button>
      </div>
      <div v-if="order.status === 'PENDING_REVIEW'" class="flex gap-3">
        <button
          @click="handleCancel"
          :disabled="canceling"
          class="px-6 py-3 bg-white text-red-600 border border-red-300 rounded-lg hover:bg-red-50 transition-colors font-medium disabled:opacity-50"
        >
          {{ canceling ? '取消中...' : '取消订单' }}
        </button>
      </div>

      <!-- After-sale -->
      <div
        v-if="order.status === 'APPROVED' || order.status === 'SHIPPED' || order.status === 'COMPLETED'"
        class="bg-white rounded-xl shadow-sm border border-gray-100 p-6"
      >
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-2">
            <RotateCcw class="w-5 h-5 text-orange-500" />
            <h2 class="text-lg font-semibold text-gray-900">售后服务</h2>
          </div>
          <button
            v-if="!showAfterSaleForm"
            @click="openAfterSaleForm"
            class="px-4 py-2 bg-orange-500 text-white rounded-lg text-sm font-medium hover:bg-orange-600 transition-colors"
          >
            申请售后
          </button>
        </div>

        <!-- After-sale form -->
        <div v-if="showAfterSaleForm" class="mt-4 border-t border-gray-100 pt-4 space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">售后类型</label>
            <div class="flex gap-3">
              <label class="flex items-center gap-2 cursor-pointer">
                <input v-model="afterSaleForm.type" type="radio" value="RETURN_REFUND" class="text-orange-500" />
                <span class="text-sm">退货退款</span>
              </label>
              <label class="flex items-center gap-2 cursor-pointer">
                <input v-model="afterSaleForm.type" type="radio" value="REFUND_ONLY" class="text-orange-500" />
                <span class="text-sm">仅退款</span>
              </label>
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">退款金额</label>
            <input
              v-model.number="afterSaleForm.refundAmount"
              type="number"
              step="0.01"
              :max="order.totalAmount"
              class="w-48 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-orange-500"
            />
            <span class="text-xs text-gray-400 ml-2">最大 ¥{{ order.totalAmount }}</span>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">申请原因</label>
            <textarea
              v-model="afterSaleForm.reason"
              rows="3"
              placeholder="请描述申请售后原因..."
              class="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-orange-500 resize-none"
            ></textarea>
          </div>
          <div class="flex gap-3">
            <button
              @click="submitAfterSale"
              :disabled="applying"
              class="px-6 py-2 bg-orange-500 text-white rounded-lg text-sm font-medium hover:bg-orange-600 transition-colors disabled:opacity-50"
            >
              {{ applying ? '提交中...' : '提交申请' }}
            </button>
            <button
              @click="showAfterSaleForm = false"
              class="px-6 py-2 bg-gray-100 text-gray-600 rounded-lg text-sm font-medium hover:bg-gray-200 transition-colors"
            >
              取消
            </button>
          </div>
        </div>
      </div>
    </div>
  </Layout>
</template>
