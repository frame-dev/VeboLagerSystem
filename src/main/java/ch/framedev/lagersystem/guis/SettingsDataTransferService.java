package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.classes.Vendor;
import ch.framedev.lagersystem.dialogs.MessageDialog;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.managers.ClientManager;
import ch.framedev.lagersystem.managers.OrderManager;
import ch.framedev.lagersystem.managers.VendorManager;
import ch.framedev.lagersystem.utils.UnicodeSymbols;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.io.*;
import java.util.*;

final class SettingsDataTransferService {

    static final class ExportSummary {
        private final int successCount;
        private final int totalTables;

        private ExportSummary(int successCount, int totalTables) {
            this.successCount = successCount;
            this.totalTables = totalTables;
        }

        int getSuccessCount() {
            return successCount;
        }

        int getTotalTables() {
            return totalTables;
        }

        boolean hasFailures() {
            return successCount < totalTables;
        }
    }

    private SettingsDataTransferService() {
    }

    static void importFromCsv() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("CSV-Datei zum Importieren auswählen");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV-Dateien", "csv"));
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setCurrentDirectory(Main.getAppDataDir());

        int result = fileChooser.showOpenDialog(null);
        if (result != JFileChooser.APPROVE_OPTION) {
            System.out.println("[SettingsGUI] Import abgebrochen");
            return;
        }

        File selectedFile = fileChooser.getSelectedFile();
        String fileName = selectedFile.getName().toLowerCase();

        if (fileName.contains("article")) {
            importArticlesFromCsv(selectedFile);
        } else if (fileName.contains("vendor") || fileName.contains("supplier")) {
            importVendorsFromCsv(selectedFile);
        } else if (fileName.contains("client") || fileName.contains("customer")) {
            importClientsFromCsv(selectedFile);
        } else if (fileName.contains("order")) {
            importOrdersFromCsv();
        } else {
            String[] options = { "Artikel", "Lieferanten", "Kunden", "Bestellungen", "Abbrechen" };
            int choice = JOptionPane.showOptionDialog(null,
                    "Welche Art von Daten möchten Sie importieren?",
                    "Datentyp auswählen",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    Main.iconSmall,
                    options,
                    options[0]);

            switch (choice) {
                case 0 -> importArticlesFromCsv(selectedFile);
                case 1 -> importVendorsFromCsv(selectedFile);
                case 2 -> importClientsFromCsv(selectedFile);
                case 3 -> importOrdersFromCsv();
                default -> System.out.println("[SettingsGUI] Import abgebrochen");
            }
        }
    }

    static ExportSummary exportToCsv() {
        int successCount = 0;
        int totalTables = 4;

        List<Article> articles = ArticleManager.getInstance().getAllArticles();
        File csvFile = new File(Main.getAppDataDir(), "articles_export.csv");
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            writer.println(
                    "Artikelnummer,Name,Details,Lagerbestand,Mindestlagerbestand,Verkaufspreis,Einkaufspreis,Lieferant");

            for (Article article : articles) {
                writer.format(Locale.ROOT,
                        "\"%s\",\"%s\",\"%s\",%d,%d,%.2f,%.2f,\"%s\"%n",
                        escapeCSV(article.getArticleNumber()),
                        escapeCSV(article.getName()),
                        escapeCSV(article.getDetails()),
                        article.getStockQuantity(),
                        article.getMinStockLevel(),
                        article.getSellPrice(),
                        article.getPurchasePrice(),
                        escapeCSV(article.getVendorName()));
            }
            System.out.println("[SettingsGUI] Artikel erfolgreich nach " + csvFile.getAbsolutePath() + " exportiert ("
                    + articles.size() + " Einträge)");
            successCount++;
        } catch (Exception e) {
            String errorMsg = "Fehler beim Exportieren der Artikel: " + e.getMessage();
            System.err.println("[SettingsGUI] " + errorMsg);
            LogManager.getLogger(SettingsGUI.class).error(errorMsg, e);
            Main.logUtils.addLog(errorMsg);
        }

        List<Vendor> vendors = VendorManager.getInstance().getVendors();
        File vendorCsvFile = new File(Main.getAppDataDir(), "vendors_export.csv");
        try (PrintWriter writer = new PrintWriter(new FileWriter(vendorCsvFile))) {
            writer.println("Name,Kontaktperson,Telefon,E-Mail,Adresse,MinBestellwert");
            for (Vendor vendor : vendors) {
                writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        escapeCSV(vendor.getName()),
                        escapeCSV(vendor.getContactPerson()),
                        escapeCSV(vendor.getPhoneNumber()),
                        escapeCSV(vendor.getEmail()),
                        escapeCSV(vendor.getAddress()),
                        escapeCSV(String.valueOf(vendor.getMinOrderValue())));
            }
            System.out.println("[SettingsGUI] Lieferanten erfolgreich nach " + vendorCsvFile.getAbsolutePath()
                    + " exportiert (" + vendors.size() + " Einträge)");
            successCount++;
        } catch (Exception ex) {
            String errorMsg = "Fehler beim Exportieren der Lieferanten: " + ex.getMessage();
            System.err.println("[SettingsGUI] " + errorMsg);
            LogManager.getLogger(SettingsGUI.class).error(errorMsg, ex);
            Main.logUtils.addLog(errorMsg);
        }

        List<Map<String, String>> clients = ClientManager.getInstance().getAllClients();
        File clientCsvFile = new File(Main.getAppDataDir(), "clients_export.csv");
        try (PrintWriter writer = new PrintWriter(new FileWriter(clientCsvFile))) {
            writer.println("Name,Abteilung");
            for (Map<String, String> clientMap : clients) {
                String name = clientMap.getOrDefault("firstLastName", "");
                String department = clientMap.getOrDefault("department", "");
                writer.printf("\"%s\",\"%s\"%n", escapeCSV(name), escapeCSV(department));
            }
            System.out.println("[SettingsGUI] Kunden erfolgreich nach " + clientCsvFile.getAbsolutePath()
                    + " exportiert (" + clients.size() + " Einträge)");
            successCount++;
        } catch (Exception ex) {
            String errorMsg = "Fehler beim Exportieren der Kunden: " + ex.getMessage();
            System.err.println("[SettingsGUI] " + errorMsg);
            LogManager.getLogger(SettingsGUI.class).error(errorMsg, ex);
            Main.logUtils.addLog(errorMsg);
        }

        List<Order> orders = OrderManager.getInstance().getOrders();
        File orderCsvFile = new File(Main.getAppDataDir(), "orders_export.csv");
        try (PrintWriter writer = new PrintWriter(new FileWriter(orderCsvFile))) {
            writer.println(
                    "Bestell-ID,Empfängername,EmpfängerKontoNummer,SenderName,SenderKontoNummer,Artikel,Bestelldatum,Status,Abteilung");
            for (Order order : orders) {
                String articlesStr = order.getOrderedArticles().entrySet().stream()
                        .map(e -> {
                            String filling = order.getArticleFilling(e.getKey());
                            if (filling == null || filling.isBlank()) {
                                return e.getKey() + ":" + e.getValue();
                            }
                            return e.getKey() + ":" + e.getValue() + " [" + filling + "]";
                        })
                        .collect(java.util.stream.Collectors.joining(";"));

                writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        escapeCSV(order.getOrderId()),
                        escapeCSV(order.getReceiverName()),
                        escapeCSV(order.getReceiverKontoNumber()),
                        escapeCSV(order.getSenderName()),
                        escapeCSV(order.getSenderKontoNumber()),
                        escapeCSV(articlesStr),
                        escapeCSV(order.getOrderDate()),
                        escapeCSV(order.getStatus()),
                        escapeCSV(order.getDepartment()));
            }
            System.out.println("[SettingsGUI] Bestellungen erfolgreich nach " + orderCsvFile.getAbsolutePath()
                    + " exportiert (" + orders.size() + " Einträge)");
            successCount++;
        } catch (Exception ex) {
            String errorMsg = "Fehler beim Exportieren der Bestellungen: " + ex.getMessage();
            System.err.println("[SettingsGUI] " + errorMsg);
            LogManager.getLogger(SettingsGUI.class).error(errorMsg, ex);
            Main.logUtils.addLog(errorMsg);
        }

        System.out.println("[SettingsGUI] CSV-Export abgeschlossen: " + successCount + "/" + totalTables
                + " Tabellen erfolgreich exportiert");
        System.out.println("[SettingsGUI] Dateien gespeichert in: " + Main.getAppDataDir().getAbsolutePath());
        return new ExportSummary(successCount, totalTables);
    }

    private static void importArticlesFromCsv(File csvFile) {
        int imported = 0;
        int errors = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                new MessageDialog()
                        .setTitle("Fehler")
                        .setMessage("Die CSV-Datei ist leer.")
                        .setMessageType(JOptionPane.ERROR_MESSAGE)
                        .display();
                return;
            }

            ArticleManager articleManager = ArticleManager.getInstance();
            String line;

            while ((line = reader.readLine()) != null) {
                try {
                    String[] parts = parseCsvLine(line);
                    if (parts.length < 8) {
                        errors++;
                        continue;
                    }

                    String articleNumber = parts[0];
                    Article article = getArticle(parts, articleNumber);

                    if (articleManager.existsArticle(articleNumber)) {
                        if (articleManager.updateArticle(article)) {
                            imported++;
                        } else {
                            errors++;
                        }
                    } else {
                        if (articleManager.insertArticle(article)) {
                            imported++;
                        } else {
                            errors++;
                        }
                    }
                } catch (Exception e) {
                    errors++;
                    System.err.println("[SettingsGUI] Fehler beim Importieren einer Zeile: " + e.getMessage());
                }
            }

            new MessageDialog()
                    .setTitle("Import Ergebnis")
                    .setMessage(String.format("<html><b>Artikel-Import abgeschlossen</b><br/><br/>" +
                                    UnicodeSymbols.CHECKMARK + " Importiert/Aktualisiert: %d<br/>" +
                                    (errors > 0 ? UnicodeSymbols.ERROR + " Fehler: %d<br/>" : "") +
                                    "</html>", imported, errors))
                    .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                    .display();

            System.out.printf("[SettingsGUI] Artikel-Import: %d erfolgreich, %d Fehler%n", imported, errors);

        } catch (Exception e) {
            new MessageDialog()
                    .setTitle("Fehler")
                    .setMessage("Fehler beim Importieren: " + e.getMessage())
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
            System.err.println("[SettingsGUI] Fehler beim Importieren der Artikel: " + e.getMessage());
            Main.logUtils.addLog(String.format("Fehler beim Importieren der Artikel: %s", e.getMessage()));
        }
    }

    private static Article getArticle(String[] parts, String articleNumber) {
        String name = parts[1];
        String details = parts[2];
        int stockQuantity = Integer.parseInt(parts[3]);
        int minStockLevel = Integer.parseInt(parts[4]);
        double sellPrice = Double.parseDouble(parts[5]);
        double purchasePrice = Double.parseDouble(parts[6]);
        String vendorName = parts[7];

        return new Article(articleNumber, name, details,
                stockQuantity, minStockLevel, sellPrice, purchasePrice, vendorName);
    }

    private static void importVendorsFromCsv(File csvFile) {
        int imported = 0;
        int errors = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                new MessageDialog()
                        .setTitle("Fehler")
                        .setMessage("Die CSV-Datei ist leer.")
                        .setMessageType(JOptionPane.ERROR_MESSAGE)
                        .display();
                return;
            }

            VendorManager vendorManager = VendorManager.getInstance();
            String line;

            while ((line = reader.readLine()) != null) {
                try {
                    String[] parts = parseCsvLine(line);
                    if (parts.length < 6) {
                        errors++;
                        continue;
                    }

                    String name = parts[0];
                    String contactPerson = parts[1];
                    String phoneNumber = parts[2];
                    String email = parts[3];
                    String address = parts[4];
                    double minOrderValue = Double.parseDouble(parts[5]);

                    Vendor vendor = new Vendor(name, contactPerson, phoneNumber, email, address, new ArrayList<>(),
                            minOrderValue);

                    if (vendorManager.existsVendor(name)) {
                        String[] columns = { "contactPerson", "phoneNumber", "email", "address" };
                        Object[] values = { contactPerson, phoneNumber, email, address };
                        if (vendorManager.updateVendor(name, columns, values)) {
                            imported++;
                        } else {
                            errors++;
                        }
                    } else {
                        if (vendorManager.insertVendor(vendor)) {
                            imported++;
                        } else {
                            errors++;
                        }
                    }
                } catch (Exception e) {
                    errors++;
                    System.err.println("[SettingsGUI] Fehler beim Importieren einer Zeile: " + e.getMessage());
                }
            }

            new MessageDialog()
                    .setMessage(
                            String.format("<html><b>Lieferanten-Import abgeschlossen</b><br/><br/>" +
                                            UnicodeSymbols.CHECKMARK + " Importiert/Aktualisiert: %d<br/>" +
                                            (errors > 0 ? UnicodeSymbols.ERROR + " Fehler: %d<br/>" : "") +
                                            "</html>", imported, errors))
                    .setTitle("Import Ergebnis")
                    .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                    .display();

            System.out.printf("[SettingsGUI] Lieferanten-Import: %d erfolgreich, %d Fehler%n", imported, errors);
            String logMessage = String.format("Lieferanten-Import: %d erfolgreich, %d Fehler", imported, errors);
            Main.logUtils.addLog(logMessage);

        } catch (Exception e) {
            new MessageDialog()
                    .setTitle("Fehler")
                    .setMessage("Fehler beim Importieren: " + e.getMessage())
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
            System.err.println("[SettingsGUI] Fehler beim Importieren der Lieferanten: " + e.getMessage());
            Main.logUtils.addLog("Fehler beim Importieren der Lieferanten: " + e.getMessage());
        }
    }

    private static void importClientsFromCsv(File csvFile) {
        int imported = 0;
        int errors = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                new MessageDialog()
                        .setTitle("Fehler")
                        .setMessage("Die CSV-Datei ist leer.")
                        .setMessageType(JOptionPane.ERROR_MESSAGE)
                        .display();
                Main.logUtils.addLog("Die CSV-Datei ist leer.");
                return;
            }

            ClientManager clientManager = ClientManager.getInstance();
            String line;

            while ((line = reader.readLine()) != null) {
                try {
                    String[] parts = parseCsvLine(line);
                    if (parts.length < 2) {
                        errors++;
                        continue;
                    }

                    String name = parts[0];
                    String department = parts[1];

                    if (clientManager.existsClient(name)) {
                        if (clientManager.updateClient(name, department)) {
                            imported++;
                        } else {
                            errors++;
                        }
                    } else {
                        if (clientManager.insertClient(name, department)) {
                            imported++;
                        } else {
                            errors++;
                        }
                    }
                } catch (Exception e) {
                    errors++;
                    System.err.println("[SettingsGUI] Fehler beim Importieren einer Zeile: " + e.getMessage());
                }
            }

            new MessageDialog()
                    .setTitle("Import Ergebnis")
                    .setMessage(String.format("<html><b>Kunden-Import abgeschlossen</b><br/><br/>" +
                                    UnicodeSymbols.CHECKMARK + " Importiert/Aktualisiert: %d<br/>" +
                                    (errors > 0 ? UnicodeSymbols.ERROR + " Fehler: %d<br/>" : "") +
                                    "</html>", imported, errors))
                    .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                    .display();

            System.out.printf("[SettingsGUI] Kunden-Import: %d erfolgreich, %d Fehler%n", imported, errors);
            String logMessage = String.format("Kunden-Import: %d erfolgreich, %d Fehler", imported, errors);
            Main.logUtils.addLog(logMessage);

        } catch (Exception e) {
            new MessageDialog()
                    .setTitle("Fehler")
                    .setMessage("Fehler beim Importieren: " + e.getMessage())
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
            System.err.println("[SettingsGUI] Fehler beim Importieren der Kunden: " + e.getMessage());
            Main.logUtils.addLog("Fehler beim Importieren der Kunden: " + e.getMessage());
        }
    }

    private static void importOrdersFromCsv() {
        new MessageDialog()
                .setTitle("Nicht verfügbar")
                .setMessage("<html><b>Bestellungs-Import nicht verfügbar</b><br/><br/>" +
                        "Der Import von Bestellungen ist aus Sicherheitsgründen deaktiviert.<br/>" +
                        "Bestellungen sollten nur über die normale Bestellfunktion erstellt werden.</html>")
                .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                .display();
        System.out.println(
                "[SettingsGUI] Bestellungs-Import wurde übersprungen (nicht implementiert aus Sicherheitsgründen)");
    }

    private static String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        if (inQuotes) {
            throw new IllegalArgumentException("Ungültiges CSV-Format: nicht geschlossenes Anführungszeichen");
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    private static String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\"\"");
    }
}
