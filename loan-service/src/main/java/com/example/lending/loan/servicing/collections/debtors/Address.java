package com.example.lending.loan.servicing.collections.debtors;

/** Address as returned by the collections API. */
public class Address {

    private String street;
    private String city;
    private String postalCode;
    private String region;
    private String countryCode;
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
