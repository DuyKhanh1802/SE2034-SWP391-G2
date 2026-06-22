package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.ServiceRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.ServiceProjection;
import lombok.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ServicesService {

    private final ServiceRepository serviceRepository;

    private static final int DINING_PAGE_SIZE = 6;

    public Page<ServiceProjection> findListDining(int page,String priceFilter,String priceSort){
        Pageable pageable = PageRequest.of(page,DINING_PAGE_SIZE);
        return serviceRepository.findListDining(priceFilter,priceSort,pageable);
    }

    public Page<ServiceProjection> findListWellness(int page,String priceFilter,String priceSort){
        Pageable pageable = PageRequest.of(page,DINING_PAGE_SIZE);
        return serviceRepository.findListWellness(priceFilter,priceSort,pageable);
    }
}
