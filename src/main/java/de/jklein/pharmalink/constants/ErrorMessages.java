package de.jklein.pharmalink.constants;

public final class ErrorMessages {

    private ErrorMessages() { }

    public static final String DEFAULT_TITLE = "Uups! Ein Fehler ist aufgetreten.";
    public static final String DEFAULT_MESSAGE = "Entschuldigen Sie die Unannehmlichkeiten. Es gab ein Problem beim Verarbeiten Ihrer Anfrage. Bitte versuchen Sie es später erneut oder kehren Sie zur Startseite zurück.";

    public static final String TITLE_404 = "Seite nicht gefunden";
    public static final String MESSAGE_404 = "Die angeforderte Seite konnte nicht gefunden werden. Möglicherweise wurde die Adresse falsch eingegeben oder die Seite existiert nicht mehr.";

    public static final String TITLE_500 = "Interner Serverfehler";
    public static final String MESSAGE_500 = "Entschuldigen Sie die Unannehmlichkeiten. Es ist ein unerwarteter Serverfehler aufgetreten. Unser Team wurde benachrichtigt und arbeitet bereits an einer Lösung.";

    public static final String NAV_HOME = "Startseite";

    public static String getTitleForCode(Object code) {
        if (code instanceof Integer) {
            switch ((int) code) {
                case 404: return TITLE_404;
                case 500: return TITLE_500;
                default: return DEFAULT_TITLE;
            }
        }
        return DEFAULT_TITLE;
    }

    public static String getMessageForCode(Object code) {
        if (code instanceof Integer) {
            switch ((int) code) {
                case 404: return MESSAGE_404;
                case 500: return MESSAGE_500;
                default: return DEFAULT_MESSAGE;
            }
        }
        return DEFAULT_MESSAGE;
    }
}