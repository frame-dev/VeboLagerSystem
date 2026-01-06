package ch.framedev.lagersystem.guis;

import javax.swing.*;

public class MainGUI extends JFrame {

    public MainGUI() {
        setTitle("Vebo Lager System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel content = new JPanel();
        setContentPane(content);

        JLabel title = new JLabel("Vebo Lager System", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(24.0f));
        content.add(title);

        JButton showArticlesButton = new JButton("Zeige alle Artikel an");
        showArticlesButton.addActionListener(e -> {
            ArticleGUI articleGUI = new ArticleGUI();
            articleGUI.setVisible(true);
        });
        content.add(showArticlesButton);

        JButton showVendorsButton = new JButton("Zeige alle Lieferanten an");
        showVendorsButton.addActionListener(e -> {
            VendorGUI vendorGUI = new VendorGUI();
            vendorGUI.setVisible(true);
        });
        content.add(showVendorsButton);

        JButton showAllOrdersButton = new JButton("Zeige alle Bestellungen an");
        showAllOrdersButton.addActionListener(e -> {
            OrderGUI orderGUI = new OrderGUI();
            orderGUI.setVisible(true);
        });
        content.add(showAllOrdersButton);

        JButton showClientsGUI = new JButton("Zeige alle Kunden an");
        showClientsGUI.addActionListener(e -> {
            ClientGUI clientGUI = new ClientGUI();
            clientGUI.setVisible(true);
        });
        content.add(showClientsGUI);

        pack();
        setLocationRelativeTo(null);
    }

    public void display() {
        setVisible(true);
    }
}
