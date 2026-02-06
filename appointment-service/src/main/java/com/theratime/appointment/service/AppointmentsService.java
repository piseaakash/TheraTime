package com.theratime.appointment.service;

import com.theratime.appointment.entity.Appointment;
import com.theratime.appointment.entity.AppointmentStatus;
import com.theratime.appointment.mapper.AppointmentMapper;
import com.theratime.appointment.repository.AppointmentRepository;
import com.theratime.appointments.model.AppointmentResponse;
import com.theratime.appointments.model.BookAppointmentRequest;
import com.theratime.appointments.model.RescheduleAppointmentRequest;
import com.theratime.calendar.service.CalendarBlockService;
import com.theratime.exception.BusinessException;
import com.theratime.exception.ConflictException;
import com.theratime.exception.ResourceNotFoundException;
import com.theratime.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.theratime.appointment.event.AppointmentEventPayload.EVENT_CANCELLED;
import static com.theratime.appointment.event.AppointmentEventPayload.EVENT_CREATED;
import static com.theratime.appointment.event.AppointmentEventPayload.EVENT_RESCHEDULED;

@Service
@RequiredArgsConstructor
public class AppointmentsService {

    private final AppointmentRepository appointmentRepository;

    private final AppointmentMapper mapper;

    private final CalendarBlockService calendarBlockService;
    private final UserService userService;
    private final OutboxService outboxService;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public AppointmentResponse bookAppointment(BookAppointmentRequest request) {
        Long tenantId = userService.getTenantId(TenantContext.getCurrentUserId());
        validateUserAndTherapist(request, tenantId);

        isTherapistBlocked(request.getTherapistId(),
                request.getStartTime().toLocalDateTime(),
                request.getEndTime().toLocalDateTime());

        existsOverlappedAppointment(request.getTherapistId(),
                request.getStartTime().toLocalDateTime(),
                request.getEndTime().toLocalDateTime());

        Appointment appointment = mapper.toEntity(request);
        appointment.setStatus(AppointmentStatus.BOOKED);
        try {
            appointmentRepository.save(appointment);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Slot already taken for this therapist and time");
        }
        outboxService.enqueueEvent(tenantId, appointment, EVENT_CREATED);

        AppointmentResponse response = mapper.toResponse(appointment);
        response.setStatus(AppointmentResponse.StatusEnum.BOOKED);
        return response;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public AppointmentResponse rescheduleAppointment(Long id, RescheduleAppointmentRequest request) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("The appointment is not present for the given therapist and user"));

        if (request.getStartTime().toLocalDateTime().equals(appointment.getStartTime())
                && request.getEndTime().toLocalDateTime().equals(appointment.getEndTime())) {
            throw new BusinessException("The start time and end time is the same, please select different date and timing to proceed with rescheduling");
        }

        isTherapistBlocked(appointment.getTherapistId(),
                request.getStartTime().toLocalDateTime(),
                request.getEndTime().toLocalDateTime());

        existsOverlappedAppointment(
                appointment.getTherapistId(),
                request.getStartTime().toLocalDateTime(),
                request.getEndTime().toLocalDateTime(),
                appointment.getId());

        appointment.setStartTime(request.getStartTime().toLocalDateTime());
        appointment.setEndTime(request.getEndTime().toLocalDateTime());
        if (!appointment.getStatus().equals(AppointmentStatus.BOOKED)) {
            appointment.setStatus(AppointmentStatus.BOOKED);
        }
        try {
            appointmentRepository.save(appointment);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ConflictException("Appointment was modified by another process. Please retry");
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Slot already taken for this therapist and time");
        }
        outboxService.enqueueEvent(TenantContext.getTenantId(), appointment, EVENT_RESCHEDULED);

        AppointmentResponse response = mapper.toResponse(appointment);
        response.setStatus(AppointmentResponse.StatusEnum.BOOKED);
        return response;
    }

    private void isTherapistBlocked(Long therapistId, LocalDateTime startTime, LocalDateTime endTime) {
        if (calendarBlockService.isTherapistBlocked(therapistId, startTime, endTime)) {
            throw new BusinessException("Therapist is unavailable for this time slot");
        }
    }

    private void existsOverlappedAppointment(Long therapistId, LocalDateTime startTime, LocalDateTime endTime) {
        if (appointmentRepository.existsOverlappingAppointment(therapistId, startTime, endTime)) {
            throw new BusinessException("Therapist already has an existing appointment for the given timing");
        }
    }

    private void existsOverlappedAppointment(Long therapistId, LocalDateTime startTime, LocalDateTime endTime, Long excludeAppointmentId) {
        if (appointmentRepository.existsOverlappingAppointment(therapistId, startTime, endTime, excludeAppointmentId)) {
            throw new BusinessException("Therapist already has an existing appointment for the given timing");
        }
    }

    private void validateUserAndTherapist(BookAppointmentRequest request, Long tenantId) {
        userService.isUserPresent(request.getUserId());
        String therapistRole = userService.getUserRole(request.getTherapistId());
        if (!"THERAPIST".equals(therapistRole)) {
            throw new BusinessException("Provided therapistId does not belong to a therapist");
        }
        Long userTenantId = userService.getTenantId(request.getUserId());
        Long therapistTenantId = userService.getTenantId(request.getTherapistId());
        if (!tenantId.equals(userTenantId) || !tenantId.equals(therapistTenantId)) {
            throw new BusinessException("User and therapist must belong to the same practice (tenant)");
        }
    }

    @Transactional
    public void cancelAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        appointment.setStatus(AppointmentStatus.CANCELLED);

        try {
            appointmentRepository.save(appointment);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ConflictException("Appointment was modified by another process. Please retry");
        }
        outboxService.enqueueEvent(TenantContext.getTenantId(), appointment, EVENT_CANCELLED);
    }
}
