package com.travel.bookings.model

enum class FareClass(val label: String, val cssClass: String) {
    ECONOMY("Economy", "fare-economy"),
    PREMIUM_ECONOMY("Premium Economy", "fare-premium"),
    BUSINESS("Business", "fare-business"),
    FIRST("First", "fare-first"),
}
