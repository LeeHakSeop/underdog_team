import { request } from '../apiClient'

export const recognizePlate = (file, plateType = 'TRAILER') => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('plateType', plateType)

  return request('/api/plate-recognition/recognize', {
    method: 'POST',
    body: formData,
  })
}
