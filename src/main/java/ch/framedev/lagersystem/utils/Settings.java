package ch.framedev.lagersystem.utils;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * A properties file handler that fully preserves comments and blank lines
 * during both load and save operations.
 *
 * <p>In contrast to the standard {@link java.util.Properties} class, this
 * implementation keeps every {@code #} / {@code !} comment line and every
 * blank line exactly where it was found.  Only key-value pairs are interpreted;
 * everything else is stored verbatim and written back unchanged.
 *
 * <p>When a key is updated with {@link #setProperty(String, String)} the
 * corresponding line in the file is updated in-place so that any surrounding
 * comments remain attached to that key.  New keys are appended at the end of
 * the file, separated by a blank line.
 *
 * <h2>Constructor</h2>
 * <pre>{@code
 * // Load from an external file, falling back to a classpath default resource:
 * Settings s = new Settings("settings.properties", MyApp.class, externalFile);
 *
 * // Standalone – only an external file, no classpath fallback:
 * Settings s = new Settings(externalFile);
 * }</pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * String value = s.getProperty("my.key");
 * s.setProperty("my.key", "new-value");
 * boolean exists = s.contains("my.key");
 * s.save();
 * }</pre>
 */
public class Settings {

    // -------------------------------------------------------------------------
    // Internal line model
    // -------------------------------------------------------------------------

    /**
     * Represents a single line in the properties file.
     * The line is either a key-value entry or a raw/verbatim line (comment,
     * blank line, or continuation that could not be parsed).
     */
    private static final class Line {
        /** {@code true} when this line holds a parsed key=value pair. */
        final boolean isEntry;
        /** Key for entry lines; {@code null} for raw lines. */
        String key;
        /** Value for entry lines; raw text for raw lines. */
        String value;
        /**
         * The separator used between the key and value ({@code =}, {@code :},
         * or a space/tab character).  Preserved to avoid reformatting.
         */
        String separator;
        /**
         * Leading whitespace / indentation found before the key.  Preserved
         * so the line is written back identically.
         */
        String leadingWhitespace;

        /** Creates a raw (non-entry) line with the given text. */
        static Line raw(String text) {
            Line l = new Line(false);
            l.value = text;
            return l;
        }

        /** Creates an entry line. */
        static Line entry(String leadingWs, String key, String separator, String value) {
            Line l = new Line(true);
            l.leadingWhitespace = leadingWs;
            l.key = key;
            l.separator = separator;
            l.value = value;
            return l;
        }

        private Line(boolean isEntry) {
            this.isEntry = isEntry;
        }

        /** Returns the line as it should be written to the file. */
        String toFileString() {
            if (!isEntry) {
                return value;
            }
            return leadingWhitespace + key + separator + value;
        }
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /** Ordered list of all lines (comments, blanks, and key=value entries). */
    private final List<Line> lines = new ArrayList<>();

    /** Fast lookup from key to its {@link Line} object. */
    private final Map<String, Line> entryMap = new LinkedHashMap<>();

    /** The external file used for {@link #save()}. */
    private final File file;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Creates a {@code Settings} instance backed by {@code file}.
     *
     * <p>If {@code file} already exists it is loaded directly.  If it does
     * <em>not</em> exist, the classpath resource {@code resourceName} is
     * located via {@code clazz.getClassLoader()} and used as initial content.
     * If neither source is available the instance starts empty.
     *
     * @param resourceName name of the classpath default resource (e.g.
     *                     {@code "settings.properties"})
     * @param clazz        class whose {@link ClassLoader} is used to locate
     *                     the resource
     * @param file         the external file to load from / save to
     */
    public Settings(String resourceName, Class<?> clazz, File file) {
        this.file = file;
        if (file != null && file.exists() && file.length() > 0) {
            load(file);
        } else {
            // File absent or empty → seed from the bundled classpath default.
            loadFromClasspath(resourceName, clazz);
        }
    }

    /**
     * Creates a {@code Settings} instance backed solely by the given
     * {@code file}.  If the file does not exist the instance starts empty.
     *
     * @param file the external file to load from / save to
     */
    public Settings(File file) {
        this.file = file;
        if (file != null && file.exists()) {
            load(file);
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the value associated with {@code key}, or {@code null} if the
     * key is not present.
     *
     * @param key the property key
     * @return the value, or {@code null}
     */
    public String getProperty(String key) {
        Line line = entryMap.get(key);
        return line != null ? unescape(line.value) : null;
    }

    /**
     * Returns the value associated with {@code key}, or {@code defaultValue}
     * if the key is not present.
     *
     * @param key          the property key
     * @param defaultValue the fallback value
     * @return the value or {@code defaultValue}
     */
    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Sets or updates the value for the given {@code key}.
     *
     * <p>If the key already exists, its line is updated in-place so surrounding
     * comments are kept.  If the key is new it is appended at the end of the
     * file (preceded by a blank line for readability).
     *
     * @param key   the property key
     * @param value the new value
     */
    public void setProperty(String key, String value) {
        Line existing = entryMap.get(key);
        if (existing != null) {
            existing.value = escape(value);
        } else {
            // Append a blank separator line + the new entry
            if (!lines.isEmpty()) {
                lines.add(Line.raw(""));
            }
            Line newLine = Line.entry("", key, "=", escape(value));
            lines.add(newLine);
            entryMap.put(key, newLine);
        }
    }

    /**
     * Returns {@code true} if the given {@code key} is present.
     *
     * @param key the property key to test
     * @return {@code true} when the key exists
     */
    public boolean contains(String key) {
        return entryMap.containsKey(key);
    }

    /**
     * Returns an unmodifiable view of all property keys in the order they
     * appear in the file.
     *
     * @return set of keys
     */
    public Set<String> keySet() {
        return Collections.unmodifiableSet(entryMap.keySet());
    }

    /**
     * Removes the entry with the given {@code key}.  The corresponding line
     * is replaced with an empty raw line so the surrounding comment structure
     * is not disrupted.
     *
     * @param key the property key to remove
     */
    public void remove(String key) {
        Line existing = entryMap.remove(key);
        if (existing == null) {
            return;
        }
        int idx = lines.indexOf(existing);
        if (idx >= 0) {
            lines.set(idx, Line.raw(""));
        }
    }

    /**
     * Writes the current state of all lines (including preserved comments and
     * blank lines) back to the backing file.
     *
     * <p>The file is written with UTF-8 encoding.  Callers should handle the
     * case where the file cannot be written (e.g. missing parent directories).
     */
    public void save() {
        if (file == null) {
            return;
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            }
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                for (int i = 0; i < lines.size(); i++) {
                    writer.write(lines.get(i).toFileString());
                    if (i < lines.size() - 1) {
                        writer.newLine();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[Settings] Failed to save settings to " + file.getAbsolutePath() + ": " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Loading
    // -------------------------------------------------------------------------

    /**
     * Loads properties from the given {@code file}.
     *
     * @param source the file to read
     */
    private void load(File source) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(source), StandardCharsets.UTF_8))) {
            parse(reader);
        } catch (IOException e) {
            System.err.println("[Settings] Failed to load settings from " + source.getAbsolutePath() + ": " + e.getMessage());
        }
    }

    /**
     * Falls back to the classpath resource if the external file was not found.
     *
     * @param resourceName the resource name to look up
     * @param clazz        class whose class-loader is used
     */
    private void loadFromClasspath(String resourceName, Class<?> clazz) {
        if (resourceName == null || clazz == null) {
            return;
        }
        URL url = clazz.getClassLoader().getResource(resourceName);
        if (url == null) {
            // Try without leading '/'
            url = clazz.getResource("/" + resourceName);
        }
        if (url == null) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
            parse(reader);
        } catch (IOException e) {
            System.err.println("[Settings] Failed to load classpath resource '" + resourceName + "': " + e.getMessage());
        }
    }

    /**
     * Parses lines from the given reader and populates {@link #lines} and
     * {@link #entryMap}.
     *
     * <p>The parser handles:
     * <ul>
     *   <li>Comment lines starting with {@code #} or {@code !} (after optional
     *       whitespace)</li>
     *   <li>Blank / whitespace-only lines</li>
     *   <li>Continuation lines ending with {@code \} (multi-line values)</li>
     *   <li>{@code key=value}, {@code key:value}, and {@code key value}
     *       separator styles</li>
     * </ul>
     *
     * @param reader the reader to consume
     * @throws IOException if an I/O error occurs
     */
    private void parse(BufferedReader reader) throws IOException {
        lines.clear();
        entryMap.clear();

        String rawLine;
        while ((rawLine = reader.readLine()) != null) {
            // Determine leading whitespace
            int start = 0;
            while (start < rawLine.length() && isWhitespace(rawLine.charAt(start))) {
                start++;
            }
            String trimmed = rawLine.substring(start);

            // Blank or comment line → store verbatim
            if (trimmed.isEmpty() || trimmed.charAt(0) == '#' || trimmed.charAt(0) == '!') {
                lines.add(Line.raw(rawLine));
                continue;
            }

            // Continuation: build the full logical line from one or more physical lines
            StringBuilder logicalSb = new StringBuilder(trimmed);
            while (isLineContinuation(logicalSb.toString())) {
                // Remove the trailing '\'
                String partial = logicalSb.toString();
                logicalSb.setLength(partial.length() - 1);
                String next = reader.readLine();
                if (next == null) {
                    break;
                }
                logicalSb.append(next.stripLeading());
            }
            String logical = logicalSb.toString();

            // Find separator (= : or whitespace)
            int sepIdx = -1;
            char sep = '=';
            for (int i = 0; i < logical.length(); i++) {
                char c = logical.charAt(i);
                if (c == '\\') {
                    i++; // skip escaped char
                    continue;
                }
                if (c == '=' || c == ':') {
                    sepIdx = i;
                    sep = c;
                    break;
                }
                if (isWhitespace(c)) {
                    sepIdx = i;
                    sep = ' ';
                    break;
                }
            }

            if (sepIdx < 0) {
                // Key with no value
                Line line = Line.entry(rawLine.substring(0, start), logical, "", "");
                lines.add(line);
                entryMap.put(logical, line);
                continue;
            }

            String key = logical.substring(0, sepIdx);
            String separatorStr;
            String valueRaw;

            if (sep == ' ') {
                // Whitespace separator: skip all leading whitespace between key and value
                int valStart = sepIdx;
                while (valStart < logical.length() && isWhitespace(logical.charAt(valStart))) {
                    valStart++;
                }
                separatorStr = logical.substring(sepIdx, valStart);
                // But if an explicit '=' or ':' follows, consume it too
                if (valStart < logical.length() && (logical.charAt(valStart) == '=' || logical.charAt(valStart) == ':')) {
                    valStart++;
                    // Skip any trailing whitespace after the explicit separator
                    while (valStart < logical.length() && isWhitespace(logical.charAt(valStart))) {
                        valStart++;
                    }
                    separatorStr = logical.substring(sepIdx, valStart);
                }
                valueRaw = logical.substring(valStart);
            } else {
                // '=' or ':' – include any surrounding whitespace in the separator string
                // so the line is reproduced exactly
                int afterSep = sepIdx + 1;
                // Also include whitespace before the separator
                int beforeSep = sepIdx;
                while (beforeSep > 0 && isWhitespace(logical.charAt(beforeSep - 1))) {
                    beforeSep--;
                }
                // Recalculate key without trailing whitespace
                key = logical.substring(0, beforeSep);
                // Include trailing whitespace after separator into separator string
                while (afterSep < logical.length() && isWhitespace(logical.charAt(afterSep))) {
                    afterSep++;
                }
                separatorStr = logical.substring(beforeSep, afterSep);
                valueRaw = logical.substring(afterSep);
            }

            Line line = Line.entry(rawLine.substring(0, start), key, separatorStr, valueRaw);
            lines.add(line);
            // If duplicate key, overwrite (last-wins behaviour like java.util.Properties)
            entryMap.put(key, line);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the given {@code line} ends with an odd number
     * of back-slashes, indicating a line continuation.
     */
    private static boolean isLineContinuation(String line) {
        int count = 0;
        for (int i = line.length() - 1; i >= 0 && line.charAt(i) == '\\'; i--) {
            count++;
        }
        return (count & 1) == 1; // odd number of trailing back-slashes
    }

    /**
     * Returns {@code true} for characters that are considered whitespace by
     * the {@code .properties} spec (space, tab, form-feed).
     */
    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\f';
    }

    /**
     * Unescapes common Java-properties escape sequences in {@code raw} so that
     * callers receive the actual string value.
     *
     * @param raw the raw (potentially escaped) value from the file
     * @return the unescaped string
     */
    private static String unescape(String raw) {
        if (raw == null || raw.indexOf('\\') < 0) {
            return raw;
        }
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\\' && i + 1 < raw.length()) {
                char next = raw.charAt(++i);
                switch (next) {
                    case 't': sb.append('\t'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case '\\': sb.append('\\'); break;
                    case 'u':
                        if (i + 4 < raw.length()) {
                            String hex = raw.substring(i + 1, i + 5);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                                i += 4;
                            } catch (NumberFormatException e) {
                                sb.append('\\').append(next);
                            }
                        } else {
                            sb.append('\\').append(next);
                        }
                        break;
                    default: sb.append(next); break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Escapes characters in {@code value} that are significant in the
     * {@code .properties} file format so they can be stored safely.
     *
     * <p>Currently only the newline and carriage-return characters are escaped;
     * all other characters are left as-is to keep the file human-readable.
     *
     * @param value the plain-text value to escape
     * @return the escaped representation suitable for writing to a properties file
     */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        // Only escape characters that would break the file structure
        return value.replace("\r", "\\r").replace("\n", "\\n");
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    /**
     * Returns a debug-friendly summary of all currently loaded key-value pairs.
     *
     * @return string representation
     */
    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ", "Settings{", "}");
        for (Map.Entry<String, Line> e : entryMap.entrySet()) {
            sj.add(e.getKey() + "=" + e.getValue().value);
        }
        return sj.toString();
    }
}


