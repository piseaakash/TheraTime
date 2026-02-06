package com.theratime.appointment.mapper;

import com.theratime.appointment.entity.Appointment;
import com.theratime.appointment.entity.AppointmentStatus;
import com.theratime.appointments.model.AppointmentResponse;
import com.theratime.appointments.model.BookAppointmentRequest;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = false) )
public interface AppointmentMapper {

    @Mapping(target = "status", constant = "BOOKED")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    Appointment toEntity(BookAppointmentRequest request);

    @Mapping(target = "status", source = "status")
    AppointmentResponse toResponse(Appointment entity);

    default LocalDateTime map(OffsetDateTime value) {
        return value == null ? null : value.toLocalDateTime();
    }

    default OffsetDateTime map(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    default AppointmentResponse.StatusEnum map(AppointmentStatus status) {
        if(status == null)
            return null;
        return switch (status) {
            case BOOKED -> AppointmentResponse.StatusEnum.BOOKED;
            case CANCELLED -> AppointmentResponse.StatusEnum.CANCELLED;
            case COMPLETED -> AppointmentResponse.StatusEnum.COMPLETED;
        };
    }
}
