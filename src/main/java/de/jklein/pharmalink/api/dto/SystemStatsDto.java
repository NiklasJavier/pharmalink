package de.jklein.pharmalink.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SystemStatsDto {
    private int actorCount;
    private int medikamentCount;
    private int myUnitsCount;
}