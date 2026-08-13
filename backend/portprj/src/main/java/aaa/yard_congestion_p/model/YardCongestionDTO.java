package aaa.yard_congestion_p.model;

import lombok.Data;

import java.util.List;

@Data
public class YardCongestionDTO {
    private YardCongestionSummaryDTO summary;
    private List<YardCongestionSectorDTO> sectors;
}
