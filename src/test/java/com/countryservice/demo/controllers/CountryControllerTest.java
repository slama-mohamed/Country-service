package com.countryservice.demo.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.countryservice.demo.beans.Country;
import com.countryservice.demo.services.CountryService;

@WebMvcTest(CountryController.class)
class CountryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    CountryService countryService;

    private final String json = "{\"name\":\"Tunisia\",\"capital\":\"Tunis\"}";

    @Test
    void getCountries() throws Exception {
        when(countryService.getAllCountries()).thenReturn(List.of(new Country(1, "Tunisia", "Tunis")));
        mockMvc.perform(get("/getcountries"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].name").value("Tunisia"));
    }

    @Test
    void getCountryById_trouve() throws Exception {
        when(countryService.getCountryById(1)).thenReturn(new Country(1, "Tunisia", "Tunis"));
        mockMvc.perform(get("/getcountries/1"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.capital").value("Tunis"));
    }

    @Test
    void getCountryById_introuvable() throws Exception {
        when(countryService.getCountryById(99)).thenReturn(null);
        mockMvc.perform(get("/getcountries/99"))
               .andExpect(status().isNotFound());
    }

    @Test
    void getCountryByName_trouve() throws Exception {
        when(countryService.getCountryByName("Tunisia")).thenReturn(new Country(1, "Tunisia", "Tunis"));
        mockMvc.perform(get("/getcountries/countryname").param("name", "Tunisia"))
               .andExpect(status().isOk());
    }

    @Test
    void getCountryByName_introuvable() throws Exception {
        when(countryService.getCountryByName("Japan")).thenReturn(null);
        mockMvc.perform(get("/getcountries/countryname").param("name", "Japan"))
               .andExpect(status().isNotFound());
    }

    @Test
    void addCountry() throws Exception {
        when(countryService.addCountry(any(Country.class))).thenReturn(new Country(1, "Tunisia", "Tunis"));
        mockMvc.perform(post("/addcountry").contentType(MediaType.APPLICATION_JSON).content(json))
               .andExpect(status().isCreated());
    }

    @Test
    void updateCountry_trouve() throws Exception {
        Country existing = new Country(1, "Old", "Old");
        when(countryService.getCountryById(1)).thenReturn(existing);
        when(countryService.updateCountry(any(Country.class))).thenReturn(existing);
        mockMvc.perform(put("/updatecountry/1").contentType(MediaType.APPLICATION_JSON).content(json))
               .andExpect(status().isOk());
        verify(countryService).updateCountry(existing);
    }

    @Test
    void updateCountry_introuvable() throws Exception {
        when(countryService.getCountryById(99)).thenReturn(null);
        mockMvc.perform(put("/updatecountry/99").contentType(MediaType.APPLICATION_JSON).content(json))
               .andExpect(status().isNotFound());
    }

    @Test
    void deleteCountry_trouve() throws Exception {
        Country c = new Country(1, "Tunisia", "Tunis");
        when(countryService.getCountryById(1)).thenReturn(c);
        mockMvc.perform(delete("/deletecountry/1"))
               .andExpect(status().isOk());
        verify(countryService).deleteCountry(c);
    }

    @Test
    void deleteCountry_introuvable() throws Exception {
        when(countryService.getCountryById(99)).thenReturn(null);
        mockMvc.perform(delete("/deletecountry/99"))
               .andExpect(status().isNotFound());
    }
}