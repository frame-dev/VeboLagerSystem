package ch.framedev.lagersystem.classes;

import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.utils.ArticleUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents an order with sender/receiver details and ordered article quantities.
 */
public class Order {

    /**
     * Unique order identifier.
     */
    private String orderId;
    /**
     * Map of articleNumber to quantity ordered.
     */
    private Map<String, Integer> orderedArticles;
    /**
     * Receiver full name.
     */
    private String receiverName;
    /**
     * Receiver account number.
     */
    private String receiverKontoNumber;
    /**
     * Order date as stored text.
     */
    private String orderDate;
    /**
     * Sender full name.
     */
    private String senderName;
    /**
     * Sender account number.
     */
    private String senderKontoNumber;
    /**
     * Department associated with the order.
     */
    private String department;
    /**
     * Current order status label.
     */
    private String status;
    /**
     * Optional filling text per article number, e.g. "500 ml".
     */
    private Map<String, String> articleFillings;

    /**
     * Creates a new order with default status "In Bearbeitung".
     *
     * @param orderId             unique order identifier
     * @param orderedArticles     map of articleNumber to quantity
     * @param receiverName        receiver name
     * @param receiverKontoNumber receiver account number
     * @param orderDate           order date text
     * @param senderName          sender name
     * @param senderKontoNumber   sender account number
     * @param department          department name
     */
    public Order(String orderId, Map<String, Integer> orderedArticles, String receiverName,
                 String receiverKontoNumber, String orderDate, String senderName, String senderKontoNumber, String department) {
        this(orderId, orderedArticles, new LinkedHashMap<>(), receiverName, receiverKontoNumber,
                orderDate, senderName, senderKontoNumber, department, "In Bearbeitung");
    }

    /**
     * Creates a new order with an explicit status.
     *
     * @param orderId             unique order identifier
     * @param orderedArticles     map of articleNumber to quantity
     * @param receiverName        receiver name
     * @param receiverKontoNumber receiver account number
     * @param orderDate           order date text
     * @param senderName          sender name
     * @param senderKontoNumber   sender account number
     * @param department          department name
     * @param status              order status label
     */
    public Order(String orderId, Map<String, Integer> orderedArticles, String receiverName, String receiverKontoNumber, String orderDate, String senderName, String senderKontoNumber, String department, String status) {
        this(orderId, orderedArticles, new LinkedHashMap<>(), receiverName, receiverKontoNumber,
                orderDate, senderName, senderKontoNumber, department, status);
    }

    public Order(String orderId, Map<String, Integer> orderedArticles, Map<String, String> articleFillings,
                 String receiverName, String receiverKontoNumber, String orderDate, String senderName,
                 String senderKontoNumber, String department) {
        this(orderId, orderedArticles, articleFillings, receiverName, receiverKontoNumber,
                orderDate, senderName, senderKontoNumber, department, "In Bearbeitung");
    }

    public Order(String orderId, Map<String, Integer> orderedArticles, Map<String, String> articleFillings,
                 String receiverName, String receiverKontoNumber, String orderDate, String senderName,
                 String senderKontoNumber, String department, String status) {
        this.orderId = orderId;
        this.orderedArticles = orderedArticles;
        this.articleFillings = articleFillings == null ? new LinkedHashMap<>() : new LinkedHashMap<>(articleFillings);
        this.receiverName = receiverName;
        this.receiverKontoNumber = receiverKontoNumber;
        this.orderDate = orderDate;
        this.senderName = senderName;
        this.senderKontoNumber = senderKontoNumber;
        this.department = department;
        this.status = status;
    }

    /**
     * Default constructor for Order. Initializes an empty order object. This constructor can be used when creating an order object that will be populated with data later, such as when deserializing from a data source or when using a builder pattern.
     */
    public Order() {
        this.articleFillings = new LinkedHashMap<>();
    }

    /**
     * Gets the unique order identifier.
     * @return unique order identifier
     */
    public String getOrderId() {
        return orderId;
    }

    /**
     * Sets the unique order identifier for the order. The order ID is a crucial piece of information that uniquely identifies each order in the system. It is used for tracking, referencing, and managing orders throughout their lifecycle. When setting the order ID, it is important to ensure that it is unique across all orders to prevent conflicts and ensure accurate order management.
     * @param orderId unique order identifier
     */
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    /**
     * Gets the map of ordered articles, where the key is the article number and the value is the quantity ordered. This map represents the specific articles that have been included in the order along with their respective quantities. It allows for easy access to the details of what has been ordered and can be used for processing the order, calculating totals, and managing inventory.
     * @return ordered article map
     */
    public Map<String, Integer> getOrderedArticles() {
        return orderedArticles;
    }

    /**
     * Sets the map of ordered articles for the order. The map should contain article numbers as keys and their corresponding quantities as values. This information is essential for processing the order, calculating the total price, and managing inventory levels. When setting the ordered articles, it is important to ensure that the article numbers are valid and that the quantities are accurate to avoid issues during order fulfillment.
     * @param orderedArticles map of articleNumber to quantity
     */
    public void setOrderedArticles(Map<String, Integer> orderedArticles) {
        this.orderedArticles = orderedArticles;
    }

    public Map<String, String> getArticleFillings() {
        if (articleFillings == null) {
            articleFillings = new LinkedHashMap<>();
        }
        return articleFillings;
    }

    public void setArticleFillings(Map<String, String> articleFillings) {
        this.articleFillings = articleFillings == null ? new LinkedHashMap<>() : new LinkedHashMap<>(articleFillings);
    }

    public String getArticleFilling(String articleNumber) {
        if (articleNumber == null || getArticleFillings().isEmpty()) {
            return "";
        }
        return getArticleFillings().getOrDefault(articleNumber, "");
    }

    /**
     * Gets the receiver's full name associated with the order. The receiver name is an important piece of information that identifies the individual or entity that will receive the goods or services specified in the order. It is used for communication, delivery, and record-keeping purposes. When retrieving the receiver name, it is important to ensure that it is accurate and up-to-date to facilitate smooth order processing and delivery.
     * @return receiver name
     */
    public String getReceiverName() {
        return receiverName;
    }

    /**
     * Sets the receiver's full name for the order. The receiver name should accurately reflect the individual or entity that is intended to receive the goods or services specified in the order. This information is crucial for ensuring that the order is delivered to the correct recipient and for maintaining clear communication throughout the order processing and delivery stages. When setting the receiver name, it is important to verify its accuracy to avoid any issues with delivery or order fulfillment.
     * @param receiverName receiver name
     */
    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    /**
     * Gets the receiver's account number associated with the order. The receiver account number is a critical piece of information that may be used for billing, tracking, and communication purposes related to the order. It helps ensure that the correct receiver details are associated with the order and can be used for financial transactions or record-keeping. When retrieving the receiver account number, it is important to ensure that it is accurate and corresponds to the intended recipient of the order.
     * @return receiver account number
     */
    public String getReceiverKontoNumber() {
        return receiverKontoNumber;
    }

    /**
     * Sets the receiver's account number for the order. The receiver account number is essential for processing the order and ensuring that the correct receiver details are associated with it. This information may be used for billing, tracking, and communication purposes related to the order. When setting the receiver account number, it is important to verify its accuracy to avoid any issues with order processing, billing, or delivery.
     * @param receiverKontoNumber receiver account number
     */
    public void setReceiverKontoNumber(String receiverKontoNumber) {
        this.receiverKontoNumber = receiverKontoNumber;
    }

    /**
     * Gets the order date as stored text. The order date is an important piece of information that indicates when the order was placed. It can be used for tracking the order's lifecycle, managing delivery schedules, and maintaining accurate records. When retrieving the order date, it is important to ensure that it is in a consistent format and accurately reflects the date and time when the order was created.
     * @return order date text
     */
    public String getOrderDate() {
        return orderDate;
    }

    /**
     * Sets the order date for the order. The order date should accurately reflect the date and time when the order was placed. This information is crucial for tracking the order's lifecycle, managing delivery schedules, and maintaining accurate records. When setting the order date, it is important to ensure that it is in a consistent format and accurately represents the date and time of order creation to avoid any issues with order processing or record-keeping.
     * @param orderDate order date text
     */
    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }

    /**
     * Gets the sender's full name associated with the order. The sender name is an important piece of information that identifies the individual or entity that is sending the goods or services specified in the order. It is used for communication, record-keeping, and may be relevant for billing or tracking purposes. When retrieving the sender name, it is important to ensure that it is accurate and up-to-date to facilitate smooth order processing and communication.
     * @return sender name
     */
    public String getSenderName() {
        return senderName;
    }

    /**
     * Sets the sender's full name for the order. The sender name should accurately reflect the individual or entity that is sending the goods or services specified in the order. This information is crucial for ensuring that the correct sender details are associated with the order and for maintaining clear communication throughout the order processing stages. When setting the sender name, it is important to verify its accuracy to avoid any issues with order processing, billing, or communication.
     * @param senderName sender name
     */
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    /**
     * Gets the sender's account number associated with the order. The sender account number is a critical piece of information that may be used for billing, tracking, and communication purposes related to the order. It helps ensure that the correct sender details are associated with the order and can be used for financial transactions or record-keeping. When retrieving the sender account number, it is important to ensure that it is accurate and corresponds to the intended sender of the order.
     * @return sender account number
     */
    public String getSenderKontoNumber() {
        return senderKontoNumber;
    }

    /**
     * The sender account number is set for the order. This information is crucial for processing the order and ensuring that the correct sender details are associated with it. The sender account number may be used for billing, tracking, and communication purposes related to the order.
     * @param senderKontoNumber sender account number
     */
    public void setSenderKontoNumber(String senderKontoNumber) {
        this.senderKontoNumber = senderKontoNumber;
    }

    /**
     * Gets the department name associated with the order. The department information can be used for categorizing orders, managing workflows, and facilitating communication within an organization. It helps ensure that the order is processed by the appropriate team or department and can be relevant for reporting and analytics purposes. When retrieving the department name, it is important to ensure that it is accurate and reflects the correct organizational unit responsible for handling the order.
     * @return department name
     */
    public String getDepartment() {
        return department;
    }

    /**
     * Sets the department name for the order. The department information is important for categorizing orders, managing workflows, and facilitating communication within an organization. It helps ensure that the order is processed by the appropriate team or department and can be relevant for reporting and analytics purposes. When setting the department name, it is important to verify its accuracy to avoid any issues with order processing or communication within the organization.
     * @param department department name
     */
    public void setDepartment(String department) {
        this.department = department;
    }

    /**
     * Gets the current order status label. The order status is a critical piece of information that indicates the current state of the order in its lifecycle. It can be used for tracking the progress of the order, managing workflows, and facilitating communication with customers or internal teams. Common order statuses might include "In Bearbeitung" (In Progress), "Abgeschlossen" (Completed), "Storniert" (Cancelled), etc. When retrieving the order status, it is important to ensure that it accurately reflects the current state of the order to provide clear information to all stakeholders involved.
     * @return order status label
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the current order status label. The order status is a critical piece of information that indicates the current state of the order in its lifecycle. It can be used for tracking the progress of the order, managing workflows, and facilitating communication with customers or internal teams. Common order statuses might include "In Bearbeitung" (In Progress), "Abgeschlossen" (Completed), "Storniert" (Cancelled), etc. When setting the order status, it is important to ensure that it accurately reflects the current state of the order to provide clear information to all stakeholders involved and to facilitate proper order management.
     * @param status order status label
     */
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
                double unitPrice = ArticleUtils.resolveEffectiveSellPrice(article, getArticleFilling(articleNumber));
                totalPrice += unitPrice * quantity;
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
