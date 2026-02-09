package ch.framedev.lagersystem.classes;

import ch.framedev.lagersystem.managers.ArticleManager;

import java.util.Map;

/**
 * Represents an order with sender/receiver details and ordered article quantities.
 */
@SuppressWarnings("unused")
public class Order {

    /** Unique order identifier. */
    private String orderId;
    /** Map of articleNumber to quantity ordered. */
    private Map<String, Integer> orderedArticles;
    /** Receiver full name. */
    private String receiverName;
    /** Receiver account number. */
    private String receiverKontoNumber;
    /** Order date as stored text. */
    private String orderDate;
    /** Sender full name. */
    private String senderName;
    /** Sender account number. */
    private String senderKontoNumber;
    /** Department associated with the order. */
    private String department;
    /** Current order status label. */
    private String status;

    /**
     * Creates a new order with default status "In Bearbeitung".
     *
     * @param orderId unique order identifier
     * @param orderedArticles map of articleNumber to quantity
     * @param receiverName receiver name
     * @param receiverKontoNumber receiver account number
     * @param orderDate order date text
     * @param senderName sender name
     * @param senderKontoNumber sender account number
     * @param department department name
     */
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
        this.status = "In Bearbeitung";
    }

    /**
     * Creates a new order with an explicit status.
     *
     * @param orderId unique order identifier
     * @param orderedArticles map of articleNumber to quantity
     * @param receiverName receiver name
     * @param receiverKontoNumber receiver account number
     * @param orderDate order date text
     * @param senderName sender name
     * @param senderKontoNumber sender account number
     * @param department department name
     * @param status order status label
     */
    public Order(String orderId, Map<String, Integer> orderedArticles, String receiverName, String receiverKontoNumber, String orderDate, String senderName, String senderKontoNumber, String department, String status) {
        this.orderId = orderId;
        this.orderedArticles = orderedArticles;
        this.receiverName = receiverName;
        this.receiverKontoNumber = receiverKontoNumber;
        this.orderDate = orderDate;
        this.senderName = senderName;
        this.senderKontoNumber = senderKontoNumber;
        this.department = department;
        this.status = status;
    }

    /** @return unique order identifier */
    public String getOrderId() {
        return orderId;
    }

    /** @param orderId unique order identifier */
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    /** @return ordered article map */
    public Map<String, Integer> getOrderedArticles() {
        return orderedArticles;
    }

    /** @param orderedArticles map of articleNumber to quantity */
    public void setOrderedArticles(Map<String, Integer> orderedArticles) {
        this.orderedArticles = orderedArticles;
    }

    /** @return receiver name */
    public String getReceiverName() {
        return receiverName;
    }

    /** @param receiverName receiver name */
    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    /** @return receiver account number */
    public String getReceiverKontoNumber() {
        return receiverKontoNumber;
    }

    /** @param receiverKontoNumber receiver account number */
    public void setReceiverKontoNumber(String receiverKontoNumber) {
        this.receiverKontoNumber = receiverKontoNumber;
    }

    /** @return order date text */
    public String getOrderDate() {
        return orderDate;
    }

    /** @param orderDate order date text */
    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }

    /** @return sender name */
    public String getSenderName() {
        return senderName;
    }

    /** @param senderName sender name */
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    /** @return sender account number */
    public String getSenderKontoNumber() {
        return senderKontoNumber;
    }

    /** @param senderKontoNumber sender account number */
    public void setSenderKontoNumber(String senderKontoNumber) {
        this.senderKontoNumber = senderKontoNumber;
    }

    /** @return department name */
    public String getDepartment() {
        return department;
    }

    /** @param department department name */
    public void setDepartment(String department) {
        this.department = department;
    }

    /** @return order status label */
    public String getStatus() {
        return status;
    }

    /** @param status order status label */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Calculates the total sales price for all ordered articles.
     *
     * @return total price based on sell price per article
     */
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

    /**
     * Builds the QR code payload string in a semicolon-separated format.
     *
     * @return QR code data payload
     */
    public String getQRCodeDataString() {
        StringBuilder sb = new StringBuilder();
        sb.append(orderId).append(";");
        for (Map.Entry<String, Integer> entry : orderedArticles.entrySet()) {
            sb.append(entry.getKey()).append(",").append(entry.getValue()).append("|");
        }
        // Remove trailing '|'
        if (!orderedArticles.isEmpty()) {
            sb.setLength(sb.length() - 1);
        }
        sb.append(";");
        sb.append(receiverName).append(";");
        sb.append(receiverKontoNumber).append(";");
        sb.append(orderDate).append(";");
        sb.append(senderName).append(";");
        sb.append(senderKontoNumber).append(";");
        sb.append(department).append(";");
        sb.append(status);
        return sb.toString();
    }
}
