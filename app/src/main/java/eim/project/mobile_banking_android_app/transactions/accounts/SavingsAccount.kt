package eim.project.mobile_banking_android_app.transactions.accounts

data class SavingsAccount(
    var cardNumber: String? = null,
    var currency: String = "RON",
    var isDeposit: Boolean = false,
    val iban: String = "",
    var interest_rate: Double = 0.0,
    var isMain: Boolean = false,
    var name: String = "Main Account",
    var liquidation_date: String? = null,
    var isExpanded : Boolean = false,
    var sold: Double = 0.0
){
    constructor() : this("", "RON", false, "", 0.0, false, "Main Account", "", false, 0.0)

    constructor(iban:String): this(iban=iban, isMain=true)

}