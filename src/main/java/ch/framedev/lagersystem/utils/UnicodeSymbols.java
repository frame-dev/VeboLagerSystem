package ch.framedev.lagersystem.utils;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.UIManager;

/**
 * Cross-platform symbols for consistent display on Windows, macOS, and Linux.
 * Uses Unicode escape sequences to keep the source ASCII-friendly.
 */
@SuppressWarnings({"UnnecessaryUnicodeEscape", "unused"})
public class UnicodeSymbols {
    private static class FontSupport {
        private static final String OS_NAME = getOSName();
        private static final boolean WINDOWS = OS_NAME.contains("win");
        private static final boolean MAC = OS_NAME.contains("mac");
        private static final Font UI_FONT = initUiFont();
        private static final Font[] EMOJI_FONTS = initEmojiFonts();
        private static final Font[] FONTS = initFonts();
        private static final Map<String, Boolean> CACHE = new HashMap<>();
    }

    private static String getOSName() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    }

    private static boolean isMac() {
        return FontSupport.MAC;
    }

    private static Font initUiFont() {
        try {
            if (GraphicsEnvironment.isHeadless()) {
                return null;
            }
            Font uiFont = UIManager.getFont("Label.font");
            if (uiFont != null) {
                return uiFont;
            }
        } catch (Exception ignored) {
            // Fall back to a logical font.
        }
        return new Font("Dialog", Font.PLAIN, 12);
    }

    private static Font[] initEmojiFonts() {
        try {
            if (GraphicsEnvironment.isHeadless()) {
                return new Font[0];
            }
            List<Font> fonts = new ArrayList<>();
            String[] candidates;

            if (FontSupport.WINDOWS) {
                candidates = new String[]{"Segoe UI Emoji", "Segoe UI Symbol", "Segoe UI"};
            } else if (FontSupport.MAC) {
                candidates = new String[]{"Apple Color Emoji", "Helvetica", "Arial"};
            } else {
                candidates = new String[]{"Noto Color Emoji", "DejaVu Sans", "Liberation Sans"};
            }

            for (String name : candidates) {
                Font font = new Font(name, Font.PLAIN, 12);
                if (font.getFamily(Locale.ROOT).equalsIgnoreCase(name)) {
                    fonts.add(font);
                }
            }
            return fonts.toArray(new Font[0]);
        } catch (Exception ignored) {
            return new Font[0];
        }
    }

    private static Font[] initFonts() {
        try {
            if (GraphicsEnvironment.isHeadless()) {
                return new Font[0];
            }
            return GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        } catch (Exception ignored) {
            return new Font[0];
        }
    }

    private static boolean canDisplayAll(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }

        // First check UI font
        Font uiFont = FontSupport.UI_FONT;
        if (uiFont != null && uiFont.canDisplayUpTo(text) == -1) {
            return true;
        }

        // Check emoji fonts
        for (Font font : FontSupport.EMOJI_FONTS) {
            if (font.canDisplayUpTo(text) == -1) {
                return true;
            }
        }

        // For macOS, also check if it's a compatible character range
        if (isMac()) {
            if (isBasicMultilingualPlane(text)) {
                return true;
            }
        }

        // Check all available fonts
        for (Font font : FontSupport.FONTS) {
            if (font.canDisplayUpTo(text) == -1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the text is in the Basic Multilingual Plane (BMP).
     * macOS handles BMP characters reliably.
     */
    private static boolean isBasicMultilingualPlane(String text) {
        // In Java, chars are always 16-bit, so we check for surrogate pairs
        // that would indicate higher Unicode planes
        if (text == null || text.isEmpty()) {
            return true;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // Check if it's a high surrogate (indicates a character outside BMP)
            if (Character.isHighSurrogate(c)) {
                return false;
            }
        }
        return true;
    }

    // ===================== UI Elements =====================
    public static final String CLOSE = getSymbol("\u2715", "X");
    public static final String CHECK = getSymbol("\u2713", "OK");
    public static final String CHECKMARK = getSymbol("\u2714", "OK");
    public static final String PLUS = "\u002B";
    public static final String HEAVY_PLUS = getSymbol("\u271A", "+");
    public static final String MINUS = getSymbol("\u2212", "-");
    public static final String MULTIPLY = getSymbol("\u00D7", "x");

    // ===================== Status Icons =====================
    public static final String WARNING = getSymbol("\u26A0", "!");
    public static final String INFO = getSymbol("\u2139", "i");
    public static final String ERROR = getSymbol("\u2716", "X");
    public static final String SUCCESS = getSymbol("\u2713", "OK");
    public static final String BULLET = getSymbol("\u2022", "*");
    public static final String CIRCLE = getSymbol("\u25CF", "o");

    // ===================== Business Icons =====================
    public static final String FOLDER = getSymbol("\uD83D\uDCC1", "DIR");
    public static final String PACKAGE = getSymbol("\uD83D\uDCE6", "PKG");
    public static final String TRUCK = getSymbol("\uD83D\uDE9A", "TRK");
    public static final String EDIT = "\u270E";
    public static final String PENCIL = "\u270F";
    public static final String BETTER_PENCIL = getSymbol("\uD83D\uDCDD", "EDIT");
    public static final String DOCUMENT = getSymbol("\uD83D\uDCC4", "DOC");
    public static final String CLIPBOARD = getSymbol("\uD83D\uDCCB", "CLIP");
    public static final String CHART = getSymbol("\uD83D\uDCCA", "CHRT");
    public static final String MONEY = getSymbol("\uD83D\uDCB0", "$");
    public static final String TAG = getSymbol("\uD83C\uDFF7", "TAG");
    public static final String PERSON = getSymbol("\uD83D\uDC64", "USR");
    public static final String ID = getSymbol("\uD83C\uDD94", "ID");
    public static final String PHONE = getSymbol("\uD83D\uDCF1", "TEL");
    public static final String CLOCK = getSymbol("\uD83D\uDD70", "TIME");
    public static final String CALENDAR = getSymbol("\uD83D\uDCC5", "DATE");
    public static final String TRASH = getSymbol("\uD83D\uDDD1", "DEL");
    public static final String DOWNLOAD = getSymbol("\uD83D\uDCE5", "DL");
    public static final String UPLOAD = getSymbol("\uD83D\uDCE4", "UP");
    public static final String REFRESH = getSymbol("\uD83D\uDD04", "REF");
    public static final String GEAR = "\u2699";
    public static final String BETTER_GEAR = getSymbol("\uD83D\uDEE0", "CFG");
    public static final String SEARCH = getSymbol("\uD83D\uDD0D", "SRCH");
    public static final String PEOPLE = getSymbol("\uD83D\uDC65", "USERS");
    public static final String BUILDING = getSymbol("\uD83C\uDFE2", "BLDG");
    public static final String NUMBERS = getSymbol("\uD83D\uDD22", "#");
    public static final String MEMO = getSymbol("\uD83D\uDCDD", "MEMO");
    public static final String BROOM = getSymbol("\uD83E\uDDF9", "CLR");
    public static final String FLOPPY = getSymbol("\uD83D\uDCBE", "SAVE");
    public static final String LIST = getSymbol("\uD83D\uDCC3", "LIST");
    public static final String CLEAR = getSymbol("\uD83D\uDDD1", "DEL");
    public static final String CLIENT = getSymbol("\uD83D\uDCBC", "CLNT");
    public static final String UPDATE = getSymbol("\uD83D\uDD04", "UPD");
    public static final String DEPARTMENT = getSymbol("\uD83C\uDFE2", "DEPT");
    public static final String SETTINGS = "\u2699";
    public static final String HEALTH = getSymbol("\uD83D\uDC89", "HLTH");
    public static final String CREDIT_CARD = getSymbol("\uD83D\uDCB3", "CARD");
    public static final String CALCULATOR = getSymbol("\uD83D\uDCB1", "CALC");
    public static final String BULB = getSymbol("\uD83D\uDCA1", "TIP");
    public static final String ABC = getSymbol("\uD83D\uDD24", "ABC");
    public static final String COLOR_PALETTE = getSymbol("\uD83C\uDFA8", "COL");
    public static final String WRENCH = getSymbol("\uD83D\uDD27", "TOOL");
    public static final String GLOBE = getSymbol("\uD83C\uDF10", "WEB");
    public static final String DEVELOPER = getSymbol("\uD83D\uDCBB", "DEV");
    public static final String SHOPPING_CART = getSymbol("\uD83D\uDED2", "CART");

    // ===================== Table/Column Icons =====================
    public static final String ARTIKELNUMMER = NUMBERS;
    public static final String ARTICLE_NAME = getSymbol("\uD83D\uDCD6", "NAME");
    public static final String CATEGORY = getSymbol("\uD83D\uDCCF", "CAT");
    public static final String DETAILS = CLIPBOARD;
    public static final String STOCK_QUANTITY = PACKAGE;
    public static final String MIN_STOCK = getSymbol("\uD83D\uDD3A", "MIN");
    public static final String SELL_PRICE = MONEY;
    public static final String PURCHASE_PRICE = getSymbol("\uD83D\uDCB3", "BUY");
    public static final String SUPPLIER = PEOPLE;
    public static final String ADDED = getSymbol("\u2795", "+");
    public static final String ORDER_QUANTITY = CLIPBOARD;
    public static final String ADDED_AT = getSymbol("\u23F0", "TIME");
    public static final String ADDRESS = getSymbol("\uD83C\uDFE0", "ADDR");
    public static final String EMAIL = getSymbol("\uD83D\uDCE7", "MAIL");
    public static final String CONTACT = PERSON;
    public static final String DELIVERED_ARTICLES = PACKAGE;
    public static final String MIN_ORDER_VALUE = getSymbol("\uD83D\uDCB5", "MIN$");
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
    public static final String ARROW_RIGHT = getSymbol("\u2192", ">");
    public static final String ARROW_LEFT = getSymbol("\u2190", "<");
    public static final String ARROW_UP = getSymbol("\u2191", "^");
    public static final String ARROW_DOWN = getSymbol("\u2193", "v");
    public static final String ARROW_RIGHT_HEAVY = getSymbol("\u27A4", ">");
    public static final String PLAY = getSymbol("\u25B6", ">");
    public static final String PAUSE = getSymbol("\u23F8", "||");
    public static final String STOP = getSymbol("\u23F9", "[]");
    public static final String NEXT = getSymbol("\u23ED", ">>");
    public static final String PREVIOUS = getSymbol("\u23EE", "<<");
    public static final String FAST_FORWARD = getSymbol("\u23E9", ">>");
    public static final String REWIND = getSymbol("\u23EA", "<<");

    // ===================== File System & Data =====================
    public static final String FILE = DOCUMENT;
    public static final String FILE_TEXT = LIST;
    public static final String FILE_PDF = getSymbol("\uD83D\uDCD1", "PDF");
    public static final String ARCHIVE = getSymbol("\uD83D\uDDDC", "ZIP");
    public static final String DATABASE = getSymbol("\uD83D\uDDC3", "DB");
    public static final String CLOUD = getSymbol("\u2601", "CLD");
    public static final String CLOUD_UPLOAD = UPLOAD;
    public static final String CLOUD_DOWNLOAD = DOWNLOAD;

    // ===================== Security & Permissions =====================
    public static final String LOCK = getSymbol("\uD83D\uDD12", "LOCK");
    public static final String UNLOCK = getSymbol("\uD83D\uDD13", "UNLK");
    public static final String KEY = getSymbol("\uD83D\uDD11", "KEY");
    public static final String SHIELD = getSymbol("\uD83D\uDEE1", "SEC");
    public static final String WARNING_SHIELD = getSymbol("\u26E8", "!");
    public static final String VERIFIED = getSymbol("\u2705", "OK");
    public static final String BLOCKED = getSymbol("\u26D4", "NO");

    // ===================== User & Roles =====================
    public static final String USER = PERSON;
    public static final String USERS = PEOPLE;
    public static final String ADMIN = getSymbol("\uD83D\uDC51", "ADMIN");
    public static final String GUEST = getSymbol("\uD83D\uDC68", "GUEST");
    public static final String PROFILE = getSymbol("\uD83D\uDC72", "PROF");

    // ===================== Communication =====================
    public static final String MESSAGE = getSymbol("\uD83D\uDCAC", "MSG");
    public static final String CHAT = getSymbol("\uD83D\uDDE8", "CHAT");
    public static final String NOTIFICATION = getSymbol("\uD83D\uDD14", "NOTIF");
    public static final String MUTED = getSymbol("\uD83D\uDD15", "MUTE");
    public static final String SEND = "\u27A1";
    public static final String RECEIVE = "\u2B05";

    // ===================== Sorting & Filtering =====================
    public static final String SORT = getSymbol("\u21C5", "<>");
    public static final String SORT_UP = getSymbol("\u25B2", "^");
    public static final String SORT_DOWN = getSymbol("\u25BC", "v");
    public static final String FILTER = getSymbol("\uD83D\uDD0E", "FILTER");
    public static final String FUNNEL = getSymbol("\uD83D\uDDC4", "FILTER");

    // ===================== Status & Indicators =====================
    public static final String ONLINE = getSymbol("\uD83D\uDFE2", "ON");
    public static final String OFFLINE = getSymbol("\u26AB", "OFF");
    public static final String IN_PROGRESS = getSymbol("\u23F3", "...");
    public static final String COMPLETED = CHECKMARK;
    public static final String FAILED = getSymbol("\u274C", "X");
    public static final String PENDING = getSymbol("\u23F1", "WAIT");

    // ===================== Misc UI Helpers =====================
    public static final String EYE = getSymbol("\uD83D\uDC41", "VIEW");
    public static final String HIDDEN = getSymbol("\uD83D\uDE48", "HIDE");
    public static final String LINK = getSymbol("\uD83D\uDD17", "LINK");
    public static final String UNLINK = getSymbol("\uD83D\uDD18", "UNLNK");
    public static final String PIN = getSymbol("\uD83D\uDCCC", "PIN");
    public static final String LOCATION = getSymbol("\uD83D\uDCCD", "LOC");

    // ===================== Time & History =====================
    public static final String HISTORY = getSymbol("\u23F2", "HIST");
    public static final String TIME = getSymbol("\u23F0", "TIME");
    public static final String CALENDAR_ALT = getSymbol("\uD83D\uDCC6", "DATE");
    public static final String STATUS = WARNING;
    public static final String TITLE = ARTICLE_NAME;

    // ===================== Layout & Separators =====================
    public static final String SEPARATOR_DOT = getSymbol("\u00B7", ".");
    public static final String SEPARATOR_PIPE = "\u2502";
    public static final String SEPARATOR_DASH = "\u2014";
    public static final String SEPARATOR_DOUBLE = "\u2550";
    public static final String SEPARATOR_BULLET = getSymbol("\u2219", "*");

    // ===================== Box Drawing (Tables / Console UI) =====================
    public static final String BOX_HORIZONTAL = getSymbol("\u2500", "-");
    public static final String BOX_VERTICAL = getSymbol("\u2502", "|");
    public static final String BOX_TOP_LEFT = getSymbol("\u250C", "+");
    public static final String BOX_TOP_RIGHT = getSymbol("\u2510", "+");
    public static final String BOX_BOTTOM_LEFT = getSymbol("\u2514", "+");
    public static final String BOX_BOTTOM_RIGHT = getSymbol("\u2518", "+");
    public static final String BOX_CROSS = getSymbol("\u253C", "+");

    // ===================== Progress & Loading =====================
    public static final String PROGRESS_EMPTY = getSymbol("\u25CB", "o");
    public static final String PROGRESS_HALF = getSymbol("\u25D0", "o");
    public static final String PROGRESS_FULL = getSymbol("\u25CF", "o");
    public static final String LOADING = getSymbol("\u231B", "...");
    public static final String SPINNER = getSymbol("\u27F3", "...");

    // ===================== Ratings & Quality =====================
    public static final String STAR_EMPTY = "\u2606";
    public static final String STAR_FILLED = "\u2605";
    public static final String THUMBS_UP = getSymbol("\uD83D\uDC4D", "OK");
    public static final String THUMBS_DOWN = getSymbol("\uD83D\uDC4E", "NO");

    // ===================== System & Power =====================
    public static final String POWER = getSymbol("\u23FB", "PWR");
    public static final String RESTART = getSymbol("\u21BB", "RST");
    public static final String SHUTDOWN = getSymbol("\u23FC", "OFF");
    public static final String TERMINAL = getSymbol("\u2328", "TERM");
    public static final String CONSOLE = getSymbol("\u25A1", "CON");

    // ===================== Validation & States =====================
    public static final String YES = CHECKMARK;
    public static final String NO = ERROR;
    public static final String OPTIONAL = getSymbol("\u25CC", "OPT");
    public static final String REQUIRED = getSymbol("\u2731", "REQ");
    public static final String DISABLED = getSymbol("\u26AA", "OFF");
    public static final String ENABLED = getSymbol("\u26AB", "ON");

    // ===================== Sorting Indicators (Alt) =====================
    public static final String CARET_UP = getSymbol("\u2303", "^");
    public static final String CARET_DOWN = getSymbol("\u2304", "v");
    public static final String CHEVRON_UP = getSymbol("\u02C4", "^");
    public static final String CHEVRON_DOWN = getSymbol("\u02C5", "v");

    // ===================== Currency (Extended) =====================
    public static final String SWISS_FRANC = getSymbol("\u20A3", "CHF");
    public static final String RUPEE = getSymbol("\u20B9", "INR");
    public static final String WON = getSymbol("\u20A9", "KRW");
    public static final String BITCOIN = getSymbol("\u20BF", "BTC");

    // ===================== Legal & Documents =====================
    public static final String COPYRIGHT = "\u00A9";
    public static final String REGISTERED = "\u00AE";
    public static final String TRADEMARK = "\u2122";
    public static final String PARAGRAPH = "\u00A7";

    // ===================== Debug & Developer =====================
    public static final String BUG = getSymbol("\uD83D\uDC1B", "BUG");
    public static final String TOOLS = getSymbol("\u2692", "TOOLS");
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
    /**
     * Gets a symbol that will display on the current platform.
     * Uses emoji on Windows and macOS when font support is available,
     * falls back to ASCII text on other platforms or when emoji unavailable.
     *
     * @param emoji the primary emoji/symbol to use (Windows/macOS/Linux)
     * @param fallback ASCII text fallback (unsupported platforms)
     * @return emoji if supported, otherwise fallback text
     */
    public static String getSymbol(String emoji, String fallback) {
        String safeFallback = fallback == null ? "" : fallback;

        // Null/empty checks
        if (emoji == null || emoji.isEmpty()) {
            return safeFallback;
        }

        // For Windows and macOS, check font support and use emoji if available
        if (FontSupport.WINDOWS || FontSupport.MAC) {
            Boolean cached = FontSupport.CACHE.get(emoji);
            if (cached == null) {
                cached = canDisplayAll(emoji);
                FontSupport.CACHE.put(emoji, cached);
            }
            return cached ? emoji : safeFallback;
        }

        // For Linux and other platforms, try emoji first, fallback to text
        return emoji;
    }

    /**
     * Gets a safe symbol with font-specific rendering support.
     * This method checks if the given font can display the symbol.
     * Use this for UI elements where you control the font directly.
     *
     * @param symbol the emoji/symbol to use
     * @param fallback ASCII text fallback (used on Windows if font doesn't support)
     * @param font the Font to check for symbol support (can be null)
     * @return symbol if supported by font, otherwise fallback text
     */
    public static String safeSymbol(String symbol, String fallback, Font font) {
        if (symbol == null || symbol.isEmpty()) {
            return fallback == null ? "" : fallback;
        }
        
        // On non-Windows platforms, return symbol directly
        if (!FontSupport.WINDOWS) {
            return symbol;
        }
        
        // On Windows, check if font can display the symbol
        if (font == null) {
            return fallback == null ? "" : fallback;
        }
        
        return font.canDisplayUpTo(symbol) == -1 ? symbol : (fallback == null ? "" : fallback);
    }

    /**
     * Legacy method for backward compatibility.
     * Now delegates to getSymbol for consistent behavior.
     *
     * @param emoji the primary emoji/symbol to use
     * @param fallback ASCII text fallback
     * @return safe symbol or fallback
     */
    public static String getSafeSymbol(String emoji, String fallback) {
        return getSymbol(emoji, fallback);
    }

    /**
     * Checks if a string contains surrogate pairs (emoji outside BMP).
     * Surrogate pairs may not render reliably in Swing components on macOS.
     *
     * @param text the text to check
     * @return true if the text contains surrogate pairs
     */
    private static boolean isSurrogatePair(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isHighSurrogate(c) || Character.isLowSurrogate(c)) {
                return true;
            }
        }
        return false;
    }
}
