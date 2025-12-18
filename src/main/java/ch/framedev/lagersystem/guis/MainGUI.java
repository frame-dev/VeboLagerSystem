package ch.framedev.lagersystem.guis;

import javax.swing.*;

public class MainGUI extends JFrame {

    public MainGUI() {
        setTitle("Lager übersicht");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    public void display() {
        SwingUtilities.invokeLater(() -> {
            setVisible(true);
        });
    }
}
