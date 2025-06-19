package modules;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;

    private LocalDateTime scheduledDate;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne
    @JoinColumn(name = "car_id")
    private Car car;

    public Reminder() {
    }

    public Reminder(String message, LocalDateTime scheduledDate, Client client, Car car) {
        this.message = message;
        this.scheduledDate = scheduledDate;
        this.client = client;
        this.car = car;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDateTime scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Car getCar() {
        return car;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    @Override
    public String toString() {
        return "Reminder{" +
                "id=" + id +
                ", message='" + message + '\'' +
                ", scheduledDate=" + scheduledDate +
                ", client=" + (client != null ? client.getId() : "null") +
                ", car=" + (car != null ? car.getId() : "null") +
                '}';
    }
}
