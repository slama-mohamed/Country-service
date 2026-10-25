package com.countryservice.demo.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.countryservice.demo.beans.Country;
import com.countryservice.demo.repositories.CountryRepository;

@ExtendWith(MockitoExtension.class)
class CountryServiceTest {

    @Mock
    CountryRepository countryRep;

    @InjectMocks
    CountryService service;

    @Test
    void getAllCountries() {
        when(countryRep.findAll()).thenReturn(List.of(new Country(1, "Tunisia", "Tunis")));
        assertEquals(1, service.getAllCountries().size());
    }

    @Test
    void getCountryById_trouve() {
        Country c = new Country(1, "Tunisia", "Tunis");
        when(countryRep.findById(1)).thenReturn(Optional.of(c));
        assertEquals(c, service.getCountryById(1));
    }

    @Test
    void getCountryById_introuvable() {
        when(countryRep.findById(99)).thenReturn(Optional.empty());
        assertNull(service.getCountryById(99));
    }

    @Test
    void getCountryByName_trouve_ignoreLaCasse() {
        Country c = new Country(1, "Tunisia", "Tunis");
        when(countryRep.findAll()).thenReturn(List.of(new Country(2, "France", "Paris"), c));
        assertEquals(c, service.getCountryByName("tUnIsIa"));
    }

    @Test
    void getCountryByName_introuvable() {
        when(countryRep.findAll()).thenReturn(List.of(new Country(2, "France", "Paris")));
        assertNull(service.getCountryByName("Japan"));
    }

    @Test
    void addCountry() {
        Country c = new Country(1, "Tunisia", "Tunis");
        when(countryRep.save(c)).thenReturn(c);
        assertEquals(c, service.addCountry(c));
    }

    @Test
    void updateCountry() {
        Country c = new Country(1, "Tunisia", "Tunis");
        when(countryRep.save(c)).thenReturn(c);
        assertEquals(c, service.updateCountry(c));
    }

    @Test
    void deleteCountry() {
        Country c = new Country(1, "Tunisia", "Tunis");
        service.deleteCountry(c);
        verify(countryRep, times(1)).delete(c);
    }
}