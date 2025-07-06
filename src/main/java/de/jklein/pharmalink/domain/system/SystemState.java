package de.jklein.pharmalink.domain.system;

import de.jklein.pharmalink.domain.Actor;
import de.jklein.pharmalink.domain.Medikament;
import de.jklein.pharmalink.domain.Unit;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "system_state")
@Getter
@Setter
@NoArgsConstructor
public class SystemState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "current_actor_id", unique = true)
    private String currentActorId;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Transient
    private List<Actor> allActors = new ArrayList<>();
    @Transient
    private List<Medikament> allMedikamente = new ArrayList<>();
    @Transient
    private List<Unit> myUnits = new ArrayList<>();

    public SystemState(String currentActorId) {
        this.currentActorId = currentActorId;
        this.lastUpdated = LocalDateTime.now();
    }

    @PrePersist
    @PreUpdate
    public void setLastUpdated() {
        this.lastUpdated = LocalDateTime.now();
    }

    public void setAllActors(List<Actor> allActors) {
        this.allActors.clear();
        if (allActors != null) {
            this.allActors.addAll(allActors);
        }
    }

    public void setAllMedikamente(List<Medikament> allMedikamente) {
        this.allMedikamente.clear();
        if (allMedikamente != null) {
            this.allMedikamente.addAll(allMedikamente);
        }
    }

    public void setMyUnits(List<Unit> myUnits) {
        this.myUnits.clear();
        if (myUnits != null) {
            this.myUnits.addAll(myUnits);
        }
    }
}