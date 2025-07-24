package de.jklein.pharmalink.domain.system;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jklein.pharmalink.domain.Actor;
import de.jklein.pharmalink.domain.Medikament;
import de.jklein.pharmalink.domain.Unit;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "system_state")
@Data
@NoArgsConstructor
public class SystemState {

    @Transient
    private static final Logger entityLogger = LoggerFactory.getLogger(SystemState.class);

    @Id
    private String id;

    private String currentActorId;

    // Diese Felder werden in MongoDB gespeichert
    private String allActorsJson;
    private String allMedikamenteJson;
    private String myUnitsJson;

    // Transient-Felder werden nicht in der DB gespeichert
    @Transient
    private List<Actor> allActors;
    @Transient
    private List<Medikament> allMedikamente;
    @Transient
    private List<Unit> myUnits;

    @Transient
    private static final ObjectMapper staticObjectMapper = new ObjectMapper();

    public SystemState(String id, String currentActorId) {
        this.id = id;
        this.currentActorId = currentActorId;
        setAllActors(new ArrayList<>());
        setAllMedikamente(new ArrayList<>());
        setMyUnits(new ArrayList<>());
    }

    public List<Actor> getAllActors() {
        if (this.allActors == null && this.allActorsJson != null) {
            try {
                this.allActors = staticObjectMapper.readValue(this.allActorsJson, new TypeReference<List<Actor>>() {});
            } catch (IOException e) {
                entityLogger.error("Error deserializing allActors from JSON: {}", this.allActorsJson, e);
                this.allActors = new ArrayList<>();
            }
        } else if (this.allActors == null) {
            this.allActors = new ArrayList<>();
        }
        return this.allActors;
    }

    public List<Medikament> getAllMedikamente() {
        if (this.allMedikamente == null && this.allMedikamenteJson != null) {
            try {
                this.allMedikamente = staticObjectMapper.readValue(this.allMedikamenteJson, new TypeReference<List<Medikament>>() {});
            } catch (IOException e) {
                entityLogger.error("Error deserializing allMedikamente from JSON: {}", this.allMedikamenteJson, e);
                this.allMedikamente = new ArrayList<>();
            }
        } else if (this.allMedikamente == null) {
            this.allMedikamente = new ArrayList<>();
        }
        return this.allMedikamente;
    }

    public List<Unit> getMyUnits() {
        if (this.myUnits == null && this.myUnitsJson != null) {
            try {
                this.myUnits = staticObjectMapper.readValue(this.myUnitsJson, new TypeReference<List<Unit>>() {});
            } catch (IOException e) {
                entityLogger.error("Error deserializing myUnits from JSON: {}", this.myUnitsJson, e);
                this.myUnits = new ArrayList<>();
            }
        } else if (this.myUnits == null) {
            this.myUnits = new ArrayList<>();
        }
        return this.myUnits;
    }

    // --- Setter mit Serialisierungslogik ---

    public void setAllActors(List<Actor> allActors) {
        this.allActors = allActors;
        try {
            this.allActorsJson = staticObjectMapper.writeValueAsString(allActors);
        } catch (JsonProcessingException e) {
            entityLogger.error("Error serializing allActors: {}", e.getMessage(), e);
            this.allActorsJson = "[]";
        }
    }

    public void setAllMedikamente(List<Medikament> allMedikamente) {
        this.allMedikamente = allMedikamente;
        try {
            this.allMedikamenteJson = staticObjectMapper.writeValueAsString(allMedikamente);
        } catch (JsonProcessingException e) {
            entityLogger.error("Error serializing allMedikamente: {}", e.getMessage(), e);
            this.allMedikamenteJson = "[]";
        }
    }

    public void setMyUnits(List<Unit> myUnits) {
        this.myUnits = myUnits;
        try {
            this.myUnitsJson = staticObjectMapper.writeValueAsString(myUnits);
        } catch (JsonProcessingException e) {
            entityLogger.error("Error serializing myUnits: {}", e.getMessage(), e);
            this.myUnitsJson = "[]";
        }
    }
}