package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.dialogs.MessageDialog;

import javax.swing.*;

final class SettingsDialogService {

    private SettingsDialogService() {
    }

    static void showInfo(String title, String message) {
        show(title, message, JOptionPane.INFORMATION_MESSAGE, null);
    }

    static void showInfo(String title, String message, Integer duration) {
        show(title, message, JOptionPane.INFORMATION_MESSAGE, duration);
    }

    static void showWarning(String title, String message) {
        show(title, message, JOptionPane.WARNING_MESSAGE, null);
    }

    static void showError(String title, String message) {
        show(title, message, JOptionPane.ERROR_MESSAGE, null);
    }

    static int showYesNo(String title, String message, int messageType) {
        return showOptions(title, message, messageType, null);
    }

    static int showOptions(String title, String message, int messageType, Object[] options) {
        MessageDialog dialog = new MessageDialog()
                .setTitle(title)
                .setMessage(message)
                .setMessageType(messageType)
                .setOptionType(JOptionPane.YES_NO_OPTION);
        if (options != null) {
            dialog.setOptions(options);
        }
        return dialog.displayWithOptions();
    }

    static String showTextInput(String title, String message, int messageType) {
        return new MessageDialog()
                .setTitle(title)
                .setMessage(message)
                .setMessageType(messageType)
                .displayWithStringInput();
    }

    private static void show(String title, String message, int messageType, Integer duration) {
        MessageDialog dialog = new MessageDialog()
                .setTitle(title)
                .setMessage(message)
                .setMessageType(messageType);
        if (duration != null) {
            dialog.setDuration(duration);
        }
        dialog.display();
    }
}
