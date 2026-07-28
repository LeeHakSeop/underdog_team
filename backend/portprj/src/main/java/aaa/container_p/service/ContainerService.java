package aaa.container_p.service;

import aaa.container_p.model.ContainerDTO;
import aaa.container_p.model.ContainerMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ContainerService {

    @Resource
    ContainerMapper mapper;

    public List<ContainerDTO> list() {
        return mapper.list();
    }

    public ContainerDTO detail(Long containerId) {
        return mapper.detail(containerId);
    }

    public ContainerDTO insert(ContainerDTO dto) {
        validate(dto);

        if (mapper.findByContainerNumber(dto.getContainerNumber()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 컨테이너 번호입니다.");
        }

        if (dto.getCanExit() == null) {
            dto.setCanExit(true);
        }

        mapper.insert(dto);
        return mapper.detail(dto.getContainerId());
    }

    public ContainerDTO update(Long containerId, ContainerDTO dto) {
        if (mapper.detail(containerId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "컨테이너 정보를 찾을 수 없습니다.");
        }

        dto.setContainerId(containerId);
        validate(dto);

        ContainerDTO duplicated = mapper.findByContainerNumber(dto.getContainerNumber());
        if (duplicated != null && !containerId.equals(duplicated.getContainerId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 컨테이너 번호입니다.");
        }

        if (dto.getCanExit() == null) {
            dto.setCanExit(true);
        }

        mapper.update(dto);
        return mapper.detail(containerId);
    }

    public void delete(Long containerId) {
        if (mapper.detail(containerId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "컨테이너 정보를 찾을 수 없습니다.");
        }

        if (mapper.countWorkOrders(containerId) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "작업에 연결된 컨테이너는 삭제할 수 없습니다.");
        }

        mapper.delete(containerId);
    }

    public int allowExit(Long containerId) {
        return mapper.updateCanExit(containerId, true);
    }

    public int blockExit(Long containerId) {
        return mapper.updateCanExit(containerId, false);
    }

    private void validate(ContainerDTO dto) {
        if (dto == null || dto.getContainerNumber() == null || dto.getContainerNumber().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "컨테이너 번호는 필수입니다.");
        }

        if (dto.getSectorId() != null && !mapper.existsYardSector(dto.getSectorId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "등록된 야드 섹터를 선택하세요.");
        }
    }
}
