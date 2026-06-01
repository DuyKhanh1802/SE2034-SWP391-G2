package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Country;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.CountryRepository;
import lombok.*;
import org.springframework.data.domain.Sort;

import java.util.List;

@RequiredArgsConstructor
public class CountryService {

    private final CountryRepository countryRepository;
    public List<Country> getAllCountries(){
        return countryRepository.findAll(Sort.by(Sort.Direction.ASC, "countryName"));    }
}
