package backend.tdms.com.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "route_time_slots")
public class RouteTimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_slot_id", nullable = false)
    private TimeSlot timeSlot;

    @Column(name = "is_active")
    private Boolean isActive = true;
}