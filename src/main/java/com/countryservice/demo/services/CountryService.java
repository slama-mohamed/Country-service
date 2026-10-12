package com.countryservice.demo.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.countryservice.demo.beans.Country;
import com.countryservice.demo.repositories.CountryRepository;

@Service
public class CountryService {

    @Autowired
    CountryRepository countryRep;

    public List<Country> getAllCountries() {
        return countryRep.findAll();
    }

    public Country getCountryById(int id) {
        return countryRep.findById(id).orElse(null);
    }

    public Country getCountryByName(String name) {
        for (Country con : countryRep.findAll()) {
            if (name.equalsIgnoreCase(con.getName()))
                return con;
        }
        return null;
    }

    public Country addCountry(Country country) {
        return countryRep.save(country);
    }

    public Country updateCountry(Country country) {
        return countryRep.save(country);
    }

    public void deleteCountry(Country country) {
        countryRep.delete(country);
    }
}