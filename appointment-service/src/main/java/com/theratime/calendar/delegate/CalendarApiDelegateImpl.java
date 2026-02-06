package com.theratime.calendar.delegate;

import com.theratime.appointments.api.CalendarApiDelegate;
import com.theratime.appointments.model.BlockCalendarRequest;
import com.theratime.appointments.model.CalendarBlockResponse;
import com.theratime.appointments.model.ViewCalendar200Response;
import com.theratime.calendar.service.CalendarBlockService;
import com.theratime.exception.BusinessException;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class CalendarApiDelegateImpl implements CalendarApiDelegate {

    private final CalendarBlockService service;

    @Timed(value = "calendar.block.timer", description = "Time taken to block the calendar")
    @PreAuthorize("hasAnyRole('THERAPIST', 'ADMIN')")
    @Override
    public ResponseEntity<CalendarBlockResponse> blockCalendar(BlockCalendarRequest blockCalendarRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.blockCalendar(blockCalendarRequest));
    }

    @Override
    public ResponseEntity<ViewCalendar200Response> viewCalendar(Integer therapistId, OffsetDateTime startDate, OffsetDateTime endDate) {
        if (therapistId == null) {
            throw new BusinessException("therapistId is required");
        }
        ViewCalendar200Response body = service.viewCalendar(therapistId.longValue(), startDate, endDate);
        return ResponseEntity.ok(body);
    }

}
