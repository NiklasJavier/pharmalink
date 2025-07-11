package de.jklein.pharmalink.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class SystemStateDto {
    private String currentActorId;
    private int actorCount;
    private int medikamentCount;
    private int myUnitsCount;

    private List<ActorResponseDto> allActors;
    private List<MedikamentResponseDto> allMedikamente;
    private List<UnitResponseDto> myUnits;
}