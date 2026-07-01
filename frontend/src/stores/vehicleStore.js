import { fetchVehicles } from "@/api/vehicleApi";
import { defineStore } from "pinia";
import { ref } from "vue";

export const useVehicleStore = defineStore('vehicle', ()=>{
    const vehicles =ref([])
    const loading = ref(false)
    const errMsg= ref('')

    const loadvehicles = async () => {
    
        loading.value = true 
        errMsg.value = ''

        try{
            vehicles.value = await fetchVehicles()
        }catch(error) {
            errMsg.value = error.message
            console.error('차량목록 조회 실패:',error)
        }finally{
            loading.value = false
        }

    }

    return{
        loading,
        errMsg,
        loadvehicles,
        vehicles
    }
}
)