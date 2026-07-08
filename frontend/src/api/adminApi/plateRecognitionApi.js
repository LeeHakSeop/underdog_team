import { request } from '../apiClient'

export const recognizePlate = (file, ocrType = 'paddle') => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('ocrType', ocrType)

  return request('/api/plate-recognition/recognize', {
    method: 'POST',
    body: formData,
  })
}
