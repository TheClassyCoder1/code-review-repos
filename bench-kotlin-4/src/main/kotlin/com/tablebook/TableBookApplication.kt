package com.tablebook

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TableBookApplication

fun main(args: Array<String>) {
    runApplication<TableBookApplication>(*args)
}
