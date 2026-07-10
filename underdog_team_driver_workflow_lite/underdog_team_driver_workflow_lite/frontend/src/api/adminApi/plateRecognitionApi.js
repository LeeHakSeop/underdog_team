import { request } from '../apiClient'

export const recognizePlate = (file, ocrType = 'paddle', plateType = 'TRAILER') => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('ocrType', ocrType)
  formData.append('plateType', plateType)

  return request('/api/plate-recognition/recognize', {
    method: 'POST',
    body: formData,
  })
}
