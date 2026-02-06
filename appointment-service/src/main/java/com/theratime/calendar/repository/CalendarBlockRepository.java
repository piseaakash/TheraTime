package com.theratime.calendar.repository;

import com.theratime.appointment.entity.CalendarBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface CalendarBlockRepository extends JpaRepository<CalendarBlock, Long> {
    List<CalendarBlock> findByTherapistId(Long therapistId);

    @Query("""
    SELECT CASE WHEN COUNT(cb) > 0 THEN TRUE ELSE FALSE END
    FROM CalendarBlock cb
    WHERE cb.therapistId = :therapistId
    AND cb.startTime < :endTime
    AND cb.endTime > :startTime
    """)
    boolean isTherapistBlocked(Long therapistId, LocalDateTime startTime, LocalDateTime endTime);
}
