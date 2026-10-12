package com.countryservice.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CountryServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(CountryServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(CountryServiceApplication.class, args);
        log.info("hello GLs");
    }
}