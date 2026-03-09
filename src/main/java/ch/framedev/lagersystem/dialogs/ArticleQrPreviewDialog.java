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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
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
public final class ArticleQrPreviewDialog {
    // Reusable header panel builder
    private static JPanel buildHeaderPanel(String title, String subtitle, Runnable onClose) {
        JFrameUtils.RoundedPanel headerPanel = buildCard(20, 14, 18, 14, 18);
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(ThemeManager.getTextPrimaryColor());

        JLabel subtitleLabel = new JLabel(subtitle);
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

        JButton closeBtn = createHeaderCloseButton(onClose);

        headerPanel.add(headerText, BorderLayout.WEST);
        headerPanel.add(closeBtn, BorderLayout.EAST);

        JPanel headerWrapper = new JPanel(new BorderLayout());
        headerWrapper.setOpaque(false);
        headerWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerWrapper.add(headerPanel, BorderLayout.CENTER);
        return headerWrapper;
    }

    // Reusable card builder for QR preview and fullscreen
    private static JFrameUtils.RoundedPanel buildCard(int radius, int top, int left, int bottom, int right) {
        JFrameUtils.RoundedPanel card = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), radius);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(top, left, bottom, right)
        ));
        return card;
    }
    private static final Dimension PREVIEW_DIALOG_SIZE = new Dimension(980, 640);
    private static final Dimension PREVIEW_DIALOG_MIN_SIZE = new Dimension(820, 520);
    private static final int TILE_SIZE = 170;
    private static final int TILE_GAP = 16;
    private static final int GRID_TARGET_CELL_WIDTH = 250;

    private ArticleQrPreviewDialog() {
    }

    /**
     * Shows the QR code preview dialog for the given list of selected articles. The dialog will display a grid of generated QR codes based on the article data, along with options to view them in fullscreen and export them as a PDF. If no articles are selected, a warning message will be shown instead.
     * @param parent the parent component for dialog positioning
     * @param selectedArticles the list of articles to generate QR codes for and display in the preview
     */
    public static void show(Component parent, List<Article> selectedArticles) {
        if (parent == null) {
            throw new IllegalArgumentException("Parent component must not be null.");
        }
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
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.getContentPane().setBackground(ThemeManager.getBackgroundColor());

        // ===== Top area (Header + Toolbar) =====
        JPanel topContainer = new JPanel();
        topContainer.setBackground(ThemeManager.getBackgroundColor());
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setBorder(BorderFactory.createEmptyBorder(14, 14, 10, 14));
        topContainer.add(buildHeaderPanel(
            UnicodeSymbols.PHONE + " QR-Code Vorschau",
            UnicodeSymbols.INFO + " Vorschau und PDF-Export für " + selectedArticles.size() + " Artikel",
            dialog::dispose
        ));
        topContainer.add(Box.createVerticalStrut(10));
        // Toolbar card
        JFrameUtils.RoundedPanel toolbar = buildCard(18, 10, 12, 10, 12);
        toolbar.setLayout(new BorderLayout(10, 0));
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
        exportPdfButton.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        styleAccentButton(exportPdfButton, ThemeManager.getAccentColor());
        toolbar.add(leftInfo, BorderLayout.CENTER);
        toolbar.add(exportPdfButton, BorderLayout.EAST);
        JPanel toolbarWrapper = new JPanel(new BorderLayout());
        toolbarWrapper.setOpaque(false);
        toolbarWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        toolbarWrapper.add(toolbar, BorderLayout.CENTER);
        topContainer.add(toolbarWrapper);
        dialog.add(topContainer, BorderLayout.NORTH);

        // ===== Main card with grid =====
        JFrameUtils.RoundedPanel card = buildCard(18, 12, 12, 12, 12);
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
        scrollPane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateGridColumns(gridPanel, scrollPane);
            }
        });
        updateGridColumns(gridPanel, scrollPane);

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
            int failedCount = 0;

            @Override
            protected List<QrPreviewItem> doInBackground() {
                List<QrPreviewItem> items = new ArrayList<>();
                for (Article article : selectedArticles) {
                    try {
                        String url = buildQrCodeUrl(article);
                        BufferedImage image = QRCodeGenerator.generateQRCodeBufferedImage(url, 220, 220);
                        items.add(new QrPreviewItem(article, image));
                    } catch (Exception ex) {
                        failedCount++;
                        Main.logUtils.addLog("QR preview generation failed for article: " +
                                (article == null ? "null" : article.getArticleNumber()));
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
                        gridPanel.add(createPreviewTile(dialog, item));
                    }

                    progress.setIndeterminate(false);
                    progress.setVisible(false);

                    if (items.isEmpty()) {
                        infoLabel.setText(UnicodeSymbols.WARNING + " Keine QR-Codes konnten erzeugt werden");
                    } else if (failedCount > 0) {
                        infoLabel.setText(UnicodeSymbols.WARNING + " Geladen: " + items.size() + " / " + selectedArticles.size() + " (" + failedCount + " Fehler)");
                    } else {
                        infoLabel.setText(UnicodeSymbols.CHECKMARK + " Geladen: " + items.size() + " / " + selectedArticles.size() + " QR-Codes");
                    }
                    exportPdfButton.setEnabled(!items.isEmpty());

                    gridPanel.revalidate();
                    gridPanel.repaint();
                } catch (Exception ex) {
                    progress.setIndeterminate(false);
                    progress.setVisible(false);
                    infoLabel.setText(UnicodeSymbols.CLOSE + " Fehler beim Laden: " + ex.getMessage());
                } finally {
                    dialog.setCursor(Cursor.getDefaultCursor());
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
            if (fileToSave.exists()) {
                int overwrite = JOptionPane.showConfirmDialog(
                        parent,
                        "Die Datei existiert bereits. Überschreiben?",
                        "Datei überschreiben",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        Main.iconSmall
                );
                if (overwrite != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            // UI feedback
            exportPdfButton.setEnabled(false);
            progress.setVisible(true);
            progress.setIndeterminate(true);
            infoLabel.setText(UnicodeSymbols.CLOCK + " Exportiere PDF …");
            dialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

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
                    dialog.setCursor(Cursor.getDefaultCursor());

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

        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        dialog.getRootPane().registerKeyboardAction(
                e -> {
                    if (exportPdfButton.isEnabled()) {
                        exportPdfButton.doClick();
                    }
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        dialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        worker.execute();

        dialog.setSize(PREVIEW_DIALOG_SIZE);
        dialog.setMinimumSize(PREVIEW_DIALOG_MIN_SIZE);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private static JButton createHeaderCloseButton(Runnable onClose) {
        JButton closeBtn = new JButton(UnicodeSymbols.CLOSE);
        closeBtn.setToolTipText("Schließen");
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setOpaque(false);
        closeBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 18));
        closeBtn.setForeground(ThemeManager.getTextSecondaryColor());
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeBtn.setForeground(ThemeManager.getErrorColor());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeBtn.setForeground(ThemeManager.getTextSecondaryColor());
            }
        });
        closeBtn.addActionListener(e -> {
            if (onClose != null) {
                onClose.run();
            }
        });
        return closeBtn;
    }

    private static void styleAccentButton(JButton button, Color base) {
        if (button == null || base == null) {
            return;
        }
        Color hover = ThemeManager.getButtonHoverColor(base);
        Color pressed = ThemeManager.getButtonPressedColor(base);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBackground(base);
        button.setForeground(ThemeManager.getTextOnPrimaryColor());
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(base.darker(), 1),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(hover);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(base);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(pressed);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                button.setBackground(button.contains(e.getPoint()) && button.isEnabled() ? hover : base);
            }
        });
    }

    private static void updateGridColumns(JPanel gridPanel, JScrollPane scrollPane) {
        if (gridPanel == null || scrollPane == null) {
            return;
        }
        int viewportWidth = Math.max(0, scrollPane.getViewport().getWidth());
        if (viewportWidth <= 0) {
            return;
        }
        int columns = Math.max(1, viewportWidth / GRID_TARGET_CELL_WIDTH);
        LayoutManager layout = gridPanel.getLayout();
        if (layout instanceof GridLayout gridLayout && gridLayout.getColumns() != columns) {
            gridLayout.setColumns(columns);
            gridLayout.setHgap(TILE_GAP);
            gridLayout.setVgap(TILE_GAP);
            gridPanel.revalidate();
        }
    }

    private static JPanel createPreviewTile(JDialog parentDialog, QrPreviewItem item) {
        JFrameUtils.RoundedPanel cell = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), 16);
        cell.setLayout(new BorderLayout());

        Color shadowNormal = new Color(0, 0, 0, 40);
        Color shadowHover = new Color(0, 0, 0, 80);

        Border normalBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 5, 5, shadowNormal),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)
                )
        );

        Border hoverBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 7, 7, shadowHover),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                        BorderFactory.createEmptyBorder(8, 10, 12, 10)
                )
        );

        cell.setBorder(normalBorder);
        cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Image scaled = item.image().getScaledInstance(TILE_SIZE, TILE_SIZE, Image.SCALE_SMOOTH);
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

        MouseAdapter tileMouse = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                cell.setBorder(hoverBorder);
                cell.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), cell);
                if (!cell.contains(p)) {
                    cell.setBorder(normalBorder);
                    cell.repaint();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() >= 1) {
                    showQrCodeFullscreen(parentDialog, item);
                }
            }
        };

        cell.addMouseListener(tileMouse);
        imageLabel.addMouseListener(tileMouse);
        textLabel.addMouseListener(tileMouse);
        return cell;
    }

    private static String buildQrCodeUrl(Article article) {
        String data = (article != null) ? article.getQrCodeData() : null;
        if (data == null) data = "";
        String encodedData = URLEncoder.encode(data, StandardCharsets.UTF_8);
        String serverUrl = "https://framedev.ch/vebo/scan.php";
        String serverUrlSettings = Main.settings.getProperty("server_url");
        if (serverUrlSettings != null && !serverUrlSettings.isBlank()) {
            if(serverUrl.startsWith("https://framedev.ch/vebo")) {
                serverUrl = serverUrlSettings + "/scan.php";
            } else {
                serverUrl = serverUrlSettings;
            }
        }
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
        String titleText = item.article().getArticleNumber() + " — " + item.article().getName();
        JPanel headerWrap = new JPanel(new BorderLayout());
        headerWrap.setOpaque(false);
        headerWrap.setBorder(BorderFactory.createEmptyBorder(12, 12, 10, 12));
        headerWrap.add(buildHeaderPanel(
            UnicodeSymbols.PHONE + " QR-Code – " + titleText,
            UnicodeSymbols.INFO + " ESC zum Schließen",
            preview::dispose
        ), BorderLayout.CENTER);
        preview.add(headerWrap, BorderLayout.NORTH);

        // Content card with the QR image (scaled to fit, but can scroll if needed)
        JFrameUtils.RoundedPanel contentCard = buildCard(18, 18, 18, 18, 18);
        contentCard.setLayout(new BorderLayout());

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
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
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
