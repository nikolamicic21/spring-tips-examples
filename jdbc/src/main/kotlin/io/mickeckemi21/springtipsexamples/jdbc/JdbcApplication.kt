package io.mickeckemi21.springtipsexamples.jdbc

import org.simpleflatmapper.jdbc.spring.JdbcTemplateMapperFactory
import org.simpleflatmapper.jdbc.spring.ResultSetExtractorImpl
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.NamingStrategy
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.SqlParameter
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Types
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Stream
import javax.sql.DataSource

@SpringBootApplication
class JdbcApplication

fun main(args: Array<String>) {
    runApplication<JdbcApplication>(*args)
}

@Component
@Order(1)
class QueryCustomersAndOrdersCount(private val jdbcTemplate: JdbcTemplate) : ApplicationRunner {

    companion object {
        private val log = LoggerFactory.getLogger(QueryCustomersAndOrdersCount::class.java)
    }

    override fun run(args: ApplicationArguments) {
        StringUtils.line()
        val queryCustomersAndOrdersCountSql =
            "SELECT c.*, (" +
                    "SELECT COUNT(o.id) FROM orders o WHERE o.customer_fk = c.id" +
                    ") count FROM customers c"
        jdbcTemplate.query(queryCustomersAndOrdersCountSql) { rs, _ ->
            CustomerOrderReport(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getLong("count")
            )
        }.forEach { log.info(it.toString()) }
    }

    data class CustomerOrderReport(
        private val id: Long,
        private val name: String,
        private val email: String,
        private val count: Long
    )
}

@Component
@Order(2)
class QueryCustomersAndOrders(private val jdbcTemplate: JdbcTemplate) : ApplicationRunner {

    companion object {
        private val log = LoggerFactory.getLogger(QueryCustomersAndOrders::class.java)
    }

    override fun run(args: ApplicationArguments) {
        StringUtils.line()
        val queryCustomersAndOrdersSql =
            "SELECT c.id cid, c.*, o.id oid, o.* " +
                    "FROM customers c " +
                    "LEFT JOIN orders o " +
                    "ON c.id = o.customer_fk " +
                    "ORDER BY cid"
        val resultSetExtractor = ResultSetExtractor<Collection<Customer>> { rs ->
            val customerMap = ConcurrentHashMap<Long, Customer>()
            var currentCustomer: Customer? = null
            while (rs.next()) {
                val cid = rs.getLong("cid")
                if (currentCustomer == null || currentCustomer.id != cid) {
                    currentCustomer = Customer(
                        cid,
                        rs.getString("name"),
                        rs.getString("email"),
                        mutableSetOf()
                    )
                }
                val oid = rs.getLong("oid")
                if (oid != 0L) {
                    val sku = rs.getString("sku")
                    val order = Order(oid, sku)
                    currentCustomer.orders.add(order)
                }
                customerMap[cid] = currentCustomer
            }
            customerMap.values
        }
        jdbcTemplate.query(queryCustomersAndOrdersSql, resultSetExtractor)
            ?.forEach { log.info(it.toString()) }
    }

    data class Customer(
        val id: Long,
        private val name: String,
        private val email: String,
        val orders: MutableSet<Order>
    )

    data class Order(
        private val id: Long,
        private val sku: String
    )
}

@Component
@Order(3)
class QueryCustomersAndOrdersSimpleFlatMapper(private val jdbcTemplate: JdbcTemplate) : ApplicationRunner {

    companion object {
        private val log = LoggerFactory.getLogger(QueryCustomersAndOrders::class.java)
    }

    override fun run(args: ApplicationArguments) {
        StringUtils.line()
        val queryCustomersAndOrdersSql =
            "SELECT c.id id, c.name name, c.email email, o.id orders_id, o.sku orders_sku " +
                    "FROM customers c " +
                    "LEFT JOIN orders o " +
                    "ON c.id = o.customer_fk " +
                    "ORDER BY id"
        val resultSetExtractor: ResultSetExtractorImpl<Customer> = JdbcTemplateMapperFactory
            .newInstance()
            .addKeys("id")
            .newResultSetExtractor(Customer::class.java)

        jdbcTemplate.query(queryCustomersAndOrdersSql, resultSetExtractor)
            ?.map { customer ->
                if (customer.orders.any { order -> order.id == null }) customer.orders = mutableSetOf()

                customer
            }
            ?.forEach { log.info(it.toString()) }
    }

    data class Customer(
        val id: Long,
        private val name: String,
        private val email: String,
        var orders: MutableSet<Order>
    )

    data class Order(
        val id: Long?,
        private val sku: String?
    )
}

@Component
@Order(4)
class JdbcTemplateWriter(private val jdbcTemplate: JdbcTemplate) : ApplicationRunner {

    private val customerRowMapper = RowMapper<Customer> { rs, _ -> Customer(rs.getLong("id"), rs.getString("name"), rs.getString("email")) }

    companion object {
        private val log = LoggerFactory.getLogger(JdbcTemplateWriter::class.java)
    }

    override fun run(args: ApplicationArguments) {
        StringUtils.line()
        Stream.of("A", "B", "C").forEach { insert(it, "$it@$it.com") }
        jdbcTemplate.query("SELECT * FROM customers", customerRowMapper).forEach { log.info(it.toString()) }
    }

    fun insert(name: String, email: String): Customer {
        val keyHolder = GeneratedKeyHolder()
        jdbcTemplate.update({ conn ->
            val pst = conn.prepareStatement("INSERT INTO customers(name, email) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)
            pst.setString(1, name)
            pst.setString(2, email)
            pst
        }, keyHolder)
        val idOfNewCustomer = keyHolder.key?.toLong()
        return jdbcTemplate.queryForObject("SELECT c.* FROM customers c WHERE c.id = ?", customerRowMapper, idOfNewCustomer)!!
    }

    data class Customer(
        val id: Long,
        private val name: String,
        private val email: String,
    )
}

@Component
@Order(5)
class JdbcObjectWriter(private val dataSource: DataSource) : ApplicationRunner {

    companion object {
        private val log = LoggerFactory.getLogger(JdbcObjectWriter::class.java)
    }

    private val simpleJdbcInsert = SimpleJdbcInsert(dataSource)
        .withTableName("customers")
        .usingGeneratedKeyColumns("id")

    private val findById = CustomerMappingSqlQuery(
        dataSource,
        "SELECT * FROM customers WHERE id = ?",
        SqlParameter(Types.BIGINT)
    )

    private val findAll = CustomerMappingSqlQuery(
        dataSource,
        "SELECT * FROM customers"
    )

    private class CustomerMappingSqlQuery(ds: DataSource, sql: String, vararg params: SqlParameter) :
        MappingSqlQuery<Customer>(ds, sql) {

        init {
            setParameters(*params)
            afterPropertiesSet()
        }

        override fun mapRow(rs: ResultSet, rowNum: Int): Customer =
            Customer(rs.getLong("id"), rs.getString("name"), rs.getString("email"))
    }

    override fun run(args: ApplicationArguments) {
        StringUtils.line()
        Stream.of("A", "B", "C").forEach { insert(it, "$it@$it.com") }
        findAll.execute().forEach { log.info(it.toString()) }
    }

    fun insert(name: String, email: String): Customer {
        val parameters = mapOf(
            "name" to name,
            "email" to email
        )
        val id = simpleJdbcInsert.executeAndReturnKey(parameters).toLong()
        return findById.findObject(id)!!
    }

    data class Customer(
        val id: Long,
        private val name: String,
        private val email: String,
    )
}

@Component
@Order(6)
class SpringDataJdbc(private val customerRepository: CustomerRepository) : ApplicationRunner {

    companion object {
        private val log = LoggerFactory.getLogger(SpringDataJdbc::class.java)
    }

    override fun run(args: ApplicationArguments) {
        StringUtils.line()
        Stream.of("A", "B", "C").forEach { customerRepository.save(Customer(null, it, "$it@$it.com")) }
        customerRepository.findAll().forEach { log.info(it.toString()) }
        log.info("FIND BY EMAIL")
        customerRepository.findByEmail("B@B.com").forEach { log.info(it.toString()) }
    }
}

data class Customer(
    @Id
    val id: Long?,
    private val name: String,
    private val email: String,
)

@Repository
interface CustomerRepository : CrudRepository<Customer, Long> {

    @Query("SELECT * FROM customers c WHERE c.email = :email")
    fun findByEmail(@Param("email") email: String): Collection<Customer>

}

@Configuration
@EnableJdbcRepositories
class SpringDataJdbcConfig {

    @Bean
    fun namingStrategy(): NamingStrategy = object : NamingStrategy {
        override fun getTableName(type: Class<*>): String = type.simpleName.toLowerCase() + "s"
    }

}

abstract class StringUtils {

    companion object {
        private val log = LoggerFactory.getLogger(StringUtils::class.java)

        fun line(): Unit = log.info("===================================")
    }

}
