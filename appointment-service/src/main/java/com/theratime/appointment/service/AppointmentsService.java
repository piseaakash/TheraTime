package com.theratime.appointment.service;

import com.theratime.appointment.entity.Appointment;
import com.theratime.appointment.entity.AppointmentStatus;
import com.theratime.appointment.exception.BusinessException;
import com.theratime.appointment.exception.ConflictException;
import com.theratime.appointment.exception.ResourceNotFoundException;
import com.theratime.appointment.mapper.AppointmentMapper;
import com.theratime.appointment.repository.AppointmentRepository;
import com.theratime.appointments.model.AppointmentResponse;
import com.theratime.appointments.model.BookAppointmentRequest;
import com.theratime.appointments.model.RescheduleAppointmentRequest;
import com.theratime.calendar.service.CalendarBlockService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AppointmentsService {

    private final AppointmentRepository appointmentRepository;

    private final AppointmentMapper mapper;

    private final CalendarBlockService calendarBlockService;


    public AppointmentResponse bookAppointment(BookAppointmentRequest request) {

        isTherapistBlocked(request.getTherapistId(),
                request.getStartTime().toLocalDateTime(),
                request.getEndTime().toLocalDateTime());


        existsOverlappedAppointment(request.getTherapistId(),
                request.getStartTime().toLocalDateTime(),
                request.getEndTime().toLocalDateTime());

        Appointment appointment = mapper.toEntity(request);
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointmentRepository.save(appointment);

        AppointmentResponse response = mapper.toResponse(appointment);
        response.setStatus(AppointmentResponse.StatusEnum.BOOKED);
        return response;
    }

    @Transactional
    public AppointmentResponse rescheduleAppointment(Long id, RescheduleAppointmentRequest request) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow( () -> new ResourceNotFoundException("The appointment is not present for the given therapist and user"
                ));

        if(request.getStartTime().toLocalDateTime().equals(appointment.getStartTime())
                && request.getEndTime().toLocalDateTime().equals(appointment.getEndTime())) {
            throw new BusinessException("The start time and end time is the same, please select different date and timing to proceed with rescheduling");
        }

        isTherapistBlocked(appointment.getTherapistId(),
                request.getStartTime().toLocalDateTime(),
                request.getEndTime().toLocalDateTime());


        existsOverlappedAppointment(appointment.getTherapistId(),
                request.getStartTime().toLocalDateTime(),
                request.getEndTime().toLocalDateTime());


        appointment.setStartTime(request.getStartTime().toLocalDateTime());
        appointment.setEndTime(request.getEndTime().toLocalDateTime());
        if(!appointment.getStatus().equals(AppointmentStatus.BOOKED))
            appointment.setStatus(AppointmentStatus.BOOKED);
        try {
            appointmentRepository.save(appointment);
        } catch(ObjectOptimisticLockingFailureException ex) {
            throw new ConflictException("Appointment was modified by another process. Please retry");
        }

        AppointmentResponse response = mapper.toResponse(appointment);
        response.setStatus(AppointmentResponse.StatusEnum.BOOKED);
        return response;
    }

    private void isTherapistBlocked(Long id, LocalDateTime startTime, LocalDateTime endTime) {
        if(calendarBlockService.isTherapistBlocked(
                id,
                startTime,
                endTime
        )){
            throw new BusinessException("Therapist is unavailable for this time slot");
        }
    }

    private void existsOverlappedAppointment(Long therapistId, LocalDateTime startTime, LocalDateTime endTime) {
        if(appointmentRepository.existsOverlappingAppointment(
                therapistId,
                startTime,
                endTime
        )) {
            throw new BusinessException("Therapist already has an existing appointment for the given timing");
        }
    }

    @Transactional
    public void cancelAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        appointment.setStatus(AppointmentStatus.CANCELLED);

        try {
            appointmentRepository.save(appointment);
        } catch(ObjectOptimisticLockingFailureException ex) {
            throw new ConflictException("Appointment was modified by another process. Please retry");
        }
    }
}
