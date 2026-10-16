package com.travel.bookings.model

data class PaymentCard(
    val holderName: String,
    val number: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val cvv: String,
) {
    override fun toString(): String =
        "PaymentCard(holder=$holderName, number=****${number.takeLast(4)}, expiry=$expiryMonth/$expiryYear)"
}
