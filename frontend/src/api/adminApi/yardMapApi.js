import { fetchContainers } from './containerApi'
import { fetchGateLogs } from './gateLogApi'
import { fetchYardSectors } from './yardSectorApi'

export const fetchYardMapSnapshot = async () => {
  const [containers, gateLogs, yardSectors] = await Promise.allSettled([
    fetchContainers(),
    fetchGateLogs(),
    fetchYardSectors(),
  ])

  return {
    containers: containers.status === 'fulfilled' ? containers.value : null,
    gateLogs: gateLogs.status === 'fulfilled' ? gateLogs.value : null,
    yardSectors: yardSectors.status === 'fulfilled' ? yardSectors.value : null,
    hasError: [containers, gateLogs, yardSectors].some((result) => result.status === 'rejected'),
  }
}
