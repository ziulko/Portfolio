package modules;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Mechanic extends Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String specialization;
    private int experienceYears;

    @OneToMany(mappedBy = "recipient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NotificationLog> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "mechanic", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceVisit> serviceVisits = new ArrayList<>();

    public Mechanic() {}

    public Mechanic(String firstName, String lastName, String phoneNumber, String email, String specialization) {
        super(firstName, lastName, phoneNumber, email);
        this.specialization = specialization;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public int getExperienceYears() { return experienceYears; }
    public void setExperienceYears(int experienceYears) { this.experienceYears = experienceYears; }

    public List<NotificationLog> getNotifications() { return notifications; }

    public void addNotification(NotificationLog log) {
        if (log != null) {
            notifications.add(log);
        }
    }

    public List<ServiceVisit> getServiceVisits() { return serviceVisits; }

    public void addServiceVisit(ServiceVisit visit) {
        if (visit != null && !serviceVisits.contains(visit)) {
            serviceVisits.add(visit);
        }
    }

    public void printServiceHistory() {
        System.out.println("Historia wizyt mechanika: " + getFirstName() + " " + getLastName());
        for (ServiceVisit visit : serviceVisits) {
            System.out.println(" - " + visit.getVisitDate() + ": " + visit.getDescription());
        }
    }
}
