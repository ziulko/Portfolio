package modules;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
public class Client extends Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Car> cars = new ArrayList<>();

    @OneToMany(mappedBy = "recipient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NotificationLog> notifications = new ArrayList<>();

    public Client() {}

    public Client(String firstName, String lastName, String phoneNumber, String email) {
        super(firstName, lastName, phoneNumber, email);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Car> getCars() {
        return cars;
    }

    public void addCar(Car car) {
        if (car != null && !cars.contains(car)) {
            cars.add(car);
            car.setOwner(this);
        }
    }

    @Override
    public String getFirstName() {
        return super.getFirstName();
    }

    @Override
    public String getEmail() {
        return super.getEmail();
    }

    @Override
    public String getLastName() {
        return super.getLastName();
    }

    @Override
    public String getPhoneNumber() {
        return super.getPhoneNumber();
    }


    public List<NotificationLog> getNotifications() {
        return notifications;
    }

    public void addNotification(NotificationLog log) {
        if (log != null) {
            notifications.add(log);
        }
    }

}