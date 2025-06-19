package modules;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Payment {

    public enum PaymentMethod {
        CASH,
        CREDIT_CARD,
        BANK_TRANSFER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double amount;

    private LocalDateTime paymentDate;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @ManyToOne
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    public Payment() {
    }

    public Payment(double amount, LocalDateTime paymentDate, PaymentMethod method, Invoice invoice) {
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.method = method;
        this.invoice = invoice;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    @Override
    public String toString() {
        return "Payment{" +
                "id=" + id +
                ", amount=" + amount +
                ", paymentDate=" + paymentDate +
                ", method=" + method +
                ", invoice=" + (invoice != null ? invoice.getId() : "null") +
                '}';
    }
}
