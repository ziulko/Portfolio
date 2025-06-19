package modules;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class Order {
    public enum OrderStatus {
        NEW,
        PROCESSING,
        COMPLETED,
        CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Date orderDate;

    private Date deliveryDate;

    @ManyToMany(cascade = CascadeType.ALL)
    private List<Part> parts = new ArrayList<>();

    private String supplier;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.NEW;

    private String notes;

    public Order() {}

    public Order(Date orderDate, List<Part> parts, String supplier) {
        this.orderDate = orderDate;
        this.parts = parts != null ? parts : new ArrayList<>();
        this.supplier = supplier;
        this.status = OrderStatus.NEW;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Date getOrderDate() { return orderDate; }
    public void setOrderDate(Date orderDate) { this.orderDate = orderDate; }

    public Date getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(Date deliveryDate) { this.deliveryDate = deliveryDate; }

    public List<Part> getParts() { return parts; }
    public void setParts(List<Part> parts) { this.parts = parts; }

    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public double calculateTotalCost() {
        return parts.stream()
                .mapToDouble(p -> p.getPrice() * p.getQuantity())
                .sum();
    }
}
