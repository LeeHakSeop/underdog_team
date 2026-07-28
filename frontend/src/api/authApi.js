import { request } from './apiClient'

/**
 * 로그인
 */
export const login = (loginData) => {
    return request('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify(loginData)
    })
}

/**
 * 회원가입
 */
export const register = (registerData) => {
    return request('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify(registerData)
    })
}

/**
 * 아이디 중복 확인
 */
export const checkLoginId = (loginId) => {
    return request(`/api/auth/login-id/check?loginId=${encodeURIComponent(loginId)}`)
}

/**
 * 회원 목록 조회 (관리자)
 */
export const getUsers = () => {
    return request('/api/auth/users')
}

/**
 * 회원 승인 / 반려
 */
export const updateUserStatus = (userId, status) => {
    return request(`/api/auth/users/${userId}/status`, {
        method: 'PATCH',
        body: JSON.stringify({
            status
        })
    })
}
