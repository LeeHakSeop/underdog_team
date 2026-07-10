import { request } from "./apiClient"

export const fetchVehicles = () => {

    return request('/api/vehicle/list')
}

export const fetchVehicleById = (vehicleId) => {
  return request(`/api/vehicle/detail/${vehicleId}`)
}

export const fetchVehiclesByCarrier = (carrierId) => {
  return request(`/api/vehicle/carrier/${carrierId}`)
}

export const fetchVehicleByDriver = (driverId) => {
  return request(`/api/vehicle/driver/${driverId}`)
}

export const createVehicle = (vehicle) => {
  return request('/api/vehicle/reg', {
    method: 'POST',
    body: JSON.stringify(vehicle),
  })
}

export const updateVehicleApproval = (vehicleId, isRegistered) => {
  return request(`/api/vehicle/${vehicleId}/approval`, {
    method: 'PATCH',
    body: JSON.stringify({
      isRegistered,
    }),
  })
}

export const updateVehicle = (vehicleId, vehicle) => {
  return request(`/api/vehicle/modify/${vehicleId}`, {
    method: 'PUT',
    body: JSON.stringify(vehicle),
  })
}

export const deleteVehicle = (vehicleId) => {
  return request(`/api/vehicle/delete/${vehicleId}`, {
    method: 'DELETE',
  })
}

