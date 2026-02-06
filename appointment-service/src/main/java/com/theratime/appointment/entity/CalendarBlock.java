package com.theratime.appointment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table( name = "calendar_blocks" )
public class CalendarBlock {

    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY)
    private Long id;

    @Column( name = "therapist_id", nullable = false )
    private Long therapistId;

    @Column( nullable = false )
    private LocalDateTime startTime;

    @Column( nullable = false )
    private LocalDateTime endTime;

    private String reason;
}
