import { login, register } from '@/api/authApi'

const USER_KEY = 'portGateUser'
const TOKEN_KEY = 'token'

export const roleOptions = [
  { code: 'CARRIER', label: '운송사 담당자', home: '/carrier/dashboard' },
  { code: 'DRIVER', label: '화물 기사', home: '/driver/dashboard' },
  { code: 'ADMIN', label: '관리자', home: '/admin/main' },
]

const readJson = (key, fallback) => {
  try {
    return JSON.parse(
      localStorage.getItem(key) || JSON.stringify(fallback),
    )
  } catch {
    return fallback
  }
}

const writeJson = (key, value) => {
  localStorage.setItem(key, JSON.stringify(value))
}

export const findRole = (roleCode) => {
  return (
    roleOptions.find((role) => role.code === roleCode) ||
    roleOptions[0]
  )
}

export const readCurrentUser = () => {
  return readJson(USER_KEY, null)
}

export const clearCurrentUser = () => {
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(TOKEN_KEY)
}

export const loginAccount = async ({
  username,
  password,
  roleCode,
}) => {
  const data = await login({
    loginId: username,
    password,
    roleCode,
  })

  const role = findRole(data.roleCode)

  const user = {
    userId: data.userId,
    username: data.loginId,
    loginId: data.loginId,
    userName: data.userName,
    roleCode: data.roleCode,
    roleName: role.label,
    status: data.status,
    displayName: data.userName || data.loginId,
  }

  const token = data.token || data.accessToken

  if (!token) {
    throw new Error('로그인 토큰을 받지 못했습니다.')
  }

  localStorage.setItem(TOKEN_KEY, token)
  writeJson(USER_KEY, user)

  return {
    user,
    home: role.home,
  }
}

export const registerAccount = async (account) => {
  return register({
    loginId: account.username,
    password: account.password,
    userName: account.displayName || account.username,
    roleCode: account.roleCode,

    carrierName: account.carrierName,
    carrierContact: account.carrierContact,
    managerName: account.managerName,

    driverName: account.driverName,
    driverContact: account.driverContact,
    carrierId: account.carrierId,

    plateNumber: account.plateNumber,
    vehicleType: account.vehicleType,
  })
}

export const readAccounts = () => {
  return []
}

export const saveAccounts = () => {}

export const updateAccountStatus = () => {}
