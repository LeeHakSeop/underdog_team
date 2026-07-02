import { request } from "./apiClient"

export const fetchVehicles = () => {

    return request('/api/vehicle')
}

export const fetchVehicleById = (vehicleId) => {
  return request(`/api/vehicle/${vehicleId}`)
}

export const createVehicle = (vehicle) => {
  return request('/api/vehicle', {
    method: 'POST',
    body: JSON.stringify(vehicle),
  })
}

export const updateVehicle = (vehicleId, vehicle) => {
  return request(`/api/vehicle/${vehicleId}`, {
    method: 'PUT',
    body: JSON.stringify(vehicle),
  })
}

export const deleteVehicle = (vehicleId) => {
  return request(`/api/vehicle/${vehicleId}`, {
    method: 'DELETE',
  })
}

