package de.jklein.pharmalinkclient.views.actorexplorer;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.jklein.pharmalinkclient.dto.ActorUpdateRequestDto;
import de.jklein.pharmalinkclient.views.MainLayout;
import jakarta.annotation.security.PermitAll;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;


import de.jklein.pharmalinkclient.dto.ActorFilterCriteriaDto;
import de.jklein.pharmalinkclient.dto.ActorResponseDto;
import de.jklein.pharmalinkclient.dto.IpfsEntry;
import de.jklein.pharmalinkclient.service.StateService;
import de.jklein.pharmalinkclient.service.ActorService;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.ArrayList;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;

@PageTitle("Actor Explorer")
@Route(value = "actorExplorer", layout = MainLayout.class)
@PermitAll
public class ActorExplorerView extends VerticalLayout {

    private final ObjectProvider<ActorMasterContent> actorMasterContentProvider;
    private final ObjectProvider<ActorDetailContent> actorDetailContentProvider;
    private final StateService stateService;
    private final ActorService actorService;

    private TextField searchField;


    @Autowired
    public ActorExplorerView(ObjectProvider<ActorMasterContent> actorMasterContentProvider,
                             ObjectProvider<ActorDetailContent> actorDetailContentProvider,
                             StateService stateService,
                             ActorService actorService) {
        this.actorMasterContentProvider = actorMasterContentProvider;
        this.actorDetailContentProvider = actorDetailContentProvider;
        this.stateService = stateService;
        this.actorService = actorService;

        addClassName("actor-explorer-view");
        setSizeFull();
        setSpacing(false);
        setPadding(false);

        HorizontalLayout searchBar = new HorizontalLayout();
        searchBar.setWidthFull();
        searchBar.setPadding(true);
        searchBar.setAlignItems(FlexComponent.Alignment.CENTER);
        searchBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        searchBar.addClassName("actor-explorer-search-bar");

        searchField = new TextField();
        searchField.setPlaceholder("Akteur suchen (Bezeichnung, ID, E-Mail)...");
        searchField.setWidth("400px");
        searchField.setValueChangeMode(ValueChangeMode.ON_CHANGE);
        searchField.addValueChangeListener(event -> performActorSearch());

        MenuBar actionsMenuBar = createActorActionsMenuBar();

        searchBar.add(searchField, actionsMenuBar);
        add(searchBar);


        ActorMasterContent master = actorMasterContentProvider.getObject();
        ActorDetailContent detail = actorDetailContentProvider.getObject();

        SplitLayout splitLayout = new SplitLayout(master, detail);
        splitLayout.setSplitterPosition(50);
        splitLayout.setSizeFull();
        splitLayout.addClassName("actor-explorer-split-layout");

        add(splitLayout);
        expand(splitLayout);

        performActorSearch();
    }

    private void performActorSearch() {
        String searchTerm = searchField.getValue();
        ActorFilterCriteriaDto criteria = new ActorFilterCriteriaDto(searchTerm);
        stateService.setCurrentActorFilterCriteria(criteria);
    }

    private MenuBar createActorActionsMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_DROPDOWN_INDICATORS);

        MenuItem meineStammdaten = menuBar.addItem("Meine Stammdaten");
        SubMenu stammdatenSubMenu = meineStammdaten.getSubMenu();

        stammdatenSubMenu.addItem("Stammdaten anzeigen", event -> {
            String currentActorId = stateService.getCurrentActorId();
            if (currentActorId != null && !currentActorId.isEmpty()) {
                List<ActorResponseDto> allActors = stateService.getAllLoadedActors();

                Optional<ActorResponseDto> myActorDetailsOptional = allActors.stream()
                        .filter(actor -> actor.getActorId() != null && actor.getActorId().equals(currentActorId))
                        .findFirst();

                if (myActorDetailsOptional.isPresent()) {
                    showActorDetailsPopup(myActorDetailsOptional.get());
                } else {
                    Notification.show("Konnte Stammdaten für Akteur ID " + currentActorId + " in den lokal geladenen Daten nicht finden. Möglicherweise sind die Daten noch nicht vollständig geladen oder der Akteur existiert nicht.", 5000, Notification.Position.MIDDLE);
                }
            } else {
                Notification.show("Ihre Akteur-ID ist nicht verfügbar. Bitte melden Sie sich erneut an oder prüfen Sie das Dashboard.", 5000, Notification.Position.MIDDLE);
            }
        });

        stammdatenSubMenu.addItem("Stammdaten bearbeiten", event -> {
            String currentActorId = stateService.getCurrentActorId();
            if (currentActorId != null && !currentActorId.isEmpty()) {
                List<ActorResponseDto> allActors = stateService.getAllLoadedActors();

                Optional<ActorResponseDto> myActorDetailsOptional = allActors.stream()
                        .filter(actor -> actor.getActorId() != null && actor.getActorId().equals(currentActorId))
                        .findFirst();

                if (myActorDetailsOptional.isPresent()) {
                    showActorEditPopup(myActorDetailsOptional.get());
                } else {
                    Notification.show("Konnte Stammdaten für Akteur ID " + currentActorId + " in den lokal geladenen Daten nicht finden. Bearbeitung nicht möglich.", 5000, Notification.Position.MIDDLE);
                }
            } else {
                Notification.show("Ihre Akteur-ID ist nicht verfügbar. Bearbeitung nicht möglich.", 5000, Notification.Position.MIDDLE);
            }
        });

        return menuBar;
    }

    private void showActorDetailsPopup(ActorResponseDto actor) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Meine Stammdaten: " + actor.getBezeichnung());

        VerticalLayout dialogContent = new VerticalLayout();
        dialogContent.setSpacing(false);
        dialogContent.setPadding(false);

        FormLayout detailsForm = new FormLayout();
        detailsForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("400px", 2));
        detailsForm.add(createReadOnlyTextField("Akteur ID", actor.getActorId()));
        detailsForm.add(createReadOnlyTextField("Bezeichnung", actor.getBezeichnung()));
        detailsForm.add(createReadOnlyTextField("Rolle", actor.getRole()));
        detailsForm.add(createReadOnlyTextField("E-Mail", actor.getEmail()));
        dialogContent.add(detailsForm);

        if (actor.getIpfsData() != null && !actor.getIpfsData().isEmpty()) {
            dialogContent.add(new H4("Zusätzliche IPFS Daten:"));
            Grid<Map.Entry<String, Object>> ipfsGrid = new Grid<>();
            ipfsGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
            ipfsGrid.addColumn(Map.Entry::getKey).setHeader("Key").setAutoWidth(true);
            ipfsGrid.addColumn(Map.Entry::getValue).setHeader("Value").setAutoWidth(true);
            ipfsGrid.setItems(actor.getIpfsData().entrySet());
            ipfsGrid.setHeight("200px");
            dialogContent.add(ipfsGrid);
        } else {
            dialogContent.add(new Span("Keine zusätzlichen IPFS Daten vorhanden."));
        }

        dialog.add(dialogContent);

        Button closeButton = new Button("Schließen", event -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialog.getFooter().add(closeButton);

        dialog.open();
    }

    private void showActorEditPopup(ActorResponseDto actorToEdit) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Stammdaten bearbeiten: " + actorToEdit.getBezeichnung());
        dialog.setWidth("800px");
        dialog.setHeight("auto");

        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(55);

        VerticalLayout leftSide = new VerticalLayout(new H4("Allgemeine Daten"));
        leftSide.setSpacing(true);
        leftSide.setPadding(true);

        VerticalLayout rightSide = new VerticalLayout(new H4("Zusätzliche IPFS Daten"));
        rightSide.setSpacing(true);
        rightSide.setPadding(true);

        Binder<ActorResponseDto> binder = new Binder<>(ActorResponseDto.class);
        FormLayout detailsForm = new FormLayout();
        detailsForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        TextField actorIdField = createReadOnlyTextField("Akteur ID", actorToEdit.getActorId());
        TextField bezeichnungField = new TextField("Bezeichnung");
        TextField roleField = createReadOnlyTextField("Rolle", actorToEdit.getRole());
        TextField emailField = new TextField("E-Mail");

        detailsForm.add(actorIdField, roleField, bezeichnungField, emailField);
        leftSide.add(detailsForm);

        binder.forField(bezeichnungField).asRequired("Bezeichnung darf nicht leer sein").bind(ActorResponseDto::getBezeichnung, ActorResponseDto::setBezeichnung);
        binder.forField(emailField).asRequired("E-Mail ist erforderlich").bind(ActorResponseDto::getEmail, ActorResponseDto::setEmail);
        binder.readBean(actorToEdit);

        Grid<IpfsEntry> ipfsEditGrid = new Grid<>(IpfsEntry.class, false);
        ipfsEditGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
        ipfsEditGrid.setHeight("200px");

        List<IpfsEntry> editableIpfsData = new ArrayList<>();
        if (actorToEdit.getIpfsData() != null) {
            actorToEdit.getIpfsData().forEach((key, value) -> editableIpfsData.add(new IpfsEntry(key, value != null ? value.toString() : "")));
        }
        ListDataProvider<IpfsEntry> ipfsDataProvider = new ListDataProvider<>(editableIpfsData);
        ipfsEditGrid.setDataProvider(ipfsDataProvider);

        ipfsEditGrid.addColumn(IpfsEntry::getKey).setHeader("Key Bezeichnung").setSortable(true);
        ipfsEditGrid.addColumn(IpfsEntry::getValue).setHeader("Key Value").setSortable(true);

        GridContextMenu<IpfsEntry> contextMenu = ipfsEditGrid.addContextMenu();
        contextMenu.addItem("Zeile bearbeiten", event -> event.getItem().ifPresent(item -> showIpfsEntryRowEditDialog(item, ipfsDataProvider)));

        Button addRowButton = new Button("Zeile hinzufügen", VaadinIcon.PLUS.create());
        addRowButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        addRowButton.addClickListener(e -> showIpfsEntryRowEditDialog(new IpfsEntry("", ""), ipfsDataProvider));

        Button removeRowButton = new Button("Zeile entfernen", VaadinIcon.TRASH.create());
        removeRowButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
        removeRowButton.setVisible(false);

        ipfsEditGrid.asSingleSelect().addValueChangeListener(event -> {
            removeRowButton.setVisible(event.getValue() != null);
        });

        removeRowButton.addClickListener(e -> {
            IpfsEntry selectedEntry = ipfsEditGrid.asSingleSelect().getValue();
            if (selectedEntry != null) {
                editableIpfsData.remove(selectedEntry);
                ipfsDataProvider.refreshAll();
                Notification.show("Zeile entfernt.", 2000, Notification.Position.BOTTOM_START);
            }
        });

        HorizontalLayout buttonLayout = new HorizontalLayout(addRowButton, removeRowButton);
        rightSide.add(ipfsEditGrid, buttonLayout);

        splitLayout.addToPrimary(leftSide);
        splitLayout.addToSecondary(rightSide);
        dialog.add(splitLayout);

        Button saveButton = new Button("Speichern");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> {
            try {
                binder.writeBean(actorToEdit);

                ActorUpdateRequestDto updateRequest = new ActorUpdateRequestDto();
                updateRequest.setName(actorToEdit.getBezeichnung());
                updateRequest.setEmail(actorToEdit.getEmail()); // E-Mail wird jetzt gesetzt
                Map<String, Object> updatedIpfsData = editableIpfsData.stream()
                        .filter(entry -> entry.getKey() != null && !entry.getKey().isEmpty())
                        .collect(Collectors.toMap(IpfsEntry::getKey, IpfsEntry::getValue));
                updateRequest.setIpfsData(updatedIpfsData);

                boolean success = actorService.updateActor(actorToEdit.getActorId(), updateRequest);

                if (success) {
                    Notification.show("Stammdaten erfolgreich gespeichert.", 3000, Notification.Position.MIDDLE);
                    dialog.close();
                    performActorSearch();
                } else {
                    Notification.show("Fehler beim Speichern der Stammdaten im Backend.", 5000, Notification.Position.MIDDLE);
                }

            } catch (ValidationException ex) {
                Notification.show("Fehler beim Speichern: Bitte alle Pflichtfelder ausfüllen.", 3000, Notification.Position.MIDDLE);
            }
        });


        Button cancelButton = new Button("Abbrechen", event -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialog.getFooter().add(cancelButton, saveButton);

        dialog.open();
    }

    private void showIpfsEntryRowEditDialog(IpfsEntry entry, ListDataProvider<IpfsEntry> dataProvider) {
        Dialog rowEditDialog = new Dialog();
        boolean isNewEntry = entry.getKey() == null || entry.getKey().isEmpty();
        rowEditDialog.setHeaderTitle(isNewEntry ? "Neue IPFS Zeile" : "IPFS Zeile bearbeiten");

        Binder<IpfsEntry> rowBinder = new Binder<>(IpfsEntry.class);
        TextField keyField = new TextField("Bezeichnung");
        TextField valueField = new TextField("Eintrag");
        keyField.setWidthFull();
        valueField.setWidthFull();

        rowBinder.forField(keyField).asRequired("Key darf nicht leer sein").bind(IpfsEntry::getKey, IpfsEntry::setKey);
        rowBinder.forField(valueField).bind(IpfsEntry::getValue, IpfsEntry::setValue);
        rowBinder.setBean(entry);

        rowEditDialog.add(new VerticalLayout(keyField, valueField));

        Button saveRowButton = new Button("Speichern");
        saveRowButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveRowButton.addClickListener(e -> {
            try {
                rowBinder.writeBean(entry);
                if (isNewEntry && !((List<IpfsEntry>) dataProvider.getItems()).contains(entry)) {
                    ((List<IpfsEntry>) dataProvider.getItems()).add(entry);
                }
                dataProvider.refreshAll();
                rowEditDialog.close();
            } catch (ValidationException ex) {
                Notification.show("Fehler: " + ex.getValidationErrors().get(0).getErrorMessage(), 3000, Notification.Position.BOTTOM_START);
            }
        });

        Button cancelRowButton = new Button("Abbrechen", e -> rowEditDialog.close());
        rowEditDialog.getFooter().add(cancelRowButton, saveRowButton);
        rowEditDialog.open();
    }

    private TextField createReadOnlyTextField(String label, String value) {
        TextField textField = new TextField(label);
        textField.setReadOnly(true);
        textField.setValue(value != null ? value : "");
        textField.setWidthFull();
        return textField;
    }
}