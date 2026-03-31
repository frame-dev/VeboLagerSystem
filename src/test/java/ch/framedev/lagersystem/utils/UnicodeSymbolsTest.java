package ch.framedev.lagersystem.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Font;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnicodeSymbolsTest {

    @Test
    @DisplayName("getSymbol(symbol, fallback): null symbol returns fallback")
    void getSymbol_nullSymbol_returnsFallback() {
        assertEquals("fallback", UnicodeSymbols.getSymbol(null, "fallback"));
    }

    @Test
    @DisplayName("getSymbol(symbol, fallback): empty symbol returns fallback")
    void getSymbol_emptySymbol_returnsFallback() {
        assertEquals("fallback", UnicodeSymbols.getSymbol("", "fallback"));
    }

    @Test
    @DisplayName("getSymbol(symbol, fallback): null fallback becomes empty string")
    void getSymbol_nullFallback_becomesEmptyString() {
        assertEquals("", UnicodeSymbols.getSymbol((String) null, (String) null));
    }

    @Test
    @DisplayName("getSymbol(fallback, candidates): no candidates returns fallback")
    void getSymbol_noCandidates_returnsFallback() {
        assertEquals("fallback", UnicodeSymbols.getSymbol("fallback", (String[]) null));
        assertEquals("fallback", UnicodeSymbols.getSymbol("fallback"));
    }

    @Test
    @DisplayName("getSymbol(fallback, candidates): blank and null candidates fall back")
    void getSymbol_blankAndNullCandidates_fallBack() {
        assertEquals("fallback", UnicodeSymbols.getSymbol("fallback", "", null, ""));
    }

    @Test
    @DisplayName("safeSymbol(symbol, fallback, font): null symbol returns fallback")
    void safeSymbol_nullSymbol_returnsFallback() {
        assertEquals("fallback", UnicodeSymbols.safeSymbol(null, "fallback", new Font("Dialog", Font.PLAIN, 12)));
    }

    @Test
    @DisplayName("safeSymbol(symbol, fallback, font): returns symbol when font can display it")
    void safeSymbol_returnsSymbolWhenFontCanDisplayIt() {
        Font font = new Font("Dialog", Font.PLAIN, 12);

        assertEquals("A", UnicodeSymbols.safeSymbol("A", "fallback", font));
    }

    @Test
    @DisplayName("safeSymbol(fallback, font, candidates): empty candidates return fallback")
    void safeSymbolCandidates_emptyCandidates_returnFallback() {
        Font font = new Font("Dialog", Font.PLAIN, 12);

        assertEquals("fallback", UnicodeSymbols.safeSymbol("fallback", font, (String[]) null));
    }

    @Test
    @DisplayName("safeSymbol(fallback, font, candidates): returns first displayable candidate")
    void safeSymbolCandidates_returnsFirstDisplayableCandidate() {
        Font font = new Font("Dialog", Font.PLAIN, 12);

        assertEquals("A", UnicodeSymbols.safeSymbol("fallback", font, "", null, "A", "B"));
    }

    @Test
    @DisplayName("platform helpers and constants: return sane values")
    void platformHelpersAndConstants_returnSaneValues() {
        assertNotNull(UnicodeSymbols.SWISS_FRANC);
        assertEquals("CHF", UnicodeSymbols.SWISS_FRANC);
        assertTrue(UnicodeSymbols.isWindows() || UnicodeSymbols.isMac() || UnicodeSymbols.isLinux()
                || (!UnicodeSymbols.isWindows() && !UnicodeSymbols.isMac() && !UnicodeSymbols.isLinux()));
        assertFalse(UnicodeSymbols.COL_NAME.isBlank());
        assertFalse(UnicodeSymbols.COL_STATUS.isBlank());
    }
}
