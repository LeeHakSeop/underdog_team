/*
=========================================
Router Guard
=========================================

역할
- 로그인 여부 확인
- 권한(Role) 확인
- 접근 가능한 화면으로 이동

현재
- LocalStorage 로그인 확인

추후
- JWT 검증
- Token 만료 확인
- 권한별 접근 제어
=========================================
*/

export function authGuard(to) {
  if (to.path === '/login') {
    return true
  }

  const user = JSON.parse(localStorage.getItem('portGateUser') || 'null')

  if (!user) {
    return '/login'
  }

  const roleRoot = {
    CARRIER: '/carrier',
    DRIVER: '/driver',
    ADMIN: '/admin',
  }[user.roleCode]

  if (roleRoot && !to.path.startsWith(roleRoot)) {
    return roleRoot === '/admin' ? '/admin/main' : `${roleRoot}/dashboard`
  }

  return true
}