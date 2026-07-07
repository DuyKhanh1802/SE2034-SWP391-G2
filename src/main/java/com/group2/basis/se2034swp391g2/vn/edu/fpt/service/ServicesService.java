package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.ServiceRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.ServiceProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Set;

@RequiredArgsConstructor
@Service
public class ServicesService {

    private final ServiceRepository serviceRepository;

    private static final int SERVICE_PAGE_SIZE = 6;

    private static final Set<String> VALID_PRICE_FILTERS = Set.of(
            "ALL",
            "UNDER_200",
            "FROM_200_TO_500",
            "OVER_500"
    );

    private static final Set<String> VALID_PRICE_SORTS = Set.of(
            "DEFAULT",
            "ASC",
            "DESC"
    );

    public Page<ServiceProjection> findListDining(int page, String priceFilter, String priceSort) {
        page = normalizePage(page);
        priceFilter = normalizePriceFilter(priceFilter);
        priceSort = normalizePriceSort(priceSort);

        Pageable pageable = PageRequest.of(page, SERVICE_PAGE_SIZE);
        return serviceRepository.findListDining(priceFilter, priceSort, pageable);
    }

    public Page<ServiceProjection> findListWellness(int page, String priceFilter, String priceSort) {
        page = normalizePage(page);
        priceFilter = normalizePriceFilter(priceFilter);
        priceSort = normalizePriceSort(priceSort);

        Pageable pageable = PageRequest.of(page, SERVICE_PAGE_SIZE);
        return serviceRepository.findListWellness(priceFilter, priceSort, pageable);
    }

    public int normalizePage(int page) {
        return Math.max(page, 0);
    }

    public String normalizePriceFilter(String priceFilter) {
        if (priceFilter == null || !VALID_PRICE_FILTERS.contains(priceFilter)) {
            return "ALL";
        }
        return priceFilter;
    }

    public String normalizePriceSort(String priceSort) {
        if (priceSort == null || !VALID_PRICE_SORTS.contains(priceSort)) {
            return "DEFAULT";
        }
        return priceSort;
    }
}