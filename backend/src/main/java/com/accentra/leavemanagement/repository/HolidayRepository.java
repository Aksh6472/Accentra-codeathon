package com.accentra.leavemanagement.repository;

import com.accentra.leavemanagement.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    List<Holiday> findByDateBetweenOrderByDate(LocalDate from, LocalDate to);

    List<Holiday> findAllByOrderByDate();

    boolean existsByDate(LocalDate date);
}
