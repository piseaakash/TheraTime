package com.theratime.appointment.controller;

import com.theratime.appointment.service.AppointmentsService;
import com.theratime.appointments.api.AppointmentsApiDelegate;
import com.theratime.appointments.model.AppointmentResponse;
import com.theratime.appointments.model.BookAppointmentRequest;
import com.theratime.appointments.model.RescheduleAppointmentRequest;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class AppointmentApiDelegateImpl implements AppointmentsApiDelegate {
    private final AppointmentsService service;

    @Timed(value = "appointments.create.timer", description = "Time taken to create an appointment")
    @Override
    public ResponseEntity<AppointmentResponse> bookAppointment(BookAppointmentRequest request) {

        return ResponseEntity.ok(
                service.bookAppointment(request)
        );
    }

    public ResponseEntity<AppointmentResponse> rescheduleAppointment(Long id,
                                                                     RescheduleAppointmentRequest rescheduleAppointmentRequest) {
        return ResponseEntity.ok(
                service.rescheduleAppointment(id, rescheduleAppointmentRequest)
        );
    }

    public ResponseEntity<Void> cancelAppointment(Long id) {
        service.cancelAppointment(id);
        return ResponseEntity.ok().build();
    }

}
