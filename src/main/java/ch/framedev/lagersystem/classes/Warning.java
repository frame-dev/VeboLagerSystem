package ch.framedev.lagersystem.classes;

public class Warning {

    private String title;
    private String message;
    private WarningType type;
    private String date;
    private boolean isResolved;
    private boolean isDisplayed;

    public Warning(String title, String message, WarningType type, String date, boolean isResolved, boolean isDisplayed) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.date = date;
        this.isResolved = isResolved;
        this.isDisplayed = isDisplayed;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public WarningType getType() {
        return type;
    }

    public void setType(WarningType type) {
        this.type = type;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public boolean isResolved() {
        return isResolved;
    }

    public void setResolved(boolean resolved) {
        isResolved = resolved;
    }

    public boolean isDisplayed() {
        return isDisplayed;
    }

    public void setDisplayed(boolean displayed) {
        isDisplayed = displayed;
    }

    /**
     * The type of warning.
     */
    public static enum WarningType {
        LOW_STOCK("Geringer Lagerbestand"),
        ORDER_NEEDED("Bestellung erforderlich"),
        OTHER("Sonstiges");

        final String displayName;

        WarningType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
