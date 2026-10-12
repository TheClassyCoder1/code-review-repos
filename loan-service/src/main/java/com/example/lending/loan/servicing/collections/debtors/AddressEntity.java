package com.example.lending.loan.servicing.collections.debtors;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Postal address stored with a debtor. */
@Embeddable
public class AddressEntity {

    @Column(name = "street")
    private String street;
    @Column(name = "city")
    private String city;
    @Column(name = "postal_code")
    private String postalCode;
    @Column(name = "region")
    private String region;
    @Column(name = "country_code")
    private String countryCode;
    @Column(name = "country")
    private String country;

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}
