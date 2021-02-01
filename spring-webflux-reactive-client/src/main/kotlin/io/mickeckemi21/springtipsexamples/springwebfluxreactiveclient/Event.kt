package io.mickeckemi21.springtipsexamples.springwebfluxreactiveclient

import java.util.*

class Event() {

    var id: Long? = null
    var date: Date? = null

    constructor(id: Long, date: Date): this() {
        this.id = id
        this.date = date
    }

    override fun toString(): String {
        return "Event(id=$id, date=$date)"
    }

}
