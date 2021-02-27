package io.mickeckemi21.springtipsexamples.bpmflowable

import org.flowable.engine.RuntimeService
import org.flowable.engine.TaskService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class BpmFlowableApplicationTests {

    companion object {
        private const val CUSTOMER_ID_VARIABLE = "customerId"
        private const val EMAIL_VARIABLE = "email"

        private val log = LoggerFactory.getLogger(BpmFlowableApplicationTests::class.java)
    }

    @Autowired
    private lateinit var runtimeService: RuntimeService

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var emailService: EmailService

    @Test
    fun test() {
        // given
        val customerId = "1"
        val email = "email@email.com"

        // when
        val processInstanceId = beginCustomerEnrollmentProcess(customerId, email)
        log.info("process instance ID: $processInstanceId")
        // then
        assertNotNull(processInstanceId)

        // when
        val tasks = taskService.createTaskQuery()
            .taskName("confirm-email-task")
            .includeProcessVariables()
            .processVariableValueEquals(CUSTOMER_ID_VARIABLE, customerId)
            .list()
        // then
        assertTrue(tasks.size >= 1)

        // when
        tasks.forEach {
            taskService.claim(it.id, "user")
            taskService.complete(it.id)
        }
        // then
        assertEquals(1, emailService.sends[email]!!.get())
    }

    private fun beginCustomerEnrollmentProcess(customerId: String, email: String): String? {
        val vars = mapOf(
            CUSTOMER_ID_VARIABLE to customerId,
            EMAIL_VARIABLE to email
        )
        val processInstance = runtimeService.startProcessInstanceByKey(
            "signup-process",
            vars
        )

        return processInstance.id
    }

}
