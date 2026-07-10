package aaa.carrier_p.model;

import lombok.Data;

@Data
public class CarrierDTO {
    private Long carrierId;
    private String carrierName;
    private String carrierContact;
    private String managerName;
    private String carrierStatus;
    private Long userId;
}
