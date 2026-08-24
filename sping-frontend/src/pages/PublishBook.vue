<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'
import { bookApi, type BookParams, type BookType } from '@/api/book'
import { Upload, Package, X } from 'lucide-vue-next'
import Layout from '@/components/Layout.vue'

const router = useRouter()
const { isLoggedIn } = useAuth()
const loading = ref(false)
const uploadingCover = ref(false)
const error = ref('')
const success = ref('')
const bookTypes = ref<BookType[]>([])
const coverFile = ref<File | null>(null)
const coverPreview = ref<string>('')

const form = reactive<BookParams>({
  bookName: '',
  bookAuthor: '',
  bookPrice: 0,
  bookTypeId: 1,
  bookDesc: '',
  bookImg: '',
  bookStock: 1,
})

onMounted(async () => {
  if (!isLoggedIn.value) {
    router.push('/')
    return
  }
  try {
    const res = await bookApi.getBookTypes()
    if (res.data.code === 200) {
      bookTypes.value = res.data.data || []
      if (bookTypes.value.length > 0) form.bookTypeId = bookTypes.value[0].bookTypeId
    }
  } catch { /* ignore */ }
})

function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  if (input.files && input.files[0]) {
    coverFile.value = input.files[0]
    coverPreview.value = URL.createObjectURL(input.files[0])
  }
}

function clearCover() {
  coverFile.value = null
  coverPreview.value = ''
  form.bookImg = ''
}

async function handleSubmit() {
  error.value = ''
  success.value = ''
  if (!form.bookName.trim()) { error.value = '请输入书名'; return }
  if (!form.bookAuthor.trim()) { error.value = '请输入作者'; return }
  if (!form.bookDesc.trim()) { error.value = '请输入描述'; return }
  if (!form.bookPrice || form.bookPrice <= 0) { error.value = '请输入有效价格'; return }

  loading.value = true
  try {
    // 先上传封面
    if (coverFile.value) {
      uploadingCover.value = true
      const uploadRes = await bookApi.uploadCover(coverFile.value)
      if (uploadRes.data.code === 200) {
        form.bookImg = uploadRes.data.data.url
      } else {
        error.value = uploadRes.data.message || '封面上传失败'
        loading.value = false
        uploadingCover.value = false
        return
      }
      uploadingCover.value = false
    }

    const res = await bookApi.publishBook(form)
    if (res.data.code === 200) {
      success.value = '发布成功！请等待管理员审核通过后即可在首页展示。'
      // 重置表单
      form.bookName = ''
      form.bookAuthor = ''
      form.bookPrice = 0
      form.bookDesc = ''
      form.bookImg = ''
      form.bookStock = 1
      clearCover()
    } else {
      error.value = res.data.message || '发布失败'
    }
  } catch (e: any) {
    error.value = e.response?.data?.message || '发布失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <Layout>
    <div class="max-w-2xl mx-auto">
      <div class="flex items-center gap-2 mb-6">
        <Package class="w-5 h-5 text-indigo-600" />
        <h1 class="text-2xl font-bold text-gray-900">发布图书</h1>
      </div>

      <div class="bg-white rounded-xl shadow-sm p-8">
        <!-- Success -->
        <div v-if="success" class="mb-6 p-4 bg-green-50 border border-green-200 text-green-700 rounded-lg text-sm">
          {{ success }}
          <div class="mt-3 flex gap-3">
            <button @click="success = ''" class="text-indigo-600 hover:text-indigo-700 text-sm font-medium">继续发布</button>
            <router-link to="/my-published" class="text-indigo-600 hover:text-indigo-700 text-sm font-medium">查看我的发布 →</router-link>
          </div>
        </div>

        <!-- Error -->
        <div v-if="error" class="mb-6 p-3 bg-red-50 border border-red-200 text-red-600 rounded-lg text-sm">{{ error }}</div>

        <form v-if="!success" @submit.prevent="handleSubmit" class="space-y-5">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">书名 <span class="text-red-500">*</span></label>
            <input
              v-model="form.bookName"
              type="text"
              class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-colors"
              placeholder="请输入书名"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">作者 <span class="text-red-500">*</span></label>
            <input
              v-model="form.bookAuthor"
              type="text"
              class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-colors"
              placeholder="请输入作者"
            />
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">价格 (¥) <span class="text-red-500">*</span></label>
              <input
                v-model.number="form.bookPrice"
                type="number"
                min="0"
                step="0.01"
                class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-colors"
                placeholder="0.00"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">库存</label>
              <input
                v-model.number="form.bookStock"
                type="number"
                min="1"
                class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-colors"
              />
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">分类</label>
            <select
              v-model="form.bookTypeId"
              class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-colors"
            >
              <option v-for="t in bookTypes" :key="t.bookTypeId" :value="t.bookTypeId">{{ t.bookTypeName }}</option>
            </select>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">描述 <span class="text-red-500">*</span></label>
            <textarea
              v-model="form.bookDesc"
              rows="4"
              class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-colors resize-none"
              placeholder="请输入图书描述"
            ></textarea>
          </div>

          <!-- Cover -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">封面图片</label>
            <div class="flex items-start gap-4">
              <div v-if="coverPreview" class="w-32 h-40 rounded-lg overflow-hidden border border-gray-200 flex-shrink-0">
                <img :src="coverPreview" alt="封面预览" class="w-full h-full object-cover" />
              </div>
              <div v-else class="w-32 h-40 rounded-lg border-2 border-dashed border-gray-300 flex items-center justify-center flex-shrink-0 bg-gray-50">
                <Upload class="w-6 h-6 text-gray-400" />
              </div>
              <div class="flex-1">
                <label class="inline-flex items-center gap-2 px-4 py-2.5 bg-white border border-gray-300 rounded-lg cursor-pointer hover:border-indigo-400 hover:text-indigo-600 transition-colors text-sm">
                  <Upload class="w-4 h-4" />
                  选择图片
                  <input type="file" accept="image/*" class="hidden" @change="handleFileSelect" />
                </label>
                <p class="text-xs text-gray-400 mt-2">支持 jpg、png、gif、webp 格式</p>
                <button v-if="coverPreview" @click="clearCover" type="button" class="mt-2 text-xs text-red-500 hover:text-red-600 flex items-center gap-1">
                  <X class="w-3 h-3" /> 移除封面
                </button>
              </div>
            </div>
          </div>

          <div class="flex gap-3 pt-2">
            <router-link
              to="/home"
              class="flex-1 border border-gray-300 text-gray-700 py-2.5 rounded-lg hover:bg-gray-50 transition-colors text-center"
            >
              取消
            </router-link>
            <button
              type="submit"
              :disabled="loading"
              class="flex-1 bg-indigo-600 text-white py-2.5 rounded-lg hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {{ uploadingCover ? '上传封面中...' : loading ? '发布中...' : '提交发布' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </Layout>
</template>
