package com.theratime.calendar.service;

import com.theratime.appointment.entity.CalendarBlock;
import com.theratime.appointment.exception.BusinessException;
import com.theratime.appointment.repository.AppointmentRepository;
import com.theratime.appointments.model.BlockCalendarRequest;
import com.theratime.appointments.model.CalendarBlockResponse;
import com.theratime.calendar.mapper.CalendarBlockMapper;
import com.theratime.calendar.repository.CalendarBlockRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarBlockService {

    private final CalendarBlockRepository calendarBlockRepository;
    private final AppointmentRepository appointmentRepository;
    private final CalendarBlockMapper mapper;

    public boolean isTherapistBlocked(Long therapistId, LocalDateTime startTime, LocalDateTime endTime) {
        return calendarBlockRepository.isTherapistBlocked(therapistId, startTime, endTime);
    }

    @Transactional
    public CalendarBlockResponse blockCalendar(BlockCalendarRequest request) {
        boolean hasOverlap = calendarBlockRepository.isTherapistBlocked(
                request.getTherapistId(),
                request.getStartTime().toLocalDateTime(),
                request.getEndTime().toLocalDateTime()
        );
        if(hasOverlap) {
            throw new BusinessException("Calendar block overlaps with an existing block");
        }

        int cancelledCount = appointmentRepository.cancelAppointmentsInRange(
                request.getTherapistId(),
                request.getStartTime().toLocalDateTime(),
                request.getEndTime().toLocalDateTime()
        );
        log.info("Cancelled {} appointments for therapist {}", cancelledCount, request.getTherapistId());

        CalendarBlock block = mapper.toEntity(request);
        calendarBlockRepository.save(block);

        return mapper.toResponse(block);
    }
}
