package ch.framedev.lagersystem.guis;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import ch.framedev.lagersystem.utils.OrderLoggingUtils;
import ch.framedev.lagersystem.utils.UnicodeSymbols;
import ch.framedev.lagersystem.utils.VendorOrderLogging;

public class LogsGUI extends JFrame {

    private final List<Object[]> orderLogsData = new ArrayList<>();
    private final List<Object[]> supplierLogsData = new ArrayList<>();
    private final JTextArea logTextArea = new JTextArea();
    private final List<String[]> supplierOrderLogs = new ArrayList<>();

    public LogsGUI() {
        setTitle(UnicodeSymbols.CLIPBOARD + " Logs Übersicht");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Main panel with padding and background
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        mainPanel.setBackground(new Color(242, 245, 250));
        add(mainPanel, BorderLayout.CENTER);

        // Header panel with gradient and shadow
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(0, 0, new Color(30, 58, 95), getWidth(), 0, new Color(41, 128, 185));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g2.setColor(new Color(0,0,0,30));
                g2.fillRoundRect(4, getHeight()-8, getWidth()-8, 8, 8, 8); // subtle shadow
                g2.dispose();
                super.paintComponent(g);
            }
        };
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(18, 32, 18, 32));

        JLabel titleLabel = new JLabel(UnicodeSymbols.CLIPBOARD + " Logs Übersicht");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 30));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JLabel subtitleLabel = new JLabel(UnicodeSymbols.INFO + " Protokolle und Systemereignisse anzeigen");
        subtitleLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 17));
        subtitleLabel.setForeground(new Color(220, 230, 240, 230));
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Button panel for log categories (floating card look)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 18));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(18, 0, 18, 0));
        JButton orderLogsButton = new JButton(UnicodeSymbols.PACKAGE + " Bestellungs-Protokolle");
        styleButton(orderLogsButton, new Color(39, 174, 96), Color.WHITE);
        orderLogsButton.addActionListener(e -> {
            orderLogsData.clear();
            loadOrderLogs();
            populateOrderLogs();
        });
        JButton supplierLogsButton = new JButton(UnicodeSymbols.TRUCK + " Lieferanten-Protokolle");
        styleButton(supplierLogsButton, new Color(41, 128, 185), Color.WHITE);
        supplierLogsButton.addActionListener(e -> {
            supplierLogsData.clear();
            loadSupplierLogs();
            populateSupplierLogs();
        });
        JButton supplierOrderLogsButton = new JButton(UnicodeSymbols.DOCUMENT + " Lieferanten-Bestellungs-Protokolle");
        styleButton(supplierOrderLogsButton, new Color(142, 68, 173), Color.WHITE);
        supplierOrderLogsButton.addActionListener(e -> {
            supplierOrderLogs.clear();
            loadSupplierOrderLogs();
            populateSupplierOrderLogs();
        });
        buttonPanel.add(supplierOrderLogsButton);
        buttonPanel.add(orderLogsButton);
        buttonPanel.add(supplierLogsButton);
        mainPanel.add(buttonPanel, BorderLayout.BEFORE_FIRST_LINE);

        // Card-like content panel for logs (fills all remaining space)
        JPanel contentPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(new Color(0,0,0,10));
                g2.fillRoundRect(4, getHeight()-8, getWidth()-8, 8, 8, 8); // subtle shadow
                g2.dispose();
                super.paintComponent(g);
            }
        };
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        logTextArea.setEditable(false);
        logTextArea.setFont(SettingsGUI.getFontByName(Font.PLAIN, 15));
        logTextArea.setBackground(new Color(250, 250, 250));
        logTextArea.setForeground(new Color(44, 62, 80));
        logTextArea.setMargin(new Insets(16, 18, 16, 18));
        logTextArea.setLineWrap(true);
        logTextArea.setWrapStyleWord(true);
        logTextArea.setBorder(BorderFactory.createEmptyBorder());
        JScrollPane scrollPane = new JScrollPane(logTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // Button hover effects with smooth color transitions
        for(JButton button : new JButton[]{orderLogsButton, supplierLogsButton, supplierOrderLogsButton}) {
            button.addMouseListener(new MouseAdapter() {
                final Color orig = button.getBackground();
                final Color hover = orig.brighter();
                final Color pressed = orig.darker();
                @Override
                public void mouseEntered(MouseEvent e) {
                    button.setBackground(hover);
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    button.setBackground(orig);
                }
                @Override
                public void mousePressed(MouseEvent e) {
                    button.setBackground(pressed);
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    button.setBackground(button.contains(e.getPoint()) ? hover : orig);
                }
            });
        }
    }

    /**
     * Helper method to style buttons consistently
     */
    private void styleButton(JButton button, Color bgColor, Color fgColor) {
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 2),
                BorderFactory.createEmptyBorder(14, 24, 14, 24)
        ));
        button.setPreferredSize(new Dimension(290, 48));
    }

    private void loadOrderLogs() {
        OrderLoggingUtils orderLoggingUtils = OrderLoggingUtils.getInstance();
        for(String logEntry : orderLoggingUtils.getAllLogs()) {
            orderLogsData.add(new Object[]{logEntry});
        }
    }

    public void populateOrderLogs() {
        StringBuilder logContent = new StringBuilder();
        if(orderLogsData.isEmpty()) {
            logContent.append("Es sind keine Bestellungs-Protokolle vorhanden.");
        }
        for (Object[] logEntry : orderLogsData) {
            logContent.append(logEntry[0]).append("\n");
        }
        logTextArea.setText(logContent.toString());
    }

    private void loadSupplierLogs() {
        VendorOrderLogging vendorOrderLogging = VendorOrderLogging.getInstance();
        for(String logEntry : vendorOrderLogging.getLogs()) {
            supplierLogsData.add(new Object[]{logEntry});
        }
    }

    public void populateSupplierLogs() {
        StringBuilder logContent = new StringBuilder();
        if(supplierLogsData.isEmpty()) {
            logContent.append("Es sind keine Lieferanten-Protokolle vorhanden.");
        }
        for (Object[] logEntry : supplierLogsData) {
            logContent.append(logEntry[0]).append("\n");
        }
        logTextArea.setText(logContent.toString());
    }

    private void loadSupplierOrderLogs() {
        List<String> logs = SupplierOrderGUI.getAllSupplierOrders();
        for (String logEntry : logs) {
            supplierOrderLogs.add(new String[]{logEntry});
        }
    }

    private void populateSupplierOrderLogs() {
        StringBuilder logContent = new StringBuilder();
        if(supplierOrderLogs.isEmpty()) {
            logContent.append("Es sind keine Lieferanten-Bestellungs-Protokolle vorhanden.");
        }
        for (String[] logEntry : supplierOrderLogs) {
            logContent.append(logEntry[0]).append("\n");
        }
        logTextArea.setText(logContent.toString());
    }
}
