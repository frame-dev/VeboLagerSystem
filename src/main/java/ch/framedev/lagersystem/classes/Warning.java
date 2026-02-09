package ch.framedev.lagersystem.classes;

/**
 * Represents a warning with severity type, message, and resolution flags.
 */
public class Warning {

    /** Short warning title. */
    private String title;
    /** Detailed warning message. */
    private String message;
    /** Warning category. */
    private WarningType type;
    /** Warning date as stored text. */
    private String date;
    /** Whether the warning has been resolved. */
    private boolean isResolved;
    /** Whether the warning has been displayed to the user. */
    private boolean isDisplayed;

    /**
     * Creates a new warning.
     *
     * @param title short warning title
     * @param message detailed warning message
     * @param type warning category
     * @param date warning date text
     * @param isResolved whether the warning is resolved
     * @param isDisplayed whether the warning has been displayed
     */
    public Warning(String title, String message, WarningType type, String date, boolean isResolved, boolean isDisplayed) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.date = date;
        this.isResolved = isResolved;
        this.isDisplayed = isDisplayed;
    }

    /** @return short warning title */
    public String getTitle() {
        return title;
    }

    /** @param title short warning title */
    public void setTitle(String title) {
        this.title = title;
    }

    /** @return detailed warning message */
    public String getMessage() {
        return message;
    }

    /** @param message detailed warning message */
    public void setMessage(String message) {
        this.message = message;
    }

    /** @return warning category */
    public WarningType getType() {
        return type;
    }

    /** @param type warning category */
    public void setType(WarningType type) {
        this.type = type;
    }

    /** @return warning date text */
    public String getDate() {
        return date;
    }

    /** @param date warning date text */
    public void setDate(String date) {
        this.date = date;
    }

    /** @return true when resolved */
    public boolean isResolved() {
        return isResolved;
    }

    /** @param resolved whether the warning is resolved */
    public void setResolved(boolean resolved) {
        isResolved = resolved;
    }

    /** @return true when displayed */
    public boolean isDisplayed() {
        return isDisplayed;
    }

    /** @param displayed whether the warning has been displayed */
    public void setDisplayed(boolean displayed) {
        isDisplayed = displayed;
    }

    /**
     * The type of warning.
     */
    public static enum WarningType {
        LOW_STOCK("Mindest Lagerbestand"),
        CRITICAL_STOCK("Kritischer Lagerbestand"),
        ORDER_NEEDED("Bestellung erforderlich"),
        OTHER("Sonstiges");

        /** Localized display name. */
        final String displayName;

        /** @param displayName localized label */
        WarningType(String displayName) {
            this.displayName = displayName;
        }

        /** @return localized display name */
        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Builds the QR code payload string using labeled fields.
     *
     * @return QR code data payload
     */
    public String getQRCodeData() {
        return "title:" + title + ";" +
               "message:" + message + ";" +
               "type:" + type.name() + ";" +
               "date:" + date + ";" +
               "isResolved:" + isResolved + ";" +
               "isDisplayed:" + isDisplayed;
    }
}
