package com.theratime.appointment.repository;

import com.theratime.appointment.entity.OutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEntity, Long> {

    List<OutboxEntity> findByStatusOrderByCreatedAtAsc(String status);
}
