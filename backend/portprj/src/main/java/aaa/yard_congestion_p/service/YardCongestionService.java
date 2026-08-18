package aaa.yard_congestion_p.service;

import aaa.yard_congestion_p.model.YardCongestionDTO;
import aaa.yard_congestion_p.model.YardCongestionMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class YardCongestionService {

    @Resource
    YardCongestionMapper mapper;

    public YardCongestionDTO congestion() {
        YardCongestionDTO dto = new YardCongestionDTO();
        dto.setSummary(mapper.summary());
        dto.setSectors(mapper.sectors());
        return dto;
    }
}
