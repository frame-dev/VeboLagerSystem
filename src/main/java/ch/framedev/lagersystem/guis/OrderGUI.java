package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.actions.OrderActions;

import javax.swing.*;
import java.awt.*;

public class OrderGUI extends JFrame {

    public OrderGUI() {
        setTitle("Bestellungen Verwaltung");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1200, 600);
        setPreferredSize(new Dimension(800, 600));

        JPanel content = new JPanel(new GridBagLayout());
        setContentPane(content);
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 10, 10);
        JLabel title = new JLabel("Bestellungen Verwaltung", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(24.0f));
        content.add(title, gbc);

        // Styled card area (rounded white background) with toolbar-like buttons and info text
        RoundedPanel card = new RoundedPanel(Color.WHITE, 16);
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        card.setLayout(new BorderLayout(12, 12));

        // Toolbar area with rounded buttons
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 12));
        toolbar.setOpaque(false);
        JButton newOrderButton = createRoundedButton("Neue Bestellung erstellen");
        JButton editOrderButton = createRoundedButton("Bestellung bearbeiten");
        JButton deleteOrderButton = createRoundedButton("Bestellung löschen");
        JButton completeOrderButton = createRoundedButton("Bestellung abschließen");
        toolbar.add(newOrderButton);
        toolbar.add(editOrderButton);
        toolbar.add(deleteOrderButton);
        toolbar.add(completeOrderButton);

        card.add(toolbar, BorderLayout.NORTH);

        // Center info / placeholder - use multi-line HTML for nicer centering
        JLabel infoLabel = new JLabel("<html><div style='text-align:center'>Hier können Sie eine neue Bestellung erstellen.<br/>Wählen Sie eine Bestellung zum Bearbeiten oder Löschen aus.</div></html>", SwingConstants.CENTER);
        infoLabel.setFont(infoLabel.getFont().deriveFont(16.0f));
        infoLabel.setForeground(new Color(60, 60, 60));
        JPanel centerInfo = new JPanel(new GridBagLayout());
        centerInfo.setOpaque(false);
        centerInfo.add(infoLabel);
        card.add(centerInfo, BorderLayout.CENTER);

        // Place card into main content
        gbc.gridy = 1;
        content.add(card, gbc);

        // Wire actions (keep existing OrderActions wiring for creation)
        newOrderButton.addActionListener(new OrderActions.CreateOrderAction());
        // For edit/delete we leave placeholders (could open dialogs similar to ArticleGUI/VendorGUI)
        editOrderButton.addActionListener(e -> JOptionPane.showMessageDialog(this, "Bestellung bearbeiten (noch nicht implementiert)"));
        deleteOrderButton.addActionListener(e -> JOptionPane.showMessageDialog(this, "Bestellung löschen (noch nicht implementiert)"));
        completeOrderButton.addActionListener(new OrderActions.CompleteOrderAction());
        pack();
        setLocationRelativeTo(null);

        for (JButton button : new JButton[]{newOrderButton, editOrderButton, deleteOrderButton}) {
            button.setFont(button.getFont().deriveFont(16.0f));
        }
    }

    private JButton createRoundedButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBackground(new Color(237, 242, 247));
        button.setForeground(new Color(20, 30, 40));
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 210, 220), 2),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    // small rounded panel for card styling
    private static class RoundedPanel extends JPanel {
        private final Color bg;
        private final int radius;

        RoundedPanel(Color bg, int radius) {
            this.bg = bg;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
