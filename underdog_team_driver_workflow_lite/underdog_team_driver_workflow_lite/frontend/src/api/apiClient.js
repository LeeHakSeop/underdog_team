// .env.development에 작성한 서버 기본 주소를 가져옵니다.
// 예: http://localhost
const BASE_URL =
  import.meta.env.VITE_API_BASE_URL || 'http://localhost'

export const request = async (path, options = {}) => {
  const token = localStorage.getItem('token')
  const headers = new Headers(options.headers)

  if (
    options.body &&
    !(options.body instanceof FormData) &&
    !headers.has('Content-Type')
  ) {
    headers.set('Content-Type', 'application/json')
  }

  const isPublicRequest =
    path === '/api/login' ||
    path === '/api/register' ||
    path === '/api/auth/login' ||
    path === '/api/auth/register' ||
    path === '/api/auth/signup' ||
    path === '/api/auth/admin-init'

  if (token && !isPublicRequest) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers,
  })

  if (response.status === 401 || response.status === 403) {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    localStorage.removeItem('portGateUser')

    if (window.location.pathname !== '/login') {
      window.location.href = '/login'
    }

    throw new Error('로그인이 필요합니다.')
  }

  if (!response.ok) {
    let message = '요청 처리에 실패했습니다.'

    try {
      const data = await response.json()
      message =
        data.message ||
        data.error ||
        data.detail ||
        message
    } catch {
      try {
        const text = await response.text()

        if (text) {
          message = text
        }
      } catch {
        // 응답 본문을 읽을 수 없으면 기본 메시지 사용
      }
    }

    throw new Error(message)
  }

  if (response.status === 204) {
    return null
  }

  const text = await response.text()

  if (!text) {
    return null
  }

  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}