package com.theratime.calendar.mapper;

import com.theratime.appointment.entity.CalendarBlock;
import com.theratime.appointments.model.BlockCalendarRequest;
import com.theratime.appointments.model.CalendarBlockResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = false))
public interface CalendarBlockMapper {

    CalendarBlock toEntity(BlockCalendarRequest request);

    CalendarBlockResponse toResponse(CalendarBlock block);

    default LocalDateTime map(OffsetDateTime value) { return value == null ? null : value.toLocalDateTime(); }

    default OffsetDateTime map(LocalDateTime value) { return value == null ? null : value.atOffset(ZoneOffset.UTC); }
}
