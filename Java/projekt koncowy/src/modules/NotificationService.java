package modules;

import java.util.ArrayList;
import java.util.List;

public class NotificationService {

    public enum NotificationType {
        EMAIL, SMS
    }

    private List<NotificationLog> history = new ArrayList<>();

    public void sendNotification(Client client, String message) {
        sendNotification((Person) client, message, NotificationType.EMAIL);
    }

    public void sendNotification(Person person, String message, NotificationType type) {
        if (person == null || person.getEmail() == null || person.getEmail().isBlank()) {
            System.err.println("Błąd: brak adresu email.");
            return;
        }

        NotificationLog log = new NotificationLog(person, message, type);
        person.addNotification(log);
        history.add(log);

        System.out.println("[" + type + "] Wysyłanie powiadomienia do " + person.getEmail() + ": " + message);
    }

    public List<NotificationLog> getHistory() {
        return history;
    }

    public void printHistory() {
        System.out.println("Historia powiadomień:");
        for (NotificationLog log : history) {
            System.out.println(log);
        }
    }
}
