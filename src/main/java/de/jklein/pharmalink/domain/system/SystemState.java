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

    // Logger Instanz für die Entity-Klasse
    private static final Logger entityLogger = LoggerFactory.getLogger(SystemState.class); // <<-- Hinzugefügt

    @Id
    private String id; // <<-- Dies muss ein String sein, passend zu SYSTEM_STATE_ID in SystemStateService

    private String currentActorId;

    @Lob // Für große Textfelder in PostgreSQL (TEXT/CLOB)
    @Column(columnDefinition = "TEXT") // <<-- WICHTIG: Stellt sicher, dass der SQL-Typ TEXT ist
    private String allActorsJson;
    @Lob
    @Column(columnDefinition = "TEXT") // <<-- WICHTIG: Stellt sicher, dass der SQL-Typ TEXT ist
    private String allMedikamenteJson;
    @Lob
    @Column(columnDefinition = "TEXT") // <<-- WICHTIG: Stellt sicher, dass der SQL-Typ TEXT ist
    private String myUnitsJson;

    // Transient fields for actual object lists (werden nicht direkt in DB gemappt)
    private transient List<Actor> allActors;
    private transient List<Medikament> allMedikamente;
    private transient List<Unit> myUnits;

    // ObjectMapper Instanz zum Serialisieren/Deserialisieren von JSON
    private static final ObjectMapper staticObjectMapper = new ObjectMapper(); // <<-- WICHTIG: Dies ist Ihre Instanz

    // Konstruktor für die initiale Erstellung
    public SystemState(String id, String currentActorId) {
        this.id = id;
        this.currentActorId = currentActorId;
        // Listen initialisieren
        this.allActors = new ArrayList<>();
        this.allMedikamente = new ArrayList<>();
        this.myUnits = new ArrayList<>();
        // JSON-Strings initialisieren (optional, können auch null bleiben bis zum ersten Speichern)
        try {
            this.allActorsJson = staticObjectMapper.writeValueAsString(this.allActors);
            this.allMedikamenteJson = staticObjectMapper.writeValueAsString(this.allMedikamente);
            this.myUnitsJson = staticObjectMapper.writeValueAsString(this.myUnits);
        } catch (JsonProcessingException e) {
            entityLogger.error("Error initializing SystemState JSON fields", e);
            this.allActorsJson = "[]"; // Fallback zu leerem Array JSON
            this.allMedikamenteJson = "[]";
            this.myUnitsJson = "[]";
        }
    }

    // Custom Getter/Setter für JPA-Persistenz der Listen als JSON-Strings
    public List<Actor> getAllActors() {
        if (this.allActors == null && this.allActorsJson != null) {
            try {
                this.allActors = staticObjectMapper.readValue(this.allActorsJson, new TypeReference<List<Actor>>() {});
            } catch (IOException e) {
                entityLogger.error("Error deserializing allActors from JSON: {}", this.allActorsJson, e);
                this.allActors = new ArrayList<>(); // Bei Fehler leere Liste zurückgeben
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
            entityLogger.info("Serialized allActors to JSON: {}", this.allActorsJson); // <<-- Logging hinzugefügt
        } catch (JsonProcessingException e) {
            entityLogger.error("Error serializing allActors: {}", e.getMessage(), e);
            this.allActorsJson = null; // Setzt auf null bei Fehler
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
            entityLogger.info("Serialized allMedikamente to JSON: {}", this.allMedikamenteJson); // <<-- Logging hinzugefügt
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
            entityLogger.info("Serialized myUnits to JSON: {}", this.myUnitsJson); // <<-- Logging hinzugefügt
        } catch (JsonProcessingException e) {
            entityLogger.error("Error serializing myUnits: {}", e.getMessage(), e);
            this.myUnitsJson = null;
        }
    }
}