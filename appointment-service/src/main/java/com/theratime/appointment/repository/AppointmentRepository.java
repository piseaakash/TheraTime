package com.theratime.appointment.repository;

import com.theratime.appointment.entity.Appointment;
import org.springframework.cglib.core.Local;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByTherapistId(Long therapistId);
    List<Appointment> findByUserId(Long userId);

    Optional<Appointment> findById(Long id);

    @Query("""
    SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END
    FROM Appointment a
    WHERE a.therapistId = :therapistId
    AND a.startTime < :endTime
    AND a.endTime > :startTime
    """)
    boolean existsOverlappingAppointment(Long therapistId, LocalDateTime startTime, LocalDateTime endTime);

    @Modifying
    @Query("UPDATE Appointment a SET a.status = 'CANCELLED' WHERE a.id = :id")
    int cancelAppointment(Long id);

    @Modifying
    @Query("""
            UPDATE Appointment a
            SET a.status = 'CANCELLED'
            WHERE a.therapistId = :therapistId
            AND a.startTime < :endTime
            AND a.endTime > :startTime
            """)
    int cancelAppointmentsInRange(Long therapistId, LocalDateTime startTime, LocalDateTime endTime);
}
