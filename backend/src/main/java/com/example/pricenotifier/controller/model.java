package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.model.TrackedItem;
import com.example.demo.service.TrackedItemService;
import com.example.demo.service.PriceUpdateService;
import com.example.demo.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/items")
@Validated
@CrossOrigin(origins = "*")
public class TrackedItemController {

    @Autowired
    private TrackedItemService trackedItemService;

    @Autowired
    private PriceUpdateService priceUpdateService;

    @Autowired
    private NotificationService notificationService;

    /**
     * Create a new tracked item with validation and duplicate checking
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TrackedItem>> createItem(@Valid @RequestBody CreateTrackedItemRequest request) {
        try {
            // Check for duplicate URLs
            if (trackedItemService.existsByUrl(request.getUrl())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ApiResponse<>(false, "Item with this URL already exists", null));
            }

            TrackedItem item = trackedItemService.createTrackedItem(request);

            // Trigger initial price fetch
            priceUpdateService.fetchInitialPrice(item);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(true, "Item created successfully", item));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to create item: " + e.getMessage(), null));
        }
    }

    /**
     * Get all items with pagination, filtering, and sorting
     */
    @GetMapping
    public ResponseEntity<PagedResponse<TrackedItem>> getAllItems(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String retailer,
            @RequestParam(required = false) Boolean isActive) {

        try {
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
                    Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<TrackedItem> itemsPage = trackedItemService.findItemsWithFilters(
                    category, retailer, isActive, pageable);

            PagedResponse<TrackedItem> response = new PagedResponse<>(
                    itemsPage.getContent(),
                    itemsPage.getNumber(),
                    itemsPage.getSize(),
                    itemsPage.getTotalElements(),
                    itemsPage.getTotalPages(),
                    itemsPage.isLast()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get single item by ID with price history
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TrackedItemDetailResponse>> getItemById(@PathVariable Long id) {
        try {
            Optional<TrackedItem> item = trackedItemService.findById(id);

            if (item.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "Item not found", null));
            }

            TrackedItemDetailResponse response = trackedItemService.getItemWithDetails(item.get());

            return ResponseEntity.ok(new ApiResponse<>(true, "Item retrieved successfully", response));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to retrieve item", null));
        }
    }

    /**
     * Update tracked item
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TrackedItem>> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTrackedItemRequest request) {

        try {
            Optional<TrackedItem> existingItem = trackedItemService.findById(id);

            if (existingItem.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "Item not found", null));
            }

            TrackedItem updatedItem = trackedItemService.updateTrackedItem(existingItem.get(), request);

            return ResponseEntity.ok(new ApiResponse<>(true, "Item updated successfully", updatedItem));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to update item: " + e.getMessage(), null));
        }
    }

    /**
     * Delete tracked item (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(@PathVariable Long id) {
        try {
            boolean deleted = trackedItemService.softDeleteItem(id);

            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "Item not found", null));
            }

            return ResponseEntity.ok(new ApiResponse<>(true, "Item deleted successfully", null));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to delete item", null));
        }
    }

    /**
     * Manually trigger price update for specific item
     */
    @PostMapping("/{id}/update-price")
    public ResponseEntity<ApiResponse<PriceUpdateResponse>> updateItemPrice(@PathVariable Long id) {
        try {
            Optional<TrackedItem> item = trackedItemService.findById(id);

            if (item.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "Item not found", null));
            }

            PriceUpdateResponse response = priceUpdateService.updateItemPrice(item.get());

            return ResponseEntity.ok(new ApiResponse<>(true, "Price update completed", response));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to update price: " + e.getMessage(), null));
        }
    }

    /**
     * Bulk update prices for all active items
     */
    @PostMapping("/bulk-update-prices")
    public ResponseEntity<ApiResponse<BulkUpdateResponse>> bulkUpdatePrices() {
        try {
            BulkUpdateResponse response = priceUpdateService.bulkUpdatePrices();

            return ResponseEntity.ok(new ApiResponse<>(true, "Bulk update completed", response));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Bulk update failed: " + e.getMessage(), null));
        }
    }

    /**
     * Get price history for an item
     */
    @GetMapping("/{id}/price-history")
    public ResponseEntity<ApiResponse<List<PriceHistoryResponse>>> getPriceHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {

        try {
            Optional<TrackedItem> item = trackedItemService.findById(id);

            if (item.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "Item not found", null));
            }

            List<PriceHistoryResponse> history = trackedItemService.getPriceHistory(item.get(), days);

            return ResponseEntity.ok(new ApiResponse<>(true, "Price history retrieved", history));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to retrieve price history", null));
        }
    }

    /**
     * Get price analytics for an item
     */
    @GetMapping("/{id}/analytics")
    public ResponseEntity<ApiResponse<PriceAnalyticsResponse>> getPriceAnalytics(@PathVariable Long id) {
        try {
            Optional<TrackedItem> item = trackedItemService.findById(id);

            if (item.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "Item not found", null));
            }

            PriceAnalyticsResponse analytics = trackedItemService.getPriceAnalytics(item.get());

            return ResponseEntity.ok(new ApiResponse<>(true, "Analytics retrieved", analytics));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to retrieve analytics", null));
        }
    }

    /**
     * Subscribe to price alerts for an item
     */
    @PostMapping("/{id}/subscribe")
    public ResponseEntity<ApiResponse<Void>> subscribeToAlerts(
            @PathVariable Long id,
            @Valid @RequestBody SubscriptionRequest request) {

        try {
            Optional<TrackedItem> item = trackedItemService.findById(id);

            if (item.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "Item not found", null));
            }

            notificationService.createSubscription(item.get(), request);

            return ResponseEntity.ok(new ApiResponse<>(true, "Subscribed to alerts successfully", null));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to create subscription: " + e.getMessage(), null));
        }
    }

    /**
     * Search items by name or description
     */
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<TrackedItem>> searchItems(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<TrackedItem> results = trackedItemService.searchItems(query, pageable);

            PagedResponse<TrackedItem> response = new PagedResponse<>(
                    results.getContent(),
                    results.getNumber(),
                    results.getSize(),
                    results.getTotalElements(),
                    results.getTotalPages(),
                    results.isLast()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get items with recent price drops
     */
    @GetMapping("/deals")
    public ResponseEntity<ApiResponse<List<DealResponse>>> getRecentDeals(
            @RequestParam(defaultValue = "24") @Min(1) @Max(168) int hours,
            @RequestParam(defaultValue = "5.0") @Min(0) BigDecimal minPercentage) {

        try {
            List<DealResponse> deals = trackedItemService.getRecentDeals(hours, minPercentage);

            return ResponseEntity.ok(new ApiResponse<>(true, "Recent deals retrieved", deals));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to retrieve deals", null));
        }
    }

    /**
     * Get system statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<SystemStatsResponse>> getSystemStats() {
        try {
            SystemStatsResponse stats = trackedItemService.getSystemStats();

            return ResponseEntity.ok(new ApiResponse<>(true, "Statistics retrieved", stats));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to retrieve statistics", null));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<HealthCheckResponse>> healthCheck() {
        try {
            HealthCheckResponse health = trackedItemService.performHealthCheck();

            return ResponseEntity.ok(new ApiResponse<>(true, "Health check completed", health));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ApiResponse<>(false, "Health check failed", null));
        }
    }
}

// Supporting DTOs and Response Classes
class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and setters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public LocalDateTime getTimestamp() { return timestamp; }
}

class PagedResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public PagedResponse(List<T> content, int page, int size, long totalElements, int totalPages, boolean last) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.last = last;
    }

    // Getters
    public List<T> getContent() { return content; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
    public boolean isLast() { return last; }
}

class CreateTrackedItemRequest {
    @javax.validation.constraints.NotBlank
    private String name;

    @javax.validation.constraints.NotBlank
    private String url;

    private String description;
    private String category;
    private String retailer;
    private BigDecimal targetPrice;

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getRetailer() { return retailer; }
    public void setRetailer(String retailer) { this.retailer = retailer; }
    public BigDecimal getTargetPrice() { return targetPrice; }
    public void setTargetPrice(BigDecimal targetPrice) { this.targetPrice = targetPrice; }
}

class UpdateTrackedItemRequest {
    private String name;
    private String description;
    private BigDecimal targetPrice;
    private Boolean isActive;

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getTargetPrice() { return targetPrice; }
    public void setTargetPrice(BigDecimal targetPrice) { this.targetPrice = targetPrice; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}

class SubscriptionRequest {
    @javax.validation.constraints.NotBlank
    private String email;

    // Logic needed to added here for more fixes
    // Getters and setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public BigDecimal getTargetPrice() { return targetPrice; }
    public void setTargetPrice(BigDecimal targetPrice) { this.targetPrice = targetPrice; }
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType)
    public boolean equal(TrackedItemController);