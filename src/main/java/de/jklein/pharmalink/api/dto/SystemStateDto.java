package de.jklein.pharmalink.api.dto;

import de.jklein.pharmalink.domain.Actor;
import de.jklein.pharmalink.domain.Medikament;
import de.jklein.pharmalink.domain.Unit;
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
    private List<Actor> allActors;
    private List<Medikament> allMedikamente;
    private List<Unit> myUnits;
}