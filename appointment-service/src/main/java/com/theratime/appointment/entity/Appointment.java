package com.theratime.appointment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table( name = "appointments")
@Getter
@Setter
@NoArgsConstructor( access = AccessLevel.PROTECTED )
@AllArgsConstructor( access = AccessLevel.PRIVATE )
@Builder( toBuilder = true )
public class Appointment {

    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY )
    private Long id;

    @Column( nullable = false )
    private Long userId;

    @Column( nullable = false )
    private Long therapistId;

    @Column( nullable = false )
    private LocalDateTime startTime;

    @Column( nullable = false )
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    @Version
    private Long version;
}
