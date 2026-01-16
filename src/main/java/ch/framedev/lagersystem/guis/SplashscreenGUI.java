package ch.framedev.lagersystem.guis;

import javax.swing.*;

@SuppressWarnings("unused")
public class SplashscreenGUI extends JFrame {

    public SplashscreenGUI() {
        setTitle("Lagersystem - Splashscreen");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setUndecorated(true);
    }
}
