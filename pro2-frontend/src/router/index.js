import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { isLoggedIn } from '../utils/auth'
import { openLoginDialog, authState } from '../store/user'

// 路由表：requiresAuth 需登录，adminOnly 仅管理员

const routes = [
  { path: '/', name: 'home', component: () => import('../views/HomeView.vue'), meta: { title: '首页' } },
  { path: '/post/create', name: 'post-create', component: () => import('../views/PostCreateView.vue'), meta: { title: '发帖', requiresAuth: true } },
  { path: '/post/edit/:id', name: 'post-edit', component: () => import('../views/PostCreateView.vue'), meta: { title: '编辑帖子', requiresAuth: true } },
  { path: '/post/:id', name: 'post-detail', component: () => import('../views/PostDetailView.vue'), meta: { title: '帖子详情' } },
  { path: '/rank', name: 'rank', component: () => import('../views/RankView.vue'), meta: { title: '热度排行榜' } },
  { path: '/profile', name: 'profile', component: () => import('../views/ProfileView.vue'), meta: { title: '个人中心', requiresAuth: true } },
  // 消息 / 聊天 / 他人主页
  { path: '/msg', name: 'msg', component: () => import('../views/MessageCenterView.vue'), meta: { title: '消息', requiresAuth: true } },
  { path: '/chat/:userId', name: 'chat', component: () => import('../views/ChatWindowView.vue'), meta: { title: '私聊', requiresAuth: true } },
  { path: '/user/:id', name: 'user-profile', component: () => import('../views/UserProfileView.vue'), meta: { title: '个人主页' } },
  { path: '/friends', name: 'friends', component: () => import('../views/FriendManageView.vue'), meta: { title: '好友管理', requiresAuth: true } },
  // 管理端用户表
  { path: '/admin/users', name: 'admin-users', component: () => import('../views/UserAdminView.vue'), meta: { title: '用户管理', requiresAuth: true, adminOnly: true } },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to) => {
  if (to.meta.requiresAuth && !isLoggedIn()) {
    openLoginDialog()
    return { name: 'home' }
  }
  if (to.meta.adminOnly && authState.user?.role !== 'ADMIN') {
    ElMessage.warning('仅管理员可以访问用户管理')
    return { name: 'home' }
  }
  document.title = `${to.meta.title || ''} · PulseTalk`
  return true
})

export default router
