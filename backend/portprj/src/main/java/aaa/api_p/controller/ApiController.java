package aaa.api_p.controller;


import aaa.api_p.model.OcrDTO;
import aaa.api_p.service.ApiService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@RequestMapping("/api")
public class ApiController {

    @Resource
    ApiService apiService;

    @PostMapping("/ocr")
    public Map<String, Object> receiveResult(@RequestBody OcrDTO dto){

        //System.out.println("번호판 : " + dto.getPlateNumber());

        apiService.processOcr(dto);

        return Map.of("msg", "success");
    }

}
