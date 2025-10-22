package com.theratime.calendar.controller;

import com.theratime.appointments.api.CalendarApiDelegate;
import com.theratime.appointments.model.BlockCalendarRequest;
import com.theratime.appointments.model.CalendarBlockResponse;
import com.theratime.calendar.service.CalendarBlockService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CalendarApiDelegateImpl implements CalendarApiDelegate {

    private final CalendarBlockService service;

    @Timed(value = "calendar.block.timer", description = "Time taken to block the calendar")
    @Override
    public ResponseEntity<CalendarBlockResponse> blockCalendar(BlockCalendarRequest blockCalendarRequest) {
        return ResponseEntity.ok(
                service.blockCalendar(blockCalendarRequest)
        );
    }

}
