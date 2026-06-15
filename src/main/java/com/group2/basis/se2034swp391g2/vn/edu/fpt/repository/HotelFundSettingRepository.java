package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.HotelFundSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HotelFundSettingRepository extends JpaRepository<HotelFundSetting, Long> {
    Optional<HotelFundSetting> findTopByOrderByConfiguredAtDesc();
}
