package ch.framedev.lagersystem.dialogs;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.guis.SettingsGUI;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.QRCodeGenerator;
import ch.framedev.lagersystem.utils.JFrameUtils;
import ch.framedev.lagersystem.utils.UnicodeSymbols;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static ch.framedev.lagersystem.utils.ArticleExporter.sanitizeForWinAnsi;

/**
 * Dialog for previewing QR codes for selected articles and exporting them as a PDF.
 */
@SuppressWarnings("DuplicatedCode")
public final class ArticleQrPreviewDialog {

    private ArticleQrPreviewDialog() {
    }

    /**
     * Shows the QR code preview dialog for the given list of selected articles. The dialog will display a grid of generated QR codes based on the article data, along with options to view them in fullscreen and export them as a PDF. If no articles are selected, a warning message will be shown instead.
     * @param parent the parent component for dialog positioning
     * @param selectedArticles the list of articles to generate QR codes for and display in the preview
     */
    public static void show(Component parent, List<Article> selectedArticles) {
        if (selectedArticles == null || selectedArticles.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                    "Bitte wählen Sie mindestens einen Artikel aus.",
                    "Keine Auswahl",
                    JOptionPane.WARNING_MESSAGE,
                    Main.iconSmall);
            return;
        }

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "QR-Code Vorschau", Dialog.ModalityType.APPLICATION_MODAL);
        ThemeManager.applyUIDefaults();
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(ThemeManager.getBackgroundColor());

        // ===== Top area (Header + Toolbar) =====
        JPanel topContainer = new JPanel();
        topContainer.setBackground(ThemeManager.getBackgroundColor());
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setBorder(BorderFactory.createEmptyBorder(14, 14, 10, 14));

        // Header card
        JFrameUtils.RoundedPanel headerPanel = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), 20);
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)
        ));
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(UnicodeSymbols.PHONE + " QR-Code Vorschau");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(ThemeManager.getTextPrimaryColor());

        JLabel subtitleLabel = new JLabel(UnicodeSymbols.INFO + " Vorschau und PDF-Export für " + selectedArticles.size() + " Artikel");
        subtitleLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        subtitleLabel.setForeground(ThemeManager.getTextSecondaryColor());

        JPanel headerText = new JPanel();
        headerText.setOpaque(false);
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerText.add(titleLabel);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(subtitleLabel);

        // Close button
        JButton closeBtn = new JButton(UnicodeSymbols.CLOSE);
        closeBtn.setToolTipText("Schließen");
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setOpaque(false);
        closeBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 18));
        closeBtn.setForeground(ThemeManager.getTextSecondaryColor());
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                closeBtn.setForeground(ThemeManager.getErrorColor());
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                closeBtn.setForeground(ThemeManager.getTextSecondaryColor());
            }
        });
        closeBtn.addActionListener(e -> dialog.dispose());

        headerPanel.add(headerText, BorderLayout.WEST);
        headerPanel.add(closeBtn, BorderLayout.EAST);

        JPanel headerWrapper = new JPanel(new BorderLayout());
        headerWrapper.setOpaque(false);
        headerWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerWrapper.add(headerPanel, BorderLayout.CENTER);

        // Toolbar card
        JFrameUtils.RoundedPanel toolbar = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), 18);
        toolbar.setLayout(new BorderLayout(10, 0));
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JLabel infoLabel = new JLabel(UnicodeSymbols.CLOCK + " Lade Vorschau …");
        infoLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        infoLabel.setForeground(ThemeManager.getTextPrimaryColor());

        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setBorderPainted(false);
        progress.setOpaque(false);

        JPanel leftInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftInfo.setOpaque(false);
        leftInfo.add(infoLabel);
        leftInfo.add(progress);

        JButton exportPdfButton = new JButton(UnicodeSymbols.DOWNLOAD + " Als PDF exportieren");
        exportPdfButton.setEnabled(false);
        exportPdfButton.setFocusPainted(false);
        exportPdfButton.setBorderPainted(true);
        exportPdfButton.setContentAreaFilled(true);
        exportPdfButton.setOpaque(true);
        exportPdfButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exportPdfButton.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));

        // themed button palette (accent)
        Color base = ThemeManager.getAccentColor();
        Color hover = ThemeManager.getButtonHoverColor(base);
        Color pressed = ThemeManager.getButtonPressedColor(base);
        exportPdfButton.setBackground(base);
        exportPdfButton.setForeground(ThemeManager.getTextOnPrimaryColor());
        exportPdfButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(base.darker(), 1),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));
        exportPdfButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (exportPdfButton.isEnabled()) exportPdfButton.setBackground(hover);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                exportPdfButton.setBackground(base);
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (exportPdfButton.isEnabled()) exportPdfButton.setBackground(pressed);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                exportPdfButton.setBackground(exportPdfButton.contains(e.getPoint()) && exportPdfButton.isEnabled() ? hover : base);
            }
        });

        toolbar.add(leftInfo, BorderLayout.CENTER);
        toolbar.add(exportPdfButton, BorderLayout.EAST);

        JPanel toolbarWrapper = new JPanel(new BorderLayout());
        toolbarWrapper.setOpaque(false);
        toolbarWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        toolbarWrapper.add(toolbar, BorderLayout.CENTER);

        topContainer.add(headerWrapper);
        topContainer.add(Box.createVerticalStrut(10));
        topContainer.add(toolbarWrapper);
        dialog.add(topContainer, BorderLayout.NORTH);

        // ===== Main card with grid =====
        JFrameUtils.RoundedPanel card = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), 18);
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        card.setLayout(new BorderLayout(8, 8));

        JPanel gridPanel = new JPanel(new GridLayout(0, 3, 16, 16));
        gridPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        gridPanel.setBackground(ThemeManager.getCardBackgroundColor());

        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        scrollPane.getViewport().setBackground(ThemeManager.getCardBackgroundColor());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        card.add(scrollPane, BorderLayout.CENTER);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(ThemeManager.getBackgroundColor());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        centerWrapper.add(card, gbc);
        dialog.add(centerWrapper, BorderLayout.CENTER);

        List<QrPreviewItem> previewItems = new ArrayList<>();

        SwingWorker<List<QrPreviewItem>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<QrPreviewItem> doInBackground() {
                List<QrPreviewItem> items = new ArrayList<>();
                for (Article article : selectedArticles) {
                    try {
                        String url = buildQrCodeUrl(article);
                        BufferedImage image = QRCodeGenerator.generateQRCodeBufferedImage(url, 220, 220);
                        items.add(new QrPreviewItem(article, image));
                    } catch (Exception ex) {
                        // skip broken entries but continue
                        // log for diagnostics (don’t spam UI)
                        ex.printStackTrace();
                    }
                }
                return items;
            }

            @Override
            protected void done() {
                try {
                    List<QrPreviewItem> items = get();
                    previewItems.clear();
                    previewItems.addAll(items);

                    gridPanel.removeAll();

                    for (QrPreviewItem item : items) {
                        JFrameUtils.RoundedPanel cell = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), 16);
                        cell.setLayout(new BorderLayout());

                        // Subtle shadow (normal) + stronger shadow (hover)
                        Color shadowNormal = new Color(0, 0, 0, 40);
                        Color shadowHover = new Color(0, 0, 0, 80);

                        Border normalBorder = BorderFactory.createCompoundBorder(
                                // subtle drop shadow bottom/right
                                BorderFactory.createMatteBorder(0, 0, 5, 5, shadowNormal),
                                // real card border + padding
                                BorderFactory.createCompoundBorder(
                                        BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                                        BorderFactory.createEmptyBorder(10, 10, 10, 10)
                                )
                        );

                        Border hoverBorder = BorderFactory.createCompoundBorder(
                                // stronger drop shadow bottom/right
                                BorderFactory.createMatteBorder(0, 0, 7, 7, shadowHover),
                                // keep border, but shift padding up (top smaller, bottom larger) to simulate lift
                                BorderFactory.createCompoundBorder(
                                        BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                                        BorderFactory.createEmptyBorder(8, 10, 12, 10)
                                )
                        );

                        cell.setBorder(normalBorder);
                        cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                        Image scaled = item.image().getScaledInstance(170, 170, Image.SCALE_SMOOTH);
                        JLabel imageLabel = new JLabel(new ImageIcon(scaled));
                        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        imageLabel.setBorder(BorderFactory.createEmptyBorder(6, 6, 8, 6));
                        imageLabel.setToolTipText("Klicken zum Vergrößern");
                        imageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        cell.add(imageLabel, BorderLayout.CENTER);

                        String labelText = item.article().getArticleNumber() + " — " + item.article().getName();
                        JLabel textLabel = new JLabel(labelText);
                        textLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
                        textLabel.setForeground(ThemeManager.getTextPrimaryColor());
                        textLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
                        cell.add(textLabel, BorderLayout.SOUTH);

                        // Shared hover/click handler (needed because child components otherwise "steal" mouse events)
                        MouseAdapter tileMouse = new MouseAdapter() {
                            @Override
                            public void mouseEntered(MouseEvent e) {
                                cell.setBorder(hoverBorder);
                                cell.repaint();
                            }

                            @Override
                            public void mouseExited(MouseEvent e) {
                                // Only revert when the mouse truly leaves the tile (not just moving between children)
                                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), cell);
                                if (!cell.contains(p)) {
                                    cell.setBorder(normalBorder);
                                    cell.repaint();
                                }
                            }

                            @Override
                            public void mouseClicked(MouseEvent e) {
                                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() >= 1) {
                                    showQrCodeFullscreen(dialog, item);
                                }
                            }
                        };

                        cell.addMouseListener(tileMouse);
                        imageLabel.addMouseListener(tileMouse);
                        textLabel.addMouseListener(tileMouse);

                        gridPanel.add(cell);
                    }

                    progress.setIndeterminate(false);
                    progress.setVisible(false);

                    infoLabel.setText(UnicodeSymbols.CHECKMARK + " Geladen: " + items.size() + " / " + selectedArticles.size() + " QR-Codes");
                    exportPdfButton.setEnabled(!items.isEmpty());

                    gridPanel.revalidate();
                    gridPanel.repaint();
                } catch (Exception ex) {
                    progress.setIndeterminate(false);
                    progress.setVisible(false);
                    infoLabel.setText(UnicodeSymbols.CLOSE + " Fehler beim Laden: " + ex.getMessage());
                }
            }
        };

        exportPdfButton.addActionListener(e -> {
            if (previewItems.isEmpty()) {
                JOptionPane.showMessageDialog(parent,
                        "Keine QR-Codes zum Export vorhanden.",
                        "QR-Codes exportieren",
                        JOptionPane.WARNING_MESSAGE,
                        Main.iconSmall);
                return;
            }

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("PDF Speichern");
            fileChooser.setSelectedFile(new File("QR_Codes_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".pdf"));

            int userSelection = fileChooser.showSaveDialog(parent);
            if (userSelection != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".pdf")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".pdf");
            }

            // UI feedback
            exportPdfButton.setEnabled(false);
            progress.setVisible(true);
            progress.setIndeterminate(true);
            infoLabel.setText(UnicodeSymbols.CLOCK + " Exportiere PDF …");

            File finalFileToSave = fileToSave;
            new SwingWorker<Void, Void>() {
                Exception error;

                @Override
                protected Void doInBackground() {
                    try {
                        exportQrCodesToPdf(finalFileToSave, previewItems);
                    } catch (Exception ex) {
                        error = ex;
                    }
                    return null;
                }

                @Override
                protected void done() {
                    progress.setIndeterminate(false);
                    progress.setVisible(false);
                    exportPdfButton.setEnabled(true);

                    if (error != null) {
                        JOptionPane.showMessageDialog(parent,
                                "Fehler beim PDF-Export: " + error.getMessage(),
                                "QR-Codes exportieren",
                                JOptionPane.ERROR_MESSAGE,
                                Main.iconSmall);
                        infoLabel.setText(UnicodeSymbols.CLOSE + " Fehler beim Export");
                        return;
                    }

                    JOptionPane.showMessageDialog(parent,
                            "PDF erfolgreich exportiert:\n" + finalFileToSave.getAbsolutePath(),
                            "QR-Codes exportieren",
                            JOptionPane.INFORMATION_MESSAGE,
                            Main.iconSmall);
                    infoLabel.setText(UnicodeSymbols.CHECKMARK + " PDF exportiert: " + finalFileToSave.getName());
                }
            }.execute();
        });

        worker.execute();

        dialog.setSize(980, 640);
        dialog.setMinimumSize(new Dimension(820, 520));
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private static String buildQrCodeUrl(Article article) {
        String data = (article != null) ? article.getQrCodeData() : null;
        if (data == null) data = "";
        String encodedData = URLEncoder.encode(data, StandardCharsets.UTF_8);
        String serverUrl = "https://framedev.ch/vebo/scan.php";
        return serverUrl + "?data=" + encodedData;
    }

    private static void exportQrCodesToPdf(File fileToSave, List<QrPreviewItem> items) throws IOException {
        Objects.requireNonNull(fileToSave, "fileToSave");
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("No QR codes to export");
        }

        try (PDDocument doc = new PDDocument()) {
            PDFont regularFont = PDType1Font.HELVETICA;

            PDRectangle pageSize = PDRectangle.A4;
            float margin = 30f;
            float pageWidth = pageSize.getWidth();
            float pageHeight = pageSize.getHeight();
            int columns = 3;
            float cellWidth = (pageWidth - (2 * margin)) / columns;
            float cellHeight = cellWidth + 40;
            float imageSize = cellWidth - 20;
            float fontSize = 9f;

            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(doc, page)) {
                float startY = pageHeight - margin;
                int col = 0;
                int row = 0;

                PDPageContentStream cs = contentStream;
                for (QrPreviewItem item : items) {
                    float x = margin + (col * cellWidth);
                    float yTop = startY - (row * cellHeight);

                    if (yTop - cellHeight < margin) {
                        cs.close();
                        page = new PDPage(pageSize);
                        doc.addPage(page);
                        cs = new PDPageContentStream(doc, page);
                        col = 0;
                        row = 0;
                        x = margin;
                        yTop = startY;
                    }

                    PDImageXObject image = LosslessFactory.createFromImage(doc, item.image());
                    float imageX = x + (cellWidth - imageSize) / 2f;
                    float imageY = yTop - imageSize;
                    cs.drawImage(image, imageX, imageY, imageSize, imageSize);

                    String labelText = item.article().getArticleNumber() + " - " + item.article().getName();
                    labelText = sanitizeForWinAnsi(labelText);
                    labelText = trimTextToWidth(labelText, regularFont, fontSize, cellWidth - 12);
                    cs.beginText();
                    cs.setFont(regularFont, fontSize);
                    cs.newLineAtOffset(x + 6, imageY - 14);
                    cs.showText(labelText);
                    cs.endText();

                    col++;
                    if (col >= columns) {
                        col = 0;
                        row++;
                    }
                }

                // If we created additional content streams, ensure the last one is closed.
                if (cs != contentStream) {
                    cs.close();
                }
            }

            doc.save(fileToSave);
        }
    }

    private static String trimTextToWidth(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        String trimmed = text;
        float textWidth = font.getStringWidth(trimmed) / 1000f * fontSize;
        if (textWidth <= maxWidth) {
            return trimmed;
        }
        while (trimmed.length() > 2 && textWidth > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
            textWidth = font.getStringWidth(trimmed + "..") / 1000f * fontSize;
        }
        return trimmed + "..";
    }

    private static void showQrCodeFullscreen(Component parent, QrPreviewItem item) {
        if (item == null || item.image() == null) return;

        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog preview = new JDialog(owner, "QR-Code Vorschau", Dialog.ModalityType.APPLICATION_MODAL);
        preview.setLayout(new BorderLayout());
        preview.getContentPane().setBackground(ThemeManager.getBackgroundColor());

        // Header (same visual language as the main dialog)
        JFrameUtils.RoundedPanel header = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), 18);
        header.setLayout(new BorderLayout());
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        String titleText = item.article().getArticleNumber() + " — " + item.article().getName();
        JLabel title = new JLabel(UnicodeSymbols.PHONE + " QR-Code – " + titleText);
        title.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        title.setForeground(ThemeManager.getTextPrimaryColor());

        JLabel hint = new JLabel(UnicodeSymbols.INFO + " ESC zum Schließen");
        hint.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        hint.setForeground(ThemeManager.getTextSecondaryColor());

        JPanel titleStack = new JPanel();
        titleStack.setOpaque(false);
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleStack.add(title);
        titleStack.add(Box.createVerticalStrut(2));
        titleStack.add(hint);

        JButton close = new JButton(UnicodeSymbols.CLOSE);
        close.setToolTipText("Schließen");
        close.setFocusPainted(false);
        close.setBorderPainted(false);
        close.setContentAreaFilled(false);
        close.setOpaque(false);
        close.setFont(SettingsGUI.getFontByName(Font.BOLD, 18));
        close.setForeground(ThemeManager.getTextSecondaryColor());
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                close.setForeground(ThemeManager.getErrorColor());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                close.setForeground(ThemeManager.getTextSecondaryColor());
            }
        });
        close.addActionListener(e -> preview.dispose());

        header.add(titleStack, BorderLayout.WEST);
        header.add(close, BorderLayout.EAST);

        JPanel headerWrap = new JPanel(new BorderLayout());
        headerWrap.setOpaque(false);
        headerWrap.setBorder(BorderFactory.createEmptyBorder(12, 12, 10, 12));
        headerWrap.add(header, BorderLayout.CENTER);
        preview.add(headerWrap, BorderLayout.NORTH);

        // Content card with the QR image (scaled to fit, but can scroll if needed)
        JFrameUtils.RoundedPanel contentCard = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), 18);
        contentCard.setLayout(new BorderLayout());
        contentCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));

        // Scale the image to a comfortable size based on screen
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int maxW = Math.max(600, (int) (screen.width * 0.75));
        int maxH = Math.max(600, (int) (screen.height * 0.75));
        int target = Math.min(maxW, maxH);
        Image scaled = item.image().getScaledInstance(target, target, Image.SCALE_SMOOTH);

        JLabel image = new JLabel(new ImageIcon(scaled));
        image.setHorizontalAlignment(SwingConstants.CENTER);
        image.setVerticalAlignment(SwingConstants.CENTER);
        image.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane sp = new JScrollPane(image);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(ThemeManager.getCardBackgroundColor());
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.getHorizontalScrollBar().setUnitIncrement(16);

        contentCard.add(sp, BorderLayout.CENTER);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(ThemeManager.getBackgroundColor());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 12, 12, 12);
        center.add(contentCard, gbc);

        preview.add(center, BorderLayout.CENTER);

        // ESC closes
        preview.getRootPane().registerKeyboardAction(
                e -> preview.dispose(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        preview.setSize(Math.min(1100, (int) (screen.width * 0.85)), Math.min(800, (int) (screen.height * 0.85)));
        preview.setMinimumSize(new Dimension(720, 520));
        preview.setLocationRelativeTo(parent);
        preview.setVisible(true);
    }

    private record QrPreviewItem(Article article, BufferedImage image) {
    }
}
