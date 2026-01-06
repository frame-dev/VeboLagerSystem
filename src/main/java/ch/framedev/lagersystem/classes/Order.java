package ch.framedev.lagersystem.classes;

import ch.framedev.lagersystem.managers.ArticleManager;

import java.util.Map;

public class Order {

    private String orderId;
    private Map<String, Integer> orderedArticles; // Map of articleNumber to quantity
    private String receiverName;
    private String receiverKontoNumber;
    private String orderDate;
    private String senderName;
    private String senderKontoNumber;
    private String department;

    public Order(String orderId, Map<String, Integer> orderedArticles, String receiverName,
                 String receiverKontoNumber, String orderDate, String senderName, String senderKontoNumber, String department) {
        this.orderId = orderId;
        this.orderedArticles = orderedArticles;
        this.receiverName = receiverName;
        this.receiverKontoNumber = receiverKontoNumber;
        this.orderDate = orderDate;
        this.senderName = senderName;
        this.senderKontoNumber = senderKontoNumber;
        this.department = department;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Map<String, Integer> getOrderedArticles() {
        return orderedArticles;
    }

    public void setOrderedArticles(Map<String, Integer> orderedArticles) {
        this.orderedArticles = orderedArticles;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverKontoNumber() {
        return receiverKontoNumber;
    }

    public void setReceiverKontoNumber(String receiverKontoNumber) {
        this.receiverKontoNumber = receiverKontoNumber;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderKontoNumber() {
        return senderKontoNumber;
    }

    public void setSenderKontoNumber(String senderKontoNumber) {
        this.senderKontoNumber = senderKontoNumber;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public double getTotalOrderPrice() {
        double totalPrice = 0.0;
        ArticleManager articleManager = ArticleManager.getInstance();
        for (Map.Entry<String, Integer> entry : orderedArticles.entrySet()) {
            String articleNumber = entry.getKey();
            int quantity = entry.getValue();
            Article article = articleManager.getArticleByNumber(articleNumber);
            if (article != null) {
                totalPrice += article.getSellPrice() * quantity;
            }
        }
        return totalPrice;
    }
}
