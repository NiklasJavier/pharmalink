package de.jklein.pharmalink.constants;

public final class ErrorMessages {

    private ErrorMessages() { }

    public static final String DEFAULT_TITLE = "Ein Fehler ist aufgetreten.";
    public static final String DEFAULT_MESSAGE = "Entschuldigen Sie die Unannehmlichkeiten. Es gab ein Problem beim Verarbeiten Ihrer Anfrage. Bitte versuchen Sie es später erneut oder kehren Sie zur Startseite zurück.";

    public static final String TITLE_404 = "Seite nicht gefunden";
    public static final String MESSAGE_404 = "Die angeforderte Seite konnte nicht gefunden werden. Möglicherweise wurde die Adresse falsch eingegeben oder die Seite existiert nicht mehr.";

    public static final String TITLE_500 = "Interner Serverfehler";
    public static final String MESSAGE_500 = "Entschuldigen Sie die Unannehmlichkeiten. Es ist ein unerwarteter Serverfehler aufgetreten. Unser Team wurde benachrichtigt und arbeitet bereits an einer Lösung.";

    public static final String NAV_HOME = "Startseite";

    public static String getTitleForCode(int code) {
        return switch (code) {
            case 404 -> TITLE_404;
            case 500 -> TITLE_500;
            default -> DEFAULT_TITLE;
        };
    }

    public static String getMessageForCode(int code) {
        return switch (code) {
            case 404 -> MESSAGE_404;
            case 500 -> MESSAGE_500;
            default -> DEFAULT_MESSAGE;
        };
    }
}