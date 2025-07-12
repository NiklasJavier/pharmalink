package de.jklein.pharmalinkclient.views.actorexplorer;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.jklein.pharmalinkclient.dto.ActorResponseDto;
import de.jklein.pharmalinkclient.dto.ActorFilterCriteriaDto;
import de.jklein.pharmalinkclient.service.ActorService;
import de.jklein.pharmalinkclient.service.StateService;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

@SpringComponent
@UIScope
public class ActorMasterContent extends Div {

    private final ActorService actorService;
    private final StateService stateService;
    private Grid<ActorResponseDto> grid;

    @Autowired
    public ActorMasterContent(ActorService actorService, StateService stateService) {
        this.actorService = actorService;
        this.stateService = stateService;

        addClassName("actor-master-content");
        getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        getStyle().set("padding", "var(--lumo-space-m)");
        setSizeFull();

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        add(headerLayout);

        grid = new Grid<>(ActorResponseDto.class, false);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);
        grid.setSizeFull();

        grid.addComponentColumn(actor -> {
            String bezeichnung = actor.getBezeichnung();
            if (bezeichnung == null || bezeichnung.trim().isEmpty()) {
                Span span = new Span("nicht angegeben");
                span.addClassName(LumoUtility.TextColor.SECONDARY);
                return span;
            }
            return new Span(bezeichnung);
        }).setHeader("Bezeichnung").setAutoWidth(true).setSortable(true);

        grid.addComponentColumn(actor -> {
            String email = actor.getEmail();
            if (email == null || email.trim().isEmpty()) {
                Span span = new Span("nicht angegeben");
                span.addClassName(LumoUtility.TextColor.SECONDARY);
                return span;
            }
            return new Span(email);
        }).setHeader("E-Mail").setAutoWidth(true).setSortable(true);

        grid.addColumn("role").setHeader("Rolle").setAutoWidth(true).setSortable(true);
        grid.addColumn("actorId").setHeader("Akteur ID").setAutoWidth(true).setSortable(true);

        add(grid);

        stateService.addActorFilterCriteriaListener(this::updateGridWithFilters);
        updateGridWithFilters(stateService.getCurrentActorFilterCriteria());

        grid.asSingleSelect().addValueChangeListener(event -> {
            stateService.setSelectedActor(event.getValue());
        });
    }

    public void updateGridWithFilters(Optional<ActorFilterCriteriaDto> criteriaOptional) {
        List<ActorResponseDto> fetchedActors;

        String searchTerm = "";
        if (criteriaOptional.isPresent()) {
            searchTerm = criteriaOptional.get().getSearchTerm();
        }

        fetchedActors = actorService.searchActors(null, searchTerm, null);
        stateService.setAllLoadedActors(fetchedActors);
        grid.setItems(fetchedActors);

        if (fetchedActors.isEmpty()) {
            grid.asSingleSelect().clear();
        } else {
            if (!grid.asSingleSelect().isEmpty() && !fetchedActors.contains(grid.asSingleSelect().getValue())) {
                grid.asSingleSelect().clear();
            }
        }
    }
}