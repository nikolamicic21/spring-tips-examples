package io.mickeckemi21.springtipsexamples.webmvcfn

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.function.*
import org.springframework.web.servlet.function.ServerResponse.ok
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.servlet.FilterChain
import javax.servlet.GenericFilter
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import kotlin.properties.Delegates

@SpringBootApplication
class WebmvcFnApplication {

	companion object {
		private val log = LoggerFactory.getLogger(WebmvcFnApplication::class.java)
	}

	@Bean
	fun routes(personHandler: PersonHandler): RouterFunction<ServerResponse> = router {
		GET("/people", personHandler::handleGetAll)
		GET("/people/{id}", personHandler::handleGetById)
		POST("/people", personHandler::handlePost)
		filter { request, function ->
			try {
				log.info("entering ${WebmvcFnApplication::class.java.name}")
				function.invoke(request)
			} finally {
				log.info("exiting ${WebmvcFnApplication::class.java.name}")
			}
		}
	}

}

fun main(args: Array<String>) {
	runApplication<WebmvcFnApplication>(*args)
}

@Component
class SimpleGenericFilter : GenericFilter() {

	companion object {
		private val log = LoggerFactory.getLogger(SimpleGenericFilter::class.java)
	}

	override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
		log.info("entering ${SimpleGenericFilter::class.java.name}")
		chain.doFilter(request, response)
		log.info("exiting ${SimpleGenericFilter::class.java.name}")
	}

}

@Component
class PersonHandler(private val personService: PersonService) {

	fun handleGetAll(request: ServerRequest): ServerResponse =
		ok().body(personService.all())

	fun handleGetById(request: ServerRequest): ServerResponse =
		ok().body(personService.byId(request.pathVariable("id").toLong()))

	fun handlePost(request: ServerRequest): ServerResponse {
		val result = personService.save(request.body(Person::class.java))
		val uri = URI.create("/people/${result.id}")

		return ServerResponse.created(uri).body(result)
	}

}

@RestController
class GreetingsRestController {
	
	@GetMapping("/greet/{name}")
	fun greet(@PathVariable name: String): String = "Hello $name!"
	
}

@Service
class PersonService {

	private val counter = AtomicLong()
	private val people = ConcurrentHashMap.newKeySet<Person>().apply {
		addAll(setOf(
			Person().apply { id=counter.incrementAndGet(); name="Jane" },
			Person().apply { id=counter.incrementAndGet(); name="Josh" },
			Person().apply { id=counter.incrementAndGet(); name="Gordon" },
			Person().apply { id=counter.incrementAndGet(); name="Tommie" },
		))
	}

	fun all(): Set<Person> = people

	fun byId(id: Long): Person = people
		.stream()
		.filter { it.id == id }
		.findFirst()
		.get()

	fun save(p: Person): Person {
		val personName = p.name

		val newPerson = Person().apply {
			id = counter.incrementAndGet()
			name = personName
		}
		people.add(newPerson)

		return newPerson
	}

}

class Person {
	var id by Delegates.notNull<Long>()
	var name by Delegates.notNull<String>()

	override fun toString(): String {
		return "Person(id=$id, name='$name')"
	}
}
