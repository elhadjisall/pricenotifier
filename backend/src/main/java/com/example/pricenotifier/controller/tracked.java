// Tracked item logic

// Product.java
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, unique = true)
    private String url;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal currentPrice;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private ProductCategory category;

    private String retailer;

    @Column(nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // One product can have many subscriptions
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<UserProductSubscription> subscriptions = new ArrayList<>();

    // One product can have many price history records
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<PriceHistory> priceHistory = new ArrayList<>();

    // Constructors, getters, setters
    public Product() {}

    public Product(String name, String url, BigDecimal currentPrice) {
        this.name = name;
        this.url = url;
        this.currentPrice = currentPrice;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public ProductCategory getCategory() { return category; }
    public void setCategory(ProductCategory category) { this.category = category; }

    public String getRetailer() { return retailer; }
    public void setRetailer(String retailer) { this.retailer = retailer; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public List<UserProductSubscription> getSubscriptions() { return subscriptions; }
    public List<PriceHistory> getPriceHistory() { return priceHistory; }
}

// User.java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private NotificationPreference notificationPreference = NotificationPreference.EMAIL;

    @Column(nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // One user can have many subscriptions
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserProductSubscription> subscriptions = new ArrayList<>();

    // Constructors, getters, setters
    public User() {}

    public User(String email, String name) {
        this.email = email;
        this.name = name;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public NotificationPreference getNotificationPreference() { return notificationPreference; }
    public void setNotificationPreference(NotificationPreference notificationPreference) {
        this.notificationPreference = notificationPreference;
    }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public List<UserProductSubscription> getSubscriptions() { return subscriptions; }
}

// UserProductSubscription.java
@Entity
@Table(name = "user_product_subscriptions")
public class UserProductSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal targetPrice;

    @Enumerated(EnumType.STRING)
    private AlertType alertType = AlertType.PRICE_DROP;

    @Column(nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // Constructors, getters, setters
    public UserProductSubscription() {}

    public UserProductSubscription(User user, Product product, BigDecimal targetPrice) {
        this.user = user;
        this.product = product;
        this.targetPrice = targetPrice;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public BigDecimal getTargetPrice() { return targetPrice; }
    public void setTargetPrice(BigDecimal targetPrice) { this.targetPrice = targetPrice; }

    public AlertType getAlertType() { return alertType; }
    public void setAlertType(AlertType alertType) { this.alertType = alertType; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}

// PriceHistory.java
@Entity
@Table(name = "price_history")
public class PriceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    private String source; // e.g., "web_scraper", "api", "manual"

    // Constructors, getters, setters
    public PriceHistory() {
        this.recordedAt = LocalDateTime.now();
    }

    public PriceHistory(Product product, BigDecimal price) {
        this.product = product;
        this.price = price;
        this.recordedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}

// PriceAlert.java
@Entity
@Table(name = "price_alerts")
public class PriceAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private UserProductSubscription subscription;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal oldPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal newPrice;

    @Enumerated(EnumType.STRING)
    private AlertType alertType;

    @Column(nullable = false)
    private LocalDateTime triggeredAt;

    @Enumerated(EnumType.STRING)
    private AlertStatus status = AlertStatus.PENDING;

    private LocalDateTime sentAt;

    // Constructors, getters, setters
    public PriceAlert() {
        this.triggeredAt = LocalDateTime.now();
    }

    public PriceAlert(UserProductSubscription subscription, BigDecimal oldPrice, BigDecimal newPrice, AlertType alertType) {
        this.subscription = subscription;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
        this.alertType = alertType;
        this.triggeredAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserProductSubscription getSubscription() { return subscription; }
    public void setSubscription(UserProductSubscription subscription) { this.subscription = subscription; }

    public BigDecimal getOldPrice() { return oldPrice; }
    public void setOldPrice(BigDecimal oldPrice) { this.oldPrice = oldPrice; }

    public BigDecimal getNewPrice() { return newPrice; }
    public void setNewPrice(BigDecimal newPrice) { this.newPrice = newPrice; }

    public AlertType getAlertType() { return alertType; }
    public void setAlertType(AlertType alertType) { this.alertType = alertType; }

    public LocalDateTime getTriggeredAt() { return triggeredAt; }
    public void setTriggeredAt(LocalDateTime triggeredAt) { this.triggeredAt = triggeredAt; }

    public AlertStatus getStatus() { return status; }
    public void setStatus(AlertStatus status) { this.status = status; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}

// Enums
enum ProductCategory {
    ELECTRONICS,
    CLOTHING,
    HOME,
    BOOKS,
    SPORTS,
    BEAUTY,
    OTHER
}

enum NotificationPreference {
    EMAIL,
    SMS,
    BOTH,
    NONE
}

enum AlertType {
    PRICE_DROP,
    TARGET_REACHED,
    PERCENTAGE_DROP,
    BACK_IN_STOCK
}

enum AlertStatus {
    PENDING,
    SENT,
    FAILED,
    CANCELLED
}
