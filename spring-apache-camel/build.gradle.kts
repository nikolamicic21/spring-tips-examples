extra["apacheCamelVersion"] = "3.7.2"

dependencyManagement {
	imports {
		mavenBom("org.apache.camel.springboot:camel-spring-boot-bom:${property("apacheCamelVersion")}")
	}
}

dependencies {
	implementation("org.apache.camel.springboot:camel-spring-boot-starter")
	implementation("org.apache.camel.springboot:camel-spring-integration-starter")
	implementation("org.apache.camel.springboot:camel-metrics-starter")
	implementation("org.apache.camel.springboot:camel-micrometer-starter")
	implementation("org.apache.camel.springboot:camel-jms-starter")
	implementation("org.springframework.boot:spring-boot-starter-integration")
	implementation("org.springframework.integration:spring-integration-jms")
	implementation("org.springframework.boot:spring-boot-starter-activemq")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.integration:spring-integration-test")
}
