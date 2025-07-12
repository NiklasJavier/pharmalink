package de.jklein.pharmalinkclient.dto;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;

public class DeleteUnitsRequestDto {
    @NotEmpty(message = "Die Liste der Unit-IDs darf nicht leer sein.")
    private List<String> unitIds;

    public DeleteUnitsRequestDto() {
    }

    public DeleteUnitsRequestDto(List<String> unitIds) {
        this.unitIds = unitIds;
    }

    public List<String> getUnitIds() {
        return unitIds;
    }

    public void setUnitIds(List<String> unitIds) {
        this.unitIds = unitIds;
    }
}