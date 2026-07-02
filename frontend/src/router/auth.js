/*
=========================================
인증(Auth) Router
=========================================

역할
- 로그인 화면
- 회원가입 (추후)
- 비밀번호 찾기 (추후)

관련 View
- LoginView
=========================================
*/

import LoginView from '@/views/LoginView.vue'

export default [

  {
    path: '/',
    redirect: '/login',
  },

  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: {
      shell: false
    }
  }

]