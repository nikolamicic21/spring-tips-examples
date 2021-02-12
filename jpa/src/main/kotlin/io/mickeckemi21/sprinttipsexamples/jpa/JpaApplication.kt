package io.mickeckemi21.sprinttipsexamples.jpa

import org.apache.commons.lang3.builder.ToStringBuilder
import org.hibernate.envers.Audited
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.domain.AuditorAware
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.repository.history.RevisionRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*

@SpringBootApplication
@EnableJpaRepositories(repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean::class)
@EnableJpaAuditing
class JpaApplication

fun main(args: Array<String>) {
    runApplication<JpaApplication>(*args)
}

@Controller
class CustomerViewController(private val customerRepository: CustomerRepository) {

    @GetMapping("/customers.view")
    fun customers(model: Model): String {
        model.addAttribute("customers", customerRepository.findAll())
        return "customers"
    }

}

@Component
class JpaAppWriter(
    private val em: EntityManager,
    private val transactionTemplate: TransactionTemplate,
    private val customerRepository: CustomerRepository
) : ApplicationRunner {

    companion object {
        private val log = LoggerFactory.getLogger(JpaAppWriter::class.java)
    }

    override fun run(args: ApplicationArguments) {
        transactionTemplate.execute {
            "Dave,Syer;Phil,Webb;Mark,Fisher".split(";").stream()
                .map { it.split(",") }
                .forEach {
                    em.persist(Customer().apply {
                        first = it[0]
                        last = it[1]
                    })
                }

            val customers = em.createQuery("select c from Customer c", Customer::class.java)
            customers.resultList.forEach { log.info("TypedQuery Result: ${ToStringBuilder.reflectionToString(it)}") }
        }

        transactionTemplate.execute {
            customerRepository.findAll().forEach { customer ->
                val countOfOrders = (Math.random() * 5).toInt()
                (0..countOfOrders).forEach { i ->
                    customer.orders.add(Order().apply {
                        sku = "sku_$i"
                    })
                    customerRepository.save(customer)
                }
            }
        }

        transactionTemplate.execute {
            log.info("=====================================")
            customerRepository.findByFirstAndLast("Dave", "Syer")
                .forEach { log.info(ToStringBuilder.reflectionToString(it)) }
            log.info("=====================================")
            customerRepository.byFullName("Dave", "Syer").forEach { log.info(ToStringBuilder.reflectionToString(it)) }
            log.info("=====================================")
            customerRepository.orderSummary().forEach { log.info("sku ${it.getSku()} has ${it.getCount()} instances") }
        }

        Thread.sleep(1000 * 2);

        transactionTemplate.execute {
            customerRepository.byFullName("Dave", "Syer").forEach { dave ->
                dave.first = "David"
                customerRepository.save(dave)
            }
        }

        transactionTemplate.execute {
            customerRepository.byFullName("David", "Syer").forEach { david ->
                customerRepository.findRevisions(david.id!!).forEach { revision ->
                    log.info(
                        "revision " + ToStringBuilder.reflectionToString(revision.metadata) +
                                " for entity " + ToStringBuilder.reflectionToString(revision.entity)
                    )

                }
            }
        }
    }
}

interface CustomerRepository : RevisionRepository<Customer, Long, Int>, JpaRepository<Customer, Long> {

    fun findByFirstAndLast(first: String, last: String): Collection<Customer>

    @Query("select c from Customer c where c.first = :first and c.last = :last")
    fun byFullName(@Param("first") first: String, @Param("last") last: String): Collection<Customer>

    @Query(nativeQuery = true)
    fun orderSummary(): Collection<OrderSummary>

}

interface OrderSummary {
    fun getCount(): Long
    fun getSku(): String
}

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
open class MappedAuditableBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @CreatedDate
    open var createdAt: LocalDateTime? = null

    @LastModifiedDate
    open var lastModifiedAt: LocalDateTime? = null

    @CreatedBy
    open var createdBy: String? = null

    @LastModifiedBy
    open var lastModifiedBy: String? = null

}

@Component
class Auditor(
    @Value("\${user.name}") private val user: String
) : AuditorAware<String> {

    override fun getCurrentAuditor(): Optional<String> = Optional.of(user)

}

@Entity
@Audited
@Table(name = "customers")
@NamedNativeQueries(
    NamedNativeQuery(
        name = "Customer.orderSummary",
        query = "select sku as sku, count(id) as count from orders group by sku"
    )
)
open class Customer : MappedAuditableBase() {

    @Column(name = "first_name")
    open var first: String? = null

    @Column(name = "last_name")
    open var last: String? = null

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "customer_fk")
    open var orders: MutableSet<Order> = mutableSetOf()

}

@Entity
@Audited
@Table(name = "orders")
open class Order : MappedAuditableBase() {
    open var sku: String? = null
}
