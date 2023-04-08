package eim.project.mobile_banking_android_app.transactions.transfers

data class Transfer(
    val IBANdest: String,
    val IBANscr: String,
    val amount: Double,
    val currency: String,
    val description: String,
    val date: String
)
{
    constructor() : this("","", 0.0, "RON", "", "")
}