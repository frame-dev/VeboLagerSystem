package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.managers.ArticleManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Helper methods for bulk actions on article selections.
 */
public final class ArticleBulkActions {

    private ArticleBulkActions() {
    }

    public static void deleteSelectedArticles(Component parent,
                                              JTable articleTable,
                                              Runnable updateCountLabel,
                                              Consumer<Boolean> setUpdatingTable,
                                              Icon icon) {
        int[] selectedRows = articleTable.getSelectedRows();
        if (selectedRows == null || selectedRows.length == 0) {
            JOptionPane.showMessageDialog(parent,
                    "Bitte w\u00e4hlen Sie mindestens einen Artikel aus.",
                    "Keine Auswahl",
                    JOptionPane.WARNING_MESSAGE,
                    icon);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(parent,
                "Moechten Sie " + selectedRows.length + " Artikel wirklich loeschen?",
                "Artikel loeschen",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                icon);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        List<Integer> modelRows = new ArrayList<>();
        for (int selectedRow : selectedRows) {
            modelRows.add(articleTable.convertRowIndexToModel(selectedRow));
        }
        modelRows.sort(Comparator.reverseOrder());

        ArticleManager articleManager = ArticleManager.getInstance();
        setUpdatingTable.accept(true);
        for (int modelRow : modelRows) {
            String artikelnummer = String.valueOf(model.getValueAt(modelRow, 0));
            if (articleManager.deleteArticleByNumber(artikelnummer)) {
                model.removeRow(modelRow);
            }
        }
        setUpdatingTable.accept(false);
        updateCountLabel.run();
    }

    public static void adjustStockForSelectedArticles(Component parent,
                                                      JTable articleTable,
                                                      List<Article> selectedArticles,
                                                      Consumer<Boolean> setUpdatingTable,
                                                      Icon icon) {
        if (selectedArticles == null || selectedArticles.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                    "Bitte w\u00e4hlen Sie mindestens einen Artikel aus.",
                    "Keine Auswahl",
                    JOptionPane.WARNING_MESSAGE,
                    icon);
            return;
        }

        String input = JOptionPane.showInputDialog(parent,
                "Bestandsaenderung eingeben (z.B. 5 oder -3):",
                "Bestand anpassen",
                JOptionPane.QUESTION_MESSAGE);
        if (input == null) {
            return;
        }
        Integer delta = parseInteger(input.trim());
        if (delta == null) {
            JOptionPane.showMessageDialog(parent,
                    "Bitte eine gueltige Ganzzahl eingeben.",
                    "Ungueltige Eingabe",
                    JOptionPane.WARNING_MESSAGE,
                    icon);
            return;
        }

        boolean hasNegative = false;
        for (Article article : selectedArticles) {
            if (article.getStockQuantity() + delta < 0) {
                hasNegative = true;
                break;
            }
        }
        if (hasNegative) {
            int confirm = JOptionPane.showConfirmDialog(parent,
                    "Einige Artikel wuerden negativ werden.\nBestand fuer diese Artikel auf 0 setzen?",
                    "Bestand anpassen",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    icon);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        ArticleManager articleManager = ArticleManager.getInstance();
        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        setUpdatingTable.accept(true);
        for (int selectedRow : articleTable.getSelectedRows()) {
            int modelRow = articleTable.convertRowIndexToModel(selectedRow);
            String artikelnummer = String.valueOf(model.getValueAt(modelRow, 0));
            Article article = articleManager.getArticleByNumber(artikelnummer);
            if (article == null) {
                continue;
            }
            int newStock = Math.max(0, article.getStockQuantity() + delta);
            Article updated = new Article(
                    article.getArticleNumber(),
                    article.getName(),
                    article.getDetails(),
                    newStock,
                    article.getMinStockLevel(),
                    article.getSellPrice(),
                    article.getPurchasePrice(),
                    article.getVendorName()
            );
            if (articleManager.updateArticle(updated)) {
                model.setValueAt(newStock, modelRow, 4);
            }
        }
        setUpdatingTable.accept(false);
    }

    public static void exportSelectedArticles(Component parent,
                                              List<Article> selectedArticles,
                                              Icon icon) {
        if (selectedArticles == null || selectedArticles.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                    "Bitte w\u00e4hlen Sie mindestens einen Artikel aus.",
                    "Keine Auswahl",
                    JOptionPane.WARNING_MESSAGE,
                    icon);
            return;
        }

        Object[] options = {"CSV", "PDF", "Abbrechen"};
        int choice = JOptionPane.showOptionDialog(parent,
                "Auswahl exportieren als:",
                "Export",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                icon,
                options,
                options[0]);
        if (choice == 0) {
            ArticleCsvExporter.exportArticlesToCsv(parent, selectedArticles, icon);
        } else if (choice == 1) {
            ArticleSelectionPdfExporter.exportSelectedArticlesPdf(parent, selectedArticles, icon);
        }
    }

    private static Integer parseInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str) {
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
