package eim.project.mobile_banking_android_app.home

data class SavingsAccount(
    var sold: Double = 0.0,
    var currency: String = "RON",
    val IBAN: String = currency.take(2).plus((1..22).map { (0..9).random() }.joinToString("")),
    var cardNumber: String? = null,
    var isCurrent: Boolean = false,
    var isDeposit: Boolean = false,
    var interest_rate: Double = 0.0,
    var liquidation_date: String? = null,
){
    constructor() : this(0.0, "RON")
}