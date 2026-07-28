const vehicleTypeLabels = {
  TRACTOR: '트랙터',
  트랙터: '트랙터',
  TRAILER: '트레일러',
  트레일러: '트레일러',
  CARGO: '카고',
  윙바디: '윙바디',
  TANK_LORRY: '탱크로리',
  탱크로리: '탱크로리',
  REFRIGERATED: '냉동탑',
  냉동탑: '냉동탑',
}

export const vehicleTypeLabel = (vehicleType) => {
  if (!vehicleType) return '-'

  const normalizedType = String(vehicleType).trim().toUpperCase()
  return vehicleTypeLabels[vehicleType] || vehicleTypeLabels[normalizedType] || vehicleType
}
