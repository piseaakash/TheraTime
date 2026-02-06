package com.theratime.calendar.delegate;

import com.theratime.appointments.model.BlockCalendarRequest;
import com.theratime.appointments.model.CalendarBlockResponse;
import com.theratime.appointments.model.ViewCalendar200Response;
import com.theratime.calendar.service.CalendarBlockService;
import com.theratime.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarApiDelegateImplTest {

    @Mock
    private CalendarBlockService service;

    @InjectMocks
    private CalendarApiDelegateImpl delegate;

    @Test
    void blockCalendar_returns201AndBody() {
        BlockCalendarRequest request = new BlockCalendarRequest()
                .therapistId(2L)
                .startTime(LocalDateTime.now().plusDays(1).atOffset(ZoneOffset.UTC))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2).atOffset(ZoneOffset.UTC));
        CalendarBlockResponse response = new CalendarBlockResponse().id(1L);
        when(service.blockCalendar(request)).thenReturn(response);

        ResponseEntity<CalendarBlockResponse> result = delegate.blockCalendar(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(response);
        verify(service).blockCalendar(request);
    }

    @Test
    void viewCalendar_returns200AndBody() {
        Integer therapistId = Integer.valueOf(2);
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = OffsetDateTime.now().plusDays(7);
        ViewCalendar200Response response = new ViewCalendar200Response();
        when(service.viewCalendar(therapistId.longValue(), start, end)).thenReturn(response);

        ResponseEntity<ViewCalendar200Response> result = delegate.viewCalendar(therapistId, start, end);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
        verify(service).viewCalendar(2L, start, end);
    }

    @Test
    void viewCalendar_therapistIdNull_throwsBusinessException() {
        assertThatThrownBy(() -> delegate.viewCalendar(null, OffsetDateTime.now(), OffsetDateTime.now().plusDays(1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("therapistId is required");
        verify(service, org.mockito.Mockito.never()).viewCalendar(anyLong(), any(), any());
    }
}
