package ch.framedev.lagersystem.classes;

/**
 * Represents a catalog article with inventory and pricing data.
 */
@SuppressWarnings("unused")
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
     * @return unique article identifier
     */
    public String getArticleNumber() {
        return articleNumber;
    }

    /**
     * @param articleNumber unique article identifier
     */
    public void setArticleNumber(String articleNumber) {
        this.articleNumber = articleNumber;
    }

    /**
     * @return display name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name display name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return optional description
     */
    public String getDetails() {
        return details;
    }

    /**
     * @param details optional description
     */
    public void setDetails(String details) {
        this.details = details;
    }

    /**
     * @return current stock quantity
     */
    public int getStockQuantity() {
        return stockQuantity;
    }

    /**
     * @param stockQuantity current stock quantity
     */
    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    /**
     * @return minimum stock level threshold
     */
    public int getMinStockLevel() {
        return minStockLevel;
    }

    /**
     * @param minStockLevel minimum stock level threshold
     */
    public void setMinStockLevel(int minStockLevel) {
        this.minStockLevel = minStockLevel;
    }

    /**
     * @return sales price per unit
     */
    public double getSellPrice() {
        return sellPrice;
    }

    /**
     * @param sellPrice sales price per unit
     */
    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }

    /**
     * @return purchase price per unit
     */
    public double getPurchasePrice() {
        return purchasePrice;
    }

    /**
     * @param purchasePrice purchase price per unit
     */
    public void setPurchasePrice(double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    /**
     * @return vendor or supplier name
     */
    public String getVendorName() {
        return vendorName;
    }

    /**
     * @param vendorName vendor or supplier name
     */
    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    /**
     * Builds the QR code payload string using labeled, semicolon-separated fields.
     *
     * @return QR code data payload
     */
    public String getQrCodeData() {
        String data = "";
        data += "artikelNr:" + articleNumber + ";";
        data += "name:" + name + ";";
        data += "details:" + details + ";";
        data += "einkaufspreis:" + purchasePrice + ";";
        data += "verkaufspreis:" + sellPrice + ";";
        data += "lieferant:" + vendorName + ";";
        return data;
    }
}
