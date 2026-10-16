package com.acme.tracking.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "carrier")
public class Carrier extends PanacheEntity {

    @Column(nullable = false, unique = true, length = 16)
    public String code;

    @Column(name = "display_name", nullable = false)
    public String displayName;

    @Column(name = "contact_email", nullable = false)
    public String contactEmail;
}
