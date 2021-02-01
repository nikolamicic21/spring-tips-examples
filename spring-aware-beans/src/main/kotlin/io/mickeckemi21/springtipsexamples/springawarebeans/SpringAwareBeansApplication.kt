package io.mickeckemi21.springtipsexamples.springawarebeans

import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.InjectionPoint
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.SmartLifecycle
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.util.Assert
import java.util.logging.Logger
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

@SpringBootApplication
class SpringAwareBeansApplication {

	@Bean
	fun foo(): Foo = Foo()

	@Bean
	fun fooBeanFactory(): FactoryBean<FooBean> = FooBeanFactory()

	@Bean
	@Scope("prototype")
	fun logger(injectionPoint: InjectionPoint): Logger =
		Logger.getLogger(injectionPoint.member.declaringClass.name)

}

fun main(args: Array<String>) {
	runApplication<SpringAwareBeansApplication>(*args)
}

interface FooBean

class Foo1 : FooBean

class Foo2 : FooBean

class FooBeanFactory : FactoryBean<FooBean> {

	var preferFirst = true

	override fun getObject(): FooBean =
		if (preferFirst) Foo1() else Foo2()

	override fun getObjectType(): Class<*> = FooBean::class.java

}

class Foo : SmartLifecycle {// : InitializingBean, DisposableBean {

	private var bar: Bar? = null

	override fun start() {
	}

	override fun stop() {
	}

	override fun isRunning(): Boolean {
		return false
	}

	@PostConstruct
	// Replacing with @PostConstruct
	/*override*/ fun afterPropertiesSet() {
//		Assert.notNull(bar, "bar can't be null")
	}

	@PreDestroy
	// Replacing with @PreDestroy
	/*override*/ fun destroy() {
		bar = null
	}
}

class Bar

@Component
class LoggingComponent(private val logger: Logger) {

	init {
	    logger.info("Hello, World!")
	}

}
