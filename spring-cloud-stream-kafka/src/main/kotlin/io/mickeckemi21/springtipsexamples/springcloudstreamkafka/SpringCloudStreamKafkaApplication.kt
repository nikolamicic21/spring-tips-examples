package io.mickeckemi21.springtipsexamples.springcloudstreamkafka

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.kstream.Grouped
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.KTable
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import kotlin.properties.Delegates
import kotlin.random.Random

fun main(args: Array<String>) {
    runApplication<SpringCloudStreamKafkaApplication>(*args)
}

@SpringBootApplication
class SpringCloudStreamKafkaApplication {

    @Component
    class PageViewEventSource {

        companion object {
            private val log = LoggerFactory.getLogger(PageViewEventSource::class.java)
        }

        @Bean
        fun pageViewEventSupplier(): Supplier<PageViewEvent> = Supplier {
                val pages = listOf("blog", "sitemap", "initializr", "news", "colophon", "about")
                val names = listOf("mfisher", "dsyer", "schacko", "abilan", "grussell")

                val rPage = pages[Random.nextInt(pages.size)]
                val rName = names[Random.nextInt(names.size)]
                val pageViewEvent = PageViewEvent().apply {
                    userId = rName
                    page = rPage
                    duration = if (Math.random() > 0.5) 10 else 1000
                }
                log.info("sent: $pageViewEvent")
                pageViewEvent
            }

    }

    @Component
    class PageViewEventProcessor {

        @Bean
        fun pageViewEventProcessor(): Function<KStream<String, PageViewEvent>, KStream<String, Long>> = Function { pageView ->
            pageView
                .filter { _, value -> value.duration > 10 }
                .map { _, value -> KeyValue(value.page, "0") }
                .groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
                .count(Materialized.`as`(PAGE_COUNT_MATERIALIZED_VIEW))
                .toStream()
        }

    }

    @Component
    class PageCountSink {

        companion object {
            private val log = LoggerFactory.getLogger(PageCountSink::class.java)
        }

        @Bean
        fun pageCountSink(): Consumer<KTable<String, Long>> = Consumer { counts ->
            counts
                .toStream()
                .foreach { key, value -> log.info("$key = $value") }
        }

    }

}

@RestController
class CountRestController(
    private val queryService: InteractiveQueryService
) {

    @GetMapping("/counts")
    fun counts(): Map<String, Long> {
        val countsMap = mutableMapOf<String, Long>()

        val readOnlyKeyValueStore = queryService.getQueryableStore(
            PAGE_COUNT_MATERIALIZED_VIEW,
            QueryableStoreTypes.keyValueStore<String, Long>()
        )

        val counts = readOnlyKeyValueStore.all()
        while (counts.hasNext()) {
            val next = counts.next()
            countsMap[next.key] = next.value
        }

        return countsMap
    }

}

const val PAGE_COUNT_MATERIALIZED_VIEW = "pc-mv"

class PageViewEvent {
    var userId by Delegates.notNull<String>()
    var page by Delegates.notNull<String>()
    var duration by Delegates.notNull<Long>()

    override fun toString(): String {
        return "PageViewEvent(userId='$userId', page='$page', duration=$duration)"
    }

}
