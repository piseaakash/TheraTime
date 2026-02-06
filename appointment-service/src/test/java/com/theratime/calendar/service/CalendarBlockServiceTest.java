package com.theratime.calendar.service;

import com.theratime.appointment.entity.Appointment;
import com.theratime.appointment.entity.CalendarBlock;
import com.theratime.appointment.mapper.AppointmentMapper;
import com.theratime.appointment.repository.AppointmentRepository;
import com.theratime.appointment.service.UserService;
import com.theratime.appointments.model.AppointmentResponse;
import com.theratime.appointments.model.BlockCalendarRequest;
import com.theratime.appointments.model.CalendarBlockResponse;
import com.theratime.calendar.mapper.CalendarBlockMapper;
import com.theratime.calendar.repository.CalendarBlockRepository;
import com.theratime.exception.BusinessException;
import com.theratime.security.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarBlockServiceTest {

    @Mock
    private CalendarBlockRepository calendarBlockRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private CalendarBlockMapper mapper;
    @Mock
    private AppointmentMapper appointmentMapper;
    @Mock
    private UserService userService;

    @InjectMocks
    private CalendarBlockService service;

    @BeforeEach
    void setUp() {
        var context = SecurityContextHolder.createEmptyContext();
        var auth = new TestingAuthenticationToken("user", null);
        auth.setAuthenticated(true);
        auth.setDetails(10L);
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        TenantContext.setTenantId(1L);
    }

    @Test
    void isTherapistBlocked_returnsTrueWhenBlocked() {
        when(calendarBlockRepository.isTherapistBlocked(eq(2L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(true);
        assertThat(service.isTherapistBlocked(2L, LocalDateTime.now(), LocalDateTime.now().plusHours(1))).isTrue();
    }

    @Test
    void isTherapistBlocked_returnsFalseWhenNotBlocked() {
        when(calendarBlockRepository.isTherapistBlocked(eq(2L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(false);
        assertThat(service.isTherapistBlocked(2L, LocalDateTime.now(), LocalDateTime.now().plusHours(1))).isFalse();
    }

    @Test
    void blockCalendar_happyPath_savesAndReturns() {
        BlockCalendarRequest request = new BlockCalendarRequest()
                .therapistId(2L)
                .startTime(LocalDateTime.now().plusDays(1).atOffset(ZoneOffset.UTC))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2).atOffset(ZoneOffset.UTC));
        CalendarBlock block = new CalendarBlock();
        block.setId(1L);
        block.setTherapistId(2L);
        CalendarBlockResponse response = new CalendarBlockResponse().id(1L);

        when(userService.getTenantId(any())).thenReturn(1L);
        when(userService.getUserRole(2L)).thenReturn("THERAPIST");
        when(userService.getTenantId(2L)).thenReturn(1L);
        when(calendarBlockRepository.isTherapistBlocked(eq(2L), any(), any())).thenReturn(false);
        when(appointmentRepository.cancelAppointmentsInRange(eq(2L), any(), any())).thenReturn(0);
        when(mapper.toEntity(request)).thenReturn(block);
        when(calendarBlockRepository.save(block)).thenReturn(block);
        when(mapper.toResponse(block)).thenReturn(response);

        CalendarBlockResponse result = service.blockCalendar(request);

        assertThat(result.getId()).isEqualTo(1L);
        verify(calendarBlockRepository).save(block);
    }

    @Test
    void blockCalendar_overlap_throwsBusinessException() {
        BlockCalendarRequest request = new BlockCalendarRequest()
                .therapistId(2L)
                .startTime(LocalDateTime.now().plusDays(1).atOffset(ZoneOffset.UTC))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2).atOffset(ZoneOffset.UTC));

        when(userService.getTenantId(any())).thenReturn(1L);
        when(userService.getUserRole(2L)).thenReturn("THERAPIST");
        when(calendarBlockRepository.isTherapistBlocked(eq(2L), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.blockCalendar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("overlaps with an existing block");
        verify(calendarBlockRepository, never()).save(any());
    }

    @Test
    void blockCalendar_notTherapist_throwsBusinessException() {
        BlockCalendarRequest request = new BlockCalendarRequest()
                .therapistId(2L)
                .startTime(LocalDateTime.now().plusDays(1).atOffset(ZoneOffset.UTC))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2).atOffset(ZoneOffset.UTC));

        when(userService.getTenantId(any())).thenReturn(1L);
        when(userService.getUserRole(2L)).thenReturn("USER");

        assertThatThrownBy(() -> service.blockCalendar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not belong to a therapist");
        verify(calendarBlockRepository, never()).save(any());
    }

    @Test
    void blockCalendar_differentTenant_throwsBusinessException() {
        BlockCalendarRequest request = new BlockCalendarRequest()
                .therapistId(2L)
                .startTime(LocalDateTime.now().plusDays(1).atOffset(ZoneOffset.UTC))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2).atOffset(ZoneOffset.UTC));

        when(userService.getTenantId(any())).thenReturn(1L);
        when(userService.getUserRole(2L)).thenReturn("THERAPIST");
        when(userService.getTenantId(2L)).thenReturn(2L);

        assertThatThrownBy(() -> service.blockCalendar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("belong to your practice");
        verify(calendarBlockRepository, never()).save(any());
    }

    @Test
    void viewCalendar_usesNullStartEndAsMinMax() {
        Long therapistId = 2L;
        when(userService.getTenantId(any())).thenReturn(1L);
        when(userService.getUserRole(2L)).thenReturn("THERAPIST");
        when(userService.getTenantId(2L)).thenReturn(1L);
        when(appointmentRepository.findByTherapistId(2L)).thenReturn(List.of());
        when(calendarBlockRepository.findByTherapistId(2L)).thenReturn(List.of());

        var result = service.viewCalendar(therapistId, null, null);

        assertThat(result.getAppointments()).isEmpty();
        assertThat(result.getBlocks()).isEmpty();
        verify(appointmentRepository).findByTherapistId(2L);
        verify(calendarBlockRepository).findByTherapistId(2L);
    }

    @Test
    void viewCalendar_filtersByRange() {
        Long therapistId = 2L;
        OffsetDateTime start = OffsetDateTime.of(2025, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = OffsetDateTime.of(2025, 6, 30, 23, 59, 59, 0, ZoneOffset.UTC);
        Appointment apt = Appointment.builder().build();
        apt.setId(1L);
        apt.setStartTime(LocalDateTime.of(2025, 6, 15, 10, 0));
        apt.setEndTime(LocalDateTime.of(2025, 6, 15, 11, 0));
        CalendarBlock block = new CalendarBlock();
        block.setId(1L);
        block.setStartTime(LocalDateTime.of(2025, 6, 10, 9, 0));
        block.setEndTime(LocalDateTime.of(2025, 6, 10, 10, 0));

        when(userService.getTenantId(any())).thenReturn(1L);
        when(userService.getUserRole(2L)).thenReturn("THERAPIST");
        when(userService.getTenantId(2L)).thenReturn(1L);
        when(appointmentRepository.findByTherapistId(2L)).thenReturn(List.of(apt));
        when(calendarBlockRepository.findByTherapistId(2L)).thenReturn(List.of(block));
        when(appointmentMapper.toResponse(apt)).thenReturn(new AppointmentResponse().id(1L));
        when(mapper.toResponse(block)).thenReturn(new CalendarBlockResponse().id(1L));

        var result = service.viewCalendar(therapistId, start, end);

        assertThat(result.getAppointments()).hasSize(1);
        assertThat(result.getBlocks()).hasSize(1);
    }
}
