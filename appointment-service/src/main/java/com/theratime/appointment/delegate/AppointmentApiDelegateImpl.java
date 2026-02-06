package com.theratime.appointment.delegate;

import com.theratime.appointment.service.AppointmentsService;
import com.theratime.appointments.api.AppointmentsApiDelegate;
import com.theratime.appointments.model.AppointmentResponse;
import com.theratime.appointments.model.BookAppointmentRequest;
import com.theratime.appointments.model.RescheduleAppointmentRequest;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class AppointmentApiDelegateImpl implements AppointmentsApiDelegate {
    private final AppointmentsService service;

    @Timed(value = "appointments.create.timer", description = "Time taken to create an appointment")
    @PreAuthorize("hasAnyRole('THERAPIST', 'ADMIN')")
    @Override
    public ResponseEntity<AppointmentResponse> bookAppointment(BookAppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.bookAppointment(request));
    }

    @Override
    public ResponseEntity<AppointmentResponse> rescheduleAppointment(Long id,
                                                                     RescheduleAppointmentRequest rescheduleAppointmentRequest) {
        return ResponseEntity.ok(
                service.rescheduleAppointment(id, rescheduleAppointmentRequest)
        );
    }

    @Override
    public ResponseEntity<Void> cancelAppointment(Long id) {
        service.cancelAppointment(id);
        return ResponseEntity.ok().build();
    }

}
