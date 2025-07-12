package de.jklein.pharmalinkclient.views.dashboard;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import de.jklein.pharmalinkclient.dto.SystemStatsDto;
import de.jklein.pharmalinkclient.security.UserSession;
import de.jklein.pharmalinkclient.service.StateService;
import de.jklein.pharmalinkclient.service.SystemService;
import de.jklein.pharmalinkclient.views.MainLayout;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@PageTitle("Dashboard")
@Route(value = "", layout = MainLayout.class)
@PermitAll
@SpringComponent
@UIScope
public class DashboardView extends Main {

    private final StateService stateService;
    private final UserSession userSession;
    private final SystemService systemService;

    private final TextField usernameField;
    private final TextField roleField;

    private final TextField actorCountField;
    private final TextField medikamentCountField;
    private final TextField myUnitsCountField;

    private final TextField actorIdReadonlyField;
    private Image qrCodeImage;

    private Consumer<String> actorIdListener;
    private Consumer<SystemStatsDto> cacheStatsListener;

    @Autowired
    public DashboardView(StateService stateService, UserSession userSession, SystemService systemService) { // NEU: SystemService im Konstruktor
        this.stateService = stateService;
        this.userSession = userSession;
        this.systemService = systemService;
        addClassName("dashboard-view");

        VerticalLayout rootLayout = new VerticalLayout();
        rootLayout.setSpacing(true);
        rootLayout.setPadding(true);
        rootLayout.setAlignItems(FlexComponent.Alignment.START);

        VerticalLayout userInfoCard = createCardLayout();
        HorizontalLayout userInfoTitleRow = new HorizontalLayout(VaadinIcon.USER_CARD.create(), new H4("Ihre Informationen"));
        userInfoTitleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        userInfoCard.add(userInfoTitleRow);

        usernameField = createReadOnlyTextField("Benutzername", "");
        usernameField.setValue(userSession.getUsername() != null ? userSession.getUsername() : "Nicht angemeldet");

        roleField = createReadOnlyTextField("Ihre Rolle", "Lädt...");

        FormLayout userForm = new FormLayout(usernameField, roleField);
        userInfoCard.add(userForm);


        VerticalLayout statsCard = createCardLayout();
        HorizontalLayout statsTitleRow = new HorizontalLayout(VaadinIcon.CHART_GRID.create(), new H4("Statistiken"));
        statsTitleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        statsCard.add(statsTitleRow);

        actorCountField = createReadOnlyTextField("Akteur Anzahl", "Lädt...");
        medikamentCountField = createReadOnlyTextField("Medikament Anzahl", "Lädt...");
        myUnitsCountField = createReadOnlyTextField("Meine Einheiten Anzahl", "Lädt...");
        FormLayout statsFormLayout = new FormLayout(actorCountField, medikamentCountField, myUnitsCountField);
        statsFormLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 3));
        statsCard.add(statsFormLayout);

        VerticalLayout actorIdCard = createCardLayout();
        actorIdCard.setAlignItems(FlexComponent.Alignment.CENTER);
        actorIdCard.setWidth("320px");

        HorizontalLayout actorIdTitleRow = new HorizontalLayout(VaadinIcon.BARCODE.create(), new H4("Ihre Akteur-ID"));
        actorIdTitleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        actorIdTitleRow.getStyle().set("align-self", "start");
        actorIdCard.add(actorIdTitleRow);

        actorIdReadonlyField = createReadOnlyTextField("", "Lädt...");
        actorIdReadonlyField.setWidthFull();

        qrCodeImage = new Image();
        qrCodeImage.setAlt("QR-Code für Akteur-ID");
        qrCodeImage.setVisible(false);
        qrCodeImage.setWidth("200px");
        qrCodeImage.setHeight("200px");
        qrCodeImage.addClassName("qr-code-image");

        actorIdCard.add(qrCodeImage, actorIdReadonlyField);

        HorizontalLayout topRow = new HorizontalLayout(userInfoCard, statsCard);
        topRow.addClassName(Gap.MEDIUM);

        rootLayout.add(topRow, actorIdCard);
        add(rootLayout);

        addCopyToClipboardOnClick(usernameField);
        addCopyToClipboardOnClick(roleField);
        addCopyToClipboardOnClick(actorIdReadonlyField);
        addCopyToClipboardOnClick(actorCountField);
        addCopyToClipboardOnClick(medikamentCountField);
        addCopyToClipboardOnClick(myUnitsCountField);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        if (!stateService.isSystemDataLoadedForSession()) {
            loadSystemDataOnce();
        } else {
            updateFieldsFromStateService();
        }

        actorIdListener = rawActorId -> UI.getCurrent().access(() -> {
            if (rawActorId != null && !rawActorId.isEmpty()) {
                actorIdReadonlyField.setValue(rawActorId);
                String role = rawActorId.contains("-") ? rawActorId.split("-", 2)[0] : "Unbekannt";
                roleField.setValue(role);
                String base64QrCode = generateQrCodeBase64(rawActorId);
                if (base64QrCode != null) {
                    qrCodeImage.setSrc("data:image/png;base64," + base64QrCode);
                    qrCodeImage.setVisible(true);
                }
            } else {
                roleField.setValue("N/A");
                actorIdReadonlyField.setValue("N/A");
                qrCodeImage.setVisible(false);
            }
        });
        stateService.addCurrentActorIdListener(actorIdListener);

        cacheStatsListener = stats -> UI.getCurrent().access(() -> {
            if (stats != null) {
                actorCountField.setValue(String.valueOf(stats.getActorCount()));
                medikamentCountField.setValue(String.valueOf(stats.getMedikamentCount()));
                myUnitsCountField.setValue(String.valueOf(stats.getMyUnitsCount()));
            } else {
                actorCountField.setValue("N/A");
                medikamentCountField.setValue("N/A");
                myUnitsCountField.setValue("N/A");
            }
        });
        stateService.addCacheStatsListener(cacheStatsListener);

        updateFieldsFromStateService();
    }

    private void loadSystemDataOnce() {
        UI ui = UI.getCurrent();

        systemService.getCurrentActorId()
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        actorId -> ui.access(() -> stateService.setCurrentActorId(actorId)),
                        error -> ui.access(() -> System.err.println("Fehler beim Laden der Akteur-ID: " + error.getMessage()))
                );

        systemService.getCacheStats()
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        stats -> ui.access(() -> stateService.setCacheStats(stats)),
                        error -> ui.access(() -> System.err.println("Fehler beim Laden der Cache-Statistiken: " + error.getMessage()))
                );

        systemService.getCacheState()
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        systemState -> ui.access(() -> {
                            stateService.setSystemState(systemState);
                            stateService.setAllSystemActors(systemState.getAllActors());
                            stateService.setAllSystemMedikamente(systemState.getAllMedikamente());
                            stateService.setMySystemUnits(systemState.getMyUnits());
                            stateService.setSystemDataLoadedForSession(true); // Flag setzen
                        }),
                        error -> ui.access(() -> System.err.println("Fehler beim Laden des Systemzustands: " + error.getMessage()))
                );
    }

    private void updateFieldsFromStateService() {
        String currentActorId = stateService.getCurrentActorId();
        if (currentActorId != null && !currentActorId.isEmpty()) {
            actorIdReadonlyField.setValue(currentActorId);
            String role = currentActorId.contains("-") ? currentActorId.split("-", 2)[0] : "Unbekannt";
            roleField.setValue(role);
            String base64QrCode = generateQrCodeBase64(currentActorId);
            if (base64QrCode != null) {
                qrCodeImage.setSrc("data:image/png;base64," + base64QrCode);
                qrCodeImage.setVisible(true);
            }
        } else {
            actorIdReadonlyField.setValue("N/A");
            roleField.setValue("N/A");
            qrCodeImage.setVisible(false);
        }

        SystemStatsDto currentCacheStats = stateService.getCacheStats();
        if (currentCacheStats != null) {
            actorCountField.setValue(String.valueOf(currentCacheStats.getActorCount()));
            medikamentCountField.setValue(String.valueOf(currentCacheStats.getMedikamentCount()));
            myUnitsCountField.setValue(String.valueOf(currentCacheStats.getMyUnitsCount()));
        } else {
            actorCountField.setValue("N/A");
            medikamentCountField.setValue("N/A");
            myUnitsCountField.setValue("N/A");
        }
    }


    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (actorIdListener != null) stateService.removeCurrentActorIdListener(actorIdListener);
        if (cacheStatsListener != null) stateService.removeCacheStatsListener(cacheStatsListener);
    }

    private TextField createReadOnlyTextField(String label, String initialValue) {
        TextField field = new TextField(label);
        field.setReadOnly(true);
        field.setValue(initialValue);
        field.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        return field;
    }

    private void addCopyToClipboardOnClick(TextField textField) {
        textField.getElement().addEventListener("click", e -> UI.getCurrent().getPage()
                .executeJs("const i=$0.inputElement||$0.shadowRoot.querySelector('input'); i.select(); document.execCommand('copy');",
                        textField.getElement()));
    }

    private VerticalLayout createCardLayout() {
        VerticalLayout card = new VerticalLayout();
        card.addClassName("dashboard-card");
        card.setSpacing(false);

        card.setPadding(true);
        return card;
    }

    private String generateQrCodeBase64(String data) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix bitMatrix = new QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, 200, 200, hints);
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (WriterException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}