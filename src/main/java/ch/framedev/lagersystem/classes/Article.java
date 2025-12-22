package ch.framedev.lagersystem.classes;

public class Article {

    private String articleNumber;
    private String name;
    private String details;
    private int stockQuantity;
    private int minStockLevel;
    private double sellPrice;
    private double purchasePrice;
    private String vendorName;

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

    public String getArticleNumber() {
        return articleNumber;
    }

    public void setArticleNumber(String articleNumber) {
        this.articleNumber = articleNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public int getMinStockLevel() {
        return minStockLevel;
    }

    public void setMinStockLevel(int minStockLevel) {
        this.minStockLevel = minStockLevel;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public double getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }
}
