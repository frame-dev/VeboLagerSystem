package ch.framedev.lagersystem.utils;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.UIManager;

/**
 * Provides a set of cross-platform symbols (Unicode and emoji) for use in Swing UIs.
 * This class ensures consistent display on Windows, macOS, and Linux by detecting font support
 * and providing ASCII fallbacks where necessary.
 *
 * <p>Key features:
 * <ul>
 *   <li>A rich set of constants for common UI icons (e.g., save, delete, warning).</li>
 *   <li>Intelligent symbol resolution via {@link #getSymbol(String, String)} which checks if the current
 *       operating system and installed fonts can render a given Unicode symbol.</li>
 *   <li>ASCII fallbacks to ensure that something meaningful is displayed even if the symbol is not supported.</li>
 *   <li>Performance caching for font support checks to minimize overhead.</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * JButton closeButton = new JButton(UnicodeSymbols.CLOSE);
 * JLabel warningLabel = new JLabel(UnicodeSymbols.WARNING + " Please check your input.");
 * }</pre>
 *
 * <p>The Unicode symbols are represented as escape sequences to maintain source code compatibility
 * with ASCII file encodings.
 */
@SuppressWarnings({"UnnecessaryUnicodeEscape"})
public final class UnicodeSymbols {

    private static final Font DEFAULT_UI_FONT = new Font("Dialog", Font.PLAIN, 12);
    private static final Font[] EMPTY_FONTS = new Font[0];
    private static final String VARIATION_SELECTOR_16 = "\uFE0F";
    private static final String ZERO_WIDTH_JOINER = "\u200D";

    private UnicodeSymbols() {
        // utility class
    }

    private static final class FontSupport {
        private static final String OS_NAME = getOSName();
        private static final boolean WINDOWS = OS_NAME.contains("win");
        private static final boolean MAC = OS_NAME.contains("mac");
        private static final boolean LINUX = OS_NAME.contains("nux") || OS_NAME.contains("nix");
        private static final Font UI_FONT = initUiFont();
        private static final Set<String> AVAILABLE_FONT_FAMILIES = initAvailableFontFamilies();
        private static final Font[] RELIABLE_FONTS = initReliableFonts();
        private static volatile Font[] FONTS;
        private static final Object FONTS_LOCK = new Object();
        private static final Map<String, Boolean> CACHE = new ConcurrentHashMap<>();

        private FontSupport() {
        }
    }

    private static String getOSName() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    }

    private static boolean isHeadlessEnvironment() {
        return GraphicsEnvironment.isHeadless();
    }

    private static String normalizeFallback(String fallback) {
        return fallback == null ? "" : fallback;
    }

    private static boolean isAvailableFont(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return FontSupport.AVAILABLE_FONT_FAMILIES.contains(name.toLowerCase(Locale.ROOT));
    }

    private static Font initUiFont() {
        try {
            if (isHeadlessEnvironment()) {
                return null;
            }
            Font uiFont = UIManager.getFont("Label.font");
            if (uiFont != null) {
                return uiFont;
            }
        } catch (Exception ignored) {
            // Fall back to a logical font.
        }
        return DEFAULT_UI_FONT;
    }

    private static Set<String> initAvailableFontFamilies() {
        Set<String> familyNames = new HashSet<>();
        try {
            if (isHeadlessEnvironment()) {
                return familyNames;
            }
            for (String familyName : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ROOT)) {
                if (familyName != null && !familyName.isBlank()) {
                    familyNames.add(familyName.toLowerCase(Locale.ROOT));
                }
            }
        } catch (Exception ignored) {
            // Keep the set empty and fall back gracefully.
        }
        return familyNames;
    }

    private static Font[] initReliableFonts() {
        try {
            if (isHeadlessEnvironment()) {
                return EMPTY_FONTS;
            }

            List<Font> fonts = new ArrayList<>();
            addFontIfPresent(fonts, FontSupport.UI_FONT);

            for (String logicalName : new String[]{"Dialog", "SansSerif", "Serif", "Monospaced"}) {
                addFontIfPresent(fonts, new Font(logicalName, Font.PLAIN, 12));
            }

            String[] platformCandidates;
            if (FontSupport.WINDOWS) {
                platformCandidates = new String[]{
                        "Segoe UI Emoji",
                        "Segoe UI Symbol",
                        "Segoe UI",
                        "Arial Unicode MS"
                };
            } else if (FontSupport.MAC) {
                platformCandidates = new String[]{
                        "Apple Color Emoji",
                        "SF Pro Text",
                        "Helvetica Neue",
                        "Helvetica",
                        "Arial Unicode MS"
                };
            } else if (FontSupport.LINUX) {
                platformCandidates = new String[]{
                        "Noto Color Emoji",
                        "Noto Emoji",
                        "Noto Sans Symbols 2",
                        "Noto Sans Symbols",
                        "Noto Sans",
                        "DejaVu Sans",
                        "Liberation Sans"
                };
            } else {
                platformCandidates = new String[]{"Dialog", "SansSerif"};
            }

            for (String name : platformCandidates) {
                if (isAvailableFont(name)) {
                    addFontIfPresent(fonts, new Font(name, Font.PLAIN, 12));
                }
            }

            return fonts.isEmpty() ? EMPTY_FONTS : fonts.toArray(EMPTY_FONTS);
        } catch (Exception ignored) {
            return EMPTY_FONTS;
        }
    }

    private static void addFontIfPresent(List<Font> fonts, Font font) {
        if (font == null) {
            return;
        }

        String family = font.getFamily(Locale.ROOT);
        for (Font existing : fonts) {
            if (existing.getFamily(Locale.ROOT).equalsIgnoreCase(family)) {
                return;
            }
        }
        fonts.add(font);
    }

    private static Font[] getAllFonts() {
        Font[] fonts = FontSupport.FONTS;
        if (fonts != null) {
            return fonts;
        }
        synchronized (FontSupport.FONTS_LOCK) {
            fonts = FontSupport.FONTS;
            if (fonts != null) {
                return fonts;
            }
            try {
                if (isHeadlessEnvironment()) {
                    fonts = EMPTY_FONTS;
                } else {
                    fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
                }
            } catch (Exception ignored) {
                fonts = EMPTY_FONTS;
            }
            FontSupport.FONTS = fonts;
            return fonts;
        }
    }

    private static boolean isEmojiLike(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        if (text.contains(VARIATION_SELECTOR_16) || text.contains(ZERO_WIDTH_JOINER)) {
            return true;
        }

        return text.codePoints().anyMatch(codePoint -> codePoint > Character.MAX_VALUE);
    }

    private static boolean canDisplayWithFonts(String text, Font[] fonts) {
        for (Font font : fonts) {
            if (font != null && font.canDisplayUpTo(text) == -1) {
                return true;
            }
        }
        return false;
    }

    private static boolean canDisplayAll(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }

        String normalized = text.replace(VARIATION_SELECTOR_16, "");
        if (normalized.isEmpty()) {
            normalized = text;
        }

        if (canDisplayWithFonts(normalized, FontSupport.RELIABLE_FONTS)) {
            return true;
        }

        if (isEmojiLike(normalized)) {
            return false;
        }

        for (Font font : getAllFonts()) {
            if (font != null && font.canDisplayUpTo(normalized) == -1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether the current runtime is Windows.
     *
     * @return true if running on Windows
     */
    public static boolean isWindows() {
        return FontSupport.WINDOWS;
    }

    /**
     * Returns whether the current runtime is macOS.
     *
     * @return true if running on macOS
     */
    public static boolean isMac() {
        return FontSupport.MAC;
    }

    /**
     * Returns whether the current runtime is Linux/Unix-like.
     *
     * @return true if running on Linux
     */
    public static boolean isLinux() {
        return FontSupport.LINUX;
    }


    // ===================== UI Elements =====================
    /** A symbol for closing UI elements, typically a cross or 'X'. Unicode: U+2715. */
    public static final String CLOSE = getSymbol("\u2715", "X");
    /** A checkmark symbol, often used for confirmation. Unicode: U+2713. */
    public static final String CHECK = getSymbol("\u2713", "OK");
    /** A heavier checkmark symbol. Unicode: U+2714. */
    public static final String CHECKMARK = getSymbol("\u2714", "OK");
    /** A plus sign, for additions. Unicode: U+002B. */
    public static final String PLUS = "\u002B";
    /** A heavy plus sign symbol. Unicode: U+271A. */
    public static final String HEAVY_PLUS = getSymbol("\u271A", "+");
    /** A minus sign, for subtractions or removals. Unicode: U+2212. */
    public static final String MINUS = getSymbol("\u2212", "-");
    /** A multiplication symbol (cross). Unicode: U+00D7. */
    public static final String MULTIPLY = getSymbol("\u00D7", "x");

    // ===================== Status Icons =====================
    /** A warning symbol, typically a triangle with an exclamation mark. Unicode: U+26A0. */
    public static final String WARNING = getSymbol("\u26A0", "!");
    /** An information symbol. Unicode: U+2139. */
    public static final String INFO = getSymbol("\u2139", "i");
    /** An error symbol, often a cross. Unicode: U+2716. */
    public static final String ERROR = getSymbol("\u2716", "X");
    /** A success symbol, typically a checkmark. Unicode: U+2713. */
    public static final String SUCCESS = getSymbol("\u2713", "OK");
    /** A bullet point symbol. Unicode: U+2022. */
    public static final String BULLET = getSymbol("\u2022", "*");
    /** A solid circle symbol. Unicode: U+25CF. */
    public static final String CIRCLE = getSymbol("\u25CF", "o");

    // ===================== Business Icons =====================
    /** A folder icon. Unicode: U+D83D U+DCC1. */
    public static final String FOLDER = getSymbol("DIR", "\u25A4", "\uD83D\uDCC1");
    /** A package or box icon. Unicode: U+D83D U+DCE6. */
    public static final String PACKAGE = getSymbol("PKG", "\u25A3", "\uD83D\uDCE6");
    /** A truck icon, for deliveries or logistics. Unicode: U+D83D U+DE9A. */
    public static final String TRUCK = getSymbol("TRK", "\u21E2", "\uD83D\uDE9A");
    /** An edit symbol (pen). Unicode: U+270E. */
    public static final String EDIT = "\u270E";
    /** A pencil icon. Unicode: U+270F. */
    public static final String PENCIL = "\u270F";
    /** A more detailed pencil icon for editing. Unicode: U+D83D U+DCDD. */
    public static final String BETTER_PENCIL = getSymbol("EDIT", "\u270E", "\uD83D\uDCDD");
    /** A document icon. Unicode: U+D83D U+DCC4. */
    public static final String DOCUMENT = getSymbol("DOC", "\u2630", "\uD83D\uDCC4");
    /** A clipboard icon. Unicode: U+D83D U+DCCB. */
    public static final String CLIPBOARD = getSymbol("CLIP", "\u2630", "\uD83D\uDCCB");
    /** A chart or graph icon. Unicode: U+D83D U+DCCA. */
    public static final String CHART = getSymbol("CHRT", "\u2263", "\uD83D\uDCCA");
    /** A money bag or currency symbol. Unicode: U+D83D U+DCB0. */
    public static final String MONEY = getSymbol("$", "\u00A4", "\uD83D\uDCB0");
    /** A price tag icon. Unicode: U+D83C U+DFF7. */
    public static final String TAG = getSymbol("TAG", "#", "\uD83C\uDFF7");
    /** A person or user icon. Unicode: U+D83D U+DC64. */
    public static final String PERSON = getSymbol("USR", "\u263A", "\uD83D\uDC64");
    /** An ID card or identifier symbol. Unicode: U+D83C U+DD94. */
    public static final String ID = getSymbol("ID", "\u2116", "\uD83C\uDD94");
    /** A mobile phone icon. Unicode: U+D83D U+DCF1. */
    public static final String PHONE = getSymbol("TEL", "\u260E", "\uD83D\uDCF1");
    /** A clock icon. Unicode: U+D83D U+DD70. */
    public static final String CLOCK = getSymbol("TIME", "\u23F0", "\uD83D\uDD70");
    /** A calendar icon. Unicode: U+D83D U+DCC5. */
    public static final String CALENDAR = getSymbol("DATE", "\u25A6", "\uD83D\uDCC5");
    /** A trash can icon for deletion. Unicode: U+D83D U+DDD1. */
    public static final String TRASH = getSymbol("DEL", "\u232B", "\uD83D\uDDD1");
    /** A download icon (arrow pointing down). Unicode: U+D83D U+DCE5. */
    public static final String DOWNLOAD = getSymbol("DL", "\u2B07", "\uD83D\uDCE5");
    /** An upload icon (arrow pointing up). Unicode: U+D83D U+DCE4. */
    public static final String UPLOAD = getSymbol("UP", "\u2B06", "\uD83D\uDCE4");
    /** A refresh or reload icon. Unicode: U+D83D U+DD04. */
    public static final String REFRESH = getSymbol("REF", "\u21BB", "\uD83D\uDD04");
    /** A gear icon for settings. Unicode: U+2699. */
    public static final String GEAR = "\u2699";
    /** A more detailed gear icon for configuration. Unicode: U+D83D U+DEE0. */
    public static final String BETTER_GEAR = getSymbol("CFG", "\u2699", "\uD83D\uDEE0");
    /** A magnifying glass for search. Unicode: U+D83D U+DD0D. */
    public static final String SEARCH = getSymbol("SRCH", "\u2315", "\uD83D\uDD0D");
    /** A group of people or users. Unicode: U+D83D U+DC65. */
    public static final String PEOPLE = getSymbol("USERS", "\u263A", "\uD83D\uDC65");
    /** A building or office icon. Unicode: U+D83C U+DFE2. */
    public static final String BUILDING = getSymbol("BLDG", "\u2302", "\uD83C\uDFE2");
    /** A numbers or digits icon (1234). Unicode: U+D83D U+DD22. */
    public static final String NUMBERS = getSymbol("#", "\u2116", "\uD83D\uDD22");
    /** A memo or note icon. Unicode: U+D83D U+DCDD. */
    public static final String MEMO = getSymbol("MEMO", "\u270E", "\uD83D\uDCDD");
    /** A broom icon for clearing or cleaning. Unicode: U+D83E U+DDF9. */
    public static final String BROOM = getSymbol("CLR", "\u232B", "\uD83E\uDDF9");
    /** A floppy disk icon for saving. Unicode: U+D83D U+DCBE. */
    public static final String FLOPPY = getSymbol("SAVE", "\u25A3", "\uD83D\uDCBE");
    /** A list or document with lines. Unicode: U+D83D U+DCC3. */
    public static final String LIST = getSymbol("LIST", "\u2630", "\uD83D\uDCC3");
    /** A clear or delete symbol. Unicode: U+D83D U+DDD1. */
    public static final String CLEAR = getSymbol("DEL", "\u2715", "\u232B", "\uD83D\uDDD1");
    /** A client or briefcase icon. Unicode: U+D83D U+DCBC. */
    public static final String CLIENT = getSymbol("CLNT", "\u263A", "\uD83D\uDCBC");
    /** An update or refresh symbol. Unicode: U+D83D U+DD04. */
    public static final String UPDATE = getSymbol("UPD", "\u21BB", "\uD83D\uDD04");
    /** A department or building icon. Unicode: U+D83C U+DFE2. */
    public static final String DEPARTMENT = getSymbol("DEPT", "\u2302", "\u25A6", "\uD83C\uDFE2");
    /** A settings gear icon. Unicode: U+2699. */
    public static final String SETTINGS = "\u2699";
    /** A health or medical symbol (syringe). Unicode: U+D83D U+DC89. */
    public static final String HEALTH = getSymbol("HLTH", "\u2695", "\uD83D\uDC89");
    /** A credit card icon. Unicode: U+D83D U+DCB3. */
    public static final String CREDIT_CARD = getSymbol("CARD", "\u00A4", "\uD83D\uDCB3");
    /** A calculator icon. Unicode: U+D83D U+DCB1. */
    public static final String CALCULATOR = getSymbol("CALC", "\u2211", "\uD83D\uDCB1");
    /** A light bulb icon for tips or ideas. Unicode: U+D83D U+DCA1. */
    public static final String BULB = getSymbol("TIP", "\u2600", "\uD83D\uDCA1");
    /** The letters 'ABC' in a block. Unicode: U+D83D U+DD24. */
    public static final String ABC = "ABC";
    /** A color palette icon. Unicode: U+D83C U+DFA8. */
    public static final String COLOR_PALETTE = getSymbol("COL", "\u25A7", "\uD83C\uDFA8");
    /** A wrench icon for tools or utilities. Unicode: U+D83D U+DD27. */
    public static final String WRENCH = getSymbol("TOOL", "\u2692", "\uD83D\uDD27");
    /** A globe icon for web or internationalization. Unicode: U+D83C U+DF10. */
    public static final String GLOBE = getSymbol("WEB", "\u2295", "\uD83C\uDF10");
    /** A computer or developer icon. Unicode: U+D83D U+DCBB. */
    public static final String DEVELOPER = getSymbol("DEV", "\u2328", "\uD83D\uDCBB");
    /** A shopping cart icon. Unicode: U+D83D U+DED2. */
    public static final String SHOPPING_CART = getSymbol("CART", "\u229E", "\uD83D\uDED2");
    /** A laptop or computer icon. Unicode: U+D83D U+DCBB. */
    public static final String LAPTOP = getSymbol("LAP", "\u2328", "\uD83D\uDCBB");
    /** A monitor icon. Unicode: U+D83D U+DCFA. */
    public static final String MONITOR = getSymbol("MON", "\u25A1", "\uD83D\uDCFA");
    /** An experiment icon. Unicode: U+D83D U+DCAD. */
    public static final String EXPERIMENT = getSymbol("EXP", "\u2736", "\uD83D\uDCAD");
    /** A test tube icon. Unicode: U+D83E U+DD2C. */
    public static final String TEST_TUBE = getSymbol("TEST", "\u2697", "\uD83E\uDD2C");
    /** A beta icon. Unicode: U+1F171. */
    public static final String BETA = getSymbol("BETA", "\u03B2", "\uD83C\uDD71");
    /** An alpha icon. Unicode: U+1F170. */
    public static final String ALPHA = getSymbol("ALPHA", "\u03B1", "\uD83C\uDD70");
    /** A box icon. Unicode: U+1F4E6. */
    public static final String BOX = getSymbol("BOX", "\u25A3", "\uD83D\uDCE6");
    /** A test icon. Unicode: U+1F4DD. */
    public static final String TEST = getSymbol("TEST", "\u270E", "\uD83D\uDCDD");
    // ===================== Table/Column Icons =====================
    /** An icon for an article number column. */
    public static final String ARTIKELNUMMER = NUMBERS;
    /** An icon for an article name column. Unicode: U+D83D U+DCD6. */
    public static final String ARTICLE_NAME = getSymbol("NAME", "\u2630", "\uD83D\uDCD6");
    /** An icon for a category column. Unicode: U+D83D U+DCCF. */
    public static final String CATEGORY = getSymbol("CAT", "\u25A6", "\uD83D\uDCCF");
    /** An icon for a details column. */
    public static final String DETAILS = CLIPBOARD;
    /** An icon for a stock quantity column. */
    public static final String STOCK_QUANTITY = PACKAGE;
    /** An icon for a minimum stock column. Unicode: U+D83D U+DD3A. */
    public static final String MIN_STOCK = getSymbol("MIN", "\u25B3", "\uD83D\uDD3A");
    /** An icon for a sales price column. */
    public static final String SELL_PRICE = MONEY;
    /** An icon for a purchase price column. Unicode: U+D83D U+DCB3. */
    public static final String PURCHASE_PRICE = getSymbol("BUY", "\u00A4", "\uD83D\uDCB3");
    /** An icon for a supplier column. */
    public static final String SUPPLIER = PEOPLE;
    /** An icon for adding a quantity. Unicode: U+2795. */
    public static final String ADDED = getSymbol("\u2795", "+");
    /** An icon for an order quantity column. */
    public static final String ORDER_QUANTITY = CLIPBOARD;
    /** An icon representing the time something was added. Unicode: U+23F0. */
    public static final String ADDED_AT = getSymbol("TIME", "\u23F0", "\uD83D\uDD70");
    /** An icon for an address column. Unicode: U+D83C U+DFE0. */
    public static final String ADDRESS = getSymbol("ADDR", "\u2302", "\uD83C\uDFE0");
    /** An icon for an email column. Unicode: U+D83D U+DCE7. */
    public static final String EMAIL = getSymbol("MAIL", "\u2709", "\uD83D\uDCE7");
    /** An icon for a contact person column. */
    public static final String CONTACT = PERSON;
    /** An icon for delivered articles. */
    public static final String DELIVERED_ARTICLES = PACKAGE;
    /** An icon for a minimum order value column. Unicode: U+D83D U+DCB5. */
    public static final String MIN_ORDER_VALUE = getSymbol("MIN$", "\u00A4", "\uD83D\uDCB5");
    /** An icon for a vendor column. */
    public static final String VENDOR = PEOPLE;
    /** An icon for a supplier order. */
    public static final String SUPPLIER_ORDER = PACKAGE;
    /** An icon indicating something was delivered. */
    public static final String DELIVERED = CHECKMARK;
    /** An icon for a quantity column. */
    public static final String QUANTITY = CLIPBOARD;
    /** An icon for a name column. */
    public static final String NAME = ARTICLE_NAME;
    /** An alternative icon for an email column. */
    public static final String EMAIL_ALT = EMAIL;
    /** An alternative icon for a phone column. */
    public static final String PHONE_ALT = PHONE;
    /** An alternative icon for an address column. */
    public static final String ADDRESS_ALT = ADDRESS;
    /** An icon for a minimum order column. */
    public static final String MIN_ORDER = MIN_ORDER_VALUE;
    /** An icon for delivered items. */
    public static final String DELIVERED_ITEMS = PACKAGE;
    /** An alternative icon for a category column. */
    public static final String CATEGORY_ALT = CATEGORY;
    /** An alternative icon for a supplier column. */
    public static final String SUPPLIER_ALT = SUPPLIER;
    /** An icon for ordered items. */
    public static final String ORDERED = PACKAGE;

    // ===================== Navigation & Media =====================
    /** A right-pointing arrow. Unicode: U+2192. */
    public static final String ARROW_RIGHT = getSymbol("\u2192", ">");
    /** A left-pointing arrow. Unicode: U+2190. */
    public static final String ARROW_LEFT = getSymbol("\u2190", "<");
    /** An upward-pointing arrow. Unicode: U+2191. */
    public static final String ARROW_UP = getSymbol("\u2191", "^");
    /** A downward-pointing arrow. Unicode: U+2193. */
    public static final String ARROW_DOWN = getSymbol("\u2193", "v");
    /** A heavy right-pointing arrow. Unicode: U+27A4. */
    public static final String ARROW_RIGHT_HEAVY = getSymbol("\u27A4", ">");
    /** A play symbol (triangle). Unicode: U+25B6. */
    public static final String PLAY = getSymbol("\u25B6", ">");
    /** A pause symbol (two vertical bars). Unicode: U+23F8. */
    public static final String PAUSE = getSymbol("\u23F8", "||");
    /** A stop symbol (square). Unicode: U+23F9. */
    public static final String STOP = getSymbol("\u23F9", "[]");
    /** A next track symbol. Unicode: U+23ED. */
    public static final String NEXT = getSymbol("\u23ED", ">>");
    /** A previous track symbol. Unicode: U+23EE. */
    public static final String PREVIOUS = getSymbol("\u23EE", "<<");
    /** A fast-forward symbol. Unicode: U+23E9. */
    public static final String FAST_FORWARD = getSymbol("\u23E9", ">>");
    /** A rewind symbol. Unicode: U+23EA. */
    public static final String REWIND = getSymbol("\u23EA", "<<");

    // ===================== File System & Data =====================
    /** A generic file icon. */
    public static final String FILE = DOCUMENT;
    /** A text file icon. */
    public static final String FILE_TEXT = LIST;
    /** A PDF file icon. Unicode: U+D83D U+DCD1. */
    public static final String FILE_PDF = getSymbol("PDF", "\u25A4", "\uD83D\uDCD1");
    /** A file archive (zip) icon. Unicode: U+D83D U+DDDC. */
    public static final String ARCHIVE = getSymbol("ZIP", "\u25A6", "\uD83D\uDDDC");
    /** A database icon. Unicode: U+D83D U+DDC3. */
    public static final String DATABASE = getSymbol("DB", "\u25A6", "\uD83D\uDDC3");
    /** A cloud icon. Unicode: U+2601. */
    public static final String CLOUD = getSymbol("\u2601", "CLD");
    /** A cloud upload icon. */
    public static final String CLOUD_UPLOAD = UPLOAD;
    /** A cloud download icon. */
    public static final String CLOUD_DOWNLOAD = DOWNLOAD;

    // ===================== Security & Permissions =====================
    /** A lock icon (locked). Unicode: U+D83D U+DD12. */
    public static final String LOCK = getSymbol("LOCK", "\u26BF", "\uD83D\uDD12");
    /** An unlock icon (unlocked). Unicode: U+D83D U+DD13. */
    public static final String UNLOCK = getSymbol("UNLK", "\u26BF", "\uD83D\uDD13");
    /** A key icon. Unicode: U+D83D U+DD11. */
    public static final String KEY = getSymbol("KEY", "\u26BF", "\uD83D\uDD11");
    /** A shield icon for security. Unicode: U+D83D U+DEE1. */
    public static final String SHIELD = getSymbol("SEC", "\u26E8", "\uD83D\uDEE1");
    /** A shield with a warning symbol. Unicode: U+26E8. */
    public static final String WARNING_SHIELD = getSymbol("\u26E8", "!");
    /** A verification or success checkmark in a box. Unicode: U+2705. */
    public static final String VERIFIED = getSymbol("OK", "\u2714", "\u2705");
    /** A blocked or no-entry symbol. Unicode: U+26D4. */
    public static final String BLOCKED = getSymbol("\u26D4", "NO");

    // ===================== User & Roles =====================
    /** A single user icon. */
    public static final String USER = PERSON;
    /** A multiple users icon. */
    public static final String USERS = PEOPLE;
    /** An administrator icon (crown). Unicode: U+D83D U+DC51. */
    public static final String ADMIN = getSymbol("ADMIN", "\u2605", "\uD83D\uDC51");
    /** A guest user icon. Unicode: U+D83D U+DC68. */
    public static final String GUEST = getSymbol("GUEST", "\u263A", "\uD83D\uDC68");
    /** A user profile icon. Unicode: U+D83D U+DC72. */
    public static final String PROFILE = getSymbol("PROF", "\u263A", "\uD83D\uDC72");

    // ===================== Communication =====================
    /** A message or speech bubble icon. Unicode: U+D83D U+DCAC. */
    public static final String MESSAGE = getSymbol("MSG", "\u2709", "\uD83D\uDCAC");
    /** A chat or multiple messages icon. Unicode: U+D83D U+DDE8. */
    public static final String CHAT = getSymbol("CHAT", "\u2709", "\uD83D\uDDE8");
    /** A notification bell icon. Unicode: U+D83D U+DD14. */
    public static final String NOTIFICATION = getSymbol("NOTIF", "\u266A", "\uD83D\uDD14");
    /** A muted or silent notification bell. Unicode: U+D83D U+DD15. */
    public static final String MUTED = getSymbol("MUTE", "\u266B", "\uD83D\uDD15");
    /** A send icon (paper airplane). Unicode: U+27A1. */
    public static final String SEND = "\u27A1";
    /** A receive icon (arrow pointing left). Unicode: U+2B05. */
    public static final String RECEIVE = "\u2B05";

    // ===================== Sorting & Filtering =====================
    /** A sort icon (up and down arrows). Unicode: U+21C5. */
    public static final String SORT = getSymbol("\u21C5", "<>");
    /** A sort-up icon (upward triangle). Unicode: U+25B2. */
    public static final String SORT_UP = getSymbol("\u25B2", "^");
    /** A sort-down icon (downward triangle). Unicode: U+25BC. */
    public static final String SORT_DOWN = getSymbol("\u25BC", "v");
    /** A filter icon (magnifying glass with funnel). Unicode: U+D83D U+DD0E. */
    public static final String FILTER = getSymbol("FILTER", "\u2315", "\uD83D\uDD0E");
    /** A funnel icon for filtering. Unicode: U+D83D U+DDC4. */
    public static final String FUNNEL = getSymbol("FILTER", "\u2301", "\uD83D\uDDC4");

    // ===================== Status & Indicators =====================
    /** An online or active status indicator (green circle). Unicode: U+D83D U+DFE2. */
    public static final String ONLINE = getSymbol("ON", "\u25CF", "\uD83D\uDFE2");
    /** An offline or inactive status indicator (black circle). Unicode: U+26AB. */
    public static final String OFFLINE = getSymbol("\u26AB", "OFF");
    /** An in-progress or loading indicator (hourglass). Unicode: U+23F3. */
    public static final String IN_PROGRESS = getSymbol("\u23F3", "...");
    /** A completed status indicator. */
    public static final String COMPLETED = CHECKMARK;
    /** A failed status indicator (red 'X'). Unicode: U+274C. */
    public static final String FAILED = getSymbol("\u274C", "X");
    /** A pending or waiting indicator (hourglass). Unicode: U+23F1. */
    public static final String PENDING = getSymbol("\u23F1", "WAIT");

    // ===================== Misc UI Helpers =====================
    /** An eye icon for viewing or showing. Unicode: U+D83D U+DC41. */
    public static final String EYE = getSymbol("VIEW", "\u25C9", "\uD83D\uDC41");
    /** An icon for hidden or redacted content. Unicode: U+D83D U+DE48. */
    public static final String HIDDEN = getSymbol("HIDE", "\u25CC", "\uD83D\uDE48");
    /** A link icon. Unicode: U+D83D U+DD17. */
    public static final String LINK = getSymbol("LINK", "\u21C4", "\uD83D\uDD17");
    /** An unlink icon. Unicode: U+D83D U+DD18. */
    public static final String UNLINK = getSymbol("UNLNK", "\u21AE", "\uD83D\uDD18");
    /** A pin icon. Unicode: U+D83D U+DCCC. */
    public static final String PIN = getSymbol("PIN", "\u2022", "\uD83D\uDCCC");
    /** A location or map marker icon. Unicode: U+D83D U+DCCD. */
    public static final String LOCATION = getSymbol("LOC", "\u2316", "\uD83D\uDCCD");

    // ===================== Time & History =====================
    /** A history or clock icon. Unicode: U+23F2. */
    public static final String HISTORY = getSymbol("\u23F2", "HIST");
    /** A time or clock icon. Unicode: U+23F0. */
    public static final String TIME = getSymbol("TIME", "\u23F0", "\uD83D\uDD70");
    /** An alternative calendar icon. Unicode: U+D83D U+DCC6. */
    public static final String CALENDAR_ALT = getSymbol("DATE", "\u25A6", "\uD83D\uDCC6");
    /** A status icon. */
    public static final String STATUS = WARNING;
    /** A title icon. */
    public static final String TITLE = ARTICLE_NAME;

    // ===================== Layout & Separators =====================
    /** A separator dot. Unicode: U+00B7. */
    public static final String SEPARATOR_DOT = getSymbol("\u00B7", ".");
    /** A pipe separator. Unicode: U+2502. */
    public static final String SEPARATOR_PIPE = "\u2502";
    /** A dash separator. Unicode: U+2014. */
    public static final String SEPARATOR_DASH = "\u2014";
    /** A double-line separator. Unicode: U+2550. */
    public static final String SEPARATOR_DOUBLE = "\u2550";
    /** A bullet separator. Unicode: U+2219. */
    public static final String SEPARATOR_BULLET = getSymbol("\u2219", "*");

    // ===================== Box Drawing (Tables / Console UI) =====================
    /** A horizontal box drawing line. Unicode: U+2500. */
    public static final String BOX_HORIZONTAL = getSymbol("\u2500", "-");
    /** A vertical box drawing line. Unicode: U+2502. */
    public static final String BOX_VERTICAL = getSymbol("\u2502", "|");
    /** A top-left corner for box drawing. Unicode: U+250C. */
    public static final String BOX_TOP_LEFT = getSymbol("\u250C", "+");
    /** A top-right corner for box drawing. Unicode: U+2510. */
    public static final String BOX_TOP_RIGHT = getSymbol("\u2510", "+");
    /** A bottom-left corner for box drawing. Unicode: U+2514. */
    public static final String BOX_BOTTOM_LEFT = getSymbol("\u2514", "+");
    /** A bottom-right corner for box drawing. Unicode: U+2518. */
    public static final String BOX_BOTTOM_RIGHT = getSymbol("\u2518", "+");
    /** A cross intersection for box drawing. Unicode: U+253C. */
    public static final String BOX_CROSS = getSymbol("\u253C", "+");

    // ===================== Progress & Loading =====================
    /** An empty circle for progress indicators. Unicode: U+25CB. */
    public static final String PROGRESS_EMPTY = getSymbol("\u25CB", "o");
    /** A half-filled circle for progress indicators. Unicode: U+25D0. */
    public static final String PROGRESS_HALF = getSymbol("\u25D0", "o");
    /** A fully-filled circle for progress indicators. Unicode: U+25CF. */
    public static final String PROGRESS_FULL = getSymbol("\u25CF", "o");
    /** A loading symbol (hourglass). Unicode: U+231B. */
    public static final String LOADING = getSymbol("\u231B", "...");
    /** A spinner icon for loading. Unicode: U+27F3. */
    public static final String SPINNER = getSymbol("\u27F3", "...");

    // ===================== Ratings & Quality =====================
    /** An empty star for ratings. Unicode: U+2606. */
    public static final String STAR_EMPTY = "\u2606";
    /** A filled star for ratings. Unicode: U+2605. */
    public static final String STAR_FILLED = "\u2605";
    /** A thumbs-up icon. Unicode: U+D83D U+DC4D. */
    public static final String THUMBS_UP = getSymbol("OK", "\u2713", "\uD83D\uDC4D");
    /** A thumbs-down icon. Unicode: U+D83D U+DC4E. */
    public static final String THUMBS_DOWN = getSymbol("NO", "\u2716", "\uD83D\uDC4E");

    // ===================== System & Power =====================
    /** A power symbol. Unicode: U+23FB. */
    public static final String POWER = getSymbol("\u23FB", "PWR");
    /** A restart symbol. Unicode: U+21BB. */
    public static final String RESTART = getSymbol("RST", "\u21BB", "\uD83D\uDD04");
    /** A shutdown symbol. Unicode: U+23FC. */
    public static final String SHUTDOWN = getSymbol("OFF", "\u23FB", "\u23FC");
    /** A terminal or command line icon. Unicode: U+2328. */
    public static final String TERMINAL = getSymbol("\u2328", "TERM");
    /** A console or empty square icon. Unicode: U+25A1. */
    public static final String CONSOLE = getSymbol("\u25A1", "CON");

    // ===================== Validation & States =====================
    /** A 'yes' or confirmation symbol. */
    public static final String YES = CHECKMARK;
    /** A 'no' or error symbol. */
    public static final String NO = ERROR;
    /** An optional field indicator. Unicode: U+25CC. */
    public static final String OPTIONAL = getSymbol("\u25CC", "OPT");
    /** A required field indicator (asterisk). Unicode: U+2731. */
    public static final String REQUIRED = getSymbol("\u2731", "REQ");
    /** A disabled or off-state indicator. Unicode: U+26AA. */
    public static final String DISABLED = getSymbol("\u26AA", "OFF");
    /** An enabled or on-state indicator. Unicode: U+26AB. */
    public static final String ENABLED = getSymbol("\u26AB", "ON");

    // ===================== Sorting Indicators (Alt) =====================
    /** An upward-pointing caret for sorting. Unicode: U+2303. */
    public static final String CARET_UP = getSymbol("\u2303", "^");
    /** A downward-pointing caret for sorting. Unicode: U+2304. */
    public static final String CARET_DOWN = getSymbol("\u2304", "v");
    /** An upward-pointing chevron for sorting. Unicode: U+02C4. */
    public static final String CHEVRON_UP = getSymbol("\u02C4", "^");
    /** A downward-pointing chevron for sorting. Unicode: U+02C5. */
    public static final String CHEVRON_DOWN = getSymbol("\u02C5", "v");

    // ===================== Currency (Extended) =====================
    /** Swiss Franc label. CHF is used because there is no dedicated modern Unicode symbol. */
    public static final String SWISS_FRANC = "CHF";
    
    // ===================== Legal & Documents =====================
    /** The copyright symbol (©). Unicode: U+00A9. */
    public static final String COPYRIGHT = "\u00A9";
    /** The registered trademark symbol (®). Unicode: U+00AE. */
    public static final String REGISTERED = "\u00AE";
    /** The trademark symbol (™). Unicode: U+2122. */
    public static final String TRADEMARK = "\u2122";
    /** The paragraph symbol (§), also known as the section sign. Unicode: U+00A7. */
    public static final String PARAGRAPH = "\u00A7";

    // ===================== Debug & Developer =====================
    /** A bug icon for debugging purposes. Unicode: U+D83D U+DC1B. */
    public static final String BUG = getSymbol("BUG", "\u2731", "\uD83D\uDC1B");
    /** A tools icon for developer or configuration utilities. Unicode: U+2692. */
    public static final String TOOLS = getSymbol("\u2692", "TOOLS");
    /** A code or script icon, represented by a pencil. */
    public static final String CODE = BETTER_PENCIL;
    /** Brackets symbol ({}), often used to represent code or data structures. */
    public static final String BRACKETS = "\u007B\u007D";

    // ===================== Table/Column Helper =====================
    /** Column header for "Artikelnummer" (Article Number). */
    public static final String COL_ARTIKELNUMMER = columnLabel(ARTIKELNUMMER, "Artikelnummer");
    /** Column header for "Name". */
    public static final String COL_NAME = columnLabel(NAME, "Name");
    /** Column header for "Kategorie" (Category). */
    public static final String COL_KATEGORIE = columnLabel(CATEGORY, "Kategorie");
    /** Column header for "Details". */
    public static final String COL_DETAILS = columnLabel(DETAILS, "Details");
    /** Column header for "Lagerbestand" (Stock Quantity). */
    public static final String COL_LAGERBESTAND = columnLabel(STOCK_QUANTITY, "Lagerbestand");
    /** Column header for "Mindestbestand" (Minimum Stock). */
    public static final String COL_MINDESTBESTAND = columnLabel(MIN_STOCK, "Mindestbestand");
    /** Column header for "Verkaufspreis" (Sell Price). */
    public static final String COL_VERKAUFSPREIS = columnLabel(SELL_PRICE, "Verkaufspreis");
    /** Column header for "Einkaufspreis" (Purchase Price). */
    public static final String COL_EINKAUFSPREIS = columnLabel(PURCHASE_PRICE, "Einkaufspreis");
    /** Column header for "Lieferant" (Supplier). */
    public static final String COL_LIEFERANT = columnLabel(SUPPLIER, "Lieferant");
    /** Column header for "Bestell Menge" (Order Quantity). */
    public static final String COL_BESTELL_MENGE = columnLabel(ORDER_QUANTITY, "Bestell Menge");
    /** Column header for "Lager Menge" (Stock Quantity). */
    public static final String COL_LAGER_MENGE = columnLabel(STOCK_QUANTITY, "Lager Menge");
    /** Column header for "Hinzugefügt" (Added). */
    public static final String COL_HINZUGEFUEGT = columnLabel(ADDED, "Hinzugefuegt");
    /** Column header for "Status". */
    public static final String COL_STATUS = columnLabel(STATUS, "Status");
    /** Column header for "Typ" (Type). */
    public static final String COL_TYP = columnLabel(TAG, "Typ");
    /** Column header for "Titel" (Title). */
    public static final String COL_TITEL = columnLabel(TITLE, "Titel");
    /** Column header for "Nachricht" (Message). */
    public static final String COL_NACHRICHT = columnLabel(MESSAGE, "Nachricht");
    /** Column header for "Datum" (Date). */
    public static final String COL_DATUM = columnLabel(CALENDAR, "Datum");
    /** Column header for "Abteilung" (Department). */
    public static final String COL_ABTEILUNG = columnLabel(DEPARTMENT, "Abteilung");
    /** Column header for "Kontakt" (Contact). */
    public static final String COL_KONTAKT = columnLabel(CONTACT, "Kontakt");
    /** Column header for "Telefon" (Phone). */
    public static final String COL_TELEFON = columnLabel(PHONE, "Telefon");
    /** Column header for "Email". */
    public static final String COL_EMAIL = columnLabel(EMAIL, "Email");
    /** Column header for "Adresse" (Address). */
    public static final String COL_ADRESSE = columnLabel(ADDRESS, "Adresse");
    /** Column header for "Gelieferte Artikel" (Delivered Articles). */
    public static final String COL_GELIEFERTE_ARTIKEL = columnLabel(DELIVERED_ARTICLES, "Gelieferte Artikel");
    /** Column header for "Mindestbestellwert" (Minimum Order Value). */
    public static final String COL_MINDESTBESTELLWERT = columnLabel(MIN_ORDER_VALUE, "Mindestbestellwert");

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
    public static String getSymbol(String symbol, String fallback) {
        String safeFallback = normalizeFallback(fallback);

        if (symbol == null || symbol.isEmpty()) {
            return safeFallback;
        }

        boolean supported = FontSupport.CACHE.computeIfAbsent(symbol, UnicodeSymbols::canDisplayAll);
        return supported ? symbol : safeFallback;
    }

    /**
     * Resolves the first reliably displayable symbol from the provided candidates.
     * This prefers symbols that work with common UI/system fonts across Windows, macOS,
     * and Linux before falling back to ASCII text.
     *
     * @param fallback ASCII fallback text
     * @param candidates ordered list of symbol candidates, best to least preferred
     * @return the first supported symbol, otherwise the fallback
     */
    public static String getSymbol(String fallback, String... candidates) {
        String safeFallback = normalizeFallback(fallback);

        if (candidates == null || candidates.length == 0) {
            return safeFallback;
        }

        for (String candidate : candidates) {
            if (candidate == null || candidate.isEmpty()) {
                continue;
            }

            boolean supported = FontSupport.CACHE.computeIfAbsent(candidate, UnicodeSymbols::canDisplayAll);
            if (supported) {
                return candidate;
            }
        }

        return safeFallback;
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
        String safeFallback = normalizeFallback(fallback);

        if (symbol == null || symbol.isEmpty()) {
            return safeFallback;
        }

        if (font == null) {
            return getSymbol(symbol, safeFallback);
        }

        return font.canDisplayUpTo(symbol) == -1 ? symbol : safeFallback;
    }

    /**
     * Variant of {@link #safeSymbol(String, String, Font)} that accepts multiple
     * ordered symbol candidates and returns the first one displayable by the
     * provided font.
     *
     * @param fallback ASCII fallback text
     * @param font font to check directly
     * @param candidates ordered symbol candidates
     * @return first symbol supported by the font, otherwise the fallback
     */
    public static String safeSymbol(String fallback, Font font, String... candidates) {
        String safeFallback = normalizeFallback(fallback);
        if (candidates == null || candidates.length == 0) {
            return safeFallback;
        }

        if (font == null) {
            return getSymbol(safeFallback, candidates);
        }

        for (String candidate : candidates) {
            if (candidate != null && !candidate.isEmpty() && font.canDisplayUpTo(candidate) == -1) {
                return candidate;
            }
        }
        return safeFallback;
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

    private static String columnLabel(String icon, String label) {
        String safeIcon = normalizeFallback(icon).trim();
        String safeLabel = normalizeFallback(label).trim();
        if (safeIcon.isEmpty()) {
            return safeLabel;
        }
        if (safeLabel.isEmpty()) {
            return safeIcon;
        }
        return safeIcon + " " + safeLabel;
    }

}
