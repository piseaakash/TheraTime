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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentsServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private AppointmentMapper appointmentMapper;
    @Mock
    private CalendarBlockService calendarBlockService;
    @Mock
    private UserService userService;
    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private AppointmentsService appointmentsService;

    @BeforeEach
    void setupTenant() {
        var context = SecurityContextHolder.createEmptyContext();
        var auth = new TestingAuthenticationToken("user", null);
        auth.setAuthenticated(true);
        auth.setDetails(10L);
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        TenantContext.setTenantId(1L);
    }

    @Test
    void bookAppointment_happyPath_enqueuesOutboxAndReturnsResponse() {
        BookAppointmentRequest request = new BookAppointmentRequest()
                .therapistId(2L)
                .userId(3L)
                .startTime(LocalDateTime.now().plusDays(1).atOffset(java.time.ZoneOffset.UTC))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1).atOffset(java.time.ZoneOffset.UTC));

        Appointment appointment = Appointment.builder().build();
        appointment.setId(42L);
        appointment.setTherapistId(2L);
        appointment.setUserId(3L);

        when(userService.getTenantId(10L)).thenReturn(1L);
        when(userService.getTenantId(3L)).thenReturn(1L);
        when(userService.getTenantId(2L)).thenReturn(1L);
        when(userService.isUserPresent(3L)).thenReturn(true);
        when(userService.getUserRole(2L)).thenReturn("THERAPIST");
        when(calendarBlockService.isTherapistBlocked(anyLong(), any(), any())).thenReturn(false);
        when(appointmentMapper.toEntity(request)).thenReturn(appointment);
        when(appointmentMapper.toResponse(appointment)).thenReturn(new AppointmentResponse().id(42L));

        AppointmentResponse response = appointmentsService.bookAppointment(request);

        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getStatus()).isEqualTo(AppointmentResponse.StatusEnum.BOOKED);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.BOOKED);
        verify(appointmentRepository).save(appointment);
        verify(outboxService).enqueueEvent(1L, appointment, "appointment.created");
    }

    @Test
    void bookAppointment_whenTherapistBlocked_throwsBusinessException() {
        BookAppointmentRequest request = new BookAppointmentRequest()
                .therapistId(2L)
                .userId(3L)
                .startTime(LocalDateTime.now().plusDays(1).atOffset(java.time.ZoneOffset.UTC))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1).atOffset(java.time.ZoneOffset.UTC));

        when(userService.getTenantId(10L)).thenReturn(1L);
        when(userService.getTenantId(3L)).thenReturn(1L);
        when(userService.getTenantId(2L)).thenReturn(1L);
        when(userService.isUserPresent(3L)).thenReturn(true);
        when(userService.getUserRole(2L)).thenReturn("THERAPIST");
        when(calendarBlockService.isTherapistBlocked(anyLong(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> appointmentsService.bookAppointment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Therapist is unavailable");

        verify(appointmentRepository, never()).save(any());
        verify(outboxService, never()).enqueueEvent(anyLong(), any(), anyString());
    }

    @Test
    void bookAppointment_whenSlotAlreadyTaken_translatesToConflictException() {
        BookAppointmentRequest request = new BookAppointmentRequest()
                .therapistId(2L)
                .userId(3L)
                .startTime(LocalDateTime.now().plusDays(1).atOffset(java.time.ZoneOffset.UTC))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1).atOffset(java.time.ZoneOffset.UTC));

        Appointment appointment = Appointment.builder().build();
        appointment.setTherapistId(2L);
        appointment.setUserId(3L);

        when(userService.getTenantId(anyLong())).thenReturn(1L);
        when(userService.isUserPresent(anyLong())).thenReturn(true);
        when(userService.getUserRole(anyLong())).thenReturn("THERAPIST");
        when(calendarBlockService.isTherapistBlocked(anyLong(), any(), any())).thenReturn(false);
        when(appointmentMapper.toEntity(request)).thenReturn(appointment);
        when(appointmentRepository.save(appointment)).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> appointmentsService.bookAppointment(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Slot already taken");
    }

    @Test
    void rescheduleAppointment_whenSameTimes_throwsBusinessException() {
        Long id = 1L;
        RescheduleAppointmentRequest request = new RescheduleAppointmentRequest()
                .startTime(LocalDateTime.now().plusDays(1).atOffset(java.time.ZoneOffset.UTC))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1).atOffset(java.time.ZoneOffset.UTC));

        Appointment existing = Appointment.builder().build();
        existing.setId(id);
        existing.setStartTime(request.getStartTime().toLocalDateTime());
        existing.setEndTime(request.getEndTime().toLocalDateTime());
        existing.setTherapistId(2L);
        existing.setStatus(AppointmentStatus.BOOKED);

        when(appointmentRepository.findById(id)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> appointmentsService.rescheduleAppointment(id, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("The start time and end time is the same");
    }

    @Test
    void cancelAppointment_notFound_throwsResourceNotFound() {
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentsService.cancelAppointment(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancelAppointment_optimisticLockFailure_translatesToConflict() {
        Appointment appointment = Appointment.builder().build();
        appointment.setId(5L);
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any()))
                .thenThrow(new ObjectOptimisticLockingFailureException(Appointment.class, 5L));

        assertThatThrownBy(() -> appointmentsService.cancelAppointment(5L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("modified by another process");
    }

    @Test
    void bookAppointment_userNotTherapist_throwsBusinessException() {
        BookAppointmentRequest request = new BookAppointmentRequest()
                .therapistId(2L)
                .userId(3L)
                .startTime(LocalDateTime.now().plusDays(1).atOffset(java.time.ZoneOffset.UTC))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1).atOffset(java.time.ZoneOffset.UTC));

        when(userService.getTenantId(10L)).thenReturn(1L);
        when(userService.isUserPresent(3L)).thenReturn(true);
        when(userService.getUserRole(2L)).thenReturn("USER");

        assertThatThrownBy(() -> appointmentsService.bookAppointment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not belong to a therapist");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void bookAppointment_differentTenant_throwsBusinessException() {
        BookAppointmentRequest request = new BookAppointmentRequest()
                .therapistId(2L)
                .userId(3L)
                .startTime(LocalDateTime.now().plusDays(1).atOffset(java.time.ZoneOffset.UTC))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1).atOffset(java.time.ZoneOffset.UTC));

        when(userService.getTenantId(10L)).thenReturn(1L);
        when(userService.getTenantId(3L)).thenReturn(2L);
        when(userService.getTenantId(2L)).thenReturn(1L);
        when(userService.isUserPresent(3L)).thenReturn(true);
        when(userService.getUserRole(2L)).thenReturn("THERAPIST");

        assertThatThrownBy(() -> appointmentsService.bookAppointment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("same practice");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void bookAppointment_overlappingAppointment_throwsBusinessException() {
        BookAppointmentRequest request = new BookAppointmentRequest()
                .therapistId(2L)
                .userId(3L)
                .startTime(LocalDateTime.now().plusDays(1).atOffset(java.time.ZoneOffset.UTC))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1).atOffset(java.time.ZoneOffset.UTC));

        when(userService.getTenantId(anyLong())).thenReturn(1L);
        when(userService.isUserPresent(anyLong())).thenReturn(true);
        when(userService.getUserRole(anyLong())).thenReturn("THERAPIST");
        when(calendarBlockService.isTherapistBlocked(anyLong(), any(), any())).thenReturn(false);
        when(appointmentRepository.existsOverlappingAppointment(eq(2L), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> appointmentsService.bookAppointment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("existing appointment");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void rescheduleAppointment_notFound_throwsResourceNotFound() {
        Long id = 99L;
        RescheduleAppointmentRequest request = new RescheduleAppointmentRequest()
                .startTime(LocalDateTime.now().plusDays(2).atOffset(java.time.ZoneOffset.UTC))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(1).atOffset(java.time.ZoneOffset.UTC));
        when(appointmentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentsService.rescheduleAppointment(id, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not present");
    }

    @Test
    void rescheduleAppointment_overlapping_throwsBusinessException() {
        Long id = 1L;
        RescheduleAppointmentRequest request = new RescheduleAppointmentRequest()
                .startTime(LocalDateTime.now().plusDays(2).atOffset(java.time.ZoneOffset.UTC))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(1).atOffset(java.time.ZoneOffset.UTC));
        Appointment existing = Appointment.builder().build();
        existing.setId(id);
        existing.setTherapistId(2L);
        existing.setStartTime(LocalDateTime.now().plusDays(3));
        existing.setEndTime(LocalDateTime.now().plusDays(3).plusHours(1));
        existing.setStatus(AppointmentStatus.BOOKED);

        when(appointmentRepository.findById(id)).thenReturn(Optional.of(existing));
        when(calendarBlockService.isTherapistBlocked(anyLong(), any(), any())).thenReturn(false);
        when(appointmentRepository.existsOverlappingAppointment(eq(2L), any(), any(), eq(id))).thenReturn(true);

        assertThatThrownBy(() -> appointmentsService.rescheduleAppointment(id, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("existing appointment");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void rescheduleAppointment_statusNotBooked_setsBookedAndReturns() {
        Long id = 1L;
        RescheduleAppointmentRequest request = new RescheduleAppointmentRequest()
                .startTime(LocalDateTime.now().plusDays(2).atOffset(java.time.ZoneOffset.UTC))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(1).atOffset(java.time.ZoneOffset.UTC));
        Appointment existing = Appointment.builder().build();
        existing.setId(id);
        existing.setTherapistId(2L);
        existing.setStartTime(LocalDateTime.now().plusDays(3));
        existing.setEndTime(LocalDateTime.now().plusDays(3).plusHours(1));
        existing.setStatus(AppointmentStatus.CANCELLED);

        when(appointmentRepository.findById(id)).thenReturn(Optional.of(existing));
        when(calendarBlockService.isTherapistBlocked(anyLong(), any(), any())).thenReturn(false);
        when(appointmentRepository.existsOverlappingAppointment(eq(2L), any(), any(), eq(id))).thenReturn(false);
        when(appointmentMapper.toResponse(any(Appointment.class))).thenReturn(new AppointmentResponse().id(id));

        AppointmentResponse response = appointmentsService.rescheduleAppointment(id, request);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(existing.getStatus()).isEqualTo(AppointmentStatus.BOOKED);
        verify(appointmentRepository).save(existing);
        verify(outboxService).enqueueEvent(TenantContext.getTenantId(), existing, "appointment.rescheduled");
    }

    @Test
    void rescheduleAppointment_dataIntegrityViolation_translatesToConflict() {
        Long id = 1L;
        RescheduleAppointmentRequest request = new RescheduleAppointmentRequest()
                .startTime(LocalDateTime.now().plusDays(2).atOffset(java.time.ZoneOffset.UTC))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(1).atOffset(java.time.ZoneOffset.UTC));

        Appointment existing = Appointment.builder().build();
        existing.setId(id);
        existing.setTherapistId(2L);
        existing.setStartTime(LocalDateTime.now().plusDays(3));
        existing.setEndTime(LocalDateTime.now().plusDays(3).plusHours(1));
        existing.setStatus(AppointmentStatus.BOOKED);

        when(appointmentRepository.findById(id)).thenReturn(Optional.of(existing));
        when(calendarBlockService.isTherapistBlocked(anyLong(), any(), any())).thenReturn(false);
        when(appointmentRepository.existsOverlappingAppointment(eq(2L), any(), any(), eq(id))).thenReturn(false);
        when(appointmentRepository.save(existing))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> appointmentsService.rescheduleAppointment(id, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Slot already taken");
    }

    @Test
    void rescheduleAppointment_optimisticLockFailure_translatesToConflict() {
        Long id = 1L;
        RescheduleAppointmentRequest request = new RescheduleAppointmentRequest()
                .startTime(LocalDateTime.now().plusDays(2).atOffset(java.time.ZoneOffset.UTC))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(1).atOffset(java.time.ZoneOffset.UTC));

        Appointment existing = Appointment.builder().build();
        existing.setId(id);
        existing.setTherapistId(2L);
        existing.setStartTime(LocalDateTime.now().plusDays(3));
        existing.setEndTime(LocalDateTime.now().plusDays(3).plusHours(1));
        existing.setStatus(AppointmentStatus.BOOKED);

        when(appointmentRepository.findById(id)).thenReturn(Optional.of(existing));
        when(calendarBlockService.isTherapistBlocked(anyLong(), any(), any())).thenReturn(false);
        when(appointmentRepository.existsOverlappingAppointment(eq(2L), any(), any(), eq(id))).thenReturn(false);
        when(appointmentRepository.save(existing))
                .thenThrow(new ObjectOptimisticLockingFailureException(Appointment.class, id));

        assertThatThrownBy(() -> appointmentsService.rescheduleAppointment(id, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("modified by another process");
    }

    @Test
    void cancelAppointment_happyPath_enqueuesCancelledEvent() {
        Long id = 7L;
        Appointment appointment = Appointment.builder().build();
        appointment.setId(id);
        appointment.setStatus(AppointmentStatus.BOOKED);
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));

        appointmentsService.cancelAppointment(id);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(appointmentRepository).save(appointment);
        verify(outboxService).enqueueEvent(TenantContext.getTenantId(), appointment, "appointment.cancelled");
    }
}

