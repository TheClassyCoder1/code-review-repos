package com.example.lending.loan.servicing.collections.debtors;

/** Converts between the stored and the API address representation. */
public final class AddressMapper {

    private AddressMapper() {
    }

    public static Address map(final AddressEntity addressEntity) {
        final Address address = new Address();
        address.setStreet(addressEntity.getStreet());
        address.setCity(addressEntity.getCity());
        address.setPostalCode(addressEntity.getPostalCode());
        address.setRegion(addressEntity.getRegion());
        address.setCountryCode(addressEntity.getCountryCode());
        address.setCountry(addressEntity.getCountry());
        return address;
    }

    public static AddressEntity map(final Address address) {
        final AddressEntity addressEntity = new AddressEntity();
        addressEntity.setStreet(address.getStreet());
        addressEntity.setCity(address.getCity());
        addressEntity.setPostalCode(address.getPostalCode());
        addressEntity.setRegion(address.getRegion());
        addressEntity.setCountryCode(address.getCountryCode());
        addressEntity.setCountry(address.getCountry());
        return addressEntity;
    }
}
