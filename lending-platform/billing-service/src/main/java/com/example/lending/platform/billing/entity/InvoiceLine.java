package com.example.lending.platform.billing.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "invoice_line", schema = "platform")
public class InvoiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "kind", nullable = false, length = 24)
    private String kind;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_amount", nullable = false)
    private double unitAmount;

    public InvoiceLine() {
    }

    public InvoiceLine(String kind, String description, int quantity, double unitAmount) {
        this.kind = kind;
        this.description = description;
        this.quantity = quantity;
        this.unitAmount = unitAmount;
    }

    public double lineTotal() {
        return quantity * unitAmount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitAmount() { return unitAmount; }
    public void setUnitAmount(double unitAmount) { this.unitAmount = unitAmount; }
}
