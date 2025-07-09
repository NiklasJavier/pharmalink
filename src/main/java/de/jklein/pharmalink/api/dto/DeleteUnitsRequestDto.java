package de.jklein.pharmalink.api.dto;

import java.util.List;

public class DeleteUnitsRequestDto {
    private List<String> unitIds;

    public List<String> getUnitIds() {
        return unitIds;
    }

    public void setUnitIds(List<String> unitIds) {
        this.unitIds = unitIds;
    }
}