package com.theratime.appointment.mapper;

import com.theratime.appointment.entity.Appointment;
import com.theratime.appointment.entity.AppointmentStatus;
import com.theratime.appointments.model.AppointmentResponse;
import com.theratime.appointments.model.BookAppointmentRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentMapperImplTest {

    private final AppointmentMapperImpl mapper = new AppointmentMapperImpl();

    @Test
    void toEntity_withValidRequest_mapsFields() {
        LocalDateTime start = LocalDateTime.of(2025, 6, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 6, 1, 11, 0);
        BookAppointmentRequest request = new BookAppointmentRequest()
                .userId(1L)
                .therapistId(2L)
                .startTime(start.atOffset(ZoneOffset.UTC))
                .endTime(end.atOffset(ZoneOffset.UTC));

        Appointment entity = mapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getUserId()).isEqualTo(1L);
        assertThat(entity.getTherapistId()).isEqualTo(2L);
        assertThat(entity.getStartTime()).isEqualTo(start);
        assertThat(entity.getEndTime()).isEqualTo(end);
        assertThat(entity.getStatus()).isEqualTo(AppointmentStatus.BOOKED);
    }

    @Test
    void toEntity_withNull_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toResponse_withEntity_mapsFields() {
        Appointment entity = Appointment.builder()
                .id(5L)
                .userId(1L)
                .therapistId(2L)
                .startTime(LocalDateTime.of(2025, 6, 1, 10, 0))
                .endTime(LocalDateTime.of(2025, 6, 1, 11, 0))
                .status(AppointmentStatus.BOOKED)
                .build();

        AppointmentResponse response = mapper.toResponse(entity);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getTherapistId()).isEqualTo(2L);
        assertThat(response.getStatus()).isEqualTo(AppointmentResponse.StatusEnum.BOOKED);
    }

    @Test
    void toResponse_withCancelledStatus_mapsToCancelled() {
        Appointment entity = Appointment.builder()
                .id(1L)
                .userId(1L)
                .therapistId(2L)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(1))
                .status(AppointmentStatus.CANCELLED)
                .build();

        AppointmentResponse response = mapper.toResponse(entity);

        assertThat(response.getStatus()).isEqualTo(AppointmentResponse.StatusEnum.CANCELLED);
    }

    @Test
    void toResponse_withCompletedStatus_mapsToCompleted() {
        Appointment entity = Appointment.builder()
                .id(1L)
                .userId(1L)
                .therapistId(2L)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(1))
                .status(AppointmentStatus.COMPLETED)
                .build();

        AppointmentResponse response = mapper.toResponse(entity);

        assertThat(response.getStatus()).isEqualTo(AppointmentResponse.StatusEnum.COMPLETED);
    }

    @Test
    void toResponse_withNull_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    void mapOffsetDateTime_null_returnsNull() {
        assertThat(mapper.map((OffsetDateTime) null)).isNull();
    }

    @Test
    void mapLocalDateTime_null_returnsNull() {
        assertThat(mapper.map((LocalDateTime) null)).isNull();
    }

    @Test
    void mapStatus_null_returnsNull() {
        assertThat(mapper.map((AppointmentStatus) null)).isNull();
    }
}
