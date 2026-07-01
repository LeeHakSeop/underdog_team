package aaa.yard_sector_p.service;

import aaa.yard_sector_p.model.YardSectorDTO;
import aaa.yard_sector_p.model.YardSectorMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class YardSectorService {

    @Resource
    YardSectorMapper mapper;

    public List<YardSectorDTO> list() {
        return mapper.list();
    }
}
