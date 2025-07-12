package de.jklein.pharmalinkclient.views.units;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.jklein.pharmalinkclient.dto.*;
import de.jklein.pharmalinkclient.service.ActorService;
import de.jklein.pharmalinkclient.service.MedikamentService;
import de.jklein.pharmalinkclient.service.UnitService;
import de.jklein.pharmalinkclient.views.MainLayout;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@PageTitle("Einheiten")
@Route(value = "units", layout = MainLayout.class)
@PermitAll
public class UnitsView extends VerticalLayout {

    private final UnitService unitService;
    private final MedikamentService medikamentService;
    private final ActorService actorService;

    private final VerticalLayout myUnitsPanel;
    private final VerticalLayout globalUnitsPanel;

    private final TreeGrid<TreeItem> myUnitsTreeGrid = new TreeGrid<>();
    private final TreeGrid<TreeItem> globalUnitsTreeGrid = new TreeGrid<>();
    private final ComboBox<MedikamentResponseDto> globalMedBezeichnungComboBox = new ComboBox<>("Nach Bezeichnung");
    private final ComboBox<MedikamentResponseDto> globalMedIdComboBox = new ComboBox<>("Nach Medikament ID");
    private final TextField globalUnitIdSearchField = new TextField("Nach Unit ID");
    private final Span notFoundMessage = new Span("Die Suche ergab kein Ergebnis.");

    private List<MedikamentResponseDto> allMedikamente;
    private List<UnitResponseDto> myUnits;

    private static class TreeItem {
        private final String id;
        private final String displayValue;
        private final String owner;
        private final int level;
        private final Object data;

        public TreeItem(MedikamentResponseDto med) {
            this.id = "med-" + med.getMedId();
            this.displayValue = String.format("%s (%s)", med.getBezeichnung(), med.getMedId());
            this.owner = "";
            this.level = 0;
            this.data = med;
        }

        public TreeItem(String medId, String chargeBezeichnung) {
            this.id = "charge-" + medId + "::" + chargeBezeichnung;
            this.displayValue = "Charge: " + chargeBezeichnung;
            this.owner = "";
            this.level = 1;
            this.data = this;
        }

        public TreeItem(UnitResponseDto unit) {
            this.id = "unit-" + unit.getUnitId();
            this.displayValue = unit.getUnitId();
            this.owner = unit.getCurrentOwnerActorId();
            this.level = 2;
            this.data = unit;
        }

        public String getDisplayValue() { return displayValue; }
        public String getOwner() { return owner; }
        public int getLevel() { return level; }
        public Object getData() { return data; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TreeItem treeItem = (TreeItem) o;
            return Objects.equals(id, treeItem.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    @Autowired
    public UnitsView(UnitService unitService, MedikamentService medikamentService, ActorService actorService) {
        this.unitService = unitService;
        this.medikamentService = medikamentService;
        this.actorService = actorService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        myUnitsPanel = createMyUnitsPanel();
        globalUnitsPanel = createGlobalUnitsPanel();

        myUnitsTreeGrid.addSelectionListener(event -> event.getFirstSelectedItem().ifPresent(this::showDetailsIfUnit));
        globalUnitsTreeGrid.addSelectionListener(event -> event.getFirstSelectedItem().ifPresent(this::showDetailsIfUnit));

        Tab myUnitsTab = new Tab("Meine Einheiten");
        Tab globalUnitsTab = new Tab("Globale Suche");
        Tabs tabs = new Tabs(myUnitsTab, globalUnitsTab);
        tabs.setWidthFull();

        Div contentContainer = new Div(myUnitsPanel, globalUnitsPanel);
        contentContainer.setSizeFull();

        tabs.addSelectedChangeListener(event -> {
            myUnitsPanel.setVisible(event.getSelectedTab() == myUnitsTab);
            globalUnitsPanel.setVisible(event.getSelectedTab() == globalUnitsTab);
        });

        myUnitsPanel.setVisible(true);
        globalUnitsPanel.setVisible(false);

        add(tabs, contentContainer);
        expand(contentContainer);

        loadInitialData();
    }

    private VerticalLayout createMyUnitsPanel() {
        MenuBar actionsMenu = createActionsMenu();
        HorizontalLayout header = new HorizontalLayout(actionsMenu);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        configureTreeGrid(myUnitsTreeGrid, "Medikament / Charge / Einheit-ID");
        VerticalLayout panel = new VerticalLayout(header, myUnitsTreeGrid);
        panel.setSizeFull();
        return panel;
    }

    private VerticalLayout createGlobalUnitsPanel() {
        Button searchUnitButton = new Button(VaadinIcon.SEARCH.create());
        searchUnitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        configureSelectors(globalMedBezeichnungComboBox, globalMedIdComboBox, globalUnitIdSearchField);
        HorizontalLayout selectorLayout = new HorizontalLayout(globalMedBezeichnungComboBox, globalMedIdComboBox, globalUnitIdSearchField, searchUnitButton);
        selectorLayout.setWidthFull();
        selectorLayout.setAlignItems(FlexComponent.Alignment.END);
        configureTreeGrid(globalUnitsTreeGrid, "Charge / Einheit-ID");
        notFoundMessage.setVisible(false);
        notFoundMessage.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.LARGE, LumoUtility.Margin.Top.XLARGE);
        globalMedBezeichnungComboBox.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                loadUnitsForGlobalGrid(event.getValue().getMedId());
            } else {
                resetGlobalSearch();
            }
        });
        searchUnitButton.addClickListener(e -> {
            if (!globalUnitIdSearchField.isEmpty()) displaySingleUnitInGlobalGrid(globalUnitIdSearchField.getValue());
        });
        VerticalLayout layout = new VerticalLayout(selectorLayout, globalUnitsTreeGrid, notFoundMessage);
        layout.setSizeFull();
        layout.setHorizontalComponentAlignment(Alignment.CENTER, notFoundMessage);
        return layout;
    }

    private MenuBar createActionsMenu() {
        MenuBar menuBar = new MenuBar();
        menuBar.addItem("Aktionen", item -> {})
                .getSubMenu().addItem("Einheit(en) transferieren", e -> showTransferPopup());
        menuBar.getItems().get(0).getSubMenu()
                .addItem("Temperatur hinzufügen", e -> showAddTemperaturePopup());
        return menuBar;
    }

    private void showTransferPopup() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Einheiten transferieren");
        dialog.setWidth("800px");

        ComboBox<MedikamentResponseDto> medSelector = new ComboBox<>("Medikament");
        ComboBox<String> chargeSelector = new ComboBox<>("Charge");
        Div rangeInfo = new Div();
        IntegerField fromField = new IntegerField("Von Nr.");
        IntegerField toField = new IntegerField("Bis Nr.");
        ComboBox<ActorResponseDto> ownerSelector = new ComboBox<>("Neuer Besitzer");
        Button transferButton = new Button("Transferieren", VaadinIcon.ARROW_RIGHT.create());

        medSelector.setItems(allMedikamente);
        medSelector.setItemLabelGenerator(m -> String.format("%s (%s)", m.getBezeichnung(), m.getMedId()));
        medSelector.getStyle().set("--vaadin-combo-box-overlay-width", "750px");

        chargeSelector.setEnabled(false);
        
        ownerSelector.setItems(actorService.searchActors("", "", ""));
        ownerSelector.setItemLabelGenerator(a -> String.format("%s (%s) - ID: %s", a.getBezeichnung(), a.getRole(), a.getActorId()));
        ownerSelector.getStyle().set("--vaadin-combo-box-overlay-width", "750px");

        medSelector.addValueChangeListener(e -> {
            rangeInfo.removeAll();
            if (e.getValue() == null) {
                chargeSelector.clear();
                chargeSelector.setEnabled(false);
                return;
            }
            List<String> charges = myUnits.stream()
                    .filter(u -> u.getMedId().equals(e.getValue().getMedId()))
                    .map(UnitResponseDto::getChargeBezeichnung).distinct().sorted().collect(Collectors.toList());
            chargeSelector.setItems(charges);
            chargeSelector.setEnabled(true);
        });

        chargeSelector.addValueChangeListener(e -> {
            if(e.getValue() != null) {
                updateRangeInfo(rangeInfo, medSelector.getValue().getMedId(), e.getValue());
            } else {
                rangeInfo.removeAll();
            }
        });

        transferButton.addClickListener(e -> {
            boolean success = unitService.transferUnitRange(new TransferUnitRangeRequestDto(
                    medSelector.getValue().getMedId(),
                    chargeSelector.getValue(),
                    fromField.getValue(),
                    toField.getValue(),
                    ownerSelector.getValue().getActorId()
            ));
            if (success) {
                Notification.show("Transfer erfolgreich gestartet.");
                loadInitialData();
            } else {
                Notification.show("Fehler beim Transfer.");
            }
            dialog.close();
        });

        FormLayout form = new FormLayout(medSelector, chargeSelector, fromField, toField, ownerSelector);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("500px", 2));
        dialog.add(new VerticalLayout(form, rangeInfo, transferButton));
        dialog.open();
    }

    private void showAddTemperaturePopup() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Temperaturmessung hinzufügen");
        ComboBox<UnitResponseDto> unitSelector = new ComboBox<>("Einheit auswählen");
        unitSelector.setItems(this.myUnits);
        unitSelector.setItemLabelGenerator(UnitResponseDto::getUnitId);
        TextField tempField = new TextField("Temperatur (°C)");
        Button saveButton = new Button("Speichern", VaadinIcon.CHECK.create());
        saveButton.setEnabled(false);
        unitSelector.addValueChangeListener(e -> saveButton.setEnabled(e.getValue() != null && !tempField.isEmpty()));
        tempField.addValueChangeListener(e -> saveButton.setEnabled(unitSelector.getValue() != null && !e.getValue().isEmpty()));
        saveButton.addClickListener(e -> {
            UnitResponseDto response = unitService.addTemperatureReading(
                    unitSelector.getValue().getUnitId(),
                    new AddTemperatureReadingRequestDto(tempField.getValue(), LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
            );
            if (response != null) {
                Notification.show("Temperaturmessung gespeichert.");
                loadInitialData();
            } else {
                Notification.show("Fehler beim Speichern.");
            }
            dialog.close();
        });
        dialog.add(new VerticalLayout(unitSelector, tempField));
        dialog.getFooter().add(new Button("Abbrechen", ev -> dialog.close()), saveButton);
        dialog.open();
    }

    private void updateRangeInfo(Div container, String medId, String charge) {
        container.removeAll();
        if (medId == null || charge == null) return;
        List<Integer> suffixes = myUnits.stream()
                .filter(u -> u.getMedId().equals(medId) && u.getChargeBezeichnung().equals(charge))
                .map(UnitResponseDto::getUnitId)
                .map(this::parseUnitSuffix)
                .filter(Objects::nonNull).sorted().collect(Collectors.toList());
        if (suffixes.isEmpty()) {
            container.add(new Span("Keine nummerierten Einheiten in dieser Charge gefunden."));
            return;
        }
        container.add(new H4("Verfügbare Bereiche:"));
        StringBuilder ranges = new StringBuilder();
        int start = suffixes.get(0);
        for (int i = 1; i < suffixes.size(); i++) {
            if (suffixes.get(i) > suffixes.get(i-1) + 1) {
                ranges.append(start).append("-").append(suffixes.get(i-1)).append(", ");
                start = suffixes.get(i);
            }
        }
        ranges.append(start).append("-").append(suffixes.get(suffixes.size()-1));
        container.add(new Span(ranges.toString()));
    }

    private Integer parseUnitSuffix(String unitId) {
        try {
            int lastDash = unitId.lastIndexOf('-');
            if (lastDash != -1 && lastDash < unitId.length() - 1) {
                return Integer.parseInt(unitId.substring(lastDash + 1));
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }

    private void showDetailsIfUnit(TreeItem item) {
        if (item.getData() instanceof UnitResponseDto unit) {
            showUnitDetailsPopup(unit);
        }
    }

    private void showUnitDetailsPopup(UnitResponseDto unit) {
        Dialog dialog = new Dialog();
        dialog.setWidth("600px");
        dialog.setHeaderTitle("Details für Einheit: " + unit.getUnitId());
        Tab detailsTab = new Tab("Details");
        Tab historyTab = new Tab("Transfer-Historie");
        Tab tempTab = new Tab("Temperatur-Messungen");
        Tabs tabs = new Tabs(detailsTab, historyTab, tempTab);
        tabs.setWidthFull();
        FormLayout detailsLayout = createDetailsLayout(unit);
        VerticalLayout historyLayout = createHistoryLayout(unit);
        VerticalLayout tempsLayout = createTempsLayout(unit);
        historyLayout.setVisible(false);
        tempsLayout.setVisible(false);
        Map<Tab, VerticalLayout> tabsToPages = new HashMap<>();
        tabsToPages.put(detailsTab, new VerticalLayout(detailsLayout));
        tabsToPages.put(historyTab, historyLayout);
        tabsToPages.put(tempTab, tempsLayout);
        Div pages = new Div(tabsToPages.values().toArray(new VerticalLayout[0]));
        tabs.addSelectedChangeListener(event -> {
            tabsToPages.values().forEach(page -> page.setVisible(false));
            tabsToPages.get(tabs.getSelectedTab()).setVisible(true);
        });
        dialog.add(tabs, pages);
        Button closeButton = new Button("Schließen", e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(closeButton);
        dialog.open();
    }

    private FormLayout createDetailsLayout(UnitResponseDto unit) {
        FormLayout formLayout = new FormLayout();
        TextField medId = new TextField("Medikament-ID");
        medId.setValue(unit.getMedId());
        medId.setReadOnly(true);
        TextField charge = new TextField("Charge");
        charge.setValue(unit.getChargeBezeichnung());
        charge.setReadOnly(true);
        TextField owner = new TextField("Aktueller Besitzer");
        owner.setValue(unit.getCurrentOwnerActorId());
        owner.setReadOnly(true);
        TextField consumed = new TextField("Status");
        consumed.setValue(unit.isConsumed() ? "Verbraucht" : "Verfügbar");
        consumed.setReadOnly(true);
        formLayout.add(medId, charge, owner, consumed);
        return formLayout;
    }

    private VerticalLayout createHistoryLayout(UnitResponseDto unit) {
        TextArea history = new TextArea();
        history.setValue(formatMapList(unit.getTransferHistory()));
        history.setReadOnly(true);
        history.setSizeFull();
        return new VerticalLayout(history);
    }

    private VerticalLayout createTempsLayout(UnitResponseDto unit) {
        TextArea temps = new TextArea();
        temps.setValue(formatMapList(unit.getTemperatureReadings()));
        temps.setReadOnly(true);
        temps.setSizeFull();
        return new VerticalLayout(temps);
    }

    private String formatMapList(List<Map<String, String>> list) {
        if (list == null || list.isEmpty()) return "Keine Daten vorhanden.";
        return list.stream()
                .map(map -> map.entrySet().stream()
                        .map(entry -> entry.getKey() + ": " + entry.getValue())
                        .collect(Collectors.joining(", ")))
                .collect(Collectors.joining("\n"));
    }

    private void configureSelectors(ComboBox<MedikamentResponseDto> bezeichnungComboBox, ComboBox<MedikamentResponseDto> idComboBox, TextField unitIdField) {
        bezeichnungComboBox.setItemLabelGenerator(MedikamentResponseDto::getBezeichnung);
        idComboBox.setItemLabelGenerator(MedikamentResponseDto::getMedId);
        AtomicBoolean isUpdating = new AtomicBoolean(false);
        bezeichnungComboBox.addValueChangeListener(event -> {
            if (isUpdating.get()) return;
            isUpdating.set(true);
            idComboBox.setValue(event.getValue());
            unitIdField.clear();
            isUpdating.set(false);
        });
        idComboBox.addValueChangeListener(event -> {
            if (isUpdating.get()) return;
            isUpdating.set(true);
            bezeichnungComboBox.setValue(event.getValue());
            unitIdField.clear();
            isUpdating.set(false);
        });
    }

    private void configureTreeGrid(TreeGrid<TreeItem> treeGrid, String hierarchyColumnHeader) {
        treeGrid.setSizeFull();
        treeGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);
        treeGrid.addHierarchyColumn(TreeItem::getDisplayValue).setHeader(hierarchyColumnHeader).setAutoWidth(true).setFlexGrow(1);
        treeGrid.addColumn(TreeItem::getOwner).setHeader("Besitzer").setAutoWidth(true);
        treeGrid.setClassNameGenerator(item -> {
            if (item.getLevel() == 0) return "medikament-level";
            if (item.getLevel() == 1) return "charge-level";
            return "unit-level";
        });
    }

    private void loadInitialData() {
        allMedikamente = medikamentService.searchMedikamente(new MedikamentFilterCriteriaDto("", ""));
        this.myUnits = unitService.getMyUnits();
        globalMedBezeichnungComboBox.setItems(allMedikamente);
        globalMedIdComboBox.setItems(allMedikamente);
        populateMyUnitsGrid();
    }

    private void populateMyUnitsGrid() {
        TreeData<TreeItem> treeData = new TreeData<>();
        if (myUnits != null && !myUnits.isEmpty()) {
            Map<String, MedikamentResponseDto> medikamentenMap = allMedikamente.stream().collect(Collectors.toMap(MedikamentResponseDto::getMedId, med -> med, (a, b) -> a));
            Map<String, Map<String, List<UnitResponseDto>>> unitsByMedAndCharge = myUnits.stream().filter(u -> u.getMedId() != null && u.getChargeBezeichnung() != null).collect(Collectors.groupingBy(UnitResponseDto::getMedId, Collectors.groupingBy(UnitResponseDto::getChargeBezeichnung)));
            unitsByMedAndCharge.forEach((medId, charges) -> {
                MedikamentResponseDto medikament = medikamentenMap.get(medId);
                if (medikament != null) {
                    TreeItem medItem = new TreeItem(medikament);
                    treeData.addItem(null, medItem);
                    charges.forEach((chargeBezeichnung, unitList) -> {
                        TreeItem chargeItem = new TreeItem(medId, chargeBezeichnung);
                        treeData.addItem(medItem, chargeItem);
                        List<TreeItem> unitItems = unitList.stream().map(TreeItem::new).collect(Collectors.toList());
                        treeData.addItems(chargeItem, unitItems);
                    });
                }
            });
        }
        myUnitsTreeGrid.setDataProvider(new TreeDataProvider<>(treeData));
    }

    private void resetGlobalSearch() {
        globalUnitsTreeGrid.setDataProvider(new TreeDataProvider<>(new TreeData<>()));
        globalUnitsTreeGrid.setVisible(true);
        notFoundMessage.setVisible(false);
    }

    private void loadUnitsForGlobalGrid(String medId) {
        TreeData<TreeItem> treeData = new TreeData<>();
        if (medId != null && !medId.isEmpty()) {
            Map<String, List<UnitResponseDto>> unitsByCharge = unitService.getUnitsGroupedByCharge(medId);
            if (unitsByCharge != null && !unitsByCharge.isEmpty()) {
                globalUnitsTreeGrid.setVisible(true);
                notFoundMessage.setVisible(false);
                unitsByCharge.forEach((chargeBezeichnung, unitList) -> {
                    TreeItem chargeItem = new TreeItem(medId, chargeBezeichnung);
                    treeData.addItem(null, chargeItem);
                    List<TreeItem> unitItems = unitList.stream().map(TreeItem::new).collect(Collectors.toList());
                    treeData.addItems(chargeItem, unitItems);
                });
            } else {
                globalUnitsTreeGrid.setVisible(false);
                notFoundMessage.setText("Für dieses Medikament wurden keine Einheiten gefunden.");
                notFoundMessage.setVisible(true);
            }
        }
        globalUnitsTreeGrid.setDataProvider(new TreeDataProvider<>(treeData));
        globalUnitsTreeGrid.expand(treeData.getRootItems());
    }

    private void displaySingleUnitInGlobalGrid(String unitId) {
        UnitResponseDto unit = unitService.getUnitById(unitId);
        TreeData<TreeItem> treeData = new TreeData<>();
        if (unit != null) {
            globalUnitsTreeGrid.setVisible(true);
            notFoundMessage.setVisible(false);
            TreeItem chargeItem = new TreeItem(unit.getMedId(), unit.getChargeBezeichnung());
            TreeItem unitItem = new TreeItem(unit);
            treeData.addItem(null, chargeItem);
            treeData.addItem(chargeItem, unitItem);
            globalUnitsTreeGrid.setDataProvider(new TreeDataProvider<>(treeData));
            globalUnitsTreeGrid.expand(chargeItem);
            globalUnitsTreeGrid.select(unitItem);
        } else {
            globalUnitsTreeGrid.setVisible(false);
            notFoundMessage.setText("Keine Einheit mit der ID '" + unitId + "' gefunden.");
            notFoundMessage.setVisible(true);
        }
    }
}