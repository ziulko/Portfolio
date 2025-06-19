package modules;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
public class ServiceVisit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Date visitDate;
    private String description;
    private double cost;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PLANNED;

    @ManyToOne
    private Car car;

    @ManyToOne
    private Mechanic mechanic;

    @ManyToOne
    private Client client;

    @ManyToMany
    private List<Part> parts;

    @OneToOne(mappedBy = "visit", cascade = CascadeType.ALL)
    private Invoice invoice;

    public enum Status {
        PLANNED, IN_PROGRESS, COMPLETED, CANCELED
    }

    public ServiceVisit() {}

    public ServiceVisit(Date visitDate, String description, double cost, Car car, Mechanic mechanic, List<Part> parts) {
        this.visitDate = visitDate;
        this.description = description;
        this.cost = cost;
        setCar(car);
        this.mechanic = mechanic;
        this.parts = parts;
        this.status = Status.PLANNED;
    }

    @PrePersist
    protected void onCreate() {
        if (visitDate == null) {
            visitDate = new Date();
        }
    }

    public double calculatePartsCost() {
        return parts == null ? 0 : parts.stream().mapToDouble(Part::getPrice).sum();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Date getVisitDate() { return visitDate; }
    public void setVisitDate(Date visitDate) { this.visitDate = visitDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }

    public Car getCar() { return car; }
    public void setCar(Car car) {
        this.car = car;
        if (car != null) {
            this.client = car.getOwner();
        }
    }

    public Mechanic getMechanic() { return mechanic; }
    public void setMechanic(Mechanic mechanic) { this.mechanic = mechanic; }

    public List<Part> getParts() { return parts; }
    public void setParts(List<Part> parts) { this.parts = parts; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }
}
