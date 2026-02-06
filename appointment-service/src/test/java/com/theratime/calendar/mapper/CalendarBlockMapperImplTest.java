package com.theratime.calendar.mapper;

import com.theratime.appointment.entity.CalendarBlock;
import com.theratime.appointments.model.BlockCalendarRequest;
import com.theratime.appointments.model.CalendarBlockResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarBlockMapperImplTest {

    private final CalendarBlockMapperImpl mapper = new CalendarBlockMapperImpl();

    @Test
    void toEntity_withValidRequest_mapsFields() {
        LocalDateTime start = LocalDateTime.of(2025, 6, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 6, 1, 10, 0);
        BlockCalendarRequest request = new BlockCalendarRequest()
                .therapistId(2L)
                .startTime(start.atOffset(ZoneOffset.UTC))
                .endTime(end.atOffset(ZoneOffset.UTC))
                .reason("Meeting");

        CalendarBlock entity = mapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getTherapistId()).isEqualTo(2L);
        assertThat(entity.getStartTime()).isEqualTo(start);
        assertThat(entity.getEndTime()).isEqualTo(end);
        assertThat(entity.getReason()).isEqualTo("Meeting");
    }

    @Test
    void toEntity_withNull_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toResponse_withBlock_mapsFields() {
        CalendarBlock block = new CalendarBlock();
        block.setId(1L);
        block.setTherapistId(2L);
        block.setStartTime(LocalDateTime.of(2025, 6, 1, 9, 0));
        block.setEndTime(LocalDateTime.of(2025, 6, 1, 10, 0));
        block.setReason("Break");

        CalendarBlockResponse response = mapper.toResponse(block);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTherapistId()).isEqualTo(2L);
        assertThat(response.getReason()).isEqualTo("Break");
    }

    @Test
    void toResponse_withNull_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
