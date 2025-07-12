package de.jklein.pharmalinkclient.dto; 
public class SystemStatsDto {
    private int actorCount;
    private int medikamentCount;
    private int myUnitsCount;

    public SystemStatsDto() {
    }

    public SystemStatsDto(int actorCount, int medikamentCount, int myUnitsCount) {
        this.actorCount = actorCount;
        this.medikamentCount = medikamentCount;
        this.myUnitsCount = myUnitsCount;
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
}