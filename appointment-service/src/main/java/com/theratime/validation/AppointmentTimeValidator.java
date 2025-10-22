package com.theratime.validation;

import com.theratime.appointments.model.BookAppointmentRequest;
import com.theratime.appointments.model.RescheduleAppointmentRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;

public class AppointmentTimeValidator implements ConstraintValidator<ValidAppointmentTime, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if(value instanceof BookAppointmentRequest bar) {
            return validateStartEnd(bar.getStartTime().toLocalDateTime(), bar.getEndTime().toLocalDateTime());
        } else if(value instanceof RescheduleAppointmentRequest rar) {
            return validateStartEnd(rar.getStartTime().toLocalDateTime(), rar.getEndTime().toLocalDateTime());
        }
        return true;
    }

    private boolean validateStartEnd(LocalDateTime startTime, LocalDateTime endTime) {
        return endTime.isAfter(startTime);
    }
}
