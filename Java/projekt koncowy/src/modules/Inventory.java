package modules;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(cascade = CascadeType.ALL)
    private List<Part> availableParts = new ArrayList<>();

    public Inventory() {}

    public Inventory(List<Part> availableParts) {
        this.availableParts = availableParts;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public List<Part> getAllParts() { return availableParts; }

    public void addPart(Part part) {
        if (part != null && !availableParts.contains(part)) {
            part.setInventory(this);
            availableParts.add(part);
        }
    }

    public boolean removePart(Part part) {
        return availableParts.remove(part);
    }

    public Part findPartByName(String name) {
        return availableParts.stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public List<Part> getLowStockParts(int threshold) {
        return availableParts.stream()
                .filter(p -> p.getQuantity() < threshold)
                .toList();
    }

    public void restockPart(String partName, int amount) {
        Part part = findPartByName(partName);
        if (part != null && amount > 0) {
            part.setQuantity(part.getQuantity() + amount);
        }
    }

    public List<Part> findPartsByCategory(Part.PartCategory category) {
        return availableParts.stream()
                .filter(p -> p.getCategory() == category)
                .toList();
    }

    public double getTotalInventoryValue() {
        return availableParts.stream()
                .mapToDouble(p -> p.getPrice() * p.getQuantity())
                .sum();
    }

    public long countParts() {
        return availableParts.size();
    }
}
