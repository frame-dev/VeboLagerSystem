package ch.framedev.lagersystem.classes;

import ch.framedev.lagersystem.utils.ArticleUtils;

/**
 * Represents a catalog article with inventory and pricing data.
 */
public class Article {

    /**
     * Unique article identifier.
     */
    private String articleNumber;
    /**
     * Display name of the article.
     */
    private String name;
    /**
     * Optional free-text description.
     */
    private String details;
    /**
     * Current stock quantity in inventory.
     */
    private int stockQuantity;
    /**
     * Minimum stock level threshold for warnings.
     */
    private int minStockLevel;
    /**
     * Sales price per unit.
     */
    private double sellPrice;
    /**
     * Purchase price per unit.
     */
    private double purchasePrice;
    /**
     * Name of the vendor or supplier.
     */
    private String vendorName;

    /**
     * Creates a new article instance.
     *
     * @param articleNumber unique article identifier
     * @param name          display name
     * @param details       optional description
     * @param stockQuantity current stock quantity
     * @param minStockLevel minimum stock level threshold
     * @param sellPrice     sales price per unit
     * @param purchasePrice purchase price per unit
     * @param vendorName    vendor or supplier name
     */
    public Article(String articleNumber, String name, String details, int stockQuantity, int minStockLevel,
                   double sellPrice, double purchasePrice, String vendorName) {
        this.articleNumber = articleNumber;
        this.name = name;
        this.details = details;
        this.stockQuantity = stockQuantity;
        this.minStockLevel = minStockLevel;
        this.sellPrice = sellPrice;
        this.purchasePrice = purchasePrice;
        this.vendorName = vendorName;
    }

    /**
     * Gets the article number
     *
     * @return unique article identifier
     */
    public String getArticleNumber() {
        return articleNumber;
    }

    /**
     * Sets the article number
     *
     * @param articleNumber unique article identifier
     */
    public void setArticleNumber(String articleNumber) {
        this.articleNumber = articleNumber;
    }

    /**
     * Gets the name of the Article
     *
     * @return display name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name for the Article
     *
     * @param name display name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the details for the Article
     *
     * @return optional description
     */
    public String getDetails() {
        return details;
    }

    /**
     * Sets the details for the Article
     *
     * @param details optional description
     */
    public void setDetails(String details) {
        this.details = details;
    }

    /**
     * Gets the Stock quantity variable for the Article
     *
     * @return current stock quantity
     */
    public int getStockQuantity() {
        return stockQuantity;
    }

    /**
     * Sets the Stock quantity for the Article
     *
     * @param stockQuantity current stock quantity
     */
    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    /**
     * Gets the minimum stock level for the Article
     *
     * @return minimum stock level threshold
     */
    public int getMinStockLevel() {
        return minStockLevel;
    }

    /**
     * Sets the min stock level for the Article
     *
     * @param minStockLevel minimum stock level threshold
     */
    public void setMinStockLevel(int minStockLevel) {
        this.minStockLevel = minStockLevel;
    }

    /**
     * Gets the sell price for the Article
     *
     * @return sales price per unit
     */
    public double getSellPrice() {
        return sellPrice;
    }

    /**
     * Sets the sell price for the Article
     *
     * @param sellPrice sales price per unit
     */
    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }

    /**
     * Gets the purchase price for the Article
     *
     * @return purchase price per unit
     */
    public double getPurchasePrice() {
        return purchasePrice;
    }

    /**
     * Sets the purchase price for the Article
     *
     * @param purchasePrice purchase price per unit
     */
    public void setPurchasePrice(double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    /**
     * Gets the vendor name of the Article
     *
     * @return vendor or supplier name
     */
    public String getVendorName() {
        return vendorName;
    }

    /**
     * Sets the vendor name for the Article
     *
     * @param vendorName vendor or supplier name
     */
    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    /**
     * Gets the category for the Article
     *
     * @return the Article category
     */
    public String getCategory() {
        if (articleNumber == null) return null;
        return ArticleUtils.getCategoryForArticle(articleNumber);
    }

    /**
     * Builds the QR code payload string using labeled, semicolon-separated fields.
     *
     * @return QR code data payload
     */
    public String getQrCodeData() {
        if (articleNumber == null) throw new NullPointerException("Article number cannot be null");
        return "artikelNr:" + articleNumber + ";" +
                "name:" + name + ";" +
                "details:" + details + ";" +
                "einkaufspreis:" + purchasePrice + ";" +
                "verkaufspreis:" + sellPrice + ";" +
                "lieferant:" + vendorName;
    }
}
