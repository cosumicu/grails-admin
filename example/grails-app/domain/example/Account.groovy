package example
class Account {
    static belongsTo = [customer: Customer]
    Customer customer
    String accountNumber
    String accountType
    BigDecimal balance = 0G
    static constraints = {
        accountNumber blank: false, unique: true
        accountType inList: ['Checking', 'Savings']
        balance min: new BigDecimal('0')
    }
    String toString() { accountNumber }
}
