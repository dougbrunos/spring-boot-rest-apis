package br.com.douglas.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test/v1")
public class TestLogController {

    private final Logger logger = LoggerFactory.getLogger(TestLogController.class.getName());

    @GetMapping
    public String testLog() {
        logger.debug("This is an DEBUG log message");
        logger.info("This is an INFO log message");
        logger.warn("This is an WARN log message");
        logger.error("This is an ERROR log message");
        return "Logs generated successfully";
    }

}
