package com.example.backend

import com.example.backend.support.PostgresTestSupport
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class BackendApplicationTests : PostgresTestSupport() {
    @Test
    fun contextLoads() {
    }
}
