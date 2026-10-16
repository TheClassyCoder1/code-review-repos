package com.acme.tracking.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "facility")
public class Facility extends PanacheEntity {

    @Column(nullable = false, unique = true, length = 12)
    public String code;

    @Column(nullable = false)
    public String city;

    @Column(name = "country_code", nullable = false, length = 2)
    public String countryCode;
}
