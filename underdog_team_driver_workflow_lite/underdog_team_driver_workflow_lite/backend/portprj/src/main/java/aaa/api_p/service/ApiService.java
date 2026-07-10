package aaa.api_p.service;

import aaa.api_p.model.OcrDTO;
import aaa.vehicle_p.model.VehicleDTO;
import aaa.vehicle_p.model.VehicleMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class ApiService {

    @Resource
    VehicleMapper vehicleMapper;

    public void processOcr(OcrDTO dto){

        VehicleDTO vehicle =
                vehicleMapper.findByPlateNumber(dto.getPlateNumber());

        if(vehicle == null){
            System.out.println("미등록 차량");
            System.out.println("번호판 : " + dto.getPlateNumber());
        }else {
            System.out.println("등록 차량");
            System.out.println("번호판 : " + dto.getPlateNumber());
        }
    }
}
