package modules;

import javax.persistence.*;

@Entity
public class Car {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String vin;
    private String brand;
    private String model;
    private int year;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client owner;

    public Car() {}

    public Car(String vin, String brand, String model, int year, Client owner) {
        setVin(vin);
        this.brand = brand;
        this.model = model;
        setYear(year);
        this.owner = owner;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVin() { return vin; }
    public void setVin(String vin) {
        if (vin != null && vin.matches("[A-HJ-NPR-Z0-9]{17}")) {
            this.vin = vin;
        } else {
            throw new IllegalArgumentException("Nieprawidłowy numer VIN.");
        }
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getYear() { return year; }
    public void setYear(int year) {
        if (year >= 1886 && year <= java.time.Year.now().getValue()) {
            this.year = year;
        } else {
            throw new IllegalArgumentException("Nieprawidłowy rok produkcji.");
        }
    }

    public Client getOwner() { return owner; }
    public void setOwner(Client owner) { this.owner = owner; }
}
