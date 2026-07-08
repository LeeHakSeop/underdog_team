import { request } from '../apiClient'

export const recognizePlate = (file) => {
  const formData = new FormData()
  formData.append('file', file)

  return request('/api/plate-recognition/recognize', {
    method: 'POST',
    body: formData,
  })
}
