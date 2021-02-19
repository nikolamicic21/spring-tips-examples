import org.springframework.cloud.contract.verifier.config.TestFramework.JUNIT5
import org.springframework.cloud.contract.verifier.config.TestMode.EXPLICIT

plugins {
//    id("groovy")
    id("org.springframework.cloud.contract") version "3.0.1"
    `maven-publish`
}

//dependencyManagement {
//    imports {
//        mavenBom("org.springframework.cloud:spring-cloud-contract-dependencies:${property("3.0.1")}")
//    }
//}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("de.flapdoodle.embed:de.flapdoodle.embed.mongo")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-verifier")
//    testImplementation ("org.codehaus.groovy:groovy-all:${property("groovyVersion")}")
}

contracts {
    setTestFramework(JUNIT5)
    setBaseClassForTests("io.mickeckemi21.springtipsexamples.bootifulreactivetesting.producer.BaseClass")
    setTestMode(EXPLICIT)
}
