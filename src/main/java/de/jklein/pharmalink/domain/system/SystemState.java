package de.jklein.pharmalink.domain.system;

import de.jklein.pharmalink.domain.Actor;
import de.jklein.pharmalink.domain.Medikament;
import de.jklein.pharmalink.domain.Unit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger; // Import für Logger
import org.slf4j.LoggerFactory; // Import für LoggerFactory

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemState {

    private static final Logger entityLogger = LoggerFactory.getLogger(SystemState.class);

    @Id
    private String id;

    private String currentActorId;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String allActorsJson;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String allMedikamenteJson;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String myUnitsJson;

    private transient List<Actor> allActors;
    private transient List<Medikament> allMedikamente;
    private transient List<Unit> myUnits;

    private static final ObjectMapper staticObjectMapper = new ObjectMapper();

    public SystemState(String id, String currentActorId) {
        this.id = id;
        this.currentActorId = currentActorId;
        this.allActors = new ArrayList<>();
        this.allMedikamente = new ArrayList<>();
        this.myUnits = new ArrayList<>();
        try {
            this.allActorsJson = staticObjectMapper.writeValueAsString(this.allActors);
            this.allMedikamenteJson = staticObjectMapper.writeValueAsString(this.allMedikamente);
            this.myUnitsJson = staticObjectMapper.writeValueAsString(this.myUnits);
        } catch (JsonProcessingException e) {
            entityLogger.error("Error initializing SystemState JSON fields", e);
            this.allActorsJson = "[]";
            this.allMedikamenteJson = "[]";
            this.myUnitsJson = "[]";
        }
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

    public void setAllActors(List<Actor> allActors) {
        this.allActors = allActors;
        try {
            this.allActorsJson = staticObjectMapper.writeValueAsString(allActors);
            entityLogger.info("Serialized allActors to JSON: {}", this.allActorsJson);
        } catch (JsonProcessingException e) {
            entityLogger.error("Error serializing allActors: {}", e.getMessage(), e);
            this.allActorsJson = null;
        }
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

    public void setAllMedikamente(List<Medikament> allMedikamente) {
        this.allMedikamente = allMedikamente;
        try {
            this.allMedikamenteJson = staticObjectMapper.writeValueAsString(allMedikamente);
            entityLogger.info("Serialized allMedikamente to JSON: {}", this.allMedikamenteJson);
        } catch (JsonProcessingException e) {
            entityLogger.error("Error serializing allMedikamente: {}", e.getMessage(), e);
            this.allMedikamenteJson = null;
        }
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

    public void setMyUnits(List<Unit> myUnits) {
        this.myUnits = myUnits;
        try {
            this.myUnitsJson = staticObjectMapper.writeValueAsString(myUnits);
            entityLogger.info("Serialized myUnits to JSON: {}", this.myUnitsJson);
        } catch (JsonProcessingException e) {
            entityLogger.error("Error serializing myUnits: {}", e.getMessage(), e);
            this.myUnitsJson = null;
        }
    }
}