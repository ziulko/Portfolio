package modules;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class NotificationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationService.NotificationType type;

    private LocalDateTime dateTime;

    @ManyToOne
    private Person recipient;

    public NotificationLog() {
    }

    public NotificationLog(Person recipient, String message, NotificationService.NotificationType type) {
        this.recipient = recipient;
        this.message = message;
        this.type = type;
        this.dateTime = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Person getRecipient() {
        return recipient;
    }

    public String getMessage() {
        return message;
    }

    public NotificationService.NotificationType getType() {
        return type;
    }

    public LocalDateTime getTimestamp() {
        return dateTime;
    }

    @Override
    public String toString() {
        return "[" + dateTime + "] (" + type + ") Do: " + recipient.getEmail() + " - " + message;
    }
}
