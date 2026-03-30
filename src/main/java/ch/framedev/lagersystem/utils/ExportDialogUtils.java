package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.dialogs.MessageDialog;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.Locale;

public final class ExportDialogUtils {

    private ExportDialogUtils() {
    }

    public static File chooseSaveFile(Component parent, String dialogTitle, String defaultFileName,
                                      String extension, String description) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(dialogTitle);
        fileChooser.setSelectedFile(new File(defaultFileName));
        fileChooser.setFileFilter(new FileNameExtensionFilter(description, extension));

        int userSelection = fileChooser.showSaveDialog(parent);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File selectedFile = fileChooser.getSelectedFile();
        if (selectedFile == null) {
            return null;
        }

        selectedFile = ensureExtension(selectedFile, extension);
        return confirmOverwrite(selectedFile) ? selectedFile : null;
    }

    public static File ensureExtension(File file, String extension) {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
        if (extension == null || extension.isBlank()) {
            throw new IllegalArgumentException("extension must not be blank");
        }

        String normalizedExtension = normalizeExtension(extension);
        String suffix = "." + normalizedExtension;
        if (file.getName().toLowerCase(Locale.ROOT).endsWith(suffix)) {
            return file;
        }
        return new File(file.getAbsolutePath() + suffix);
    }

    public static boolean confirmOverwrite(File file) {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
        if (!file.exists()) {
            return true;
        }

        int overwrite = new MessageDialog()
                .setTitle("Datei überschreiben")
                .setMessage(UnicodeSymbols.WARNING + " Die Datei existiert bereits. Überschreiben?\n\n"
                        + file.getAbsolutePath())
                .setMessageType(JOptionPane.WARNING_MESSAGE)
                .setOptionType(JOptionPane.YES_NO_OPTION)
                .displayWithOptions();
        return overwrite == JOptionPane.YES_OPTION;
    }

    public static void showExportSuccess(String title, String exportedLabel, String... detailLines) {
        if (title == null || exportedLabel == null) {
            throw new IllegalArgumentException("title and exportedLabel must not be null");
        }
        new MessageDialog()
                .setTitle(title)
                .setMessage(buildSuccessMessage(exportedLabel, detailLines))
                .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                .display();
    }

    public static void showExportError(String title, String exportLabel, Exception exception) {
        if (title == null || exportLabel == null || exception == null) {
            throw new IllegalArgumentException("title, exportLabel and exception must not be null");
        }
        new MessageDialog()
                .setTitle(title)
                .setMessage(UnicodeSymbols.ERROR + " Fehler beim " + exportLabel + ":\n\n" + exception.getMessage()
                        + "\n\nBitte überprüfen Sie die Schreibrechte und versuchen Sie es erneut.")
                .setMessageType(JOptionPane.ERROR_MESSAGE)
                .display();
    }

    private static String buildSuccessMessage(String exportedLabel, String... detailLines) {
        StringBuilder message = new StringBuilder()
                .append(UnicodeSymbols.CHECKMARK)
                .append(' ')
                .append(exportedLabel)
                .append(" erfolgreich exportiert!");
        if (detailLines != null && detailLines.length > 0) {
            message.append("\n\n");
            for (String detailLine : detailLines) {
                if (detailLine != null && !detailLine.isBlank()) {
                    message.append(detailLine).append('\n');
                }
            }
            if (message.charAt(message.length() - 1) == '\n') {
                message.setLength(message.length() - 1);
            }
        }
        return message.toString();
    }

    private static String normalizeExtension(String extension) {
        return extension.startsWith(".") ? extension.substring(1).toLowerCase(Locale.ROOT)
                : extension.toLowerCase(Locale.ROOT);
    }
}
