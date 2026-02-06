package com.theratime.appointment.delegate;

import com.theratime.appointment.service.AppointmentsService;
import com.theratime.appointments.model.AppointmentResponse;
import com.theratime.appointments.model.BookAppointmentRequest;
import com.theratime.appointments.model.RescheduleAppointmentRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentApiDelegateImplTest {

    @Mock
    private AppointmentsService service;

    @InjectMocks
    private AppointmentApiDelegateImpl delegate;

    @Test
    void bookAppointment_returns201AndBody() {
        BookAppointmentRequest request = new BookAppointmentRequest()
                .therapistId(2L)
                .userId(3L)
                .startTime(LocalDateTime.now().plusDays(1).atOffset(ZoneOffset.UTC))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1).atOffset(ZoneOffset.UTC));
        AppointmentResponse response = new AppointmentResponse().id(42L);
        when(service.bookAppointment(request)).thenReturn(response);

        ResponseEntity<AppointmentResponse> result = delegate.bookAppointment(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(response);
        verify(service).bookAppointment(request);
    }

    @Test
    void rescheduleAppointment_returns200AndBody() {
        Long id = 1L;
        RescheduleAppointmentRequest request = new RescheduleAppointmentRequest()
                .startTime(LocalDateTime.now().plusDays(2).atOffset(ZoneOffset.UTC))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(1).atOffset(ZoneOffset.UTC));
        AppointmentResponse response = new AppointmentResponse().id(1L);
        when(service.rescheduleAppointment(eq(id), any(RescheduleAppointmentRequest.class))).thenReturn(response);

        ResponseEntity<AppointmentResponse> result = delegate.rescheduleAppointment(id, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
        verify(service).rescheduleAppointment(id, request);
    }

    @Test
    void cancelAppointment_returns200() {
        Long id = 5L;
        ResponseEntity<Void> result = delegate.cancelAppointment(id);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(service).cancelAppointment(id);
    }
}
