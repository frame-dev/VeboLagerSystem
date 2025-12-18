package ch.framedev.lagersystem.guis;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URL;

public class Splashscreen extends JFrame {

    private final JLabel titleLabel;
    private final JLabel subtitleLabel;
    private final JProgressBar progressBar;

    public Splashscreen() {
        setUndecorated(true);
        setAlwaysOnTop(true);

        // Content
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(new EmptyBorder(20, 20, 20, 20));
        content.setBackground(new Color(33, 150, 243)); // blaues Feld

        // Optionales Logo (legt eine Datei /logo.png im Ressourcenpfad ab)
        URL logoUrl = getClass().getResource("/logo.png");
        if (logoUrl != null) {
            ImageIcon icon = new ImageIcon(new ImageIcon(logoUrl).getImage()
                    .getScaledInstance(64, 64, Image.SCALE_SMOOTH));
            JLabel logo = new JLabel(icon);
            logo.setBorder(new EmptyBorder(0, 0, 0, 10));
            content.add(logo, BorderLayout.WEST);
        }

        // Labels
        JPanel texts = new JPanel(new GridLayout(2, 1));
        texts.setOpaque(false);
        titleLabel = new JLabel("Lagersystem", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
        subtitleLabel = new JLabel("Willkommen — Erstellt von Darryl Huber", SwingConstants.CENTER);
        subtitleLabel.setForeground(new Color(230, 230, 230));
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(12f));
        texts.add(titleLabel);
        texts.add(subtitleLabel);
        content.add(texts, BorderLayout.CENTER);

        // Progressbar (determiniert, Prozentanzeige)
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setMinimum(0);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setBorderPainted(false);
        progressBar.setBackground(new Color(0, 0, 0, 0));
        progressBar.setForeground(new Color(255, 255, 255, 200));
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(progressBar, BorderLayout.SOUTH);
        content.add(south, BorderLayout.SOUTH);

        setContentPane(content);
        setPreferredSize(new Dimension(480, 180));
        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Zeigt den Splashscreen für die angegebene Dauer (Millisekunden).
     * Fortschritt erhöht sich jede Sekunde bis zum Ende. Nicht-blockierend.
     */
    public void showSplash(int durationMillis) {
        if (SwingUtilities.isEventDispatchThread()) {
            doShowSplash(durationMillis);
        } else {
            SwingUtilities.invokeLater(() -> doShowSplash(durationMillis));
        }
    }

    private void doShowSplash(int durationMillis) {
        setVisible(true);

        if (durationMillis <= 0) {
            // sofort schließen
            setVisible(false);
            dispose();
            return;
        }

        final int tickInterval = 1000; // 1 Sekunde
        final int steps = (int) Math.max(1, Math.ceil(durationMillis / (double) tickInterval));

        progressBar.setMaximum(steps);
        progressBar.setValue(0);

        final Timer[] tickTimerHolder = new Timer[1];

        Timer tickTimer = new Timer(tickInterval, e -> {
            int v = progressBar.getValue() + 1;
            if (v > progressBar.getMaximum()) v = progressBar.getMaximum();
            progressBar.setValue(v);
            // Stoppen wenn erreicht
            if (v >= progressBar.getMaximum()) {
                Timer t = tickTimerHolder[0];
                if (t != null && t.isRunning()) t.stop();
            }
        });
        tickTimer.setInitialDelay(tickInterval);
        tickTimerHolder[0] = tickTimer;
        tickTimer.start();

        Timer closeTimer = new Timer(durationMillis, e -> {
            // Sicherstellen, dass Balken voll ist
            progressBar.setValue(progressBar.getMaximum());
            // Stoppe Tick-Timer
            Timer t = tickTimerHolder[0];
            if (t != null && t.isRunning()) t.stop();
            setVisible(false);
            dispose();
            MainGUI mainGUI = new MainGUI();
            mainGUI.display();
        });
        closeTimer.setRepeats(false);
        closeTimer.start();
    }
}