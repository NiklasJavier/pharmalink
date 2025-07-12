package de.jklein.pharmalinkclient.views.medikamente;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.data.provider.ListDataProvider;

import de.jklein.pharmalinkclient.dto.IpfsEntry;
import de.jklein.pharmalinkclient.dto.MedikamentResponseDto;
import de.jklein.pharmalinkclient.service.StateService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SpringComponent
@UIScope
public class MedikamentIpfsDataContent extends Div {

    private final StateService stateService;
    private final Grid<IpfsEntry> ipfsDataGrid;
    private final List<IpfsEntry> currentIpfsEntries = new ArrayList<>();
    private final ListDataProvider<IpfsEntry> ipfsDataProvider;

    public MedikamentIpfsDataContent(StateService stateService) {
        this.stateService = stateService;
        addClassName("medikament-ipfs-data-content");
        getStyle().set("background-color", "var(--lumo-base-color)");
        getStyle().set("padding", "var(--lumo-space-m)");
        getStyle().set("flex-grow", "1");

        ipfsDataGrid = new Grid<>(IpfsEntry.class, false);
        ipfsDataGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);
        ipfsDataGrid.setHeight("100%");
        ipfsDataGrid.setWidthFull();

        ipfsDataGrid.addColumn(IpfsEntry::getKey).setHeader("Key").setAutoWidth(true);
        ipfsDataGrid.addColumn(IpfsEntry::getValue).setHeader("Value").setAutoWidth(true);

        ipfsDataProvider = new ListDataProvider<>(currentIpfsEntries);
        ipfsDataGrid.setDataProvider(ipfsDataProvider);

        add(ipfsDataGrid);

        stateService.addSelectedMedikamentListener(this::updateIpfsDataGrid);
    }

    private void updateIpfsDataGrid(Optional<MedikamentResponseDto> selectedMedikamentOptional) {
        currentIpfsEntries.clear();

        if (selectedMedikamentOptional.isPresent()) {
            MedikamentResponseDto medikament = selectedMedikamentOptional.get();
            if (medikament.getIpfsData() != null && !medikament.getIpfsData().isEmpty()) {
                List<IpfsEntry> entries = medikament.getIpfsData().entrySet().stream()
                        .map(entry -> new IpfsEntry(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : ""))
                        .collect(Collectors.toList());
                currentIpfsEntries.addAll(entries);
            }
        }
        ipfsDataProvider.refreshAll();
    }
}