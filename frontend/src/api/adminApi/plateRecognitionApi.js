import { request } from '../apiClient'

<<<<<<< HEAD
export const recognizePlate = (file, ocrType = 'crnn', plateType = 'TRAILER') => {
=======
export const recognizePlate = (file, plateType = 'TRAILER') => {
>>>>>>> 7fbd6506b96f09e1a4feffc970b50aafa75abb64
  const formData = new FormData()
  formData.append('file', file)
  formData.append('plateType', plateType)

  return request('/api/plate-recognition/recognize', {
    method: 'POST',
    body: formData,
  })
}
