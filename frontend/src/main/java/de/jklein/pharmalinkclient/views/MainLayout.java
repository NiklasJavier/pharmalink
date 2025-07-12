package de.jklein.pharmalinkclient.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.jklein.pharmalinkclient.security.UserSession;
import de.jklein.pharmalinkclient.service.AuthService;
import de.jklein.pharmalinkclient.views.actorexplorer.ActorExplorerView;
import de.jklein.pharmalinkclient.views.dashboard.DashboardView;
import de.jklein.pharmalinkclient.views.medikamente.MedikamenteView;
import de.jklein.pharmalinkclient.views.units.UnitsView;
import de.jklein.pharmalinkclient.service.SystemService;
import de.jklein.pharmalinkclient.service.StateService;

import java.util.Optional;

public class MainLayout extends AppLayout {

    private final AuthService authService;
    private final UserSession userSession;
    private final SystemService systemService;
    private final StateService stateService;
    private Tabs menu;
    private Button themeToggleButton;

    public MainLayout(AuthService authService, UserSession userSession,
                      SystemService systemService, StateService stateService) {
        this.authService = authService;
        this.userSession = userSession;
        this.systemService = systemService;
        this.stateService = stateService;

        setPrimarySection(Section.NAVBAR);
        addToNavbar(false, createTwoRowHeader());
    }

    private Component createTwoRowHeader() {
        H1 appName = new H1("PharmaLink Client");
        appName.addClassNames("app-name", LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        Component rightSideControls = createRightSideControls();

        HorizontalLayout topRow = new HorizontalLayout(appName, rightSideControls);
        topRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        topRow.setSpacing(false);
        topRow.expand(appName);
        topRow.setWidthFull();
        topRow.addClassNames(LumoUtility.Padding.Vertical.SMALL, LumoUtility.Padding.Horizontal.MEDIUM);

        menu = createNavigationTabs();
        HorizontalLayout bottomRow = new HorizontalLayout(menu);
        bottomRow.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        bottomRow.setWidthFull();
        bottomRow.addClassName("nav-tab-bar");

        VerticalLayout headerLayout = new VerticalLayout(topRow, bottomRow);
        headerLayout.setPadding(false);
        headerLayout.setSpacing(false);
        headerLayout.setWidthFull();

        return headerLayout;
    }

    private Component createRightSideControls() {
        themeToggleButton = createThemeToggleButton();
        Component userMenu = createUserMenu();

        HorizontalLayout rightSide = new HorizontalLayout(themeToggleButton, userMenu);
        rightSide.setAlignItems(FlexComponent.Alignment.CENTER);
        rightSide.addClassName(LumoUtility.Gap.XSMALL);

        return rightSide;
    }

    private Button createThemeToggleButton() {
        Button toggleButton = new Button();
        toggleButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        toggleButton.getElement().setAttribute("aria-label", "Toggle theme");

        toggleButton.addClickListener(click -> {
            boolean isCurrentlyDark = UI.getCurrent().getElement().getThemeList().contains(Lumo.DARK);
            boolean nowDark = !isCurrentlyDark;

            userSession.setTheme(nowDark ? Lumo.DARK : Lumo.LIGHT);
            updateTheme(nowDark);
        });
        return toggleButton;
    }

    private Component createUserMenu() {
        if (userSession.isLoggedIn()) {
            Button userIconButton = new Button(VaadinIcon.USER.create());
            userIconButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            userIconButton.getElement().setAttribute("aria-label", "User Menu");

            ContextMenu contextMenu = new ContextMenu();
            contextMenu.setTarget(userIconButton);
            contextMenu.setOpenOnClick(true);

            contextMenu.addItem("Angemeldet als: " + userSession.getUsername(), e -> {}).setEnabled(false);
            contextMenu.addItem("Abmelden", e -> {
                authService.logout();
                UI.getCurrent().getPage().reload();
            });

            return userIconButton;
        }
        return new Span();
    }

    private Tabs createNavigationTabs() {
        Tabs tabs = new Tabs();
        tabs.add(createTab(VaadinIcon.DASHBOARD, "Dashboard", DashboardView.class));
        tabs.add(createTab(VaadinIcon.GROUP, "Akteure", ActorExplorerView.class));
        tabs.add(createTab(VaadinIcon.PILLS, "Medikamente", MedikamenteView.class));
        tabs.add(createTab(VaadinIcon.PACKAGE, "Einheiten", UnitsView.class));
        return tabs;
    }

    private Tab createTab(VaadinIcon viewIcon, String viewName, Class<? extends Component> navigationTarget) {
        Icon icon = viewIcon.create();
        icon.getStyle().set("margin-inline-end", "var(--lumo-space-s)");

        RouterLink link = new RouterLink();
        link.add(icon, new Span(viewName));
        link.setRoute(navigationTarget);
        link.setTabIndex(-1);

        Tab tab = new Tab(link);
        tab.getElement().setAttribute("data-route", navigationTarget.getName());
        return tab;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();

        String sessionTheme = userSession.getTheme();
        if (sessionTheme != null) {
            updateTheme(sessionTheme.equals(Lumo.DARK));
        } else {
            var js = "return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches";
            ui.getPage().executeJs(js).then(Boolean.class, prefersDark -> {
                ui.access(() -> updateTheme(prefersDark));
            });
        }

        if (userSession.isLoggedIn() && !stateService.isSystemDataLoadedForSession()) {
            fetchAndStoreSystemData(ui);
        }
    }

    private void updateTheme(boolean isDark) {
        ThemeList themeList = UI.getCurrent().getElement().getThemeList();
        themeList.set(Lumo.DARK, isDark);

        VaadinIcon icon = isDark ? VaadinIcon.SUN_O : VaadinIcon.MOON;
        themeToggleButton.setIcon(icon.create());
    }

    private void fetchAndStoreSystemData(UI ui) {
        systemService.getCurrentActorId()
                .subscribe(actorId -> {
                    ui.access(() -> {
                        stateService.setCurrentActorId(actorId); 
                        stateService.setSystemDataLoadedForSession(true);
                        System.out.println("Aktuelle Actor ID im Frontend (von MainLayout): " + actorId);
                    });
                }, error -> {
                    ui.access(() -> {
                        System.err.println("Fehler beim Abrufen der aktuellen Actor ID (von MainLayout): " + error.getMessage());
                        stateService.setSystemDataLoadedForSession(true);
                    });
                });

        systemService.getCacheStats()
                .subscribe(stats -> {
                    ui.access(() -> {
                        stateService.setCacheStats(stats);
                        System.out.println("Cache Statistiken im Frontend (von MainLayout): " + stats);
                    });
                }, error -> {
                    ui.access(() -> {
                        System.err.println("Fehler beim Abrufen der Cache-Statistiken (von MainLayout): " + error.getMessage());
                    });
                });
    }


    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        getTabForComponent(getContent()).ifPresent(menu::setSelectedTab);
    }

    private Optional<Tab> getTabForComponent(Component component) {
        return menu.getChildren()
                .filter(tab -> {
                    String route = ((Tab) tab).getElement().getAttribute("data-route");
                    return route != null && route.equals(component.getClass().getName());
                })
                .findFirst().map(Tab.class::cast);
    }
}