package eim.project.mobile_banking_android_app.transactions.transfers

/**
 * Data class representing a transfer between two accounts.
 */
data class Transfer(
    val destIban: String,
    val srcIban: String,
    var amount: Double = 0.0,
    var currency: String = "RON",
    val description: String? = "New transfer",
    val date: String = "01/01/2023",
    var type: String = "outcome",
    val srcName: String = "",
    val destName: String = ""
)
{
    constructor() : this("", "", 0.0, "RON", "New transfer", "01/01/2022", "income")
}