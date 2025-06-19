package modules;

import javax.persistence.*;

@Entity
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double laborCost;
    private double totalCost;

    @OneToOne
    private ServiceVisit visit;

    public Invoice() {}

    public Invoice(double laborCost, ServiceVisit visit) {
        this.laborCost = laborCost;
        this.visit = visit;
        this.totalCost = calculateTotal();
    }

    private double calculateTotal() {
        double partsCost = visit.getParts().stream().mapToDouble(Part::getPrice).sum();
        return partsCost + laborCost;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public double getLaborCost() { return laborCost; }
    public void setLaborCost(double laborCost) { this.laborCost = laborCost; }

    public double getTotalCost() { return totalCost; }

    public ServiceVisit getVisit() { return visit; }
    public void setVisit(ServiceVisit visit) { this.visit = visit; }
}