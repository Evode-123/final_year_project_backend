package backend.tdms.com.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "time_slots")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @Column(name = "is_active")
    private Boolean isActive = true;
}