// .env.development에 작성한 서버 기본 주소를 가져옵니다.
// 현재 값: http://localhost
const BASE_URL = import.meta.env.VITE_API_BASE_URL

// 다른 파일에서 사용할 수 있도록 request 함수를 export합니다.
// path: 세부 API 주소
// options: method, body, headers 등 fetch에 전달할 추가 설정
// options를 전달하지 않으면 빈 객체 {}를 기본값으로 사용합니다.
export const request = async (path, options = {}) => {
    // 브라우저 localStorage에 저장된 JWT를 가져옵니다.
    // 아직 로그인하지 않았다면 token에는 null이 들어갑니다.

    const token = localStorage.getItem('token')
    // 기존 요청 헤더를 Headers 객체로 변환합니다.
    // Headers 객체를 사용하면 has(), set() 메서드로 헤더를 쉽게 관리할 수 있습니다.
    const headers = new Headers(options.headers)

    if (
    options.body &&
    !(options.body instanceof FormData) &&
    !headers.has('Content-Type')
    ){
        headers.set('Content-Type', 'application/json')
    }
    


    // 요청 본문 body가 존재하고,
    // Content-Type 헤더가 아직 설정되지 않은 경우에만 실행합니다.
    // if (options.body && !headers.has('Content-Type')) {

    //     // 요청 본문이 JSON 형식임을 Spring 서버에 알려줍니다.
    //     headers.set('Content-Type', 'application/json')
    // }
    //요청 URL에 따라 로그인요청인지 확인 
    const isLoginRequest = path === '/api/login'

    // 로그인 후 JWT가 존재할 때와 로그인 요청이 아닌 경우만 실행합니다.
    if (token  && !isLoginRequest){
    // Spring 서버에 JWT를 전달합니다.
    // 실제 전송 형태: Authorization: Bearer JWT문자열
    headers.set('Authorization', `Bearer ${token}`)
  }

  // BASE_URL과 세부 path를 결합하여 Spring API에 요청합니다.
  // 예시:
  // BASE_URL = http://localhost
  // path = /api/admin/buildings
  // 최종 주소 = http://localhost/api/admin/buildings
  const response = await fetch(`${BASE_URL}${path}`, {

    // options에 들어 있는 method, body 등의 설정을 펼쳐서 전달합니다.
    // 예시: { method: 'POST', body: JSON.stringify(building) }
    ...options,

    // 위에서 정리한 headers를 최종 요청 헤더로 전달합니다.
    headers,
  })

  // HTTP 응답 상태가 정상 범위가 아닌 경우 실행합니다.
  // response.ok는 상태 코드가 200~299이면 true입니다.
  if (!response.ok) {

    let msg = '요청 처리 실패'

    try {
        const data = await response.json()
        msg = data.message || msg
    } catch {
        // 아무것도 안 함
    }

    throw new Error(msg)
}

  // HTTP 상태 코드가 204인 경우 실행합니다.
  // 204 No Content는 요청은 성공했지만 응답 본문이 없다는 의미입니다.
  // 주로 삭제 또는 수정 요청에서 사용할 수 있습니다.
  if (response.status === 204) {

    // JSON으로 변환할 본문이 없으므로 null을 반환합니다.
    return null
  }

  // 응답 본문이 존재하면 JSON 객체로 변환하여 반환합니다.
  // 호출한 Store 또는 API 함수가 이 값을 받습니다.
  const text = await response.text()

  if (!text) {
    return null
  }

  return JSON.parse(text)
}