package io.mickeckemi21.springtipsexamples.springstatemachine

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.statemachine.StateMachine
import org.springframework.statemachine.config.EnableStateMachineFactory
import org.springframework.statemachine.config.StateMachineConfigurerAdapter
import org.springframework.statemachine.config.StateMachineFactory
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer
import org.springframework.statemachine.listener.StateMachineListenerAdapter
import org.springframework.statemachine.state.State
import org.springframework.statemachine.support.DefaultStateMachineContext
import org.springframework.statemachine.support.StateMachineInterceptorAdapter
import org.springframework.statemachine.transition.Transition
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*

@SpringBootApplication
class SpringStatemachineApplication

fun main(args: Array<String>) {
    runApplication<SpringStatemachineApplication>(*args)
}

@Component
class Runner(
    private val orderService: OrderService
) : CommandLineRunner {

    companion object {
        private val log = LoggerFactory.getLogger(CommandLineRunner::class.java)
    }

    override fun run(vararg args: String) {
        val order = orderService.create(LocalDateTime.now())

        val paymentStateMachine = orderService.pay(order.id!!, UUID.randomUUID().toString())
        log.info("after calling pay(): ${paymentStateMachine.state.id.name}")
        log.info("order : ${orderService.orderById(order.id!!)}")
        val fulfilledStateMachine = orderService.fulfill(order.id!!)
        log.info("after calling fulfill(): ${fulfilledStateMachine.state.id.name}")
        log.info("order : ${orderService.orderById(order.id!!)}")
    }
}

@Service
class OrderService(
    private val stateMachineFactory: StateMachineFactory<OrderState, OrderEvent>,
    private val orderRepository: OrderRepository
) {

    companion object {
        private const val ORDER_ID_HEADER = "orderId"
    }

    fun orderById(id: Long): Order = orderRepository.findById(id).get()

    fun create(whenDate: LocalDateTime): Order =
        orderRepository.save(
            Order(
                whenDate,
                OrderState.SUBMITTED
            )
        )

    fun pay(orderId: Long, confirmationNumber: String): StateMachine<OrderState, OrderEvent> {
        val stateMachine = build(orderId)

        val paymentMessage = MessageBuilder.withPayload(OrderEvent.PAY)
            .setHeader(ORDER_ID_HEADER, orderId)
            .setHeader("confirmationNumber", confirmationNumber)
            .build()

        stateMachine.sendEvent(paymentMessage)
        return stateMachine
    }

    fun fulfill(orderId: Long): StateMachine<OrderState, OrderEvent> {
        val stateMachine = build(orderId)

        val fulfillmentMessage = MessageBuilder.withPayload(OrderEvent.FULFILL)
            .setHeader(ORDER_ID_HEADER, orderId)
            .build()

        stateMachine.sendEvent(fulfillmentMessage)
        return stateMachine
    }

    private fun build(orderId: Long): StateMachine<OrderState, OrderEvent> {
        val order = orderRepository.findById(orderId).get()
        val orderIdKey = order.id.toString()

        val stateMachine = stateMachineFactory.getStateMachine(orderIdKey)

        stateMachine.stop()

        stateMachine.stateMachineAccessor.doWithAllRegions { stateMachineAccess ->
            val stateMachineInterceptor = object : StateMachineInterceptorAdapter<OrderState, OrderEvent>() {
                override fun preStateChange(
                    state: State<OrderState, OrderEvent>?,
                    message: Message<OrderEvent>?,
                    transition: Transition<OrderState, OrderEvent>?,
                    stateMachine: StateMachine<OrderState, OrderEvent>?,
                    rootStateMachine: StateMachine<OrderState, OrderEvent>?
                ) {
                    if (message != null) {
                        val orderIdHeader = message.headers[ORDER_ID_HEADER] as? Long
                        if (orderIdHeader != null) {
                            val orderById = orderRepository.findById(orderIdHeader).get()
                            orderById.state = state!!.id
                            orderRepository.save(orderById)
                        }
                    }
                }
            }

            stateMachineAccess.addStateMachineInterceptor(stateMachineInterceptor)
            stateMachineAccess.resetStateMachine(
                DefaultStateMachineContext(
                    order.state,
                    null,
                    null,
                    null
                )
            )
        }

        stateMachine.start()

        return stateMachine
    }

}

@Repository
interface OrderRepository : JpaRepository<Order, Long>

@Entity(name = "ORDERS")
data class Order(
    var dateTime: LocalDateTime,
    @Enumerated(EnumType.STRING)
    var state: OrderState,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)

enum class OrderEvent {
    FULFILL,
    PAY,
    CANCEL
}

enum class OrderState {
    SUBMITTED,
    PAID,
    FULFILLED,
    CANCELLED
}

@Configuration
@EnableStateMachineFactory
class SimpleEnumStateMachineConfig : StateMachineConfigurerAdapter<OrderState, OrderEvent>() {

    companion object {
        private val log = LoggerFactory.getLogger(SimpleEnumStateMachineConfig::class.java)
    }

    override fun configure(config: StateMachineConfigurationConfigurer<OrderState, OrderEvent>) {
        val listener = object : StateMachineListenerAdapter<OrderState, OrderEvent>() {
            override fun stateChanged(from: State<OrderState, OrderEvent>?, to: State<OrderState, OrderEvent>) {
                log.info("stateChanged(from: $from, to $to)")
            }
        }
        config.withConfiguration()
            .autoStartup(false)
            .listener(listener)
    }

    override fun configure(states: StateMachineStateConfigurer<OrderState, OrderEvent>) {
        states.withStates()
            .initial(OrderState.SUBMITTED)
            .stateEntry(OrderState.SUBMITTED) { context ->
                val orderId = context.extendedState.variables.getOrDefault("orderId", -1L)
                log.info("entered submitted state!")
                log.info("orderId: $orderId")
            }
            .state(OrderState.PAID)
            .end(OrderState.FULFILLED)
            .end(OrderState.CANCELLED)
    }

    override fun configure(transitions: StateMachineTransitionConfigurer<OrderState, OrderEvent>) {
        transitions
            .withExternal().source(OrderState.SUBMITTED).target(OrderState.PAID).event(OrderEvent.PAY)
            .and()
            .withExternal().source(OrderState.PAID).target(OrderState.FULFILLED).event(OrderEvent.FULFILL)
            .and()
            .withExternal().source(OrderState.SUBMITTED).target(OrderState.CANCELLED).event(OrderEvent.CANCEL)
            .and()
            .withExternal().source(OrderState.PAID).target(OrderState.CANCELLED).event(OrderEvent.CANCEL)
    }
}
