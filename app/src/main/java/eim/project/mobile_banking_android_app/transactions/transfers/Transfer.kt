package eim.project.mobile_banking_android_app.transactions.transfers

data class Transfer(
    val destIban: String,
    val srcIban: String,
    val amount: Double,
    val currency: String,
    val description: String,
    val date: String,
    val type: String
)
{
    constructor() : this("", "", 0.0, "", "", "", "")
}