package io.mickeckemi21.springtipsexamples.springbatch

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.item.database.JdbcBatchItemWriter
import org.springframework.batch.item.database.JdbcCursorItemReader
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.batch.item.file.FlatFileItemWriter
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder
import org.springframework.batch.item.file.transform.DelimitedLineAggregator
import org.springframework.batch.item.file.transform.FieldExtractor
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.*
import org.springframework.util.ResourceUtils
import java.io.File
import javax.sql.DataSource
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

@SpringBootApplication
@EnableBatchProcessing
class SpringBatchApplication {

	@Configuration
	class Step1Config {

		@Bean
		fun fileReader(): FlatFileItemReader<Person> {
			val input = ClassPathResource("in.csv")

			return FlatFileItemReaderBuilder<Person>()
				.name("file-reader")
				.resource(input)
				.targetType(Person::class.java)
				.delimited().delimiter(",").names("firstName", "age", "email")
				.build()
		}

		@Bean
		fun jdbcWriter(dataSource: DataSource?): JdbcBatchItemWriter<Person> =
			JdbcBatchItemWriterBuilder<Person>()
				.dataSource(dataSource!!)
				.sql(
					"INSERT INTO PEOPLE(AGE, FIRST_NAME, EMAIL) " +
							"VALUES (:age, :firstName, :email)"
				)
				.beanMapped()
				.build()

	}

	@Configuration
	class Step2Config {

		@Bean
		fun jdbcReader(dataSource: DataSource?): JdbcCursorItemReader<Map<Int, Int>> =
			JdbcCursorItemReaderBuilder<Map<Int, Int>>()
				.dataSource(dataSource!!)
				.name("jdbc-reader")
				.sql("SELECT COUNT(age) c, age a FROM PEOPLE GROUP BY age")
				.rowMapper { rs, _ -> mapOf(rs.getInt("a") to rs.getInt("c")) }
				.build()

		@Bean
		fun fileWriter(): FlatFileItemWriter<Map<Int, Int>> {
			val output = PathResource("spring-batch/out.csv")
			val delimitedLineAggregator = DelimitedLineAggregator<Map<Int, Int>>()
			delimitedLineAggregator.setDelimiter(",")
			delimitedLineAggregator.setFieldExtractor( FieldExtractor<Map<Int, Int>> {
				val next = it.entries.iterator().next()
				arrayOf(next.key, next.value)
			})

			return FlatFileItemWriterBuilder<Map<Int, Int>>()
				.name("file-writer")
				.resource(output)
				.lineAggregator(delimitedLineAggregator)
				.forceSync(true)
				.append(true)
				.build()
		}

	}

	@Bean
	fun step1(
		stepBuilderFactory: StepBuilderFactory,
		step1Config: Step1Config
	): Step = stepBuilderFactory
		.get("file-db")
		.chunk<Person, Person>(1000)
		.reader(step1Config.fileReader())
		.writer(step1Config.jdbcWriter(null))
		.build()

	@Bean
	fun step2(
		stepBuilderFactory: StepBuilderFactory,
		step2Config: Step2Config
	): Step = stepBuilderFactory
		.get("db-file")
		.chunk<Map<Int, Int>, Map<Int, Int>>(1000)
		.reader(step2Config.jdbcReader(null))
		.writer(step2Config.fileWriter())
		.build()

	@Bean
	fun job(
		jobBuilderFactory: JobBuilderFactory,
		step1: Step,
		step2: Step
	): Job = jobBuilderFactory
		.get("etl")
		.incrementer(RunIdIncrementer())
		.start(step1)
		.next(step2)
		.build()

}

class Person {
	var age: Int? = null
	var firstName: String? = null
	var email: String? = null
}

fun main(args: Array<String>) {
	runApplication<SpringBatchApplication>(*args)
}
