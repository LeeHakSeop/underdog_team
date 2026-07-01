import axios from 'axios'

const carrierClient = axios.create({
  baseURL: 'http://localhost/api/carrier',
  headers: {
    'Content-Type': 'application/json',
  },
})

export async function fetchCarriers() {
  const response = await carrierClient.get('/list')
  return response.data
}

export async function registerCarrier(carrier) {
  const response = await carrierClient.post('/reg', carrier)
  return response.data
}
