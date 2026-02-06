package com.theratime.calendar.service;

import com.theratime.appointment.entity.Appointment;
import com.theratime.appointment.entity.CalendarBlock;
import com.theratime.appointment.mapper.AppointmentMapper;
import com.theratime.appointment.repository.AppointmentRepository;
import com.theratime.appointment.service.UserService;
import com.theratime.appointments.model.AppointmentResponse;
import com.theratime.appointments.model.BlockCalendarRequest;
import com.theratime.appointments.model.CalendarBlockResponse;
import com.theratime.appointments.model.ViewCalendar200Response;
import com.theratime.calendar.mapper.CalendarBlockMapper;
import com.theratime.calendar.repository.CalendarBlockRepository;
import com.theratime.exception.BusinessException;
import com.theratime.security.TenantContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarBlockService {

    private final CalendarBlockRepository calendarBlockRepository;
    private final AppointmentRepository appointmentRepository;
    private final CalendarBlockMapper mapper;
    private final AppointmentMapper appointmentMapper;
    private final UserService userService;

    public boolean isTherapistBlocked(Long therapistId, LocalDateTime startTime, LocalDateTime endTime) {
        return calendarBlockRepository.isTherapistBlocked(therapistId, startTime, endTime);
    }

    @Transactional
    public CalendarBlockResponse blockCalendar(BlockCalendarRequest request) {
        Long tenantId = userService.getTenantId(TenantContext.getCurrentUserId());
        validateTherapist(request.getTherapistId(), tenantId);

        boolean hasOverlap = calendarBlockRepository.isTherapistBlocked(
                request.getTherapistId(),
                request.getStartTime().toLocalDateTime(),
                request.getEndTime().toLocalDateTime()
        );
        if (hasOverlap) {
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

    public ViewCalendar200Response viewCalendar(Long therapistId, OffsetDateTime startDate, OffsetDateTime endDate) {
        Long tenantId = userService.getTenantId(TenantContext.getCurrentUserId());
        validateTherapist(therapistId, tenantId);

        LocalDateTime rangeStart = startDate == null ? LocalDateTime.MIN : startDate.toLocalDateTime();
        LocalDateTime rangeEnd = endDate == null ? LocalDateTime.MAX : endDate.toLocalDateTime();

        List<Appointment> appointments = appointmentRepository.findByTherapistId(therapistId);
        List<CalendarBlock> blocks = calendarBlockRepository.findByTherapistId(therapistId);

        List<AppointmentResponse> appointmentResponses = appointments.stream()
                .filter(a -> a.getStartTime().isBefore(rangeEnd) && a.getEndTime().isAfter(rangeStart))
                .map(appointmentMapper::toResponse)
                .toList();
        List<CalendarBlockResponse> blockResponses = blocks.stream()
                .filter(b -> b.getStartTime().isBefore(rangeEnd) && b.getEndTime().isAfter(rangeStart))
                .map(mapper::toResponse)
                .toList();

        return new ViewCalendar200Response()
                .appointments(appointmentResponses)
                .blocks(blockResponses);
    }

    private void validateTherapist(Long therapistId, Long tenantId) {
        String role = userService.getUserRole(therapistId);
        if (!"THERAPIST".equals(role)) {
            throw new BusinessException("Provided therapistId does not belong to a therapist");
        }
        Long therapistTenantId = userService.getTenantId(therapistId);
        if (!tenantId.equals(therapistTenantId)) {
            throw new BusinessException("Therapist must belong to your practice (tenant)");
        }
    }
}
