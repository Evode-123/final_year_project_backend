package backend.tdms.com.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "routes")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String origin;

    @Column(nullable = false)
    private String destination;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "turnaround_buffer_minutes")
    private Integer turnaroundBufferMinutes = 30;

    @Column(nullable = false)
    private Boolean isActive = true;
}