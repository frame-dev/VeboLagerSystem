package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Warning;
import ch.framedev.lagersystem.guis.MainGUI;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class SchedulerManager {

    private static SchedulerManager instance;

    private final ScheduledExecutorService executor;
    private List<Article> articles;
    private final WarningManager warningManager = WarningManager.getInstance();

    private SchedulerManager() {
        this.executor = Executors.newScheduledThreadPool(1);
        this.articles = loadArticles();
    }

    public static SchedulerManager getInstance() {
        if (instance == null) {
            instance = new SchedulerManager();
        }
        return instance;
    }

    private List<Article> loadArticles() {
        ArticleManager articleManager = ArticleManager.getInstance();
        this.articles = articleManager.getAllArticles();
        return articles;
    }

    public void run() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (Article article : this.articles) {
            if (article.getStockQuantity() <= article.getMinStockLevel()) {
                String title = "Low Stock Warning for Article " + article.getArticleNumber();
                if (!warningManager.hasWarning(title)) {
                    Warning warning = new Warning(
                            title,
                            "The stock for article '" + article.getName() + "' (ID: " + article.getArticleNumber() + ") is low. Current stock: " + article.getStockQuantity(),
                            Warning.WarningType.LOW_STOCK,
                            dateFormat.format(new Date()),
                            false,
                            true
                    );
                    if (MainGUI.articleGUI != null)
                        MainGUI.articleGUI.displayWarning(warning);
                    if (!warningManager.insertWarning(warning)) {
                        JOptionPane.showMessageDialog(null, "Failed to insert warning for article " + article.getArticleNumber(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
    }
}
