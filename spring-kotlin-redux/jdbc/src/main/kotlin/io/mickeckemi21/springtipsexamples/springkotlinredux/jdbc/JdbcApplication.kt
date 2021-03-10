package io.mickeckemi21.springtipsexamples.springkotlinredux.jdbc

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Primary
import org.springframework.context.support.beans
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class JdbcApplication

fun main(args: Array<String>) {

    SpringApplicationBuilder()
        .initializers(beans {
            bean {
                ApplicationRunner {
                    val customerService = ref<CustomerService>()
                    listOf("Merry", "Joe", "Fillip")
                        .map { Customer(it) }
                        .forEach { customerService.insert(it) }
                    customerService.all()
                        .forEach { println(it) }
                }
            }
        })
        .sources(JdbcApplication::class.java)
        .run(*args)
}

@RestController
class CustomerRestController(
    private val customerService: CustomerService
) {

    @GetMapping("/customers")
    fun getCustomers(): Collection<Customer> = customerService.all()

}

@Service
@Primary
@Transactional
class ExposedCustomerService : CustomerService {

    override fun all(): Collection<Customer> = Customers
        .selectAll()
        .map { Customer(it[Customers.name], it[Customers.id]) }

    override fun byId(id: Long): Customer? = Customers
        .select { Customers.id.eq(id) }
        .map { Customer(it[Customers.name], it[Customers.id]) }
        .firstOrNull()

    override fun insert(customer: Customer) {
        Customers.insert { it[name] = customer.name }
    }
}

object Customers : Table() {
    val id = long("id").autoIncrement()
    val name = varchar("name", 255)

    override val primaryKey = PrimaryKey(id)
}

@Service
@Transactional
class JdbcTemplateCustomerService(
    private val jdbcTemplate: JdbcTemplate
) : CustomerService {

    override fun all(): Collection<Customer> =
        jdbcTemplate.query("select * from CUSTOMERS") { rs, _ ->
            Customer(rs.getString("NAME"), rs.getLong("ID"))
        }

    override fun byId(id: Long): Customer? =
        jdbcTemplate.queryForObject("select * from CUSTOMERS where ID=?", id) { rs, _ ->
            Customer(rs.getString("NAME"), rs.getLong("ID"))
        }

    override fun insert(customer: Customer) {
        jdbcTemplate.execute("insert into CUSTOMERS(NAME) VALUES(?)") { ps ->
            ps.setString(1, customer.name)
            ps.execute()
        }
    }

}

interface CustomerService {
    fun all(): Collection<Customer>
    fun byId(id: Long): Customer?
    fun insert(customer: Customer)
}

data class Customer(
    val name: String,
    var id: Long? = null
)
