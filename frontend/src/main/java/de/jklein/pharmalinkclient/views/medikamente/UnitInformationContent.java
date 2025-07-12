package de.jklein.pharmalinkclient.views.medikamente;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import de.jklein.pharmalinkclient.dto.MedikamentResponseDto;
import de.jklein.pharmalinkclient.service.StateService;
import de.jklein.pharmalinkclient.service.UnitService;
import de.jklein.pharmalinkclient.dto.ChargeSummaryDto;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.stream.Collectors;

@SpringComponent
@UIScope
public class UnitInformationContent extends Div {

    private final StateService stateService;
    private final UnitService unitService;
    private final Grid<ChargeSummaryDto> unitGrid;

    public UnitInformationContent(StateService stateService, UnitService unitService) {
        this.stateService = stateService;
        this.unitService = unitService;
        addClassName("unit-information-content");
        getStyle().set("background-color", "var(--lumo-base-color)");
        getStyle().set("padding", "var(--lumo-space-m)");
        getStyle().set("flex-grow", "1");

        add(new H4("Chargeninformationen"));

        unitGrid = new Grid<>(ChargeSummaryDto.class, false);
        unitGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);
        unitGrid.setSizeFull();

        unitGrid.addColumn("chargeBezeichnung")
                .setHeader("Charge")
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setTextAlign(ColumnTextAlign.START);

        unitGrid.addColumn("anzahl")
                .setHeader("Anzahl")
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.START);

        add(unitGrid);

        stateService.addSelectedMedikamentListener(this::updateUnitGrid);
    }

    private void updateUnitGrid(Optional<MedikamentResponseDto> selectedMedikamentOptional) {
        if (selectedMedikamentOptional.isPresent()) {
            MedikamentResponseDto medikament = selectedMedikamentOptional.get();
            String medId = medikament.getMedId();

            if (medId != null && !medId.isEmpty()) {
                Map<String, Integer> chargeCountsMap = unitService.getChargeCountsForMedication(medId);

                List<ChargeSummaryDto> chargeSummaries = chargeCountsMap.entrySet().stream()
                        .map(entry -> new ChargeSummaryDto(entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList());

                unitGrid.setItems(chargeSummaries);
            } else {
                unitGrid.setItems(Collections.emptyList());
            }
        } else {
            unitGrid.setItems(Collections.emptyList());
        }
    }
}