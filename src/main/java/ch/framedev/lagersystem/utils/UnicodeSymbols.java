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
@SuppressWarnings("UnnecessaryUnicodeEscape")
public class UnicodeSymbols {
    private static class FontSupport {
        private static final boolean WINDOWS = isWindows();
        private static final Font UI_FONT = initUiFont();
        private static final Font[] EMOJI_FONTS = initEmojiFonts();
        private static final Font[] FONTS = initFonts();
        private static final Map<String, Boolean> CACHE = new HashMap<>();
    }

    private static boolean isWindows() {
        String osName = System.getProperty("os.name", "");
        return osName.toLowerCase(Locale.ROOT).contains("win");
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
        if (!FontSupport.WINDOWS) {
            return new Font[0];
        }
        try {
            if (GraphicsEnvironment.isHeadless()) {
                return new Font[0];
            }
            List<Font> fonts = new ArrayList<>();
            String[] candidates = {"Segoe UI Emoji", "Segoe UI Symbol", "Segoe UI"};
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
        Font uiFont = FontSupport.UI_FONT;
        if (uiFont != null && uiFont.canDisplayUpTo(text) == -1) {
            return true;
        }
        for (Font font : FontSupport.EMOJI_FONTS) {
            if (font.canDisplayUpTo(text) == -1) {
                return true;
            }
        }
        if (FontSupport.FONTS.length == 0) {
            return false;
        }
        for (Font font : FontSupport.FONTS) {
            if (font.canDisplayUpTo(text) == -1) {
                return true;
            }
        }
        return false;
    }

    // ===================== UI Elements =====================
    public static final String CLOSE = getSafeSymbol("\u2715", "X");
    public static final String CHECK = getSafeSymbol("\u2713", "OK");
    public static final String CHECKMARK = getSafeSymbol("\u2714", "OK");
    public static final String PLUS = "\u002B";
    public static final String HEAVY_PLUS = getSafeSymbol("\u271A", "+");
    public static final String MINUS = getSafeSymbol("\u2212", "-");
    public static final String MULTIPLY = getSafeSymbol("\u00D7", "x");

    // ===================== Status Icons =====================
    public static final String WARNING = getSafeSymbol("\u26A0", "!");
    public static final String INFO = getSafeSymbol("\u2139", "i");
    public static final String ERROR = getSafeSymbol("\u2716", "X");
    public static final String SUCCESS = getSafeSymbol("\u2713", "OK");
    public static final String BULLET = getSafeSymbol("\u2022", "*");
    public static final String CIRCLE = getSafeSymbol("\u25CF", "o");

    // ===================== Business Icons =====================
    public static final String FOLDER = getSafeSymbol("\uD83D\uDCC1", "DIR");
    public static final String PACKAGE = getSafeSymbol("\uD83D\uDCE6", "PKG");
    public static final String TRUCK = getSafeSymbol("\uD83D\uDE9A", "TRK");
    public static final String EDIT = "\u270E";
    public static final String PENCIL = "\u270F";
    public static final String BETTER_PENCIL = getSafeSymbol("\uD83D\uDCDD", "EDIT");
    public static final String DOCUMENT = getSafeSymbol("\uD83D\uDCC4", "DOC");
    public static final String CLIPBOARD = getSafeSymbol("\uD83D\uDCCB", "CLIP");
    public static final String CHART = getSafeSymbol("\uD83D\uDCCA", "CHRT");
    public static final String MONEY = getSafeSymbol("\uD83D\uDCB0", "$");
    public static final String TAG = getSafeSymbol("\uD83C\uDFF7", "TAG");
    public static final String PERSON = getSafeSymbol("\uD83D\uDC64", "USR");
    public static final String ID = getSafeSymbol("\uD83C\uDD94", "ID");
    public static final String PHONE = getSafeSymbol("\uD83D\uDCF1", "TEL");
    public static final String CLOCK = getSafeSymbol("\uD83D\uDD70", "TIME");
    public static final String CALENDAR = getSafeSymbol("\uD83D\uDCC5", "DATE");
    public static final String TRASH = getSafeSymbol("\uD83D\uDDD1", "DEL");
    public static final String DOWNLOAD = getSafeSymbol("\uD83D\uDCE5", "DL");
    public static final String UPLOAD = getSafeSymbol("\uD83D\uDCE4", "UP");
    public static final String REFRESH = getSafeSymbol("\uD83D\uDD04", "REF");
    public static final String GEAR = "\u2699";
    public static final String BETTER_GEAR = getSafeSymbol("\uD83D\uDEE0", "CFG");
    public static final String SEARCH = getSafeSymbol("\uD83D\uDD0D", "SRCH");
    public static final String PEOPLE = getSafeSymbol("\uD83D\uDC65", "USERS");
    public static final String BUILDING = getSafeSymbol("\uD83C\uDFE2", "BLDG");
    public static final String NUMBERS = getSafeSymbol("\uD83D\uDD22", "#");
    public static final String MEMO = getSafeSymbol("\uD83D\uDCDD", "MEMO");
    public static final String BROOM = getSafeSymbol("\uD83E\uDDF9", "CLR");
    public static final String FLOPPY = getSafeSymbol("\uD83D\uDCBE", "SAVE");
    public static final String LIST = getSafeSymbol("\uD83D\uDCC3", "LIST");
    public static final String CLEAR = getSafeSymbol("\uD83D\uDDD1", "DEL");
    public static final String CLIENT = getSafeSymbol("\uD83D\uDCBC", "CLNT");
    public static final String UPDATE = getSafeSymbol("\uD83D\uDD04", "UPD");
    public static final String DEPARTMENT = getSafeSymbol("\uD83C\uDFE2", "DEPT");
    public static final String SETTINGS = "\u2699";
    public static final String HEALTH = getSafeSymbol("\uD83D\uDC89", "HLTH");
    public static final String CREDIT_CARD = getSafeSymbol("\uD83D\uDCB3", "CARD");
    public static final String CALCULATOR = getSafeSymbol("\uD83D\uDCB1", "CALC");
    public static final String BULB = getSafeSymbol("\uD83D\uDCA1", "TIP");
    public static final String ABC = getSafeSymbol("\uD83D\uDD24", "ABC");
    public static final String COLOR_PALETTE = getSafeSymbol("\uD83C\uDFA8", "COL");
    public static final String WRENCH = getSafeSymbol("\uD83D\uDD27", "TOOL");
    public static final String GLOBE = getSafeSymbol("\uD83C\uDF10", "WEB");
    public static final String DEVELOPER = getSafeSymbol("\uD83D\uDCBB", "DEV");
    public static final String SHOPPING_CART = getSafeSymbol("\uD83D\uDED2", "CART");

    // ===================== Table/Column Icons =====================
    public static final String ARTIKELNUMMER = NUMBERS;
    public static final String ARTICLE_NAME = getSafeSymbol("\uD83D\uDCD6", "NAME");
    public static final String CATEGORY = getSafeSymbol("\uD83D\uDCCF", "CAT");
    public static final String DETAILS = CLIPBOARD;
    public static final String STOCK_QUANTITY = PACKAGE;
    public static final String MIN_STOCK = getSafeSymbol("\uD83D\uDD3A", "MIN");
    public static final String SELL_PRICE = MONEY;
    public static final String PURCHASE_PRICE = getSafeSymbol("\uD83D\uDCB3", "BUY");
    public static final String SUPPLIER = PEOPLE;
    public static final String ADDED = getSafeSymbol("\u2795", "+");
    public static final String ORDER_QUANTITY = CLIPBOARD;
    public static final String ADDED_AT = getSafeSymbol("\u23F0", "TIME");
    public static final String ADDRESS = getSafeSymbol("\uD83C\uDFE0", "ADDR");
    public static final String EMAIL = getSafeSymbol("\uD83D\uDCE7", "MAIL");
    public static final String CONTACT = PERSON;
    public static final String DELIVERED_ARTICLES = PACKAGE;
    public static final String MIN_ORDER_VALUE = getSafeSymbol("\uD83D\uDCB5", "MIN$");
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
    public static final String ARROW_RIGHT = getSafeSymbol("\u2192", ">");
    public static final String ARROW_LEFT = getSafeSymbol("\u2190", "<");
    public static final String ARROW_UP = getSafeSymbol("\u2191", "^");
    public static final String ARROW_DOWN = getSafeSymbol("\u2193", "v");
    public static final String ARROW_RIGHT_HEAVY = getSafeSymbol("\u27A4", ">");
    public static final String PLAY = getSafeSymbol("\u25B6", ">");
    public static final String PAUSE = getSafeSymbol("\u23F8", "||");
    public static final String STOP = getSafeSymbol("\u23F9", "[]");
    public static final String NEXT = getSafeSymbol("\u23ED", ">>");
    public static final String PREVIOUS = getSafeSymbol("\u23EE", "<<");
    public static final String FAST_FORWARD = getSafeSymbol("\u23E9", ">>");
    public static final String REWIND = getSafeSymbol("\u23EA", "<<");

    // ===================== File System & Data =====================
    public static final String FILE = DOCUMENT;
    public static final String FILE_TEXT = LIST;
    public static final String FILE_PDF = getSafeSymbol("\uD83D\uDCD1", "PDF");
    public static final String ARCHIVE = getSafeSymbol("\uD83D\uDDDC", "ZIP");
    public static final String DATABASE = getSafeSymbol("\uD83D\uDDC3", "DB");
    public static final String CLOUD = getSafeSymbol("\u2601", "CLD");
    public static final String CLOUD_UPLOAD = UPLOAD;
    public static final String CLOUD_DOWNLOAD = DOWNLOAD;

    // ===================== Security & Permissions =====================
    public static final String LOCK = getSafeSymbol("\uD83D\uDD12", "LOCK");
    public static final String UNLOCK = getSafeSymbol("\uD83D\uDD13", "UNLK");
    public static final String KEY = getSafeSymbol("\uD83D\uDD11", "KEY");
    public static final String SHIELD = getSafeSymbol("\uD83D\uDEE1", "SEC");
    public static final String WARNING_SHIELD = getSafeSymbol("\u26E8", "!");
    public static final String VERIFIED = getSafeSymbol("\u2705", "OK");
    public static final String BLOCKED = getSafeSymbol("\u26D4", "NO");

    // ===================== User & Roles =====================
    public static final String USER = PERSON;
    public static final String USERS = PEOPLE;
    public static final String ADMIN = getSafeSymbol("\uD83D\uDC51", "ADMIN");
    public static final String GUEST = getSafeSymbol("\uD83D\uDC68", "GUEST");
    public static final String PROFILE = getSafeSymbol("\uD83D\uDC72", "PROF");

    // ===================== Communication =====================
    public static final String MESSAGE = getSafeSymbol("\uD83D\uDCAC", "MSG");
    public static final String CHAT = getSafeSymbol("\uD83D\uDDE8", "CHAT");
    public static final String NOTIFICATION = getSafeSymbol("\uD83D\uDD14", "NOTIF");
    public static final String MUTED = getSafeSymbol("\uD83D\uDD15", "MUTE");
    public static final String SEND = "\u27A1";
    public static final String RECEIVE = "\u2B05";

    // ===================== Sorting & Filtering =====================
    public static final String SORT = getSafeSymbol("\u21C5", "<>");
    public static final String SORT_UP = getSafeSymbol("\u25B2", "^");
    public static final String SORT_DOWN = getSafeSymbol("\u25BC", "v");
    public static final String FILTER = getSafeSymbol("\uD83D\uDD0E", "FILTER");
    public static final String FUNNEL = getSafeSymbol("\uD83D\uDDC4", "FILTER");

    // ===================== Status & Indicators =====================
    public static final String ONLINE = getSafeSymbol("\uD83D\uDFE2", "ON");
    public static final String OFFLINE = getSafeSymbol("\u26AB", "OFF");
    public static final String IN_PROGRESS = getSafeSymbol("\u23F3", "...");
    public static final String COMPLETED = CHECKMARK;
    public static final String FAILED = getSafeSymbol("\u274C", "X");
    public static final String PENDING = getSafeSymbol("\u23F1", "WAIT");

    // ===================== Misc UI Helpers =====================
    public static final String EYE = getSafeSymbol("\uD83D\uDC41", "VIEW");
    public static final String HIDDEN = getSafeSymbol("\uD83D\uDE48", "HIDE");
    public static final String LINK = getSafeSymbol("\uD83D\uDD17", "LINK");
    public static final String UNLINK = getSafeSymbol("\uD83D\uDD18", "UNLNK");
    public static final String PIN = getSafeSymbol("\uD83D\uDCCC", "PIN");
    public static final String LOCATION = getSafeSymbol("\uD83D\uDCCD", "LOC");

    // ===================== Time & History =====================
    public static final String HISTORY = getSafeSymbol("\u23F2", "HIST");
    public static final String TIME = getSafeSymbol("\u23F0", "TIME");
    public static final String CALENDAR_ALT = getSafeSymbol("\uD83D\uDCC6", "DATE");
    public static final String STATUS = WARNING;
    public static final String TITLE = ARTICLE_NAME;

    // ===================== Layout & Separators =====================
    public static final String SEPARATOR_DOT = getSafeSymbol("\u00B7", ".");
    public static final String SEPARATOR_PIPE = "\u2502";
    public static final String SEPARATOR_DASH = "\u2014";
    public static final String SEPARATOR_DOUBLE = "\u2550";
    public static final String SEPARATOR_BULLET = getSafeSymbol("\u2219", "*");

    // ===================== Box Drawing (Tables / Console UI) =====================
    public static final String BOX_HORIZONTAL = getSafeSymbol("\u2500", "-");
    public static final String BOX_VERTICAL = getSafeSymbol("\u2502", "|");
    public static final String BOX_TOP_LEFT = getSafeSymbol("\u250C", "+");
    public static final String BOX_TOP_RIGHT = getSafeSymbol("\u2510", "+");
    public static final String BOX_BOTTOM_LEFT = getSafeSymbol("\u2514", "+");
    public static final String BOX_BOTTOM_RIGHT = getSafeSymbol("\u2518", "+");
    public static final String BOX_CROSS = getSafeSymbol("\u253C", "+");

    // ===================== Progress & Loading =====================
    public static final String PROGRESS_EMPTY = getSafeSymbol("\u25CB", "o");
    public static final String PROGRESS_HALF = getSafeSymbol("\u25D0", "o");
    public static final String PROGRESS_FULL = getSafeSymbol("\u25CF", "o");
    public static final String LOADING = getSafeSymbol("\u231B", "...");
    public static final String SPINNER = getSafeSymbol("\u27F3", "...");

    // ===================== Ratings & Quality =====================
    public static final String STAR_EMPTY = "\u2606";
    public static final String STAR_FILLED = "\u2605";
    public static final String THUMBS_UP = getSafeSymbol("\uD83D\uDC4D", "OK");
    public static final String THUMBS_DOWN = getSafeSymbol("\uD83D\uDC4E", "NO");

    // ===================== System & Power =====================
    public static final String POWER = getSafeSymbol("\u23FB", "PWR");
    public static final String RESTART = getSafeSymbol("\u21BB", "RST");
    public static final String SHUTDOWN = getSafeSymbol("\u23FC", "OFF");
    public static final String TERMINAL = getSafeSymbol("\u2328", "TERM");
    public static final String CONSOLE = getSafeSymbol("\u25A1", "CON");

    // ===================== Validation & States =====================
    public static final String YES = CHECKMARK;
    public static final String NO = ERROR;
    public static final String OPTIONAL = getSafeSymbol("\u25CC", "OPT");
    public static final String REQUIRED = getSafeSymbol("\u2731", "REQ");
    public static final String DISABLED = getSafeSymbol("\u26AA", "OFF");
    public static final String ENABLED = getSafeSymbol("\u26AB", "ON");

    // ===================== Sorting Indicators (Alt) =====================
    public static final String CARET_UP = getSafeSymbol("\u2303", "^");
    public static final String CARET_DOWN = getSafeSymbol("\u2304", "v");
    public static final String CHEVRON_UP = getSafeSymbol("\u02C4", "^");
    public static final String CHEVRON_DOWN = getSafeSymbol("\u02C5", "v");

    // ===================== Currency (Extended) =====================
    public static final String SWISS_FRANC = getSafeSymbol("\u20A3", "CHF");
    public static final String RUPEE = getSafeSymbol("\u20B9", "INR");
    public static final String WON = getSafeSymbol("\u20A9", "KRW");
    public static final String BITCOIN = getSafeSymbol("\u20BF", "BTC");

    // ===================== Legal & Documents =====================
    public static final String COPYRIGHT = "\u00A9";
    public static final String REGISTERED = "\u00AE";
    public static final String TRADEMARK = "\u2122";
    public static final String PARAGRAPH = "\u00A7";

    // ===================== Debug & Developer =====================
    public static final String BUG = getSafeSymbol("\uD83D\uDC1B", "BUG");
    public static final String TOOLS = getSafeSymbol("\u2692", "TOOLS");
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
        String safeFallback = fallback == null ? "" : fallback;
        if (emoji == null || emoji.isEmpty()) {
            return safeFallback;
        }
        Boolean cached = FontSupport.CACHE.get(emoji);
        if (cached == null) {
            cached = canDisplayAll(emoji);
            FontSupport.CACHE.put(emoji, cached);
        }
        return cached ? emoji : safeFallback;
    }
}
