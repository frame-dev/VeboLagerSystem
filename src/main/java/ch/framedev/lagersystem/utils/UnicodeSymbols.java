package ch.framedev.lagersystem.utils;

/**
 * Cross-platform symbols for consistent display on Windows, macOS, and Linux.
 * Uses Unicode escape sequences to keep the source ASCII-friendly.
 */
@SuppressWarnings("UnnecessaryUnicodeEscape")
public class UnicodeSymbols {
    // ===================== UI Elements =====================
    public static final String CLOSE = "\u2715";
    public static final String CHECK = "\u2713";
    public static final String CHECKMARK = "\u2714";
    public static final String PLUS = "\u002B";
    public static final String HEAVY_PLUS = "\u271A";
    public static final String MINUS = "\u2212";
    public static final String MULTIPLY = "\u00D7";

    // ===================== Status Icons =====================
    public static final String WARNING = "\u26A0";
    public static final String INFO = "\u2139";
    public static final String ERROR = "\u2716";
    public static final String SUCCESS = "\u2713";
    public static final String BULLET = "\u2022";
    public static final String CIRCLE = "\u25CF";

    // ===================== Business Icons =====================
    public static final String FOLDER = "\uD83D\uDCC1";
    public static final String PACKAGE = "\uD83D\uDCE6";
    public static final String TRUCK = "\uD83D\uDE9A";
    public static final String EDIT = "\u270E";
    public static final String PENCIL = "\u270F";
    public static final String BETTER_PENCIL = "\uD83D\uDCDD";
    public static final String DOCUMENT = "\uD83D\uDCC4";
    public static final String CLIPBOARD = "\uD83D\uDCCB";
    public static final String CHART = "\uD83D\uDCCA";
    public static final String MONEY = "\uD83D\uDCB0";
    public static final String TAG = "\uD83C\uDFF7";
    public static final String PERSON = "\uD83D\uDC64";
    public static final String ID = "\uD83C\uDD94";
    public static final String PHONE = "\uD83D\uDCF1";
    public static final String CLOCK = "\uD83D\uDD70";
    public static final String CALENDAR = "\uD83D\uDCC5";
    public static final String TRASH = "\uD83D\uDDD1";
    public static final String DOWNLOAD = "\uD83D\uDCE5";
    public static final String UPLOAD = "\uD83D\uDCE4";
    public static final String REFRESH = "\uD83D\uDD04";
    public static final String GEAR = "\u2699";
    public static final String BETTER_GEAR = "\uD83D\uDEE0";
    public static final String SEARCH = "\uD83D\uDD0D";
    public static final String PEOPLE = "\uD83D\uDC65";
    public static final String BUILDING = "\uD83C\uDFE2";
    public static final String NUMBERS = "\uD83D\uDD22";
    public static final String MEMO = "\uD83D\uDCDD";
    public static final String BROOM = "\uD83E\uDDF9";
    public static final String FLOPPY = "\uD83D\uDCBE";
    public static final String LIST = "\uD83D\uDCC3";
    public static final String CLEAR = "\uD83D\uDDD1";
    public static final String CLIENT = "\uD83D\uDCBC";
    public static final String UPDATE = "\uD83D\uDD04";
    public static final String DEPARTMENT = "\uD83C\uDFE2";
    public static final String SETTINGS = "\u2699";
    public static final String HEALTH = "\uD83D\uDC89";
    public static final String CREDIT_CARD = "\uD83D\uDCB3";
    public static final String CALCULATOR = "\uD83D\uDCB1";
    public static final String BULB = "\uD83D\uDCA1";
    public static final String ABC = "\uD83D\uDD24";
    public static final String COLOR_PALETTE = "\uD83C\uDFA8";
    public static final String WRENCH = "\uD83D\uDD27";
    public static final String GLOBE = "\uD83C\uDF10";
    public static final String DEVELOPER = "\uD83D\uDCBB";
    public static final String SHOPPING_CART = "\uD83D\uDED2";

    // ===================== Table/Column Icons =====================
    public static final String ARTIKELNUMMER = NUMBERS;
    public static final String ARTICLE_NAME = "\uD83D\uDCD6";
    public static final String CATEGORY = "\uD83D\uDCCF";
    public static final String DETAILS = CLIPBOARD;
    public static final String STOCK_QUANTITY = PACKAGE;
    public static final String MIN_STOCK = "\uD83D\uDD3A";
    public static final String SELL_PRICE = MONEY;
    public static final String PURCHASE_PRICE = "\uD83D\uDCB3";
    public static final String SUPPLIER = PEOPLE;
    public static final String ADDED = "\u2795";
    public static final String ORDER_QUANTITY = CLIPBOARD;
    public static final String ADDED_AT = "\u23F0";
    public static final String ADDRESS = "\uD83C\uDFE0";
    public static final String EMAIL = "\uD83D\uDCE7";
    public static final String CONTACT = PERSON;
    public static final String DELIVERED_ARTICLES = PACKAGE;
    public static final String MIN_ORDER_VALUE = "\uD83D\uDCB5";
    public static final String VENDOR = PEOPLE;
    public static final String SUPPLIER_ORDER = PACKAGE;
    public static final String DELIVERED = CHECKMARK;
    public static final String QUANTITY = CLIPBOARD;
    public static final String NAME = ARTICLE_NAME;
    public static final String EMAIL_ALT = EMAIL;
    public static final String PHONE_ALT = PHONE;
    public static final String ADDRESS_ALT = ADDRESS;
    public static final String MIN_ORDER = MIN_ORDER_VALUE;
    public static final String DELIVERED_ITEMS = PACKAGE;
    public static final String CATEGORY_ALT = CATEGORY;
    public static final String SUPPLIER_ALT = SUPPLIER;
    public static final String ORDERED = PACKAGE;

    // ===================== Navigation & Media =====================
    public static final String ARROW_RIGHT = "\u2192";
    public static final String ARROW_LEFT = "\u2190";
    public static final String ARROW_UP = "\u2191";
    public static final String ARROW_DOWN = "\u2193";
    public static final String ARROW_RIGHT_HEAVY = "\u27A4";
    public static final String PLAY = "\u25B6";
    public static final String PAUSE = "\u23F8";
    public static final String STOP = "\u23F9";
    public static final String NEXT = "\u23ED";
    public static final String PREVIOUS = "\u23EE";
    public static final String FAST_FORWARD = "\u23E9";
    public static final String REWIND = "\u23EA";

    // ===================== File System & Data =====================
    public static final String FILE = DOCUMENT;
    public static final String FILE_TEXT = LIST;
    public static final String FILE_PDF = "\uD83D\uDCD1";
    public static final String ARCHIVE = "\uD83D\uDDDC";
    public static final String DATABASE = "\uD83D\uDDC3";
    public static final String CLOUD = "\u2601";
    public static final String CLOUD_UPLOAD = UPLOAD;
    public static final String CLOUD_DOWNLOAD = DOWNLOAD;

    // ===================== Security & Permissions =====================
    public static final String LOCK = "\uD83D\uDD12";
    public static final String UNLOCK = "\uD83D\uDD13";
    public static final String KEY = "\uD83D\uDD11";
    public static final String SHIELD = "\uD83D\uDEE1";
    public static final String WARNING_SHIELD = "\u26E8";
    public static final String VERIFIED = "\u2705";
    public static final String BLOCKED = "\u26D4";

    // ===================== User & Roles =====================
    public static final String USER = PERSON;
    public static final String USERS = PEOPLE;
    public static final String ADMIN = "\uD83D\uDC51";
    public static final String GUEST = "\uD83D\uDC68";
    public static final String PROFILE = "\uD83D\uDC72";

    // ===================== Communication =====================
    public static final String MESSAGE = "\uD83D\uDCAC";
    public static final String CHAT = "\uD83D\uDDE8";
    public static final String NOTIFICATION = "\uD83D\uDD14";
    public static final String MUTED = "\uD83D\uDD15";
    public static final String SEND = "\u27A1";
    public static final String RECEIVE = "\u2B05";

    // ===================== Sorting & Filtering =====================
    public static final String SORT = "\u21C5";
    public static final String SORT_UP = "\u25B2";
    public static final String SORT_DOWN = "\u25BC";
    public static final String FILTER = "\uD83D\uDD0E";
    public static final String FUNNEL = "\uD83D\uDDC4";

    // ===================== Status & Indicators =====================
    public static final String ONLINE = "\uD83D\uDFE2";
    public static final String OFFLINE = "\u26AB";
    public static final String IN_PROGRESS = "\u23F3";
    public static final String COMPLETED = CHECKMARK;
    public static final String FAILED = "\u274C";
    public static final String PENDING = "\u23F1";

    // ===================== Misc UI Helpers =====================
    public static final String EYE = "\uD83D\uDC41";
    public static final String HIDDEN = "\uD83D\uDE48";
    public static final String LINK = "\uD83D\uDD17";
    public static final String UNLINK = "\uD83D\uDD18";
    public static final String PIN = "\uD83D\uDCCC";
    public static final String LOCATION = "\uD83D\uDCCD";

    // ===================== Time & History =====================
    public static final String HISTORY = "\u23F2";
    public static final String TIME = "\u23F0";
    public static final String CALENDAR_ALT = "\uD83D\uDCC6";
    public static final String STATUS = WARNING;
    public static final String TITLE = ARTICLE_NAME;

    // ===================== Layout & Separators =====================
    public static final String SEPARATOR_DOT = "\u00B7";
    public static final String SEPARATOR_PIPE = "\u2502";
    public static final String SEPARATOR_DASH = "\u2014";
    public static final String SEPARATOR_DOUBLE = "\u2550";
    public static final String SEPARATOR_BULLET = "\u2219";

    // ===================== Box Drawing (Tables / Console UI) =====================
    public static final String BOX_HORIZONTAL = "\u2500";
    public static final String BOX_VERTICAL = "\u2502";
    public static final String BOX_TOP_LEFT = "\u250C";
    public static final String BOX_TOP_RIGHT = "\u2510";
    public static final String BOX_BOTTOM_LEFT = "\u2514";
    public static final String BOX_BOTTOM_RIGHT = "\u2518";
    public static final String BOX_CROSS = "\u253C";

    // ===================== Progress & Loading =====================
    public static final String PROGRESS_EMPTY = "\u25CB";
    public static final String PROGRESS_HALF = "\u25D0";
    public static final String PROGRESS_FULL = "\u25CF";
    public static final String LOADING = "\u231B";
    public static final String SPINNER = "\u27F3";

    // ===================== Ratings & Quality =====================
    public static final String STAR_EMPTY = "\u2606";
    public static final String STAR_FILLED = "\u2605";
    public static final String THUMBS_UP = "\uD83D\uDC4D";
    public static final String THUMBS_DOWN = "\uD83D\uDC4E";

    // ===================== System & Power =====================
    public static final String POWER = "\u23FB";
    public static final String RESTART = "\u21BB";
    public static final String SHUTDOWN = "\u23FC";
    public static final String TERMINAL = "\u2328";
    public static final String CONSOLE = "\u25A1";

    // ===================== Validation & States =====================
    public static final String YES = CHECKMARK;
    public static final String NO = ERROR;
    public static final String OPTIONAL = "\u25CC";
    public static final String REQUIRED = "\u2731";
    public static final String DISABLED = "\u26AA";
    public static final String ENABLED = "\u26AB";

    // ===================== Sorting Indicators (Alt) =====================
    public static final String CARET_UP = "\u2303";
    public static final String CARET_DOWN = "\u2304";
    public static final String CHEVRON_UP = "\u02C4";
    public static final String CHEVRON_DOWN = "\u02C5";

    // ===================== Currency (Extended) =====================
    public static final String SWISS_FRANC = "\u20A3";
    public static final String RUPEE = "\u20B9";
    public static final String WON = "\u20A9";
    public static final String BITCOIN = "\u20BF";

    // ===================== Legal & Documents =====================
    public static final String COPYRIGHT = "\u00A9";
    public static final String REGISTERED = "\u00AE";
    public static final String TRADEMARK = "\u2122";
    public static final String PARAGRAPH = "\u00A7";

    // ===================== Debug & Developer =====================
    public static final String BUG = "\uD83D\uDC1B";
    public static final String TOOLS = "\u2692";
    public static final String CODE = BETTER_PENCIL;
    public static final String BRACKETS = "\u007B\u007D";

    // ===================== Table/Column Helper =====================
    public static final String COL_ARTIKELNUMMER = ARTIKELNUMMER + " Artikelnummer";
    public static final String COL_NAME = NAME + " Name";
    public static final String COL_KATEGORIE = CATEGORY + " Kategorie";
    public static final String COL_DETAILS = DETAILS + " Details";
    public static final String COL_LAGERBESTAND = STOCK_QUANTITY + " Lagerbestand";
    public static final String COL_MINDESTBESTAND = MIN_STOCK + " Mindestbestand";
    public static final String COL_VERKAUFSPREIS = SELL_PRICE + " Verkaufspreis";
    public static final String COL_EINKAUFSPREIS = PURCHASE_PRICE + " Einkaufspreis";
    public static final String COL_LIEFERANT = SUPPLIER + " Lieferant";
    public static final String COL_BESTELL_MENGE = ORDER_QUANTITY + " Bestell Menge";
    public static final String COL_LAGER_MENGE = STOCK_QUANTITY + " Lager Menge";
    public static final String COL_HINZUGEFUEGT = ADDED + " Hinzugefuegt";
    public static final String COL_STATUS = STATUS + " Status";
    public static final String COL_TYP = TAG + " Typ";
    public static final String COL_TITEL = TITLE + " Titel";
    public static final String COL_NACHRICHT = MESSAGE + " Nachricht";
    public static final String COL_DATUM = CALENDAR + " Datum";
    public static final String COL_ABTEILUNG = DEPARTMENT + " Abteilung";
    public static final String COL_KONTAKT = CONTACT + " Kontakt";
    public static final String COL_TELEFON = PHONE + " Telefon";
    public static final String COL_EMAIL = EMAIL + " Email";
    public static final String COL_ADRESSE = ADDRESS + " Adresse";
    public static final String COL_GELIEFERTE_ARTIKEL = DELIVERED_ARTICLES + " Gelieferte Artikel";
    public static final String COL_MINDESTBESTELLWERT = MIN_ORDER_VALUE + " Mindestbestellwert";

    // ===================== Utility =====================
    public static String getSafeSymbol(String emoji, String fallback) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win") && !os.contains("windows 10") && !os.contains("windows 11")) {
            return fallback;
        }
        return emoji;
    }
}
