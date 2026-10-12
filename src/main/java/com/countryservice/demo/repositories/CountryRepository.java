package com.countryservice.demo.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import com.countryservice.demo.beans.Country;

public interface CountryRepository extends JpaRepository<Country,Integer> {

}