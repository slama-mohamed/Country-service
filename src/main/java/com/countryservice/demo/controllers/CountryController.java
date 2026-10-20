package com.countryservice.demo.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.countryservice.demo.beans.Country;
import com.countryservice.demo.services.CountryService;

@RestController
public class CountryController {

    @Autowired
    CountryService countryservice;

    @GetMapping("/getcountries")
    public ResponseEntity<List<Country>> getCountries() {
        return new ResponseEntity<>(countryservice.getAllCountries(), HttpStatus.OK);
    }

    @GetMapping("/getcountries/{id}")
    public ResponseEntity<Country> getCountryById(@PathVariable(value = "id") int id) {
        Country country = countryservice.getCountryById(id);
        if (country == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(country, HttpStatus.OK);
    }

    @GetMapping("/getcountries/countryname")
    public ResponseEntity<Country> getCountryByName(@RequestParam(value = "name") String name) {
        Country country = countryservice.getCountryByName(name);
        if (country == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(country, HttpStatus.OK);
    }

    @PostMapping("/addcountry")
    public ResponseEntity<Country> addCountry(@RequestBody Country country) {
        return new ResponseEntity<>(countryservice.addCountry(country), HttpStatus.CREATED);
    }

    @PutMapping("/updatecountry/{id}")
    public ResponseEntity<Country> updateCountry(@PathVariable(value = "id") int id,
                                                 @RequestBody Country country) {
        Country existCountry = countryservice.getCountryById(id);
        if (existCountry == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        existCountry.setName(country.getName());
        existCountry.setCapital(country.getCapital());
        return new ResponseEntity<>(countryservice.updateCountry(existCountry), HttpStatus.OK);
    }

    @DeleteMapping("/deletecountry/{id}")
    public ResponseEntity<Country> deleteCountry(@PathVariable("id") int id) {
        Country country = countryservice.getCountryById(id);
        if (country == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        countryservice.deleteCountry(country);
        return new ResponseEntity<>(country, HttpStatus.OK);
    }
}