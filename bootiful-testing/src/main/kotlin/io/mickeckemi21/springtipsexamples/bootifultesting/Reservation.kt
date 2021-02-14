package io.mickeckemi21.springtipsexamples.bootifultesting

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
data class Reservation(
    @Id
    @GeneratedValue
    val id: Long?,
    val name: String
)
