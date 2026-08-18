package aaa.yard_sector_p.service;

import aaa.yard_sector_p.model.YardSectorDTO;
import aaa.yard_sector_p.model.YardSectorMapper;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class YardSectorService {

    @Resource
    YardSectorMapper mapper;

    public List<YardSectorDTO> list() {
        return mapper.list();
    }

    public YardSectorDTO detail(Long sectorId) {
        return mapper.detail(sectorId);
    }

    @Transactional
    public YardSectorDTO updateCapacity(Long sectorId, Integer capacity) {
        if (sectorId == null || mapper.detail(sectorId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "야드 섹터를 찾을 수 없습니다.");
        }

        if (capacity == null || capacity < 1 || capacity > 10000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수용량은 1 이상 10000 이하로 입력하세요.");
        }

        if (mapper.updateCapacity(sectorId, capacity) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "야드 섹터 수용량을 변경하지 못했습니다.");
        }

        return mapper.detail(sectorId);
    }
}
