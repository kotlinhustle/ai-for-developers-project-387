package com.example.backend.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class SpaController {
    @GetMapping("/booking", "/owner")
    fun forwardToIndex(): String = "forward:/index.html"
}
