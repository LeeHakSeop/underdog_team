export function authGuard(to) {
  if (to.path === '/login') {
    return true
  }

  const token = localStorage.getItem('token')
  const user = JSON.parse(localStorage.getItem('portGateUser') || 'null')

  if (!token || !user) {
    return '/login'
  }

  const roleHome = {
    CARRIER: '/carrier/dashboard',
    DRIVER: '/driver/dashboard',
    ADMIN: '/admin/main',
  }

  const roleRoot = {
    CARRIER: '/carrier',
    DRIVER: '/driver',
    ADMIN: '/admin',
  }[user.roleCode]

  if (roleRoot && !to.path.startsWith(roleRoot)) {
    return roleHome[user.roleCode] || '/login'
  }

  return true
}