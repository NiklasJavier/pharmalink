package de.jklein.pharmalinkclient.views.medikamente;

import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.jklein.pharmalinkclient.dto.*;
import de.jklein.pharmalinkclient.views.MainLayout;
import jakarta.annotation.security.PermitAll;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.NumberField;


import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

import de.jklein.pharmalinkclient.service.StateService;
import de.jklein.pharmalinkclient.security.UserSession;
import de.jklein.pharmalinkclient.service.MedikamentService;
import de.jklein.pharmalinkclient.service.UnitService;
import com.vaadin.flow.component.Component;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import com.vaadin.flow.component.UI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@PageTitle("Medikamente")
@Route(value = "medikamente", layout = MainLayout.class)
@PermitAll
public class MedikamenteView extends VerticalLayout {

    private TextField searchField;
    private HorizontalLayout operationBar;
    private Select<String> sortBySelect;
    private Div mainContentContainer;
    private Tabs tabs;
    private Tab alleTab;
    private Tab eigeneTab;
    private MenuBar menuBar;

    private final ObjectProvider<MasterContent> masterContentProvider;
    private final ObjectProvider<DetailContent> detailContentProvider;
    private final ObjectProvider<UnitInformationContent> unitInformationContentProvider;
    private final ObjectProvider<HerstellerDetailContent> herstellerDetailContentProvider;
    private final ObjectProvider<MedikamentIpfsDataContent> medikamentIpfsDataContentProvider;

    private SplitLayout masterDetailSplitLayout;
    private DetailContent detailContentInstance;


    private final StateService stateService;
    private final UserSession userSession;
    private final MedikamentService medikamentService;
    private final UnitService unitService;


    @Autowired
    public MedikamenteView(ObjectProvider<MasterContent> masterContentProvider,
                           ObjectProvider<DetailContent> detailContentProvider,
                           ObjectProvider<UnitInformationContent> unitInformationContentProvider,
                           ObjectProvider<HerstellerDetailContent> herstellerDetailContentProvider,
                           ObjectProvider<MedikamentIpfsDataContent> medikamentIpfsDataContentProvider,
                           StateService stateService,
                           UserSession userSession,
                           MedikamentService medikamentService,
                           UnitService unitService) {
        this.masterContentProvider = masterContentProvider;
        this.detailContentProvider = detailContentProvider;
        this.unitInformationContentProvider = unitInformationContentProvider;
        this.herstellerDetailContentProvider = herstellerDetailContentProvider;
        this.medikamentIpfsDataContentProvider = medikamentIpfsDataContentProvider;
        this.stateService = stateService;
        this.userSession = userSession;
        this.medikamentService = medikamentService;
        this.unitService = unitService;

        addClassName("pharmalink-view");
        setSizeFull();
        setSpacing(false);
        setPadding(false);

        alleTab = new Tab("Alle");
        eigeneTab = new Tab("Eigene");

        List<Tab> visibleTabs = new ArrayList<>();
        visibleTabs.add(alleTab);

        String currentActorId = stateService.getCurrentActorId();
        String userRole = null;
        if (currentActorId != null && currentActorId.contains("-")) {
            userRole = currentActorId.substring(0, currentActorId.indexOf("-")).toLowerCase();
        }

        List<String> rolesWithoutEigeneTab = Arrays.asList("behoerde", "apotheke", "grosshaendler");
        if (userRole == null || !rolesWithoutEigeneTab.contains(userRole)) {
            visibleTabs.add(eigeneTab);
        }

        tabs = new Tabs(visibleTabs.toArray(new Tab[0]));
        tabs.setWidthFull();
        add(tabs);

        tabs.addSelectedChangeListener(event -> {
            boolean isEigeneSelected = event.getSelectedTab() == eigeneTab;
            if (isEigeneSelected) {
                switchMainDetailLayout(true);
            } else {
                switchMainDetailLayout(false);
            }
            performSearch();
            updateMenuBarVisibility();
        });

        operationBar = new HorizontalLayout();
        operationBar.setWidthFull();
        operationBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        operationBar.setAlignItems(FlexComponent.Alignment.CENTER);
        operationBar.setPadding(true);
        operationBar.addClassName("pharmalink-operation-bar");

        HorizontalLayout searchAndSortArea = new HorizontalLayout();
        searchAndSortArea.setSpacing(true);
        searchAndSortArea.setAlignItems(FlexComponent.Alignment.CENTER);
        searchAndSortArea.addClassName("pharmalink-search-sort-area");

        sortBySelect = new Select<>();
        sortBySelect.setItems("Ohne Filter", "angelegt", "freigegeben", "abgelehnt");
        sortBySelect.setValue("Ohne Filter");
        sortBySelect.addValueChangeListener(event -> {
            performSearch();
        });

        searchField = new TextField();
        searchField.setPlaceholder("Medikament suchen...");
        searchField.setWidth("300px");
        searchField.setValueChangeMode(ValueChangeMode.ON_CHANGE);
        searchField.addValueChangeListener(event -> {
            performSearch();
        });

        HorizontalLayout searchInputLayout = new HorizontalLayout();
        searchInputLayout.setSpacing(false);
        searchInputLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        searchInputLayout.addClassName("search-input-layout");
        searchInputLayout.add(searchField);

        searchAndSortArea.add(sortBySelect, searchInputLayout);
        operationBar.add(searchAndSortArea);

        menuBar = createMenuBar();
        operationBar.add(menuBar);
        updateMenuBarVisibility();

        add(operationBar);

        mainContentContainer = new Div();
        mainContentContainer.addClassName("pharmalink-main-content-container");
        mainContentContainer.setSizeFull();
        add(mainContentContainer);
        expand(mainContentContainer);

        masterDetailSplitLayout = new SplitLayout();
        masterDetailSplitLayout.setOrientation(SplitLayout.Orientation.VERTICAL);
        masterDetailSplitLayout.setSizeFull();
        masterDetailSplitLayout.addToPrimary(masterContentProvider.getObject());

        detailContentInstance = detailContentProvider.getObject();
        masterDetailSplitLayout.addToSecondary(detailContentInstance);
        mainContentContainer.add(masterDetailSplitLayout);

        detailContentInstance.setVisible(false);
        masterDetailSplitLayout.setSplitterPosition(100);

        switchMainDetailLayout(false);
        performSearch();

        stateService.addNavigateToMedikamentListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String medId = (String) evt.getNewValue();
                if (medId != null && !medId.isEmpty()) {
                    UI.getCurrent().navigate(MedikamenteView.class);
                    tabs.setSelectedTab(alleTab);
                    searchField.setValue(medId);
                    stateService.setNavigateToMedikamentId(null);
                }
            }
        });

        stateService.addSelectedMedikamentListener(selectedMedikamentOptional -> {
            boolean isMedikamentSelected = selectedMedikamentOptional.isPresent();
            detailContentInstance.setVisible(isMedikamentSelected);
            if (isMedikamentSelected) {
                masterDetailSplitLayout.setSplitterPosition(45);
            } else {
                masterDetailSplitLayout.setSplitterPosition(100);
            }
            updateMenuBarVisibility();
        });


        masterContentProvider.getObject().addDoubleClickListener(this::showMedikamentDetailsPopup);
    }

    private void switchMainDetailLayout(boolean isEigeneSelected) {
        if (isEigeneSelected) {
            masterDetailSplitLayout.setOrientation(SplitLayout.Orientation.HORIZONTAL);
            masterDetailSplitLayout.setSplitterPosition(30);
        } else {
            masterDetailSplitLayout.setOrientation(SplitLayout.Orientation.VERTICAL);
        }
        switchRightDetailPanelContent(isEigeneSelected);
    }

    private void switchRightDetailPanelContent(boolean isEigeneSelected) {
        DetailContent detailPanel = detailContentInstance;

        SplitLayout innerRightPanelSplitLayout = new SplitLayout();
        innerRightPanelSplitLayout.setOrientation(SplitLayout.Orientation.HORIZONTAL);
        innerRightPanelSplitLayout.setSizeFull();

        innerRightPanelSplitLayout.addToPrimary(unitInformationContentProvider.getObject());

        Component dynamicRightContent;

        if (isEigeneSelected) {
            dynamicRightContent = medikamentIpfsDataContentProvider.getObject();
            innerRightPanelSplitLayout.setSplitterPosition(50);
        } else {
            dynamicRightContent = herstellerDetailContentProvider.getObject();
            innerRightPanelSplitLayout.setSplitterPosition(30);
        }

        innerRightPanelSplitLayout.addToSecondary(dynamicRightContent);

        detailPanel.setContent(innerRightPanelSplitLayout);
    }

    private void performSearch() {
        String searchTerm = searchField.getValue();
        String selectedStatus = sortBySelect.getValue();
        boolean filterByCurrentActor = tabs.getSelectedTab() == eigeneTab;

        MedikamentFilterCriteriaDto criteria = new MedikamentFilterCriteriaDto(searchTerm, selectedStatus, filterByCurrentActor);
        stateService.setCurrentMedikamentFilterCriteria(criteria);
    }

    private MenuBar createMenuBar() {
        MenuBar newMenuBar = new MenuBar();
        newMenuBar.addThemeVariants(MenuBarVariant.LUMO_DROPDOWN_INDICATORS);

        String currentActorId = stateService.getCurrentActorId();
        String userRole = null;
        if (currentActorId != null && currentActorId.contains("-")) {
            userRole = currentActorId.substring(0, currentActorId.indexOf("-")).toLowerCase();
        }

        MenuItem aktionen = newMenuBar.addItem("Aktionen");
        SubMenu aktionenSubMenu = aktionen.getSubMenu();

        aktionenSubMenu.addItem("Anzeigen", event -> {
            Optional<MedikamentResponseDto> selectedMedikament = stateService.getSelectedMedikament();
            if (selectedMedikament.isPresent()) {
                showMedikamentDetailsPopup(selectedMedikament.get());
            } else {
                Notification notification = new Notification(
                        "Bitte wählen Sie zuerst ein Medikament aus der Liste aus.",
                        3000,
                        Position.MIDDLE
                );
                notification.addThemeVariants(NotificationVariant.LUMO_WARNING);
                notification.open();
            }
        });

        if (userRole != null) {
            switch (userRole) {
                case "hersteller":
                    aktionenSubMenu.addItem("Medikament anlegen", event -> showCreateMedikamentPopup());
                    aktionenSubMenu.addItem("Medikament bearbeiten", event -> {
                        Optional<MedikamentResponseDto> selectedMedikament = stateService.getSelectedMedikament();
                        if (selectedMedikament.isPresent()) {
                            showEditMedikamentPopup(selectedMedikament.get());
                        } else {
                            Notification notification = new Notification(
                                    "Bitte wählen Sie zuerst ein Medikament zum Bearbeiten aus der Liste aus.",
                                    3000,
                                    Position.MIDDLE
                            );
                            notification.addThemeVariants(NotificationVariant.LUMO_WARNING);
                            notification.open();
                        }
                    });
                    aktionenSubMenu.addItem("Medikament löschen", event -> {
                        Optional<MedikamentResponseDto> selectedMedikament = stateService.getSelectedMedikament();
                        if (selectedMedikament.isPresent()) {
                            showDeleteMedikamentConfirmation(selectedMedikament.get());
                        } else {
                            Notification.show("Bitte wählen Sie zuerst ein Medikament zum Löschen aus.", 3000, Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_WARNING);
                        }
                    });

                    Optional<MedikamentResponseDto> selectedMedikamentForCharge = stateService.getSelectedMedikament();
                    if (selectedMedikamentForCharge.isPresent() &&
                            selectedMedikamentForCharge.get().getStatus() != null &&
                            selectedMedikamentForCharge.get().getStatus().equalsIgnoreCase("freigegeben")) {
                        aktionenSubMenu.addItem("Charge anlegen", event -> {
                            Optional<MedikamentResponseDto> currentSelectedMed = stateService.getSelectedMedikament();
                            if (currentSelectedMed.isPresent() && currentSelectedMed.get().getStatus().equalsIgnoreCase("freigegeben")) {
                                showCreateChargePopup(currentSelectedMed.get());
                            } else {
                                Notification.show("Bitte wählen Sie ein freigegebenes Medikament aus, für das Sie eine Charge anlegen möchten.", 3000, Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_WARNING);
                            }
                        });
                    }
                    break;
                case "behoerde":
                    if (tabs.getSelectedTab() == alleTab) {
                        aktionenSubMenu.addItem("Status vergeben", event -> showSetStatusPopup());
                    }
                    break;
                default:
                    break;
            }
        }
        return newMenuBar;
    }

    private void updateMenuBarVisibility() {
        operationBar.remove(menuBar);
        menuBar = createMenuBar();
        operationBar.add(menuBar);
    }

    private void showSetStatusPopup() {
        Optional<MedikamentResponseDto> selectedMedikamentOptional = stateService.getSelectedMedikament();
        if (selectedMedikamentOptional.isEmpty()) {
            Notification.show("Bitte wählen Sie zuerst ein Medikament aus der Liste aus, um den Status zu vergeben.", 3000, Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        MedikamentResponseDto selectedMedikament = selectedMedikamentOptional.get();

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Status für Medikament: " + selectedMedikament.getBezeichnung() + " festlegen");
        dialog.setWidth("400px");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(true);

        Select<String> statusSelect = new Select<>();
        statusSelect.setLabel("Neuer Status");
        statusSelect.setItems("freigegeben", "abgelehnt");
        statusSelect.setValue(selectedMedikament.getStatus());
        dialogLayout.add(statusSelect);

        Button saveButton = new Button("Status speichern");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(event -> {
            String newStatus = statusSelect.getValue();
            if (newStatus == null || newStatus.isEmpty()) {
                Notification.show("Bitte wählen Sie einen Status aus.", 3000, Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }

            UpdateMedicationStatusRequestDto request = new UpdateMedicationStatusRequestDto(newStatus);
            boolean success = medikamentService.approveMedication(selectedMedikament.getMedId(), request);

            if (success) {
                Notification.show("Status von Medikament '" + selectedMedikament.getBezeichnung() + "' erfolgreich auf '" + newStatus + "' gesetzt.", 5000, Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                performSearch();
            } else {
                Notification.show("Fehler beim Setzen des Status für Medikament '" + selectedMedikament.getBezeichnung() + "'.", 5000, Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        Button cancelButton = new Button("Abbrechen", event -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.add(dialogLayout);
        dialog.open();
    }


    private void showDeleteMedikamentConfirmation(MedikamentResponseDto medikamentToDelete) {
        Dialog confirmationDialog = new Dialog();
        confirmationDialog.setHeaderTitle("Medikament löschen: " + medikamentToDelete.getBezeichnung() + "?");

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(true);

        dialogLayout.add(new Div(new Span("Sie können das Medikament nur löschen, wenn darauf keine Einheiten angelegt wurden.")));
        dialogLayout.add(new Div(new Span("Möchten Sie das Medikament mit der Bezeichnung '" + medikamentToDelete.getBezeichnung() + "' wirklich löschen?")));
        dialogLayout.add(new Div(new Span("Bitte geben Sie die Bezeichnung zur Bestätigung ein:")));

        TextField confirmNameField = new TextField();
        confirmNameField.setPlaceholder("Bezeichnung des Medikaments eingeben");
        confirmNameField.setWidthFull();
        dialogLayout.add(confirmNameField);

        confirmationDialog.add(dialogLayout);
        confirmationDialog.setCloseOnEsc(true);
        confirmationDialog.setCloseOnOutsideClick(true);

        Button confirmButton = new Button("Löschen", event -> {
            if (confirmNameField.getValue().equalsIgnoreCase(medikamentToDelete.getBezeichnung())) {
                boolean success = medikamentService.deleteMedikamentIfNoUnits(medikamentToDelete.getMedId());
                if (success) {
                    Notification.show("Medikament '" + medikamentToDelete.getBezeichnung() + "' erfolgreich gelöscht.", 5000, Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    confirmationDialog.close();
                    performSearch();
                } else {
                    Notification.show("Fehler beim Löschen des Medikaments '" + medikamentToDelete.getBezeichnung() + "'. Möglicherweise sind noch Einheiten damit verbunden.", 5000, Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } else {
                Notification.show("Die eingegebene Bezeichnung stimmt nicht überein. Löschvorgang abgebrochen.", 3000, Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        Button cancelButton = new Button("Abbrechen", event -> confirmationDialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        confirmationDialog.getFooter().add(cancelButton, confirmButton);
        confirmationDialog.open();
    }

    public void showMedikamentDetailsPopup(MedikamentResponseDto medikament) {
        if (medikament == null) {
            Notification.show("Kein Medikament ausgewählt.", 3000, Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Medikamenten Details: " + medikament.getBezeichnung());
        dialog.setWidth("600px");
        dialog.setHeight("auto");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(true);
        dialogLayout.setSizeFull();

        FormLayout infoForm = new FormLayout();
        infoForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2));

        infoForm.add(createReadOnlyTextField("Medikament ID", medikament.getMedId()));
        infoForm.add(createReadOnlyTextField("Bezeichnung", medikament.getBezeichnung()));
        infoForm.add(createReadOnlyTextField("Hersteller ID", medikament.getHerstellerId()));
        infoForm.add(createReadOnlyTextField("Status", medikament.getStatus()));
        infoForm.add(createReadOnlyTextField("IPFS Link", medikament.getIpfsLink()));

        dialogLayout.add(infoForm);

        H4 ipfsHeader = new H4("Zusätzliche IPFS Daten");
        dialogLayout.add(ipfsHeader);

        Grid<IpfsEntry> ipfsDisplayGrid = new Grid<>(IpfsEntry.class, false);
        ipfsDisplayGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);
        ipfsDisplayGrid.setHeight("150px");
        ipfsDisplayGrid.setWidthFull();

        ipfsDisplayGrid.addColumn(IpfsEntry::getKey).setHeader("Key Bezeichnung").setAutoWidth(true);
        ipfsDisplayGrid.addColumn(IpfsEntry::getValue).setHeader("Key Value").setAutoWidth(true);

        List<IpfsEntry> ipfsEntries = new ArrayList<>();
        if (medikament.getIpfsData() != null && !medikament.getIpfsData().isEmpty()) {
            medikament.getIpfsData().forEach((key, value) -> ipfsEntries.add(new IpfsEntry(key, value != null ? value.toString() : "")));
        }
        ipfsDisplayGrid.setItems(ipfsEntries);
        dialogLayout.add(ipfsDisplayGrid);

        Button closeButton = new Button("Schließen", event -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialog.getFooter().add(closeButton);

        dialog.add(dialogLayout);
        dialog.open();
    }

    private void showCreateMedikamentPopup() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Neues Medikament anlegen");
        dialog.setWidth("800px");
        dialog.setHeight("auto");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(55);

        VerticalLayout leftSide = new VerticalLayout(new H4("Allgemeine Informationen"));
        leftSide.setSpacing(true);
        leftSide.setPadding(true);

        VerticalLayout rightSide = new VerticalLayout(new H4("Zusätzliche IPFS Daten"));
        rightSide.setSpacing(true);
        rightSide.setPadding(true);

        CreateMedikamentRequestDto newMedikament = new CreateMedikamentRequestDto();
        Binder<CreateMedikamentRequestDto> binder = new Binder<>(CreateMedikamentRequestDto.class);

        FormLayout detailsForm = new FormLayout();
        detailsForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        TextField bezeichnungField = new TextField("Bezeichnung");
        TextField infoblattHashField = new TextField("Infoblatt Hash (optional)");

        detailsForm.add(bezeichnungField, infoblattHashField);
        leftSide.add(detailsForm);

        binder.forField(bezeichnungField)
                .asRequired("Bezeichnung ist erforderlich")
                .bind(CreateMedikamentRequestDto::getBezeichnung, CreateMedikamentRequestDto::setBezeichnung);
        binder.forField(infoblattHashField)
                .bind(CreateMedikamentRequestDto::getInfoblattHash, CreateMedikamentRequestDto::setInfoblattHash);

        binder.setBean(newMedikament);

        Grid<IpfsEntry> ipfsEditGrid = new Grid<>(IpfsEntry.class, false);
        ipfsEditGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
        ipfsEditGrid.setHeight("200px");

        List<IpfsEntry> editableIpfsData = new ArrayList<>();
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
                Notification notification = new Notification("Zeile entfernt.", 2000, Position.BOTTOM_START);
                notification.open();
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
                binder.writeBean(newMedikament);

                Map<String, Object> ipfsDataMap = editableIpfsData.stream()
                        .filter(entry -> entry.getKey() != null && !entry.getKey().isEmpty())
                        .collect(Collectors.toMap(IpfsEntry::getKey, IpfsEntry::getValue));
                newMedikament.setIpfsData(ipfsDataMap);

                MedikamentResponseDto createdMedikament = medikamentService.createMedikament(newMedikament);

                if (createdMedikament != null) {
                    Notification notification = new Notification(
                            "Medikament '" + createdMedikament.getBezeichnung() + "' erfolgreich angelegt. ID: " + createdMedikament.getMedId(),
                            5000,
                            Position.BOTTOM_START
                    );
                    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    notification.open();

                    dialog.close();
                    performSearch();
                } else {
                    Notification notification = new Notification(
                            "Fehler beim Anlegen des Medikaments im Backend.",
                            5000,
                            Position.MIDDLE
                    );
                    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    notification.open();
                }

            } catch (ValidationException ex) {
                Notification notification = new Notification(
                        "Fehler beim Speichern: Bitte alle Pflichtfelder ausfüllen und überprüfen.",
                        3000,
                        Position.MIDDLE
                );
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                notification.open();
            } catch (Exception ex) {
                Notification notification = new Notification(
                        "Ein unerwarteter Fehler ist aufgetreten: " + ex.getMessage(),
                        5000,
                        Position.MIDDLE
                );
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                notification.open();
                ex.printStackTrace();
            }
        });


        Button cancelButton = new Button("Abbrechen", event -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialog.getFooter().add(cancelButton, saveButton);

        dialog.open();
    }

    private void showEditMedikamentPopup(MedikamentResponseDto medikamentToEdit) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Medikament bearbeiten: " + medikamentToEdit.getBezeichnung());
        dialog.setWidth("800px");
        dialog.setHeight("auto");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(55);

        VerticalLayout leftSide = new VerticalLayout(new H4("Allgemeine Informationen"));
        leftSide.setSpacing(true);
        leftSide.setPadding(true);

        VerticalLayout rightSide = new VerticalLayout(new H4("Zusätzliche IPFS Daten"));
        rightSide.setSpacing(true);
        rightSide.setPadding(true);

        UpdateMedikamentRequestDto updateRequestDto = new UpdateMedikamentRequestDto();
        updateRequestDto.setBezeichnung(medikamentToEdit.getBezeichnung());
        updateRequestDto.setIpfsData(medikamentToEdit.getIpfsData());


        Binder<UpdateMedikamentRequestDto> binder = new Binder<>(UpdateMedikamentRequestDto.class);

        FormLayout detailsForm = new FormLayout();
        detailsForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        TextField medIdField = createReadOnlyTextField("Medikament ID", medikamentToEdit.getMedId());
        TextField herstellerIdDisplayField = createReadOnlyTextField("Hersteller ID", medikamentToEdit.getHerstellerId());
        TextField statusDisplayField = createReadOnlyTextField("Status", medikamentToEdit.getStatus());

        TextField bezeichnungField = new TextField("Bezeichnung");
        TextField infoblattHashField = new TextField("Infoblatt Hash (optional)");

        leftSide.add(medIdField, herstellerIdDisplayField, statusDisplayField, detailsForm);
        detailsForm.add(bezeichnungField, infoblattHashField);

        binder.forField(bezeichnungField)
                .asRequired("Bezeichnung ist erforderlich")
                .bind(UpdateMedikamentRequestDto::getBezeichnung, UpdateMedikamentRequestDto::setBezeichnung);
        binder.forField(infoblattHashField)
                .bind(UpdateMedikamentRequestDto::getInfoblattHash, UpdateMedikamentRequestDto::setInfoblattHash);

        binder.setBean(updateRequestDto);


        Grid<IpfsEntry> ipfsEditGrid = new Grid<>(IpfsEntry.class, false);
        ipfsEditGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
        ipfsEditGrid.setHeight("200px");

        List<IpfsEntry> editableIpfsData = new ArrayList<>();
        if (medikamentToEdit.getIpfsData() != null) {
            medikamentToEdit.getIpfsData().forEach((key, value) -> editableIpfsData.add(new IpfsEntry(key, value != null ? value.toString() : "")));
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
                Notification notification = new Notification("Zeile entfernt.", 2000, Position.BOTTOM_START);
                notification.open();
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
                binder.writeBean(updateRequestDto);

                Map<String, Object> ipfsDataMap = editableIpfsData.stream()
                        .filter(entry -> entry.getKey() != null && !entry.getKey().isEmpty())
                        .collect(Collectors.toMap(IpfsEntry::getKey, IpfsEntry::getValue));
                updateRequestDto.setIpfsData(ipfsDataMap);

                MedikamentResponseDto updatedMedikament = medikamentService.updateMedikament(medikamentToEdit.getMedId(), updateRequestDto);

                if (updatedMedikament != null) {
                    Notification notification = new Notification(
                            "Medikament '" + updatedMedikament.getBezeichnung() + "' erfolgreich aktualisiert. ID: " + updatedMedikament.getMedId(),
                            5000,
                            Position.BOTTOM_START
                    );
                    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    notification.open();

                    dialog.close();
                    performSearch();
                } else {
                    Notification notification = new Notification(
                            "Fehler beim Aktualisieren des Medikaments im Backend.",
                            5000,
                            Position.MIDDLE
                    );
                    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    notification.open();
                }

            } catch (ValidationException ex) {
                Notification notification = new Notification(
                        "Fehler beim Speichern: Bitte alle Pflichtfelder ausfüllen und überprüfen.",
                        3000,
                        Position.MIDDLE
                );
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                notification.open();
            } catch (Exception ex) {
                Notification notification = new Notification(
                        "Ein unerwarteter Fehler ist aufgetreten: " + ex.getMessage(),
                        5000,
                        Position.MIDDLE
                );
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                notification.open();
                ex.printStackTrace();
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
        TextField keyField = new TextField("Key Bezeichnung");
        TextField valueField = new TextField("Key Value Eintrag");
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
                Notification notification = new Notification(
                        "Fehler: " + ex.getValidationErrors().get(0).getErrorMessage(),
                        3000,
                        Position.BOTTOM_START
                );
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                notification.open();
            }
        });

        Button cancelButton = new Button("Abbrechen", e -> rowEditDialog.close());
        rowEditDialog.getFooter().add(cancelButton, saveRowButton);
        rowEditDialog.open();
    }

    private TextField createReadOnlyTextField(String label, String value) {
        TextField textField = new TextField(label);
        textField.setReadOnly(true);
        textField.setValue(value != null ? value : "");
        textField.setWidthFull();
        return textField;
    }

    private void showCreateChargePopup(MedikamentResponseDto selectedMedikament) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Neue Charge für Medikament: " + selectedMedikament.getBezeichnung() + " anlegen");
        dialog.setWidth("800px");
        dialog.setHeight("auto");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(55);

        VerticalLayout leftSide = new VerticalLayout(new H4("Chargen Details"));
        leftSide.setSpacing(true);
        leftSide.setPadding(true);

        VerticalLayout rightSide = new VerticalLayout(new H4("Zusätzliche IPFS Daten für Charge"));
        rightSide.setSpacing(true);
        rightSide.setPadding(true);

        CreateUnitsRequestDto newChargeRequest = new CreateUnitsRequestDto();
        String currentActorId = stateService.getCurrentActorId();

        Binder<CreateUnitsRequestDto> binder = new Binder<>(CreateUnitsRequestDto.class);

        FormLayout detailsForm = new FormLayout();
        detailsForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        TextField medIdDisplayField = createReadOnlyTextField("Medikament ID", selectedMedikament.getMedId());
        TextField ownerActorIdDisplayField = createReadOnlyTextField("Besitzer ID", currentActorId);
        TextField chargeBezeichnungField = new TextField("Chargen Bezeichnung");
        NumberField anzahlField = new NumberField("Anzahl");
        anzahlField.setStep(1.0);
        anzahlField.setMin(1.0);
        anzahlField.setValue(1.0);

        detailsForm.add(medIdDisplayField, ownerActorIdDisplayField, chargeBezeichnungField, anzahlField);
        leftSide.add(detailsForm);

        binder.forField(chargeBezeichnungField)
                .asRequired("Chargen Bezeichnung ist erforderlich")
                .bind(CreateUnitsRequestDto::getChargeBezeichnung, CreateUnitsRequestDto::setChargeBezeichnung);
        binder.forField(anzahlField)
                .asRequired("Anzahl ist erforderlich")
                .withConverter(Double::intValue, Integer::doubleValue, "Muss eine ganze Zahl sein")
                .withValidator(anzahl -> anzahl != null && anzahl > 0, "Anzahl muss positiv sein")
                .bind(CreateUnitsRequestDto::getAnzahl, CreateUnitsRequestDto::setAnzahl);

        binder.setBean(newChargeRequest);


        Grid<IpfsEntry> ipfsEditGrid = new Grid<>(IpfsEntry.class, false);
        ipfsEditGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
        ipfsEditGrid.setHeight("200px");

        List<IpfsEntry> editableIpfsData = new ArrayList<>();
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
                Notification notification = new Notification("Zeile entfernt.", 2000, Position.BOTTOM_START);
                notification.open();
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
                binder.writeBean(newChargeRequest);

                Map<String, Object> ipfsDataMap = editableIpfsData.stream()
                        .filter(entry -> entry.getKey() != null && !entry.getKey().isEmpty())
                        .collect(Collectors.toMap(IpfsEntry::getKey, IpfsEntry::getValue));
                newChargeRequest.setIpfsData(ipfsDataMap);

                boolean success = unitService.createUnitsForMedication(selectedMedikament.getMedId(), newChargeRequest);

                if (success) {
                    Notification notification = Notification.show(
                            newChargeRequest.getAnzahl() + " Einheit(en) für Charge '" + newChargeRequest.getChargeBezeichnung() + "' erfolgreich angelegt.",
                            5000,
                            Position.BOTTOM_START
                    );
                    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    notification.open();

                    dialog.close();
                    performSearch();
                } else {
                    Notification notification = Notification.show(
                            "Fehler beim Anlegen der Charge im Backend. Bitte überprüfen Sie die Backend-Logs.",
                            5000,
                            Position.MIDDLE
                    );
                    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    notification.open();
                }

            } catch (ValidationException ex) {
                Notification notification = Notification.show(
                        "Fehler beim Speichern: Bitte alle Pflichtfelder ausfüllen und überprüfen. " + ex.getValidationErrors().get(0).getErrorMessage(),
                        3000,
                        Position.MIDDLE
                );
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                notification.open();
            } catch (Exception ex) {
                Notification notification = Notification.show(
                        "Ein unerwarteter Fehler ist aufgetreten: " + ex.getMessage(),
                        5000,
                        Position.MIDDLE
                );
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                notification.open();
                ex.printStackTrace();
            }
        });


        Button cancelButton = new Button("Abbrechen", event -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialog.getFooter().add(cancelButton, saveButton);

        dialog.open();
    }
}