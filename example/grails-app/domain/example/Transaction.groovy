package example

class Transaction {

    Account account
    String referenceNumber
    BigDecimal amount
    Date transactionDate
    static constraints = { referenceNumber unique: true }
    String toString() { referenceNumber }

}
