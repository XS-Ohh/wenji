import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/resources' },
    { path: '/login', component: () => import('../views/LoginView.vue'), meta: { guest: true } },
    { path: '/register', component: () => import('../views/RegisterView.vue'), meta: { guest: true } },
    {
      path: '/',
      component: () => import('../layouts/AppLayout.vue'),
      children: [
        { path: 'resources', component: () => import('../views/ResourcesView.vue') },
        { path: 'resources/:id', component: () => import('../views/ResourceDetailView.vue') },
        { path: 'favorites', component: () => import('../views/FavoritesView.vue'), meta: { auth: true } },
        { path: 'plans', component: () => import('../views/PlansView.vue'), meta: { auth: true } },
        { path: 'plans/create', component: () => import('../views/PlanCreateView.vue'), meta: { auth: true } },
        { path: 'plans/:id', component: () => import('../views/PlanDetailView.vue'), meta: { auth: true } },
        { path: 'profile', component: () => import('../views/ProfileView.vue'), meta: { auth: true } },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const loggedIn = Boolean(localStorage.getItem('wenji_token'))
  if (to.meta.auth && !loggedIn) return { path: '/login', query: { redirect: to.fullPath } }
  if (to.meta.guest && loggedIn) return '/resources'
})

export default router
