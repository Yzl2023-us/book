import { createRouter, createWebHistory } from 'vue-router'
import LoginPage from '@/pages/LoginPage.vue'

const routes = [
  {
    path: '/',
    name: 'login',
    component: LoginPage,
  },
  {
    path: '/home',
    name: 'home',
    component: () => import('@/pages/HomePage.vue'),
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/pages/RegisterPage.vue'),
  },
  {
    path: '/publish',
    name: 'publish-book',
    component: () => import('@/pages/PublishBook.vue'),
  },
  {
    path: '/my-published',
    name: 'my-published',
    component: () => import('@/pages/MyPublished.vue'),
  },
  {
    path: '/book/:id',
    name: 'book-detail',
    component: () => import('@/pages/BookDetail.vue'),
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('@/pages/ProfilePage.vue'),
  },
  {
    path: '/messages',
    name: 'messages',
    component: () => import('@/pages/Messages.vue'),
  },
  {
    path: '/admin/books',
    name: 'admin-books',
    component: () => import('@/pages/AdminBooks.vue'),
  },
  {
    path: '/admin/books/:id',
    name: 'admin-book-form',
    component: () => import('@/pages/BookForm.vue'),
  },
  {
    path: '/cart',
    name: 'cart',
    component: () => import('@/pages/CartPage.vue'),
  },
  {
    path: '/admin/users',
    name: 'admin-users',
    component: () => import('@/pages/AdminUsers.vue'),
  },
  {
    path: '/checkout',
    name: 'checkout',
    component: () => import('@/pages/CheckoutPage.vue'),
  },
  {
    path: '/order/:id',
    name: 'order-detail',
    component: () => import('@/pages/OrderDetailPage.vue'),
  },
  {
    path: '/orders',
    name: 'my-orders',
    component: () => import('@/pages/MyOrdersPage.vue'),
  },
  {
    path: '/admin/orders',
    name: 'admin-orders',
    component: () => import('@/pages/AdminOrdersPage.vue'),
  },
  {
    path: '/after-sales',
    name: 'my-after-sales',
    component: () => import('@/pages/MyAfterSalesPage.vue'),
  },
  {
    path: '/after-sale/:id',
    name: 'after-sale-detail',
    component: () => import('@/pages/AfterSaleDetailPage.vue'),
  },
  {
    path: '/admin/after-sales',
    name: 'admin-after-sales',
    component: () => import('@/pages/AdminAfterSalesPage.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router