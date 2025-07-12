package de.jklein.pharmalinkclient.views.medikamente;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

@SpringComponent
@UIScope
public class DetailContent extends Div {

    private final ObjectProvider<MedikamentIpfsDataContent> medikamentIpfsDataContentProvider;
    private final ObjectProvider<UnitInformationContent> unitInformationContentProvider;

    @Autowired
    public DetailContent(ObjectProvider<MedikamentIpfsDataContent> medikamentIpfsDataContentProvider,
                         ObjectProvider<UnitInformationContent> unitInformationContentProvider) {
        this.medikamentIpfsDataContentProvider = medikamentIpfsDataContentProvider;
        this.unitInformationContentProvider = unitInformationContentProvider;

        addClassName("detail-content");
        getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        getStyle().set("padding", "var(--lumo-space-m)");
        setSizeFull();

        SplitLayout internalSplitLayout = new SplitLayout();
        internalSplitLayout.setOrientation(SplitLayout.Orientation.HORIZONTAL);
        internalSplitLayout.setSizeFull();
        internalSplitLayout.setSplitterPosition(50);

        internalSplitLayout.addToPrimary(unitInformationContentProvider.getObject());
        internalSplitLayout.addToSecondary(medikamentIpfsDataContentProvider.getObject());

        add(internalSplitLayout);
    }

    public void setContent(Component content) {
        removeAll();
        if (content != null) {
            add(content);
        }
    }
}