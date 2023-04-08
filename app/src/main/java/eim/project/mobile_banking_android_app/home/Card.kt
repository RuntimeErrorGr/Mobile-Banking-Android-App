package eim.project.mobile_banking_android_app.home

import eim.project.mobile_banking_android_app.transactions.accounts.SavingsAccount
import eim.project.mobile_banking_android_app.transactions.transfers.Transfer

data class Card(
    val number: String,
    val nameOnCard: String,
    val expirationDate: String,
    val cvv: String,
    var pin: String = "1234",
    var savingsAccounts: ArrayList<SavingsAccount> = ArrayList(),
    var transfers: ArrayList<Transfer> = ArrayList(),
    var masked: Boolean = true
) {
    constructor() : this("", "", "", "", "1234", ArrayList(), ArrayList(), true)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        other as Card

        if (number != other.number) return false
        if (nameOnCard != other.nameOnCard) return false

        return true
    }

    override fun hashCode(): Int {
        var result = number.hashCode()
        result = 31 * result + nameOnCard.hashCode()
        result = 31 * result + expirationDate.hashCode()
        result = 31 * result + cvv.hashCode()
        return result
    }
}

