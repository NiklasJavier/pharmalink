package de.jklein.pharmalinkclient.views.medikamente;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import de.jklein.pharmalinkclient.dto.ActorResponseDto;
import de.jklein.pharmalinkclient.dto.MedikamentResponseDto;
import de.jklein.pharmalinkclient.service.ActorService;
import de.jklein.pharmalinkclient.service.StateService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

@SpringComponent
@UIScope
public class HerstellerDetailContent extends Div {

    private final StateService stateService;
    private final ActorService actorService;

    private TextField actorIdField;
    private TextField bezeichnungField;
    private TextField roleField;
    private TextField emailField;

    @Autowired
    public HerstellerDetailContent(StateService stateService, ActorService actorService,
                                   ObjectProvider<MedikamentIpfsDataContent> medikamentIpfsDataContentProvider) {
        this.stateService = stateService;
        this.actorService = actorService;

        addClassName("hersteller-detail-content");
        setSizeFull();

        Div manufacturerInfoWrapper = new Div();
        manufacturerInfoWrapper.getStyle().set("background-color", "var(--lumo-base-color)");
        manufacturerInfoWrapper.getStyle().set("padding", "var(--lumo-space-m)");
        manufacturerInfoWrapper.getStyle().set("flex-grow", "1");

        manufacturerInfoWrapper.add(new H4("Herstellerinformationen"));

        setupFields();
        FormLayout infoFormLayout = new FormLayout();
        infoFormLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2)
        );
        infoFormLayout.getStyle().set("padding", "0");
        infoFormLayout.setHeightFull();
        infoFormLayout.add(actorIdField, emailField, bezeichnungField, roleField);
        manufacturerInfoWrapper.add(infoFormLayout);

        add(manufacturerInfoWrapper);

        stateService.addSelectedMedikamentListener(this::updateManufacturerFields);
    }

    private void setupFields() {
        actorIdField = createReadOnlyTextField("Hersteller ID");
        bezeichnungField = createReadOnlyTextField("Bezeichnung");
        roleField = createReadOnlyTextField("Rolle");
        emailField = createReadOnlyTextField("E-Mail");
    }

    private TextField createReadOnlyTextField(String label) {
        TextField field = new TextField(label);
        field.setReadOnly(true);
        field.setWidthFull();
        field.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        return field;
    }

    private void updateManufacturerFields(Optional<MedikamentResponseDto> selectedMedikamentOptional) {
        if (selectedMedikamentOptional.isPresent()) {
            MedikamentResponseDto medikament = selectedMedikamentOptional.get();
            String herstellerId = medikament.getHerstellerId();

            if (herstellerId != null && !herstellerId.isEmpty()) {
                System.out.println("DEBUG: Suche Hersteller für ID: " + herstellerId);
                ActorResponseDto hersteller = actorService.getHerstellerById(herstellerId);
                if (hersteller != null) {
                    actorIdField.setValue(hersteller.getActorId() != null ? hersteller.getActorId() : "N/A");
                    bezeichnungField.setValue(hersteller.getBezeichnung() != null ? hersteller.getBezeichnung() : "N/A");
                    roleField.setValue(hersteller.getRole() != null ? hersteller.getRole() : "N/A");
                    emailField.setValue(hersteller.getEmail() != null ? hersteller.getEmail() : "N/A");
                    System.out.println("DEBUG: Hersteller gefunden und Felder aktualisiert für ID: " + hersteller.getActorId());
                } else {
                    clearManufacturerFields();
                    System.err.println("WARN: Hersteller mit ID " + herstellerId + " im Backend nicht gefunden. Felder wurden geleert.");
                }
            } else {
                clearManufacturerFields();
                System.out.println("WARN: Ausgewähltes Medikament hat keine Hersteller-ID. Felder wurden geleert.");
            }
        } else {
            clearManufacturerFields();
            System.out.println("INFO: Kein Medikament ausgewählt. Herstellerfelder wurden geleert.");
        }
    }

    private void clearManufacturerFields() {
        actorIdField.setValue("");
        bezeichnungField.setValue("");
        roleField.setValue("");
        emailField.setValue("");
    }
}