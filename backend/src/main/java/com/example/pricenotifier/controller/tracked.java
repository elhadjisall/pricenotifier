// Enhanced Price Tracking System with Improved Logic

// Fixed and Enhanced PriceAlert.java
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

    // Fixed getter method
    public BigDecimal getOldPrice() { return oldPrice; }
    public void setOldPrice(BigDecimal oldPrice) { this.oldPrice = oldPrice; }

    // ... rest of getters/setters as before
}

// NEW: Smart Alert Logic Service
@Service
@Transactional
public class SmartAlertLogicService {

    @Autowired
    private UserProductSubscriptionRepository subscriptionRepo;

    @Autowired
    private PriceHistoryRepository priceHistoryRepo;

    @Autowired
    private PriceAlertRepository alertRepo;

    /**
     * Advanced price change detection with multiple trigger conditions
     */
    public List<PriceAlert> evaluatePriceChanges(Product product, BigDecimal newPrice) {
        List<PriceAlert> alerts = new ArrayList<>();
        BigDecimal oldPrice = product.getCurrentPrice();

        // Get all active subscriptions for this product
        List<UserProductSubscription> subscriptions =
                subscriptionRepo.findByProductAndIsActiveTrue(product);

        for (UserProductSubscription subscription : subscriptions) {
            PriceAlert alert = evaluateSubscription(subscription, oldPrice, newPrice);
            if (alert != null) {
                alerts.add(alert);
            }
        }

        return alerts;
    }

    private PriceAlert evaluateSubscription(UserProductSubscription subscription,
                                            BigDecimal oldPrice, BigDecimal newPrice) {

        switch (subscription.getAlertType()) {
            case PRICE_DROP:
                return evaluatePriceDrop(subscription, oldPrice, newPrice);
            case TARGET_REACHED:
                return evaluateTargetReached(subscription, oldPrice, newPrice);
            case PERCENTAGE_DROP:
                return evaluatePercentageDrop(subscription, oldPrice, newPrice);
            case BACK_IN_STOCK:
                return evaluateBackInStock(subscription, oldPrice, newPrice);
            default:
                return null;
        }
    }

    /**
     * Logic: Alert if price drops below current price
     */
    private PriceAlert evaluatePriceDrop(UserProductSubscription subscription,
                                         BigDecimal oldPrice, BigDecimal newPrice) {
        if (newPrice.compareTo(oldPrice) < 0) {
            return new PriceAlert(subscription, oldPrice, newPrice, AlertType.PRICE_DROP);
        }
        return null;
    }

    /**
     * Logic: Alert if price reaches or goes below target price
     */
    private PriceAlert evaluateTargetReached(UserProductSubscription subscription,
                                             BigDecimal oldPrice, BigDecimal newPrice) {
        if (newPrice.compareTo(subscription.getTargetPrice()) <= 0) {
            return new PriceAlert(subscription, oldPrice, newPrice, AlertType.TARGET_REACHED);
        }
        return null;
    }

    /**
     * Logic: Alert if price drops by specified percentage (stored in target price as percentage)
     */
    private PriceAlert evaluatePercentageDrop(UserProductSubscription subscription,
                                              BigDecimal oldPrice, BigDecimal newPrice) {
        BigDecimal percentageThreshold = subscription.getTargetPrice(); // e.g., 10.00 for 10%
        BigDecimal actualDropPercentage = calculatePercentageChange(oldPrice, newPrice);

        if (actualDropPercentage.compareTo(percentageThreshold) >= 0) {
            return new PriceAlert(subscription, oldPrice, newPrice, AlertType.PERCENTAGE_DROP);
        }
        return null;
    }

    /**
     * Logic: Alert if price changes from 0 (out of stock) to any positive value
     */
    private PriceAlert evaluateBackInStock(UserProductSubscription subscription,
                                           BigDecimal oldPrice, BigDecimal newPrice) {
        if (oldPrice.compareTo(BigDecimal.ZERO) == 0 && newPrice.compareTo(BigDecimal.ZERO) > 0) {
            return new PriceAlert(subscription, oldPrice, newPrice, AlertType.BACK_IN_STOCK);
        }
        return null;
    }

    /**
     * Calculate percentage change between two prices
     */
    private BigDecimal calculatePercentageChange(BigDecimal oldPrice, BigDecimal newPrice) {
        if (oldPrice.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        BigDecimal difference = oldPrice.subtract(newPrice);
        return difference.divide(oldPrice, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}

// NEW: Trend Analysis Logic
@Service
public class PriceTrendAnalysisService {

    @Autowired
    private PriceHistoryRepository priceHistoryRepo;

    /**
     * Analyze price trends over different time periods
     */
    public PriceTrendAnalysis analyzeTrends(Product product, int days) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
        List<PriceHistory> history = priceHistoryRepo
                .findByProductAndRecordedAtAfterOrderByRecordedAtAsc(product, fromDate);

        if (history.isEmpty()) {
            return new PriceTrendAnalysis(TrendDirection.STABLE, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal firstPrice = history.get(0).getPrice();
        BigDecimal lastPrice = history.get(history.size() - 1).getPrice();
        BigDecimal lowestPrice = history.stream()
                .map(PriceHistory::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal highestPrice = history.stream()
                .map(PriceHistory::getPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // Determine trend direction
        TrendDirection direction = determineTrendDirection(firstPrice, lastPrice);

        return new PriceTrendAnalysis(direction, lowestPrice, highestPrice);
    }

    private TrendDirection determineTrendDirection(BigDecimal firstPrice, BigDecimal lastPrice) {
        int comparison = lastPrice.compareTo(firstPrice);
        if (comparison > 0) return TrendDirection.RISING;
        if (comparison < 0) return TrendDirection.FALLING;
        return TrendDirection.STABLE;
    }

    /**
     * Predict optimal buy time based on historical patterns
     */
    public OptimalBuyRecommendation getOptimalBuyRecommendation(Product product) {
        // Get 30-day price history
        PriceTrendAnalysis trend30 = analyzeTrends(product, 30);
        PriceTrendAnalysis trend7 = analyzeTrends(product, 7);

        BigDecimal currentPrice = product.getCurrentPrice();
        BigDecimal avgPrice = calculateAveragePrice(product, 30);

        // Logic for buy recommendation
        if (currentPrice.compareTo(trend30.getLowestPrice()) == 0) {
            return new OptimalBuyRecommendation(BuySignal.STRONG_BUY,
                    "Current price matches 30-day low");
        }

        if (trend7.getDirection() == TrendDirection.FALLING &&
                trend30.getDirection() == TrendDirection.RISING) {
            return new OptimalBuyRecommendation(BuySignal.BUY,
                    "Short-term dip in long-term upward trend");
        }

        if (currentPrice.compareTo(avgPrice.multiply(new BigDecimal("0.9"))) <= 0) {
            return new OptimalBuyRecommendation(BuySignal.BUY,
                    "Price is 10% below 30-day average");
        }

        if (trend7.getDirection() == TrendDirection.RISING &&
                trend30.getDirection() == TrendDirection.RISING) {
            return new OptimalBuyRecommendation(BuySignal.WAIT,
                    "Price trending upward - consider waiting");
        }

        return new OptimalBuyRecommendation(BuySignal.HOLD, "No clear buy signal");
    }

    private BigDecimal calculateAveragePrice(Product product, int days) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
        List<PriceHistory> history = priceHistoryRepo
                .findByProductAndRecordedAtAfter(product, fromDate);

        if (history.isEmpty()) return product.getCurrentPrice();

        BigDecimal sum = history.stream()
                .map(PriceHistory::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(new BigDecimal(history.size()), 2, RoundingMode.HALF_UP);
    }
}

// NEW: Enhanced Alert Filtering Logic
@Service
public class AlertFilteringService {

    /**
     * Prevent spam alerts with intelligent filtering
     */
    public boolean shouldSendAlert(PriceAlert newAlert) {
        // Don't send duplicate alerts within 24 hours
        if (hasSentSimilarAlertRecently(newAlert, 24)) {
            return false;
        }

        // Don't send minor price change alerts (less than $1 or 1%)
        if (isMinorPriceChange(newAlert)) {
            return false;
        }

        // Check user's alert frequency preferences
        if (exceedsUserAlertLimits(newAlert)) {
            return false;
        }

        return true;
    }

    private boolean hasSentSimilarAlertRecently(PriceAlert alert, int hours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        // Implementation would check database for similar alerts
        return false; // Simplified
    }

    private boolean isMinorPriceChange(PriceAlert alert) {
        BigDecimal priceDiff = alert.getOldPrice().subtract(alert.getNewPrice()).abs();
        BigDecimal percentChange = priceDiff.divide(alert.getOldPrice(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        return priceDiff.compareTo(new BigDecimal("1.00")) < 0 &&
                percentChange.compareTo(new BigDecimal("1.0")) < 0;
    }

    private boolean exceedsUserAlertLimits(PriceAlert alert) {
        // Check if user has received too many alerts today
        // Implementation would check user preferences and recent alert count
        return false; // Simplified
    }
}

// Supporting Classes
class PriceTrendAnalysis {
    private TrendDirection direction;
    private BigDecimal lowestPrice;
    private BigDecimal highestPrice;

    public PriceTrendAnalysis(TrendDirection direction, BigDecimal lowestPrice, BigDecimal highestPrice) {
        this.direction = direction;
        this.lowestPrice = lowestPrice;
        this.highestPrice = highestPrice;
    }

    // Getters
    public TrendDirection getDirection() { return direction; }
    public BigDecimal getLowestPrice() { return lowestPrice; }
    public BigDecimal getHighestPrice() { return highestPrice; }
}

class OptimalBuyRecommendation {
    private BuySignal signal;
    private String reason;

    public OptimalBuyRecommendation(BuySignal signal, String reason) {
        this.signal = signal;
        this.reason = reason;
    }

    public BuySignal getSignal() { return signal; }
    public String getReason() { return reason; }
}

// Additional Enums
enum TrendDirection {
    RISING, FALLING, STABLE
}

enum BuySignal {
    STRONG_BUY, BUY, HOLD, WAIT, AVOID
}