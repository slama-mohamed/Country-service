package org.sid.compteservice; // Votre package actuel (laissez tel quel)

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// 1. Importer la classe principale depuis son vrai package :
import com.countryservice.demo.CountryServiceApplication;

// 2. Indiquer la classe principale à Spring Boot :
@SpringBootTest(classes = CountryServiceApplication.class)
class CountryServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}