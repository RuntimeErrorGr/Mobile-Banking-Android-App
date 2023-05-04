package eim.project.mobile_banking_android_app.transactions.accounts

import eim.project.mobile_banking_android_app.transactions.transfers.Transfer

/**
 * Data class representing a savings account.
 * */
data class SavingsAccount(
    var cardNumber: String? = null,
    var currency: String = "RON",
    var isDeposit: Boolean = false,
    val iban: String = "",
    var interest_rate: Double = 0.0,
    var isMain: Boolean = false,
    var name: String = "New Account",
    var liquidation_date: String? = null,
    var isExpanded : Boolean = false,
    var sold: Double = 0.0,
    var transfers: ArrayList<Transfer> = ArrayList()
){
    constructor() : this("", "RON", false, "", 0.0, false, "New Account", null, false, 0.0, ArrayList())

    constructor(iban:String): this(iban=iban, isMain=true)

}