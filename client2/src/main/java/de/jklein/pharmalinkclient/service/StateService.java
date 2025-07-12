package de.jklein.pharmalinkclient.service;

import de.jklein.pharmalinkclient.dto.ActorFilterCriteriaDto;
import de.jklein.pharmalinkclient.dto.ActorResponseDto;
import de.jklein.pharmalinkclient.dto.MedikamentFilterCriteriaDto;
import de.jklein.pharmalinkclient.dto.MedikamentResponseDto;
import de.jklein.pharmalinkclient.dto.SystemStateDto;
import de.jklein.pharmalinkclient.dto.SystemStatsDto;
import de.jklein.pharmalinkclient.dto.Actor;      
import de.jklein.pharmalinkclient.dto.Medikament;
import de.jklein.pharmalinkclient.dto.Unit;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;

import java.io.Serializable;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;


@SpringComponent
@VaadinSessionScope
public class StateService implements Serializable {

    private static final long serialVersionUID = 1L;

    private Optional<MedikamentResponseDto> selectedMedikament = Optional.empty();
    private Optional<MedikamentFilterCriteriaDto> currentMedikamentFilterCriteria = Optional.empty();

    private final transient CopyOnWriteArrayList<Consumer<Optional<MedikamentResponseDto>>> selectedMedikamentListeners = new CopyOnWriteArrayList<>();
    private final transient CopyOnWriteArrayList<Consumer<Optional<MedikamentFilterCriteriaDto>>> medikamentFilterCriteriaListeners = new CopyOnWriteArrayList<>();

    private ActorResponseDto selectedActor;
    private final PropertyChangeSupport actorSelectionChangeSupport = new PropertyChangeSupport(this);

    private Optional<ActorFilterCriteriaDto> currentActorFilterCriteria = Optional.empty();
    private final transient CopyOnWriteArrayList<Consumer<Optional<ActorFilterCriteriaDto>>> actorFilterCriteriaListeners = new CopyOnWriteArrayList<>();

    private String navigateToMedikamentId;
    private final PropertyChangeSupport navigateToMedikamentSupport = new PropertyChangeSupport(this);

    private String currentActorId;
    private SystemStatsDto cacheStats; // GEÄNDERT: Typ von Map<String, Object> zu SystemStatsDto
    private SystemStateDto systemState; // NEU: Variable für den vollständigen Systemzustand

    private final transient CopyOnWriteArrayList<Consumer<String>> currentActorIdListeners = new CopyOnWriteArrayList<>();
    private final transient CopyOnWriteArrayList<Consumer<SystemStatsDto>> cacheStatsListeners = new CopyOnWriteArrayList<>(); // GEÄNDERT: Typ
    private final transient CopyOnWriteArrayList<Consumer<SystemStateDto>> systemStateListeners = new CopyOnWriteArrayList<>(); // NEU: Listener für SystemStateDto

    private boolean systemDataLoadedForSession = false;

    private List<ActorResponseDto> allLoadedActors = Collections.emptyList();
    private final transient CopyOnWriteArrayList<Consumer<List<ActorResponseDto>>> allLoadedActorsListeners = new CopyOnWriteArrayList<>();

    private List<Actor> allSystemActors = Collections.emptyList();
    private final transient CopyOnWriteArrayList<Consumer<List<Actor>>> allSystemActorsListeners = new CopyOnWriteArrayList<>();

    private List<Medikament> allSystemMedikamente = Collections.emptyList();
    private final transient CopyOnWriteArrayList<Consumer<List<Medikament>>> allSystemMedikamenteListeners = new CopyOnWriteArrayList<>();

    private List<Unit> mySystemUnits = Collections.emptyList();
    private final transient CopyOnWriteArrayList<Consumer<List<Unit>>> mySystemUnitsListeners = new CopyOnWriteArrayList<>();


    public StateService() {
        this.currentMedikamentFilterCriteria = Optional.of(new MedikamentFilterCriteriaDto("", "Ohne Filter"));
        this.currentActorFilterCriteria = Optional.of(new ActorFilterCriteriaDto(""));
    }

    public Optional<MedikamentResponseDto> getSelectedMedikament() {
        return selectedMedikament;
    }

    public void setSelectedMedikament(MedikamentResponseDto medikament) {
        this.selectedMedikament = Optional.ofNullable(medikament);
        selectedMedikamentListeners.forEach(listener -> listener.accept(this.selectedMedikament));
    }

    public void clearSelectedMedikament() {
        this.selectedMedikament = Optional.empty();
        selectedMedikamentListeners.forEach(listener -> listener.accept(this.selectedMedikament));
    }

    public void addSelectedMedikamentListener(Consumer<Optional<MedikamentResponseDto>> listener) {
        selectedMedikamentListeners.add(listener);
    }

    public void removeSelectedMedikamentListener(Consumer<Optional<MedikamentResponseDto>> listener) {
        selectedMedikamentListeners.remove(listener);
    }

    public Optional<MedikamentFilterCriteriaDto> getCurrentMedikamentFilterCriteria() {
        return currentMedikamentFilterCriteria;
    }

    public void setCurrentMedikamentFilterCriteria(MedikamentFilterCriteriaDto criteria) {
        Optional<MedikamentFilterCriteriaDto> oldCriteria = this.currentMedikamentFilterCriteria;
        this.currentMedikamentFilterCriteria = Optional.ofNullable(criteria);
        medikamentFilterCriteriaListeners.forEach(listener -> listener.accept(this.currentMedikamentFilterCriteria));
    }

    public void addMedikamentFilterCriteriaListener(Consumer<Optional<MedikamentFilterCriteriaDto>> listener) {
        medikamentFilterCriteriaListeners.add(listener);
    }

    public void removeMedikamentFilterCriteriaListener(Consumer<Optional<MedikamentFilterCriteriaDto>> listener) {
        medikamentFilterCriteriaListeners.remove(listener);
    }

    public ActorResponseDto getSelectedActor() {
        return selectedActor;
    }

    public void setSelectedActor(ActorResponseDto newSelectedActor) {
        ActorResponseDto oldSelectedActor = this.selectedActor;
        this.selectedActor = newSelectedActor;
        actorSelectionChangeSupport.firePropertyChange("selectedActor", oldSelectedActor, newSelectedActor);
    }

    public void addSelectedActorListener(PropertyChangeListener listener) {
        actorSelectionChangeSupport.addPropertyChangeListener("selectedActor", listener);
    }

    public void addSelectedActorListener(Consumer<ActorResponseDto> consumer) {
        actorSelectionChangeSupport.addPropertyChangeListener("selectedActor", evt -> {
            if (evt.getNewValue() instanceof ActorResponseDto) {
                consumer.accept((ActorResponseDto) evt.getNewValue());
            } else {
                consumer.accept(null);
            }
        });
    }

    public void removeSelectedActorListener(PropertyChangeListener listener) {
        actorSelectionChangeSupport.removePropertyChangeListener("selectedActor", listener);
    }

    public void removeSelectedActorListener(Consumer<ActorResponseDto> consumer) {
    }

    public String getNavigateToMedikamentId() {
        return navigateToMedikamentId;
    }

    public void setNavigateToMedikamentId(String navigateToMedikamentId) {
        String oldId = this.navigateToMedikamentId;
        this.navigateToMedikamentId = navigateToMedikamentId;
        if (!Objects.equals(oldId, navigateToMedikamentId)) {
            navigateToMedikamentSupport.firePropertyChange("navigateToMedikamentId", oldId, navigateToMedikamentId);
        }
    }

    public void addNavigateToMedikamentListener(PropertyChangeListener listener) {
        navigateToMedikamentSupport.addPropertyChangeListener("navigateToMedikamentId", listener);
    }

    public void addNavigateToMedikamentListener(Consumer<String> consumer) {
        navigateToMedikamentSupport.addPropertyChangeListener("navigateToMedikamentId", evt -> {
            if (evt.getNewValue() instanceof String) {
                consumer.accept((String) evt.getNewValue());
            } else {
                consumer.accept(null);
            }
        });
    }

    public void removeNavigateToMedikamentListener(PropertyChangeListener listener) {
        navigateToMedikamentSupport.removePropertyChangeListener("navigateToMedikamentId", listener);
    }

    public Optional<ActorFilterCriteriaDto> getCurrentActorFilterCriteria() {
        return currentActorFilterCriteria;
    }

    public void setCurrentActorFilterCriteria(ActorFilterCriteriaDto criteria) {
        Optional<ActorFilterCriteriaDto> oldCriteria = this.currentActorFilterCriteria;
        this.currentActorFilterCriteria = Optional.ofNullable(criteria);
        actorFilterCriteriaListeners.forEach(listener -> listener.accept(this.currentActorFilterCriteria));
    }

    public void addActorFilterCriteriaListener(Consumer<Optional<ActorFilterCriteriaDto>> listener) {
        actorFilterCriteriaListeners.add(listener);
    }

    public void removeActorFilterCriteriaListener(Consumer<Optional<ActorFilterCriteriaDto>> listener) {
        actorFilterCriteriaListeners.remove(listener);
    }

    public String getCurrentActorId() {
        return currentActorId;
    }

    public void setCurrentActorId(String currentActorId) {
        String oldActorId = this.currentActorId;
        this.currentActorId = currentActorId;
        currentActorIdListeners.forEach(listener -> listener.accept(this.currentActorId));
    }

    public void addCurrentActorIdListener(Consumer<String> listener) {
        currentActorIdListeners.add(listener);
    }

    public void removeCurrentActorIdListener(Consumer<String> listener) {
        currentActorIdListeners.remove(listener);
    }

    public SystemStatsDto getCacheStats() {
        return cacheStats;
    }

    public void setCacheStats(SystemStatsDto cacheStats) {
        SystemStatsDto oldCacheStats = this.cacheStats;
        this.cacheStats = cacheStats;
        cacheStatsListeners.forEach(listener -> listener.accept(this.cacheStats));
    }

    public void addCacheStatsListener(Consumer<SystemStatsDto> listener) {
        cacheStatsListeners.add(listener);
    }

    public void removeCacheStatsListener(Consumer<SystemStatsDto> listener) {
        cacheStatsListeners.remove(listener);
    }

    public SystemStateDto getSystemState() {
        return systemState;
    }

    public void setSystemState(SystemStateDto systemState) {
        this.systemState = systemState;
        systemStateListeners.forEach(listener -> listener.accept(this.systemState));
    }

    public void addSystemStateListener(Consumer<SystemStateDto> listener) {
        systemStateListeners.add(listener);
    }

    public void removeSystemStateListener(Consumer<SystemStateDto> listener) {
        systemStateListeners.remove(listener);
    }

    public boolean isSystemDataLoadedForSession() {
        return systemDataLoadedForSession;
    }

    public void setSystemDataLoadedForSession(boolean systemDataLoadedForSession) {
        this.systemDataLoadedForSession = systemDataLoadedForSession;
    }

    public List<ActorResponseDto> getAllLoadedActors() {
        return allLoadedActors;
    }

    public void setAllLoadedActors(List<ActorResponseDto> allLoadedActors) {
        this.allLoadedActors = allLoadedActors != null ? allLoadedActors : Collections.emptyList();
        allLoadedActorsListeners.forEach(listener -> listener.accept(this.allLoadedActors));
    }

    public void addAllLoadedActorsListener(Consumer<List<ActorResponseDto>> listener) {
        allLoadedActorsListeners.add(listener);
    }

    public void removeAllLoadedActorsListener(Consumer<List<ActorResponseDto>> listener) {
        allLoadedActorsListeners.remove(listener);
    }

    public List<Actor> getAllSystemActors() {
        return allSystemActors;
    }

    public void setAllSystemActors(List<Actor> allSystemActors) {
        this.allSystemActors = allSystemActors != null ? allSystemActors : Collections.emptyList();
        this.allSystemActorsListeners.forEach(listener -> listener.accept(this.allSystemActors));
    }

    public void addAllSystemActorsListener(Consumer<List<Actor>> listener) {
        this.allSystemActorsListeners.add(listener);
    }

    public void removeAllSystemActorsListener(Consumer<List<Actor>> listener) {
        this.allSystemActorsListeners.remove(listener);
    }

    public List<Medikament> getAllSystemMedikamente() {
        return allSystemMedikamente;
    }

    public void setAllSystemMedikamente(List<Medikament> allSystemMedikamente) {
        this.allSystemMedikamente = allSystemMedikamente != null ? allSystemMedikamente : Collections.emptyList();
        this.allSystemMedikamenteListeners.forEach(listener -> listener.accept(this.allSystemMedikamente));
    }

    public void addAllSystemMedikamenteListener(Consumer<List<Medikament>> listener) {
        this.allSystemMedikamenteListeners.add(listener);
    }

    public void removeAllSystemMedikamenteListener(Consumer<List<Medikament>> listener) {
        this.allSystemMedikamenteListeners.remove(listener);
    }

    public List<Unit> getMySystemUnits() {
        return mySystemUnits;
    }

    public void setMySystemUnits(List<Unit> mySystemUnits) {
        this.mySystemUnits = mySystemUnits != null ? mySystemUnits : Collections.emptyList();
        this.mySystemUnitsListeners.forEach(listener -> listener.accept(this.mySystemUnits));
    }

    public void addMySystemUnitsListener(Consumer<List<Unit>> listener) {
        this.mySystemUnitsListeners.add(listener);
    }

    public void removeMySystemUnitsListener(Consumer<List<Unit>> listener) {
        this.mySystemUnitsListeners.remove(listener);
    }
}