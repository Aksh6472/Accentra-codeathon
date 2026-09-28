package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.dto.CreateHolidayRequest;
import com.accentra.leavemanagement.dto.HolidayResponse;
import com.accentra.leavemanagement.entity.Holiday;
import com.accentra.leavemanagement.enums.AuditAction;
import com.accentra.leavemanagement.exception.DuplicateResourceException;
import com.accentra.leavemanagement.exception.ResourceNotFoundException;
import com.accentra.leavemanagement.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Public holiday calendar. Changes affect leave-day calculation for requests submitted afterwards;
 * already-submitted requests keep the day count they were created with.
 */
@Service
@RequiredArgsConstructor
public class HolidayService {

    private final HolidayRepository holidayRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<HolidayResponse> list(Integer year) {
        List<Holiday> holidays = year == null
                ? holidayRepository.findAllByOrderByDate()
                : holidayRepository.findByDateBetweenOrderByDate(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        return holidays.stream().map(HolidayResponse::from).toList();
    }

    @Transactional
    public HolidayResponse create(Actor actor, CreateHolidayRequest request) {
        if (holidayRepository.existsByDate(request.date())) {
            throw new DuplicateResourceException("A holiday already exists on " + request.date());
        }
        Holiday holiday = holidayRepository.save(new Holiday(request.name().trim(), request.date()));
        auditService.recordConfigurationChange(actor, AuditAction.HOLIDAY_ADDED,
                "Added holiday %s on %s".formatted(holiday.getName(), holiday.getDate()));
        return HolidayResponse.from(holiday);
    }

    @Transactional
    public void delete(Actor actor, Long id) {
        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found"));
        holidayRepository.delete(holiday);
        auditService.recordConfigurationChange(actor, AuditAction.HOLIDAY_REMOVED,
                "Removed holiday %s on %s".formatted(holiday.getName(), holiday.getDate()));
    }
}
