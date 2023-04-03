package eim.project.mobile_banking_android_app.home.home

class Card(
    val number: String,
    val nameOnCard: String,
    val expirationDate: String,
    val cvv: String,
    val sold: Double = 0.0,
    val currency: String = "RON"
)
