package eim.project.mobile_banking_android_app.home

data class Card(
    val number: String,
    val nameOnCard: String,
    val expirationDate: String,
    val cvv: String,
    var user: String = "",
    var pin: String = "1234",
    var currentSavingsAccount: SavingsAccount = SavingsAccount(cardNumber=number, isCurrent = true),
    var masked: Boolean = true
) {
    constructor() : this("", "", "", "", "")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        other as Card

        if (number != other.number) return false
        if (nameOnCard != other.nameOnCard) return false
        if (expirationDate != other.expirationDate) return false
        if (cvv != other.cvv) return false
        if (user != other.user) return false

        return true
    }

    override fun hashCode(): Int {
        var result = number.hashCode()
        result = 31 * result + nameOnCard.hashCode()
        result = 31 * result + expirationDate.hashCode()
        result = 31 * result + cvv.hashCode()
        result = 31 * result + user.hashCode()
        return result
    }
}

