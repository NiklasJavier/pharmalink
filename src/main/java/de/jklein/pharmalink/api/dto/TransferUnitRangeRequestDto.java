package de.jklein.pharmalink.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferUnitRangeRequestDto {
    private String medId;
    private String chargeBezeichnung;
    private int startCounter;
    private int endCounter;
    private String newOwnerId;
    private String transferTimestamp;
}