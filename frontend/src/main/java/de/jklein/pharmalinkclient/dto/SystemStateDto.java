package de.jklein.pharmalinkclient.dto;

import java.util.List;

public class SystemStateDto {
    private String currentActorId;
    private int actorCount;
    private int medikamentCount;
    private int myUnitsCount;
    private List<Actor> allActors;
    private List<Medikament> allMedikamente;
    private List<Unit> myUnits;

    public SystemStateDto() {
    }

    public SystemStateDto(String currentActorId, int actorCount, int medikamentCount, int myUnitsCount,
                          List<Actor> allActors, List<Medikament> allMedikamente, List<Unit> myUnits) {
        this.currentActorId = currentActorId;
        this.actorCount = actorCount;
        this.medikamentCount = medikamentCount;
        this.myUnitsCount = myUnitsCount;
        this.allActors = allActors;
        this.allMedikamente = allMedikamente;
        this.myUnits = myUnits;
    }

    public String getCurrentActorId() {
        return currentActorId;
    }

    public void setCurrentActorId(String currentActorId) {
        this.currentActorId = currentActorId;
    }

    public int getActorCount() {
        return actorCount;
    }

    public void setActorCount(int actorCount) {
        this.actorCount = actorCount;
    }

    public int getMedikamentCount() {
        return medikamentCount;
    }

    public void setMedikamentCount(int medikamentCount) {
        this.medikamentCount = medikamentCount;
    }

    public int getMyUnitsCount() {
        return myUnitsCount;
    }

    public void setMyUnitsCount(int myUnitsCount) {
        this.myUnitsCount = myUnitsCount;
    }

    public List<Actor> getAllActors() {
        return allActors;
    }

    public void setAllActors(List<Actor> allActors) {
        this.allActors = allActors;
    }

    public List<Medikament> getAllMedikamente() {
        return allMedikamente;
    }

    public void setAllMedikamente(List<Medikament> allMedikamente) {
        this.allMedikamente = allMedikamente;
    }

    public List<Unit> getMyUnits() {
        return myUnits;
    }

    public void setMyUnits(List<Unit> myUnits) {
        this.myUnits = myUnits;
    }
}