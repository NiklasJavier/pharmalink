package de.jklein.pharmalink.api.dto;

import lombok.Data;

/**
 * DTO für die Erstellung von Units für ein Medikament.
 */
@Data
public class CreateUnitsRequest {

    private int amount;
}