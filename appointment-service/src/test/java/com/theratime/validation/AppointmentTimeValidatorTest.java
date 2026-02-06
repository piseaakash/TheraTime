package com.theratime.validation;

import com.theratime.appointments.model.BookAppointmentRequest;
import com.theratime.appointments.model.RescheduleAppointmentRequest;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AppointmentTimeValidatorTest {

    @Mock
    private ConstraintValidatorContext context;

    private AppointmentTimeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AppointmentTimeValidator();
    }

    @Test
    void isValid_whenBookAppointmentRequest_validTimes_returnsTrue() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(1);
        BookAppointmentRequest req = new BookAppointmentRequest()
                .startTime(start.atOffset(ZoneOffset.UTC))
                .endTime(end.atOffset(ZoneOffset.UTC));

        assertThat(validator.isValid(req, context)).isTrue();
    }

    @Test
    void isValid_whenBookAppointmentRequest_endBeforeStart_returnsFalse() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.minusHours(1);
        BookAppointmentRequest req = new BookAppointmentRequest()
                .startTime(start.atOffset(ZoneOffset.UTC))
                .endTime(end.atOffset(ZoneOffset.UTC));

        assertThat(validator.isValid(req, context)).isFalse();
    }

    @Test
    void isValid_whenBookAppointmentRequest_startInPast_returnsFalse() {
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        BookAppointmentRequest req = new BookAppointmentRequest()
                .startTime(start.atOffset(ZoneOffset.UTC))
                .endTime(end.atOffset(ZoneOffset.UTC));

        assertThat(validator.isValid(req, context)).isFalse();
    }

    @Test
    void isValid_whenBookAppointmentRequest_endMoreThan3HoursAfterStart_returnsFalse() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(4);
        BookAppointmentRequest req = new BookAppointmentRequest()
                .startTime(start.atOffset(ZoneOffset.UTC))
                .endTime(end.atOffset(ZoneOffset.UTC));

        assertThat(validator.isValid(req, context)).isFalse();
    }

    @Test
    void isValid_whenRescheduleAppointmentRequest_validTimes_returnsTrue() {
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        LocalDateTime end = start.plusHours(2);
        RescheduleAppointmentRequest req = new RescheduleAppointmentRequest()
                .startTime(start.atOffset(ZoneOffset.UTC))
                .endTime(end.atOffset(ZoneOffset.UTC));

        assertThat(validator.isValid(req, context)).isTrue();
    }

    @Test
    void isValid_whenRescheduleAppointmentRequest_invalidTimes_returnsFalse() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = start.plusHours(1);
        RescheduleAppointmentRequest req = new RescheduleAppointmentRequest()
                .startTime(start.atOffset(ZoneOffset.UTC))
                .endTime(end.atOffset(ZoneOffset.UTC));

        assertThat(validator.isValid(req, context)).isFalse();
    }

    @Test
    void isValid_whenOtherType_returnsTrue() {
        assertThat(validator.isValid("other", context)).isTrue();
        assertThat(validator.isValid(null, context)).isTrue();
    }

    @Test
    void isValid_whenEndExactly3HoursAfterStart_returnsFalse() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(3);
        BookAppointmentRequest req = new BookAppointmentRequest()
            .startTime(start.atOffset(ZoneOffset.UTC))
            .endTime(end.atOffset(ZoneOffset.UTC));

        assertThat(validator.isValid(req, context)).isFalse();
    }
}
