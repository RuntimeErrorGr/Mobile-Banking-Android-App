package eim.project.mobile_banking_android_app.home

data class Withdrawal(
    val IBAN: String,
    val amount: Double,
    val currency: String,
    val description: String,
    val date: String
) {
    constructor() : this("", 0.0, "RON", "", "")

}