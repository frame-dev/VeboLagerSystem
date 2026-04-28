package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.managers.SchedulerManager;
import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.dialogs.MessageDialog;
import ch.framedev.lagersystem.utils.JFrameUtils;
import ch.framedev.lagersystem.utils.KeyboardShortcutUtils;
import ch.framedev.lagersystem.utils.QRCodeUtils;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.UnicodeSymbols;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.Supplier;

import static ch.framedev.lagersystem.utils.JFrameUtils.getSelectedArticles;

/**
 * Modern Main Dashboard for VEBO Lagersystem with Tabbed Interface
 */
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("DuplicatedCode")
public class MainGUI extends JFrame {
    private static final Logger logger = LogManager.getLogger(MainGUI.class);
    private static final String TAB_LOADED_PROPERTY = "loaded";
    private static final String EMBEDDED_CONTENT_PROPERTY = "embeddedContent";
    private static final String EMBEDDED_SHORTCUT_ACTION_PREFIX = "main.embedded.shortcut.";
    private static final int DASHBOARD_CARD_RADIUS = 18;
    private static final int HEADER_CLOCK_UPDATE_INTERVAL_MS = 10_000;

    private static final class EmbeddedShortcut {
        private final int tabIndex;
        private final JFrame frame;
        private final Object actionKey;

        private EmbeddedShortcut(int tabIndex, JFrame frame, Object actionKey) {
            this.tabIndex = tabIndex;
            this.frame = frame;
            this.actionKey = actionKey;
        }
    }

    private static final class EmbeddedShortcutBinding {
        @SuppressWarnings("unused")
        private final Object fallbackActionKey;
        private final Action fallbackAction;
        @SuppressWarnings("unused")
        private final String dispatcherActionId;
        private final List<EmbeddedShortcut> shortcuts = new ArrayList<>();

        private EmbeddedShortcutBinding(Object fallbackActionKey, Action fallbackAction, String dispatcherActionId) {
            this.fallbackActionKey = fallbackActionKey;
            this.fallbackAction = fallbackAction;
            this.dispatcherActionId = dispatcherActionId;
        }
    }

    private static final class TabContentWrapper extends JPanel {
        private Dimension stablePreferredSize;

        private TabContentWrapper() {
            super(new BorderLayout());
            setBackground(ThemeManager.getBackgroundColor());
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(8, 10, 10, 10),
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(ThemeManager.withAlpha(ThemeManager.getBorderColor(), 180), 1, true),
                            BorderFactory.createEmptyBorder(4, 4, 4, 4))));
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            super.setBounds(x, y, width, height);
            if (width > 0 && height > 0) {
                stablePreferredSize = new Dimension(width, height);
            }
        }

        @Override
        public Dimension getPreferredSize() {
            if (stablePreferredSize != null) {
                return new Dimension(stablePreferredSize);
            }
            return super.getPreferredSize();
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(0, 0);
        }
    }

    private JTabbedPane tabbedPane;
    private final Map<KeyStroke, EmbeddedShortcutBinding> embeddedShortcutBindings = new LinkedHashMap<>();
    private final DateTimeFormatter headerDateFormat = DateTimeFormatter.ofPattern("dd. MMMM", Locale.GERMAN);
    private final DateTimeFormatter headerTimeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN);
    private final DateTimeFormatter headerWarningTimeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN);
    private JLabel headerDateValueLabel;
    private JLabel headerTimeValueLabel;
    private JLabel headerWarningCheckLabel;
    private Timer headerClockTimer;

    /**
     * IMPORTANT: These are the actual content frames for each tab. We keep them as
     * fields to maintain state and avoid reloading when switching tabs. They are
     * initialized lazily when the user first clicks on the respective tab. Do NOT
     * dispose these frames when switching tabs, as that would kill listeners and
     * resources. Instead, we just add/remove their content panels to the tab
     * wrappers.
     */
    public static ArticleGUI articleGUI;
    private VendorGUI vendorGUI;
    private OrderGUI orderGUI;
    private ClientGUI clientGUI;
    private SupplierOrderGUI supplierOrderGUI;
    private LogsGUI logsGUI;
    private SettingsGUI settingsGUI;
    private NotesGUI notesGUI;

    // Tab wrappers
    private final TabContentWrapper articleWrapper = createTabWrapper();
    private final TabContentWrapper vendorWrapper = createTabWrapper();
    private final TabContentWrapper orderWrapper = createTabWrapper();
    private final TabContentWrapper clientWrapper = createTabWrapper();
    private final TabContentWrapper supplierOrderWrapper = createTabWrapper();
    private final TabContentWrapper logsWrapper = createTabWrapper();

    /**
     * Constructs the main GUI window for the VEBO Lagersystem application. This
     * constructor initializes the main window, sets up the layout, and prepares the
     * header, tabbed pane, and footer. It also registers the window with the
     * ThemeManager to ensure that theme changes are applied correctly. The
     * constructor does not load the content of the tabs immediately; instead, it
     * sets up lazy loading to optimize performance and resource usage. The main
     * window is configured to be maximized on startup and includes a custom icon if
     * available.
     */
    public MainGUI() {
        logger.info("Initializing MainGUI window");
        ThemeManager.getInstance().registerWindow(this);
        ThemeManager.applyUIDefaults();
        ThemeManager.JOptionPaneTheme.apply();

        setTitle("VEBO Lagersystem");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        if (Main.icon != null) {
            setIconImage(Main.icon.getImage());
        }
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(ThemeManager.getBackgroundColor());

        // HEADER
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // TABS
        initializeTabbedPane();
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // FOOTER
        JPanel footerPanel = createFooterPanel();
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        installGlobalShortcuts();
        startHeaderClock();
        logger.info("MainGUI window initialized");
    }

    @Override
    public void dispose() {
        logger.info("Disposing MainGUI window");
        stopHeaderClock();
        ThemeManager.getInstance().unregisterWindow(this);
        super.dispose();
    }

    /**
     * Creates a wrapper panel with padding for tab content
     */
    private static TabContentWrapper createTabWrapper() {
        return new TabContentWrapper();
    }

    private static JComponent extractEmbeddedTabContent(JFrame frame) {
        Object existingContent = frame.getRootPane().getClientProperty(EMBEDDED_CONTENT_PROPERTY);
        if (existingContent instanceof JComponent component) {
            return component;
        }

        Container contentPane = frame.getContentPane();
        JComponent embeddedContent;
        if (contentPane instanceof JComponent component) {
            embeddedContent = component;
        } else {
            JPanel fallback = new JPanel(new BorderLayout());
            fallback.setOpaque(false);
            fallback.add(contentPane, BorderLayout.CENTER);
            embeddedContent = fallback;
        }

        embeddedContent.setMinimumSize(new Dimension(0, 0));
        frame.getRootPane().putClientProperty(EMBEDDED_CONTENT_PROPERTY, embeddedContent);

        JPanel placeholder = new JPanel(new BorderLayout());
        placeholder.setOpaque(false);
        frame.setContentPane(placeholder);

        return embeddedContent;
    }

    private JFrame getTabFrame(int tabIndex) {
        return switch (tabIndex) {
            case 0 -> {
                if (articleGUI == null) {
                    articleGUI = new ArticleGUI();
                }
                yield articleGUI;
            }
            case 1 -> {
                if (vendorGUI == null) {
                    vendorGUI = new VendorGUI();
                }
                yield vendorGUI;
            }
            case 2 -> {
                if (orderGUI == null) {
                    orderGUI = new OrderGUI();
                }
                yield orderGUI;
            }
            case 3 -> {
                if (clientGUI == null) {
                    clientGUI = new ClientGUI();
                }
                yield clientGUI;
            }
            case 4 -> {
                if (supplierOrderGUI == null) {
                    supplierOrderGUI = new SupplierOrderGUI();
                }
                yield supplierOrderGUI;
            }
            case 5 -> {
                if (logsGUI == null) {
                    logsGUI = new LogsGUI();
                }
                yield logsGUI;
            }
            default -> null;
        };
    }

    /**
     * Loads the content for a specific tab into the wrapper.
     * IMPORTANT: We do NOT dispose of the child frames. Disposing kills
     * listeners/resources.
     */
    private void loadTabContent(int tabIndex, JPanel wrapper) {
        wrapper.removeAll();
        JFrame frame = null;
        try {
            frame = getTabFrame(tabIndex);
        } catch (Exception ex) {
            logger.error("Error loading tab content for tab {}: {}", tabIndex, ex.getMessage(), ex);
            return;
        }
        if (frame == null) {
            logger.warn("No frame found for tab index {}", tabIndex);
            return;
        }
        frame.setVisible(false);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        syncEmbeddedShortcuts(tabIndex, frame);
        wrapper.add(extractEmbeddedTabContent(frame), BorderLayout.CENTER);
        wrapper.revalidate();
        wrapper.repaint();
        logger.debug("Loaded tab content for tab {}", tabIndex);
    }

    private void syncEmbeddedShortcuts(int tabIndex, JFrame frame) {
        removeEmbeddedShortcutsForTab(tabIndex);
        if (frame == null || frame.getRootPane() == null) {
            return;
        }

        InputMap inputMap = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        if (inputMap == null) {
            return;
        }

        KeyStroke[] keys = inputMap.allKeys();
        if (keys == null) {
            return;
        }

        for (KeyStroke keyStroke : keys) {
            if (keyStroke == null || isIgnoredEmbeddedShortcut(keyStroke)) {
                continue;
            }

            Object actionKey = inputMap.get(keyStroke);
            if (actionKey == null) {
                continue;
            }

            EmbeddedShortcutBinding binding = embeddedShortcutBindings.computeIfAbsent(
                    keyStroke,
                    this::createEmbeddedShortcutBinding);
            binding.shortcuts.add(new EmbeddedShortcut(tabIndex, frame, actionKey));
        }
    }

    private EmbeddedShortcutBinding createEmbeddedShortcutBinding(KeyStroke keyStroke) {
        JRootPane rootPane = getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();
        Object fallbackActionKey = inputMap.get(keyStroke);
        Action fallbackAction = fallbackActionKey == null ? null : actionMap.get(fallbackActionKey);
        String dispatcherActionId = EMBEDDED_SHORTCUT_ACTION_PREFIX + embeddedShortcutBindings.size();

        inputMap.put(keyStroke, dispatcherActionId);
        actionMap.put(dispatcherActionId, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (!dispatchEmbeddedShortcut(keyStroke, e)) {
                    EmbeddedShortcutBinding currentBinding = embeddedShortcutBindings.get(keyStroke);
                    if (currentBinding != null && currentBinding.fallbackAction != null) {
                        currentBinding.fallbackAction.actionPerformed(e);
                    }
                }
            }
        });

        return new EmbeddedShortcutBinding(fallbackActionKey, fallbackAction, dispatcherActionId);
    }

    private boolean dispatchEmbeddedShortcut(KeyStroke keyStroke, java.awt.event.ActionEvent event) {
        EmbeddedShortcutBinding binding = embeddedShortcutBindings.get(keyStroke);
        if (binding == null || binding.shortcuts.isEmpty()) {
            return false;
        }

        int selectedTabIndex = tabbedPane == null ? -1 : tabbedPane.getSelectedIndex();
        for (int i = binding.shortcuts.size() - 1; i >= 0; i--) {
            EmbeddedShortcut shortcut = binding.shortcuts.get(i);
            if (shortcut.tabIndex != selectedTabIndex || shortcut.frame == null || shortcut.frame.getRootPane() == null) {
                continue;
            }

            Action action = shortcut.frame.getRootPane().getActionMap().get(shortcut.actionKey);
            if (action == null || !action.isEnabled()) {
                continue;
            }

            action.actionPerformed(new java.awt.event.ActionEvent(
                    shortcut.frame,
                    java.awt.event.ActionEvent.ACTION_PERFORMED,
                    String.valueOf(shortcut.actionKey)
            ));
            return true;
        }
        return false;
    }

    private void removeEmbeddedShortcutsForTab(int tabIndex) {
        for (EmbeddedShortcutBinding binding : embeddedShortcutBindings.values()) {
            binding.shortcuts.removeIf(shortcut -> shortcut.tabIndex == tabIndex);
        }
    }

    private boolean isIgnoredEmbeddedShortcut(KeyStroke keyStroke) {
        return keyStroke.getKeyCode() == KeyEvent.VK_ESCAPE && keyStroke.getModifiers() == 0;
    }

    private void reapplyEmbeddedShortcuts() {
        if (articleGUI != null) {
            syncEmbeddedShortcuts(0, articleGUI);
        }
        if (vendorGUI != null) {
            syncEmbeddedShortcuts(1, vendorGUI);
        }
        if (orderGUI != null) {
            syncEmbeddedShortcuts(2, orderGUI);
        }
        if (clientGUI != null) {
            syncEmbeddedShortcuts(3, clientGUI);
        }
        if (supplierOrderGUI != null) {
            syncEmbeddedShortcuts(4, supplierOrderGUI);
        }
        if (logsGUI != null) {
            syncEmbeddedShortcuts(5, logsGUI);
        }
    }

    /**
     * Creates the header panel with title, subtitle, settings button, and date
     */
    private JPanel createHeaderPanel() {
        JFrameUtils.GradientPanel headerPanel = getGradientPanel();
        headerPanel.add(createHeaderTextPanel(), BorderLayout.WEST);
        headerPanel.add(createHeaderRightPanel(), BorderLayout.EAST);
        headerPanel.add(createHeaderMetaStrip(), BorderLayout.SOUTH);

        return headerPanel;
    }

    private static JFrameUtils.GradientPanel getGradientPanel() {
        JFrameUtils.GradientPanel headerPanel = new JFrameUtils.GradientPanel(
                ThemeManager.getHeaderBackgroundColor(),
                ThemeManager.getHeaderGradientColor());
        headerPanel.setLayout(new BorderLayout(18, 12));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(0, 0, 0, 30)),
                BorderFactory.createEmptyBorder(16, 20, 12, 20)));
        return headerPanel;
    }

    /**
     * Creates the left side of the header with title and subtitle
     */
    private JPanel createHeaderTextPanel() {
        JPanel headerTextPanel = new JPanel(new BorderLayout(12, 0));
        headerTextPanel.setOpaque(false);

        Font headerIconFont = SettingsGUI.getFontByName(Font.BOLD, 34);
        JLabel iconLabel = new JLabel(UnicodeSymbols.safeSymbol(UnicodeSymbols.PACKAGE, "PKG", headerIconFont));
        iconLabel.setFont(getEmojiCapableFont(headerIconFont));
        iconLabel.setForeground(ThemeManager.getTitleTextColor());
        iconLabel.setVerticalAlignment(SwingConstants.TOP);
        iconLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        headerTextPanel.add(iconLabel, BorderLayout.WEST);

        JPanel copyPanel = new JPanel();
        copyPanel.setLayout(new BoxLayout(copyPanel, BoxLayout.Y_AXIS));
        copyPanel.setOpaque(false);

        JLabel dashboardBadge = createHeaderBadge(UnicodeSymbols.BULB + " Desktop-Dashboard", false);
        dashboardBadge.setAlignmentX(Component.LEFT_ALIGNMENT);
        copyPanel.add(dashboardBadge);
        copyPanel.add(Box.createVerticalStrut(6));

        JLabel titleLabel = new JLabel("VEBO Lagersystem");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 40));
        titleLabel.setForeground(ThemeManager.getTitleTextColor());
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        copyPanel.add(titleLabel);
        copyPanel.add(Box.createVerticalStrut(2));

        JLabel subtitleLabel = new JLabel("Zentrale Verwaltung fuer Artikel, Bestellungen und Lieferanten");
        subtitleLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        subtitleLabel.setForeground(ThemeManager.getSubTitleTextColor());
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        copyPanel.add(subtitleLabel);
        copyPanel.add(Box.createVerticalStrut(6));

        JLabel statusLabel = new JLabel("Direkte Schnellaktionen und eingebettete Arbeitsbereiche.");
        statusLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        statusLabel.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 210));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        copyPanel.add(statusLabel);

        headerTextPanel.add(copyPanel, BorderLayout.CENTER);

        return headerTextPanel;
    }

    /**
     * Creates the right side of the header with settings button and date
     */
    private JPanel createHeaderRightPanel() {
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);

        Font headerButtonFont = SettingsGUI.getFontByName(Font.BOLD, 13);
        JButton settingsButton = createHeaderActionButton(
            UnicodeSymbols.safeSymbol(UnicodeSymbols.BETTER_GEAR, "CFG", headerButtonFont) + " Einstellungen",
            KeyboardShortcutUtils.withShortcutHint("Einstellungen des Programms öffnen",
                    KeyboardShortcutUtils.menuKey(KeyEvent.VK_COMMA)),
            false,
            e -> settingsGUI = showOrCreateWindow(settingsGUI, SettingsGUI::new));

        JButton notesButton = createHeaderActionButton(
            UnicodeSymbols.safeSymbol(UnicodeSymbols.CLIPBOARD, "CLIP", headerButtonFont) + " Notizen",
            KeyboardShortcutUtils.withShortcutHint("Persönliche Notizen verwalten",
                    KeyboardShortcutUtils.menuShiftKey(KeyEvent.VK_N)),
            false,
            e -> notesGUI = showOrCreateWindow(notesGUI, NotesGUI::new));

        JButton converterButton = createHeaderActionButton(
            UnicodeSymbols.safeSymbol(UnicodeSymbols.CALCULATOR, "CALC", headerButtonFont)
                + " Rechner",
            KeyboardShortcutUtils.withShortcutHint("Einheitenrechner und Befüllungshilfe öffnen",
                    KeyboardShortcutUtils.menuShiftKey(KeyEvent.VK_C)),
            true,
            e -> openConverterForSelection());

        JPanel quickActionsCard = createHeaderCard(new FlowLayout(FlowLayout.LEFT, 8, 0));
        quickActionsCard.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel quickActionsLabel = new JLabel("Schnellzugriff");
        quickActionsLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 11));
        quickActionsLabel.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 220));
        quickActionsCard.add(quickActionsLabel);
        quickActionsCard.add(converterButton);
        quickActionsCard.add(notesButton);
        quickActionsCard.add(settingsButton);

        JPanel dateCard = createHeaderCard(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        dateCard.setAlignmentX(Component.RIGHT_ALIGNMENT);
        JLabel calendarIconLabel = new JLabel(UnicodeSymbols.CALENDAR);
        calendarIconLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        calendarIconLabel.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 220));
        dateCard.add(calendarIconLabel);

        headerDateValueLabel = new JLabel();
        headerDateValueLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        headerDateValueLabel.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 220));
        dateCard.add(headerDateValueLabel);

        JLabel separatorLabel = new JLabel("•");
        separatorLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        separatorLabel.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 180));
        dateCard.add(separatorLabel);

        JLabel clockIconLabel = new JLabel(UnicodeSymbols.CLOCK);
        clockIconLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        clockIconLabel.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 220));
        dateCard.add(clockIconLabel);

        headerTimeValueLabel = new JLabel();
        headerTimeValueLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        headerTimeValueLabel.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 220));
        dateCard.add(headerTimeValueLabel);
        updateHeaderDateTime();

        JPanel warningCard = createHeaderCard(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        warningCard.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel warningIconLabel = new JLabel(UnicodeSymbols.WARNING);
        warningIconLabel.setFont(getEmojiCapableFont(SettingsGUI.getFontByName(Font.PLAIN, 12)));
        warningIconLabel.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 220));
        warningCard.add(warningIconLabel);

        headerWarningCheckLabel = new JLabel();
        headerWarningCheckLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        headerWarningCheckLabel.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 220));
        warningCard.add(headerWarningCheckLabel);
        updateHeaderWarningSchedule();

        rightPanel.add(quickActionsCard);
        rightPanel.add(Box.createVerticalStrut(6));
        rightPanel.add(dateCard);
        rightPanel.add(Box.createVerticalStrut(6));
        rightPanel.add(warningCard);

        return rightPanel;
    }

    private JPanel createHeaderMetaStrip() {
        JPanel metaStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        metaStrip.setOpaque(false);
        metaStrip.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        metaStrip.add(createHeaderBadge(UnicodeSymbols.TAG + " 6 Arbeitsbereiche", false));
        metaStrip.add(createHeaderBadge(UnicodeSymbols.DOCUMENT + " "
                + KeyboardShortcutUtils.formatKeyStroke(KeyboardShortcutUtils.menuKey(KeyEvent.VK_1)) + "-"
                + KeyboardShortcutUtils.formatKeyStroke(KeyboardShortcutUtils.menuKey(KeyEvent.VK_6)), false));
        metaStrip.add(createHeaderBadge(UnicodeSymbols.CLOCK + " F1 fuer Hilfe", false));
        return metaStrip;
    }

    private void installGlobalShortcuts() {
        JRootPane rootPane = getRootPane();
        KeyboardShortcutUtils.register(rootPane, "main.settings",
                KeyboardShortcutUtils.menuKey(KeyEvent.VK_COMMA),
                () -> settingsGUI = showOrCreateWindow(settingsGUI, SettingsGUI::new));
        KeyboardShortcutUtils.register(rootPane, "main.notes",
                KeyboardShortcutUtils.menuShiftKey(KeyEvent.VK_N),
                () -> notesGUI = showOrCreateWindow(notesGUI, NotesGUI::new));
        KeyboardShortcutUtils.register(rootPane, "main.converter",
                KeyboardShortcutUtils.menuShiftKey(KeyEvent.VK_C),
                this::openConverterForSelection);
        KeyboardShortcutUtils.register(rootPane, "main.help",
                KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0),
                this::showHelp);

        for (int tabIndex = 0; tabIndex < 6; tabIndex++) {
            final int selectedTabIndex = tabIndex;
            KeyboardShortcutUtils.register(rootPane, "main.tab." + tabIndex,
                    KeyboardShortcutUtils.menuKey(KeyEvent.VK_1 + tabIndex),
                    () -> tabbedPane.setSelectedIndex(selectedTabIndex));
        }

        reapplyEmbeddedShortcuts();
    }

    private JButton createHeaderActionButton(String text, String tooltip, boolean primary,
                                             java.awt.event.ActionListener listener) {
        JButton button = new JButton(text);
        styleHeaderButton(button, primary);
        button.setToolTipText(tooltip);
        button.addActionListener(listener);
        button.setAlignmentX(Component.RIGHT_ALIGNMENT);
        Dimension preferredSize = button.getPreferredSize();
        int minWidth = primary ? 150 : 138;
        int minHeight = 40;
        button.setPreferredSize(new Dimension(Math.max(minWidth, preferredSize.width), Math.max(minHeight, preferredSize.height)));
        return button;
    }

    private void openConverterForSelection() {
        if (articleGUI == null || articleGUI.articleTable == null) {
            new MessageDialog()
                    .setTitle("Keine Auswahl")
                    .setMessage("Bitte zuerst den Artikel-Tab öffnen und einen Artikel auswählen.")
                    .setMessageType(JOptionPane.WARNING_MESSAGE)
                    .display();
            return;
        }

        List<Article> articles = getSelectedArticles(articleGUI.articleTable);
        if (articles.isEmpty()) {
            new MessageDialog()
                    .setTitle("Keine Auswahl")
                    .setMessage("Keine Artikel ausgewählt.")
                    .setMessageType(JOptionPane.WARNING_MESSAGE)
                    .display();
            return;
        }

        Article first = articles.getFirst();
        if (articles.size() > 1) {
            new MessageDialog()
                    .setTitle("Hinweis")
                    .setMessage("Mehr als ein Artikel ausgewählt. Es wird der erste Artikel verwendet: " + first.getName())
                    .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                    .display();
        }

        ConverterGUI converterGUI = new ConverterGUI(first);
        converterGUI.setVisible(true);
    }

    /**
     * Initializes the tabbed pane with all tabs and lazy loading
     */
    private void initializeTabbedPane() {
        int fontSizeTab = getTabFontSize();
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFont(getEmojiCapableFont(SettingsGUI.getFontByName(Font.BOLD, fontSizeTab + 3)));
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        tabbedPane.setBackground(ThemeManager.getBackgroundColor());
        tabbedPane.setForeground(ThemeManager.getTextPrimaryColor());
        tabbedPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8, 12, 6, 12),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeManager.withAlpha(ThemeManager.getBorderColor(), 170), 1, true),
                        BorderFactory.createEmptyBorder(2, 2, 2, 2))));

        addTabs();

        applyTabBackgrounds();
        applyTabStyling();

        setupLazyLoading();

        loadTabContent(0, articleWrapper);
        articleWrapper.putClientProperty(TAB_LOADED_PROPERTY, Boolean.TRUE);
        setJMenuBar(createMenuBar());
    }

    /**
     * Gets the tab font size from settings
     */
    private int getTabFontSize() {
        if (Main.settings == null) {
            return SettingsGUI.TAB_FONT_SIZE;
        }
        String fontSizeTabStr = Main.settings.getProperty("table_font_size_tab");
        int fontSizeTab = SettingsGUI.TAB_FONT_SIZE;
        try {
            fontSizeTab = Integer.parseInt(fontSizeTabStr);
        } catch (NumberFormatException ignored) {
        }
        return fontSizeTab;
    }

    /**
     * Adds all tabs to the tabbed pane
     */
    private void addTabs() {
        Font tabFont = tabbedPane.getFont();
        String tabPackage = UnicodeSymbols.safeSymbol(UnicodeSymbols.PACKAGE, "PKG", tabFont);
        String tabTruck = UnicodeSymbols.safeSymbol(UnicodeSymbols.TRUCK, "TRK", tabFont);
        String tabClipboard = UnicodeSymbols.safeSymbol(UnicodeSymbols.CLIPBOARD, "CLIP", tabFont);
        String tabPeople = UnicodeSymbols.safeSymbol(UnicodeSymbols.PEOPLE, "USERS", tabFont);
        addDashboardTab("Artikel", tabPackage, articleWrapper, "Artikelverwaltung", 0);
        addDashboardTab("Lieferanten", tabTruck, vendorWrapper, "Lieferantenverwaltung", 1);
        addDashboardTab("Bestellungen", tabClipboard, orderWrapper, "Bestellungsverwaltung", 2);
        addDashboardTab("Kunden", tabPeople, clientWrapper, "Kundenverwaltung", 3);
        addDashboardTab("Lieferantenbestellungen", tabTruck + " " + tabPackage, supplierOrderWrapper,
                "Lieferantenbestellungen verwalten", 4);
        addDashboardTab("Protokolle", tabClipboard, logsWrapper, "Systemprotokolle anzeigen", 5);
    }

    private void addDashboardTab(String title, String icon, JPanel content, String tooltip, int index) {
        tabbedPane.addTab(title, null, content, tooltip);
        tabbedPane.setTabComponentAt(index,
                createTabComponent(icon, title, KeyboardShortcutUtils.menuKey(KeyEvent.VK_1 + index)));
    }

    /**
     * Applies theme-safe subtle backgrounds to tabs
     */
    private void applyTabBackgrounds() {
        updateTabColors(tabbedPane.getSelectedIndex());
    }

    private void updateTabColors(int selectedIndex) {
        Color unselectedBg = ThemeManager.getSurfaceColor();
        Color selectedBg = ThemeManager.getCardBackgroundColor();
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            boolean selected = i == selectedIndex;
            tabbedPane.setBackgroundAt(i, selected ? selectedBg : unselectedBg);
            tabbedPane.setForegroundAt(i,
                    selected ? ThemeManager.getTextPrimaryColor() : ThemeManager.getTextSecondaryColor());
            updateTabComponentColors(i, selected);
        }
    }

    /**
     * Applies modern styling to tab UI (padding, borders, shadows)
     */
    private void applyTabStyling() {
        tabbedPane.setOpaque(true);
        tabbedPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeManager.withAlpha(ThemeManager.getBorderColor(), 175), 1, true),
                        BorderFactory.createEmptyBorder(3, 3, 3, 3)),
                BorderFactory.createLineBorder(
                        ThemeManager.adjustColor(ThemeManager.getBorderColor(), ThemeManager.isDarkMode() ? 16 : -8),
                        1, true)));
    }

    /**
     * Sets up lazy loading for tabs
     */
    private void setupLazyLoading() {
        tabbedPane.addChangeListener(e -> {
            int idx = tabbedPane.getSelectedIndex();

            updateTabColors(idx);

            // Lazy load content
            JPanel wrapper = (JPanel) tabbedPane.getComponentAt(idx);
            if (wrapper.getClientProperty(TAB_LOADED_PROPERTY) == null) {
                loadTabContent(idx, wrapper);
                wrapper.putClientProperty(TAB_LOADED_PROPERTY, Boolean.TRUE);
            }
        });
    }

    /**
     * Creates the footer panel with copyright information
     */
    private JPanel createFooterPanel() {
        JPanel footerPanel = new JPanel(new BorderLayout(12, 0));
        footerPanel.setBackground(ThemeManager.getSurfaceColor());
        footerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.withAlpha(ThemeManager.getBorderColor(), 170)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));

        JLabel footerLabel = new JLabel(
                UnicodeSymbols.CALENDAR + " 2026 VEBO Lagersystem  |  Entwickelt von Darryl Huber  |  Version " + Main.VERSION);
        footerLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 10));
        footerLabel.setForeground(ThemeManager.getTextSecondaryColor());
        footerPanel.add(footerLabel, BorderLayout.WEST);

        JPanel footerBadges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        footerBadges.setOpaque(false);
        footerBadges.add(createFooterBadge(UnicodeSymbols.TAG + " "
                + KeyboardShortcutUtils.formatKeyStroke(KeyboardShortcutUtils.menuKey(KeyEvent.VK_1)) + "-"
                + KeyboardShortcutUtils.formatKeyStroke(KeyboardShortcutUtils.menuKey(KeyEvent.VK_6))));
        footerBadges.add(createFooterBadge(UnicodeSymbols.BETTER_GEAR + " "
                + KeyboardShortcutUtils.formatKeyStroke(KeyboardShortcutUtils.menuKey(KeyEvent.VK_COMMA))));
        footerPanel.add(footerBadges, BorderLayout.EAST);

        return footerPanel;
    }

    /**
     * Styles a header button with theme colors and hover effects
     */
    private void styleHeaderButton(JButton button, boolean primary) {
        Color base = primary
                ? ThemeManager.getAccentColor()
                : ThemeManager.adjustColor(ThemeManager.getHeaderBackgroundColor(), ThemeManager.isDarkMode() ? 18 : 34);
        Color hover = ThemeManager.getButtonHoverColor(base);
        Color pressed = ThemeManager.getButtonPressedColor(base);
        Color borderColor = primary
                ? ThemeManager.adjustColor(base, ThemeManager.isDarkMode() ? -18 : -28)
                : ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 110);

        button.setFont(getEmojiCapableFont(SettingsGUI.getFontByName(Font.BOLD, 13)));
        button.setForeground(ThemeManager.getTextOnPrimaryColor());
        button.setBackground(base);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 1, true),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)));
        button.setMargin(new Insets(0, 0, 0, 0));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(hover);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 150), 1, true),
                        BorderFactory.createEmptyBorder(8, 15, 8, 15)));
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                button.setBackground(base);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(borderColor, 1, true),
                        BorderFactory.createEmptyBorder(8, 15, 8, 15)));
            }

            @Override
            public void mousePressed(MouseEvent evt) {
                button.setBackground(pressed);
            }

            @Override
            public void mouseReleased(MouseEvent evt) {
                button.setBackground(button.contains(evt.getPoint()) ? hover : base);
            }
        });
    }

    private JPanel createHeaderCard(LayoutManager layout) {
        JFrameUtils.RoundedPanel panel = new JFrameUtils.RoundedPanel(
                ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), ThemeManager.isDarkMode() ? 16 : 32),
                DASHBOARD_CARD_RADIUS);
        panel.setLayout(layout);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 70), 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        return panel;
    }

    private void startHeaderClock() {
        stopHeaderClock();
        headerClockTimer = new Timer(HEADER_CLOCK_UPDATE_INTERVAL_MS, e -> updateHeaderDateTime());
        headerClockTimer.setInitialDelay(0);
        headerClockTimer.start();
    }

    private void stopHeaderClock() {
        if (headerClockTimer != null) {
            headerClockTimer.stop();
            headerClockTimer = null;
        }
    }

    private void updateHeaderDateTime() {
        LocalDateTime now = LocalDateTime.now();
        if (headerDateValueLabel != null) {
            headerDateValueLabel.setText(headerDateFormat.format(now));
        }
        if (headerTimeValueLabel != null) {
            headerTimeValueLabel.setText(headerTimeFormat.format(now));
        }
        updateHeaderWarningSchedule();
    }

    private void updateHeaderWarningSchedule() {
        if (headerWarningCheckLabel == null) {
            return;
        }

        if (!isWarningDisplayEnabled()) {
            headerWarningCheckLabel.setText("Warnpruefung aus");
            return;
        }

        long nextWarningCheckMillis = SchedulerManager.getInstance().getNextWarningDisplayAtMillis();
        if (nextWarningCheckMillis <= 0L) {
            long fallbackMillis = getConfiguredWarningDisplayIntervalMillis();
            if (fallbackMillis > 0L) {
                nextWarningCheckMillis = System.currentTimeMillis() + fallbackMillis;
            }
        }

        if (nextWarningCheckMillis > 0L) {
            headerWarningCheckLabel.setText("Naechste Warnpruefung " + LocalDateTime.ofInstant(Instant.ofEpochMilli(nextWarningCheckMillis), ZoneId.systemDefault()).format(headerWarningTimeFormat));
        } else {
            headerWarningCheckLabel.setText("Warnpruefung wird vorbereitet");
        }
    }

    private boolean isWarningDisplayEnabled() {
        if (Main.settings == null) {
            return true;
        }

        String configured = Main.settings.getProperty("enable_hourly_warnings");
        if (configured != null) {
            return Boolean.parseBoolean(configured);
        }

        String legacyConfigured = Main.settings.getProperty("enable_houtly_warnings");
        return legacyConfigured == null || Boolean.parseBoolean(legacyConfigured);
    }

    private long getConfiguredWarningDisplayIntervalMillis() {
        if (Main.settings == null) {
            return 60L * 60L * 1000L;
        }

        String rawInterval = Main.settings.getProperty("warning_display_interval");
        if (rawInterval == null || rawInterval.isBlank()) {
            return 60L * 60L * 1000L;
        }

        try {
            long hours = Long.parseLong(rawInterval.trim());
            return hours > 0L ? hours * 60L * 60L * 1000L : 60L * 60L * 1000L;
        } catch (NumberFormatException ignored) {
            return 60L * 60L * 1000L;
        }
    }

    private JLabel createHeaderBadge(String text, boolean highlighted) {
        JLabel label = new JLabel(text);
        label.setFont(SettingsGUI.getFontByName(Font.BOLD, 10));
        label.setForeground(ThemeManager.getTextOnPrimaryColor());
        label.setOpaque(true);
        label.setBackground(highlighted
                ? ThemeManager.withAlpha(ThemeManager.getAccentColor(), 170)
                : ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), ThemeManager.isDarkMode() ? 18 : 32));
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 85), 1, true),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        return label;
    }

    private JPanel createTabComponent(String icon, String title, KeyStroke shortcut) {
        JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        tabPanel.setOpaque(true);
        tabPanel.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));

        JLabel iconLabel = new JLabel(icon == null ? "" : icon);
        iconLabel.setFont(getEmojiCapableFont(SettingsGUI.getFontByName(Font.BOLD, 13)));

        JLabel titleLabel = new JLabel(title == null ? "" : title);
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));

        JLabel shortcutLabel = new JLabel("(" + KeyboardShortcutUtils.formatKeyStroke(shortcut) + ")");
        shortcutLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 10));

        tabPanel.putClientProperty("main.tab.iconLabel", iconLabel);
        tabPanel.putClientProperty("main.tab.titleLabel", titleLabel);
        tabPanel.putClientProperty("main.tab.shortcutLabel", shortcutLabel);

        tabPanel.add(iconLabel);
        tabPanel.add(titleLabel);
        tabPanel.add(shortcutLabel);
        return tabPanel;
    }

    private void updateTabComponentColors(int tabIndex, boolean selected) {
        Component tabComponent = tabbedPane.getTabComponentAt(tabIndex);
        if (!(tabComponent instanceof JPanel panel)) {
            return;
        }

        Color background = selected
                ? ThemeManager.withAlpha(ThemeManager.getAccentColor(), ThemeManager.isDarkMode() ? 90 : 55)
                : ThemeManager.getSurfaceColor();
        panel.setBackground(background);

        JLabel iconLabel = (JLabel) panel.getClientProperty("main.tab.iconLabel");
        JLabel titleLabel = (JLabel) panel.getClientProperty("main.tab.titleLabel");
        JLabel shortcutLabel = (JLabel) panel.getClientProperty("main.tab.shortcutLabel");

        if (iconLabel != null) {
            iconLabel.setForeground(selected
                    ? ThemeManager.getAccentColor()
                    : ThemeManager.getTextSecondaryColor());
        }
        if (titleLabel != null) {
            titleLabel.setForeground(selected
                    ? ThemeManager.getTextPrimaryColor()
                    : ThemeManager.getTextPrimaryColor());
        }
        if (shortcutLabel != null) {
            shortcutLabel.setForeground(selected
                    ? ThemeManager.adjustColor(ThemeManager.getAccentColor(), ThemeManager.isDarkMode() ? 45 : -10)
                    : ThemeManager.getTextSecondaryColor());
        }
    }

    private JLabel createFooterBadge(String text) {
        JLabel label = new JLabel(text);
        label.setFont(SettingsGUI.getFontByName(Font.PLAIN, 10));
        label.setForeground(ThemeManager.getTextSecondaryColor());
        label.setOpaque(true);
        label.setBackground(ThemeManager.getCardBackgroundColor());
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.withAlpha(ThemeManager.getBorderColor(), 170), 1, true),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        return label;
    }

    private static Font getEmojiCapableFont(Font baseFont) {
        if (baseFont == null) {
            return null;
        }
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.contains("mac")) {
            // Use logical font to allow emoji fallback on macOS.
            return new Font("Dialog", baseFont.getStyle(), baseFont.getSize());
        }
        return baseFont;
    }

    /**
     * Displays the main GUI window by making it visible.
     * This method sets the visibility of the {@code MainGUI} frame to {@code true},
     * allowing the user to interact with the application's graphical interface.
     */
    public void display() {
        setVisible(true);
    }

    private <T extends JFrame> T showOrCreateWindow(T existing, Supplier<T> factory) {
        T window = existing;
        if (window == null || !window.isDisplayable()) {
            window = factory.get();
        }
        if (!window.isVisible()) {
            window.setVisible(true);
        }
        window.toFront();
        window.requestFocus();
        return window;
    }

    /**
     * Creates and configures the menu bar for the application.
     * The menu bar includes a "Werkzeuge" menu with options for generating QR codes
     * for all articles or for selected articles. Each menu item is associated with
     * its respective action.
     *
     * @return the configured {@code JMenuBar} instance
     */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu helpMenu = new JMenu("Hilfe");
        JMenuItem settingsMenuItem = new JMenuItem("Einstellungen");
        settingsMenuItem.setAccelerator(KeyboardShortcutUtils.menuKey(KeyEvent.VK_COMMA));
        settingsMenuItem.addActionListener(e -> settingsGUI = showOrCreateWindow(settingsGUI, SettingsGUI::new));
        JMenuItem useHelpMenuItem = new JMenuItem("Hilfe zur Anwendung");
        useHelpMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        useHelpMenuItem.addActionListener(e -> showHelp());

        helpMenu.add(settingsMenuItem);
        helpMenu.addSeparator();
        helpMenu.add(useHelpMenuItem);

        JMenu toolsMenu = new JMenu("Werkzeuge");
        JMenuItem qrCodesGeneratorMenuItem = new JMenuItem("QR-Codes generieren");
        qrCodesGeneratorMenuItem.addActionListener(e -> generateQrCodesForArticles());
        JMenuItem qrCodesSelectedMenuItem = new JMenuItem("QR-Codes für Auswahl");
        qrCodesSelectedMenuItem.addActionListener(e -> generateQrCodesForSelectedArticles());
        toolsMenu.add(qrCodesGeneratorMenuItem);
        toolsMenu.add(qrCodesSelectedMenuItem);

        menuBar.add(helpMenu);
        menuBar.add(toolsMenu);
        return menuBar;
    }

    public void showHelp() {
        try {
            Path helpFile = resolveHelpFile();
            if (helpFile == null || !Files.exists(helpFile)) {
                logger.warn("Help file not found");
                new MessageDialog()
                        .setTitle("Hilfe")
                        .setMessage("Hilfe konnte nicht geöffnet werden (help.html nicht gefunden).")
                        .setMessageType(JOptionPane.WARNING_MESSAGE)
                        .display();
                return;
            }

            java.net.URI helpUri = helpFile.toUri();
            if (openHelpUri(helpUri, helpFile)) {
                logger.info("Help opened: {}", helpUri);
                return;
            }

            logger.warn("Help file found but could not be opened: {}", helpFile);
            new MessageDialog()
                    .setTitle("Hilfe")
                    .setMessage("Hilfe konnte nicht geöffnet werden. Datei: " + helpFile.toAbsolutePath())
                    .setMessageType(JOptionPane.WARNING_MESSAGE)
                    .display();
        } catch (Exception e) {
            logger.error("Error opening help: {}", e.getMessage(), e);
            new MessageDialog()
                    .setTitle("Hilfe")
                    .setMessage("Hilfe konnte nicht geöffnet werden: " + e.getMessage())
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
        }
    }

    private Path resolveHelpFile() throws IOException {
        Path webHelpPath = Path.of("web", "help.html");
        if (Files.exists(webHelpPath)) {
            return webHelpPath.toAbsolutePath();
        }

        try (InputStream inputStream = getClass().getResourceAsStream("/help.html")) {
            if (inputStream == null) {
                return null;
            }

            Path tempFile = Files.createTempFile("vebo-help-", ".html");
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            tempFile.toFile().deleteOnExit();
            return tempFile;
        }
    }

    private boolean openHelpUri(java.net.URI helpUri, Path helpFile) {
        if (helpUri == null) {
            return false;
        }

        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(helpUri);
                    return true;
                }
            } catch (Exception ex) {
                logger.warn("Desktop browse failed for help URI: {}", helpUri, ex);
            }

            try {
                if (helpFile != null && desktop.isSupported(Desktop.Action.OPEN)) {
                    desktop.open(helpFile.toFile());
                    return true;
                }
            } catch (Exception ex) {
                logger.warn("Desktop open failed for help file: {}", helpFile, ex);
            }
        }

        return openHelpUriWithSystemCommand(helpUri);
    }

    private boolean openHelpUriWithSystemCommand(java.net.URI helpUri) {
        if (helpUri == null) {
            return false;
        }

        String uri = helpUri.toString();
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        List<String> command;

        if (osName.contains("win")) {
            command = List.of("cmd", "/c", "start", "", uri);
        } else if (osName.contains("mac")) {
            command = List.of("open", uri);
        } else {
            command = List.of("xdg-open", uri);
        }

        try {
            new ProcessBuilder(command).start();
            return true;
        } catch (IOException ex) {
            logger.warn("System help open command failed: {}", command, ex);
            return false;
        }
    }

    /**
     * Generates QR codes for all articles in the application.
     * <p>
     * This method retrieves the full list of articles using the article manager.
     * If no articles are available, it displays a warning dialog to inform the
     * user.
     * Otherwise, it invokes the {@code generateQrCodesForList} method to generate
     * QR codes for the retrieved list of articles with a confirmation prompt.
     * <p>
     * The confirmation prompt text is localized and asks the user whether they want
     * to generate QR codes for all articles.
     */
    private void generateQrCodesForArticles() {
        List<Article> articles = ArticleManager.getInstance().getAllArticles();
        if (articles == null || articles.isEmpty()) {
            new MessageDialog()
                    .setTitle("Keine Artikel")
                    .setMessage("Keine Artikel vorhanden.")
                    .setMessageType(JOptionPane.WARNING_MESSAGE)
                    .display();
            return;
        }
        generateQrCodesForList(articles, "QR-Codes fuer alle Artikel generieren?");
    }

    /**
     * Generates QR codes for the selected articles within the application.
     * <p>
     * This method retrieves a list of selected articles from the article GUI and
     * initiates QR code generation if a valid selection is made. If the article
     * GUI is not initialized or no articles are selected, it displays a warning
     * dialog to notify the user of the missing selection.
     * <p>
     * The confirmation prompt includes the total count of selected articles, asking
     * the user for confirmation before proceeding with the QR code generation.
     * <p>
     * Preconditions:
     * - The article GUI tab must be opened, and at least one article should be
     * selected.
     * <p>
     * Postconditions:
     * - Invokes the {@code generateQrCodesForList} method to generate QR codes
     * for the list of selected articles, following confirmation from the user.
     * <p>
     * Warning messages:
     * - If the article GUI is not initialized, a dialog indicates that the article
     * tab must be opened.
     * - If no articles are selected, a dialog indicates that at least one article
     * must be chosen.
     */
    private void generateQrCodesForSelectedArticles() {
        if (articleGUI == null) {
            new MessageDialog()
                    .setTitle("Keine Auswahl")
                    .setMessage("Bitte zuerst den Artikel-Tab oeffnen und eine Auswahl treffen.")
                    .setMessageType(JOptionPane.WARNING_MESSAGE)
                    .display();
            return;
        }
        List<Article> selectedArticles = getSelectedArticles(articleGUI.articleTable);
        if (selectedArticles.isEmpty()) {
            new MessageDialog()
                    .setTitle("Keine Auswahl")
                    .setMessage("Bitte waehlen Sie mindestens einen Artikel aus.")
                    .setMessageType(JOptionPane.WARNING_MESSAGE)
                    .display();
            return;
        }
        generateQrCodesForList(selectedArticles,
                "QR-Codes fuer " + selectedArticles.size() + " ausgewählte Artikel generieren?");
    }

    /**
     * Generates QR codes for a given list of articles after displaying a
     * confirmation dialog.
     * <p>
     * This method allows the user to confirm whether they want to proceed with
     * generating QR codes
     * for the provided list of articles. A progress dialog is displayed during the
     * generation process.
     * Upon completion, the user is notified about the result, including the number
     * of QR codes
     * generated and the output directory.
     *
     * @param articles   the list of articles for which QR codes will be generated
     * @param promptText the text to display in the confirmation dialog prompt
     */
    private void generateQrCodesForList(List<Article> articles, String promptText) {
        if (articles == null)
            return;
        if (promptText == null)
            promptText = "";
        File outputDir = new File(Main.getAppDataDir(), "qr_codes");
        int result = new MessageDialog()
                .setTitle("QR-Codes generieren")
                .setMessage(promptText + "\nSpeicherort: " + outputDir.getAbsolutePath())
                .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                .setOptionType(JOptionPane.OK_CANCEL_OPTION)
                .displayWithOptions();
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        JDialog progressDialog = new JDialog(this, "QR-Codes generieren", true);
        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        progressDialog.setLayout(new BorderLayout());
        JLabel label = new JLabel("Bitte warten, QR-Codes werden erstellt...");
        label.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        progressDialog.add(label, BorderLayout.CENTER);
        progressDialog.pack();
        progressDialog.setLocationRelativeTo(this);

        SwingWorker<List<File>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<File> doInBackground() {
                return QRCodeUtils.createQrCodes(articles);
            }

            @Override
            protected void done() {
                progressDialog.dispose();
                try {
                    List<File> files = get();
                    if (files == null || files.isEmpty()) {
                        new MessageDialog()
                                .setTitle("QR-Codes generieren")
                                .setMessage("Es konnten keine QR-Codes erstellt werden.")
                                .setMessageType(JOptionPane.WARNING_MESSAGE)
                                .display();
                        return;
                    }
                    new MessageDialog()
                            .setTitle("QR-Codes generieren")
                            .setMessage("QR-Codes wurden erstellt.\nAnzahl: " + files.size() + "\nOrdner: "
                                    + outputDir.getAbsolutePath())
                            .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                            .display();
                } catch (Exception ex) {
                    new MessageDialog()
                            .setTitle("QR-Codes generieren")
                            .setMessage("Fehler beim Erstellen der QR-Codes: " + ex.getMessage())
                            .setMessageType(JOptionPane.ERROR_MESSAGE)
                            .display();
                }
            }
        };

        worker.execute();
        progressDialog.setVisible(true);
    }
}
