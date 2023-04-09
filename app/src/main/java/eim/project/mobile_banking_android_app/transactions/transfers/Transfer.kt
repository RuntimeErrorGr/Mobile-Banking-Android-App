package eim.project.mobile_banking_android_app.transactions.transfers

data class Transfer(
    val destIban: String,
    val srcIban: String,
    val amount: Double = 0.0,
    val currency: String = "RON",
    val description: String? = "New transfer",
    val date: String = "01/01/2023",
    val type: String = "income",
    val srcName: String = "",
    val destName: String = ""
)
{
    constructor() : this("", "", 0.0, "RON", "New transfer", "01/01/2022", "income")
}