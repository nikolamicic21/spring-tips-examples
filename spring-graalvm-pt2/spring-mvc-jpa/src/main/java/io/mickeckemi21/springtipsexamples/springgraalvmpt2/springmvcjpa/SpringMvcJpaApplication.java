package io.mickeckemi21.springtipsexamples.springgraalvmpt2.springmvcjpa;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.stream.Stream;

@SpringBootApplication(
        exclude = SpringDataWebAutoConfiguration.class,
        proxyBeanMethods = false
)
public class SpringMvcJpaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringMvcJpaApplication.class, args);
    }

}

@Component
@RequiredArgsConstructor
@Slf4j
class AppInitializr implements ApplicationListener<ApplicationReadyEvent> {

    private final CustomerRepository customerRepository;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        Stream.of("John", "Dave", "Mike")
                .map(name -> new Customer(null, name))
                .map(this.customerRepository::save)
                .forEach(customer -> log.info(customer.toString()));
    }

}

@RestController
@RequiredArgsConstructor
@RequestMapping("/customers")
class CustomerRestController {

    private final CustomerRepository customerRepository;

    @GetMapping
    Iterable<Customer> getCustomers() {
        return this.customerRepository.findAll();
    }

}

interface CustomerRepository extends CrudRepository<Customer, Long> {

}

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

}
