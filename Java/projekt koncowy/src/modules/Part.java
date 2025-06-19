package modules;

import javax.persistence.*;

@Entity
public class Part {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String manufacturer;
    private double price;
    private int quantity;

    @Enumerated(EnumType.STRING)
    private PartCategory category;

    private int warrantyMonths;

    @ManyToOne
    private Inventory inventory;
    public enum PartCategory {
        ESSENTIAL,
        COMFORT,
        SAFETY,
        MAINTENANCE,
        ELECTRONICS,
        BODYWORK,
        OTHER
    }

    public Part() {}

    public Part(String name, String manufacturer, double price, int quantity, PartCategory category, int warrantyMonths) {
        this.name = name;
        this.manufacturer = manufacturer;
        this.price = price;
        this.quantity = quantity;
        this.category = category;
        this.warrantyMonths = warrantyMonths;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public PartCategory getCategory() { return category; }
    public void setCategory(PartCategory category) { this.category = category; }

    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }

    public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    public boolean usePart(int amount) {
        if (quantity >= amount) {
            quantity -= amount;
            return true;
        }
        return false;
    }

    public void increaseQuantity(int amount) {
        this.quantity += amount;
    }

}
