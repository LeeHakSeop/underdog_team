package aaa.plate_recognition_p.controller;

import aaa.plate_recognition_p.model.PlateRecognitionResultDTO;
import aaa.plate_recognition_p.service.PlateRecognitionService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/plate-recognition")
public class PlateRecognitionController {

    @Resource
    PlateRecognitionService plateRecognitionService;

    @PostMapping(value = "/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PlateRecognitionResultDTO recognize(@RequestParam("file") MultipartFile file) throws IOException {
        return plateRecognitionService.recognize(file);
    }
}
