package de.jklein.pharmalinkclient.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ActorIdResponse {
    private final String actorId;

    @JsonCreator
    public ActorIdResponse(@JsonProperty("actorId") String actorId) {
        this.actorId = actorId;
    }

    public String getActorId() {
        return actorId;
    }
}