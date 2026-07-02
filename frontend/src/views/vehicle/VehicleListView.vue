<template>
    <h1>차량입니다</h1>
    <table>
    <tr v-for="vehicle in vehicleStore.vehicles">

    {{ vehicle.plateNumber }}

    </tr>
    </table>
</template>
<script setup>
import { useVehicleStore } from '@/stores/vehicleStore';
import { onMounted } from 'vue';

const vehicleStore = useVehicleStore()

onMounted(() => {

    vehicleStore.loadvehicles()

})

</script>