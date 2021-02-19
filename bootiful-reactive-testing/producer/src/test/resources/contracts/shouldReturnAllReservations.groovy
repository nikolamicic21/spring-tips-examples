package contracts

import org.springframework.cloud.contract.spec.Contract
import org.springframework.http.MediaType

Contract.make {
    description("should return all reservations")
    request {
        url("/reservations")
        method(GET())
    }
    response {
        status(200)
        headers {
            contentType(MediaType.APPLICATION_JSON_VALUE)
        }
        body("""[{"name":"A", "id":"1"}, { "name" :"B", "id" : "2"}]""")
    }
}