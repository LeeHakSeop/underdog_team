/*
=========================================
차량(vehicle) Router
=========================================

역할
- 
- 
- 

접근 권한
vehicle
=========================================
*/

import VehicleListView from "@/views/vehicle/VehicleListView.vue";


export default [
  {
    path: '/vehicle',
    redirect:'/vehicle/list',
    children: [
      {
        path: 'list',
        name: 'vehicle-list',
        component: VehicleListView,
      },

    ],
  },
]