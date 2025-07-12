package de.jklein.pharmalinkclient.views.actorexplorer;

import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.splitlayout.SplitLayout;

import de.jklein.pharmalinkclient.dto.ActorResponseDto;
import de.jklein.pharmalinkclient.dto.MedikamentResponseDto;
import de.jklein.pharmalinkclient.service.StateService;
import de.jklein.pharmalinkclient.service.MedikamentService;
import de.jklein.pharmalinkclient.service.ActorService;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.dom.DomEventListener;


@SpringComponent
@UIScope
public class ActorDetailContent extends SplitLayout {

    private final StateService stateService;
    private final MedikamentService medikamentService;
    private final ActorService actorService;

    private H3 mainTitle;
    private FormLayout detailsLayout;
    private VerticalLayout ipfsDataSection;
    private Grid<Map.Entry<String, Object>> ipfsDataGrid;

    private VerticalLayout actorMedicationsSection;
    private Grid<MedikamentResponseDto> actorMedicationsGrid;

    private TextField actorIdField;
    private TextField bezeichnungField;
    private TextField roleField;
    private TextField emailField;


    @Autowired
    public ActorDetailContent(StateService stateService, MedikamentService medikamentService, ActorService actorService) {
        this.stateService = stateService;
        this.medikamentService = medikamentService;
        this.actorService = actorService;
        addClassName("actor-detail-content");
        setSizeFull();

        this.setOrientation(SplitLayout.Orientation.VERTICAL);
        this.setSplitterPosition(50);

        VerticalLayout actorInfoSection = new VerticalLayout();
        actorInfoSection.setPadding(false);
        actorInfoSection.setSpacing(false);
        actorInfoSection.setSizeFull();

        mainTitle = new H3("Akteur Details");
        actorInfoSection.add(mainTitle);

        detailsLayout = new FormLayout();
        detailsLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2)
        );

        actorIdField = createReadOnlyCopyableTextField("Akteur ID");
        bezeichnungField = createReadOnlyCopyableTextField("Bezeichnung");
        roleField = createReadOnlyCopyableTextField("Rolle");
        emailField = createReadOnlyCopyableTextField("E-Mail");

        detailsLayout.add(actorIdField, bezeichnungField, roleField, emailField);
        actorInfoSection.add(detailsLayout);

        ipfsDataSection = new VerticalLayout();
        ipfsDataSection.setPadding(false);
        ipfsDataSection.setSpacing(false);
        ipfsDataSection.setSizeFull();

        H4 ipfsDataTitle = new H4("IPFS Daten");
        ipfsDataSection.add(ipfsDataTitle);

        ipfsDataGrid = new Grid<>();
        ipfsDataGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
        ipfsDataGrid.addColumn(Map.Entry::getKey).setHeader("Key").setAutoWidth(true);
        ipfsDataGrid.addColumn(Map.Entry::getValue).setHeader("Value").setAutoWidth(true);
        ipfsDataGrid.setSizeFull();

        ipfsDataSection.add(ipfsDataGrid);
        ipfsDataSection.setVisible(false);

        SplitLayout topSplit = new SplitLayout(actorInfoSection, ipfsDataSection);
        topSplit.setOrientation(SplitLayout.Orientation.HORIZONTAL);
        topSplit.setSplitterPosition(60);
        topSplit.setSizeFull();

        actorMedicationsSection = new VerticalLayout();
        actorMedicationsSection.getStyle().set("border-top", "1px solid var(--lumo-contrast-10pct)");

        H4 medicationsTitle = new H4("Zugeordnete Medikamente");
        actorMedicationsSection.add(medicationsTitle);

        actorMedicationsGrid = new Grid<>(MedikamentResponseDto.class, false);
        actorMedicationsGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
        actorMedicationsGrid.setSizeFull();

        actorMedicationsGrid.addColumn(MedikamentResponseDto::getStatus)
                .setHeader("Status").setAutoWidth(true);
        actorMedicationsGrid.addColumn("bezeichnung").setHeader("Bezeichnung").setAutoWidth(true);
        actorMedicationsGrid.addColumn("medId").setHeader("Medikament ID").setAutoWidth(true);

        actorMedicationsGrid.asSingleSelect().addValueChangeListener(event -> {
            MedikamentResponseDto selectedMedikament = event.getValue();
            if (selectedMedikament != null) {
                UI.getCurrent().navigate(de.jklein.pharmalinkclient.views.medikamente.MedikamenteView.class);
                stateService.setNavigateToMedikamentId(selectedMedikament.getMedId());
                actorMedicationsGrid.asSingleSelect().clear();
            }
        });

        actorMedicationsSection.add(actorMedicationsGrid);
        actorMedicationsSection.setVisible(false);

        this.addToPrimary(topSplit);
        this.addToSecondary(actorMedicationsSection);

        stateService.addSelectedActorListener(this::updateDetails);

        this.setVisible(false);
    }

    private TextField createReadOnlyCopyableTextField(String label) {
        TextField textField = new TextField(label);
        textField.setReadOnly(true);
        textField.setWidthFull();
        textField.addClassNames("detail-field");

        textField.getElement().addEventListener("click", (DomEventListener) event -> {
            String valueToCopy = textField.getValue();
            if (valueToCopy != null && !valueToCopy.isEmpty()) {
                UI.getCurrent().getPage().executeJs(
                        "navigator.clipboard.writeText($0).then(function() { console.log('Text copied to clipboard'); }).catch(function(err) { console.error('Failed to copy text: ', err); });",
                        valueToCopy
                );
                Notification.show("'" + valueToCopy + "' in Zwischenablage kopiert.", 3000, Notification.Position.MIDDLE);
            }
        });
        return textField;
    }

    public void updateDetails(ActorResponseDto actor) {
        if (actor != null) {
            this.setVisible(true);
            mainTitle.setText("Details für: " + actor.getBezeichnung());
            actorIdField.setValue(actor.getActorId() != null ? actor.getActorId() : "");
            bezeichnungField.setValue(actor.getBezeichnung() != null ? actor.getBezeichnung() : "");
            roleField.setValue(actor.getRole() != null ? actor.getRole() : "");
            emailField.setValue(actor.getEmail() != null ? actor.getEmail() : "");

            if (actor.getIpfsData() != null && !actor.getIpfsData().isEmpty()) {
                List<Map.Entry<String, Object>> ipfsEntries = actor.getIpfsData().entrySet().stream()
                        .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList());
                ipfsDataGrid.setItems(ipfsEntries);
                ipfsDataSection.setVisible(true);
            } else {
                ipfsDataGrid.setItems(List.of());
                ipfsDataSection.setVisible(false);
            }

            List<MedikamentResponseDto> associatedMedikamente = List.of();
            if (actor.getActorId() != null && !actor.getActorId().isEmpty()) {
                associatedMedikamente = actorService.getMedicationsByHersteller(actor.getActorId());
            }

            actorMedicationsGrid.setItems(associatedMedikamente);
            actorMedicationsSection.setVisible(!associatedMedikamente.isEmpty());

        } else {
            this.setVisible(false);
            mainTitle.setText("Akteur Details");
            actorIdField.setValue("");
            bezeichnungField.setValue("");
            roleField.setValue("");
            emailField.setValue("");
            ipfsDataGrid.setItems(List.of());
            ipfsDataSection.setVisible(false);
            actorMedicationsGrid.setItems(List.of());
            actorMedicationsSection.setVisible(false);
        }
    }
}