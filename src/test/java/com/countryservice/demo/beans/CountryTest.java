package com.countryservice.demo.beans;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CountryTest {

    @Test
    void constructeurVide_etSetters() {
        Country c = new Country();
        c.setIdCountry(1);
        c.setName("Tunisia");
        c.setCapital("Tunis");

        assertEquals(1, c.getIdCountry());
        assertEquals("Tunisia", c.getName());
        assertEquals("Tunis", c.getCapital());
    }

    @Test
    void constructeurComplet() {
        Country c = new Country(2, "France", "Paris");

        assertEquals(2, c.getIdCountry());
        assertEquals("France", c.getName());
        assertEquals("Paris", c.getCapital());
    }
}