package io.mickeckemi21.springtipsexamples.springutilsclasses

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.slf4j.LoggerFactory
import org.springframework.aop.support.AopUtils
import org.springframework.beans.BeanUtils
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.core.ResolvableType
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import org.springframework.util.*
import org.springframework.web.bind.ServletRequestUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.springframework.web.servlet.support.RequestContextUtils
import org.springframework.web.util.WebUtils
import java.io.File
import java.io.FileReader
import javax.servlet.http.HttpServletRequest

@SpringBootApplication
class SpringUtilsClassesApplication

fun main(args: Array<String>) {
    runApplication<SpringUtilsClassesApplication>(*args)
}

@Component
class CmdRunner(
    private val clazz: Clazz
) : CommandLineRunner {

    companion object {
        private val log = LoggerFactory.getLogger(CmdRunner::class.java)
    }

    override fun run(vararg args: String) {
        // Assert class
        Assert.notNull(clazz.list, "the list can't be null")

        // BeanUtils class
        beansUtils(clazz)

        // ClassUtils class
        classUtils()

        // SystemPropertyUtils class
        systemPropertyUtils()

        // FileCopyUtils class
        fileCopyUtils()

        // WebUtils, RequestContextUtils, and ServletRequestUtils classes
        webUtils()

        // AopUtils class
        aopUtils(clazz)

        // ReflectionUtils class
        reflectionUtils()

    }

    private fun beansUtils(clazz: Clazz) {
        val propertyDescriptors = BeanUtils.getPropertyDescriptors(clazz::class.java)
        propertyDescriptors.forEach {
            log.info("property descriptor: ${it.name}")
//            log.info("property descriptor read method: ${it.readMethod}")
        }
    }

    private fun classUtils() {
        val constructor = ClassUtils.getConstructorIfAvailable(Clazz::class.java)
        log.info("constructor: $constructor")
        val newInstance = constructor!!.newInstance()
        log.info("new instance: $newInstance")
    }

    private fun systemPropertyUtils() {
        val resolvedText = SystemPropertyUtils.resolvePlaceholders("my home dir is \${user.home}")
        log.info("resolved text: $resolvedText")
    }

    private fun fileCopyUtils() {
        val file = File(
            SystemPropertyUtils.resolvePlaceholders("\${user.home}"),
            "/Desktop/content.txt"
        )
        FileReader(file).use { reader ->
            val text = FileCopyUtils.copyToString(reader)
            log.info("content: $text")
        }
    }

    private fun webUtils() {
        val template = RestTemplate()
        template.getForEntity("http://localhost:8080/hi", Unit::class.java)
    }

    private fun aopUtils(clazz: Clazz) {
        val targetClass = AopUtils.getTargetClass(clazz)
        log.info("target class: $targetClass")

        val isAopProxy = AopUtils.isAopProxy(clazz)
        log.info("is AopProxy: $isAopProxy")
        val isCglibProxy = AopUtils.isCglibProxy(clazz)
        log.info("is CglibProxy: $isCglibProxy")
    }

    private fun reflectionUtils() {
        ReflectionUtils.doWithFields(Clazz::class.java) { log.info("field: $it") }
        ReflectionUtils.doWithMethods(Clazz::class.java) { log.info("method: $it") }

        val listField = ReflectionUtils.findField(Clazz::class.java, "list")
        log.info("list field: $listField")

        val resolvableTypeForListField = ResolvableType.forField(listField!!)
        log.info("resolvable type for list field: $resolvableTypeForListField")
    }
}

@Component
class Clazz {

    companion object {
        private val log = LoggerFactory.getLogger(Clazz::class.java)
    }

    var list: List<Map<String, Any>>? = emptyList()

    fun begin() {
        log.info("begin()")
    }

    override fun toString(): String {
        return "Clazz(list=$list)"
    }
}

@RestController
class SimpleRestController {

    companion object {
        private val log = LoggerFactory.getLogger(SimpleRestController::class.java)
    }

    @GetMapping("/hi")
    fun hi(request: HttpServletRequest) {
        val age = ServletRequestUtils.getIntParameter(request, "age", -1)
        log.info("age is: $age")

        val tempDir = WebUtils.getTempDir(request.servletContext)
        log.info("temp dir of web server: ${tempDir.absolutePath}")

        val applicationContext = RequestContextUtils.findWebApplicationContext(request)
        val environment = applicationContext!!.getBean(Environment::class.java)
        log.info("application context resolved property: ${environment.getProperty("user.home")}")
    }

}

@Aspect
@Component
class SimpleBeforeAspect {

    companion object {
        private val log = LoggerFactory.getLogger(SimpleBeforeAspect::class.java)
    }

    @Before("execution(* *.begin(..))")
    fun beforeBegin(joinPoint: JoinPoint) {
        log.info("before begin()")
        log.info("signature: ${joinPoint.signature}")
    }

}
