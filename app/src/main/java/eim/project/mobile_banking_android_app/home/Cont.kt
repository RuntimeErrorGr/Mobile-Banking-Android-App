package eim.project.mobile_banking_android_app.home

data class Cont(
    val IBAN: String,
    var sold: Double,
    var currency: String,
    var card: Card
){
    constructor() : this("", 0.0, "RON", Card())
}