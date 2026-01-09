package backend.tdms.com.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "vehicles")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plate_no", unique = true, nullable = false)
    private String plateNo;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "vehicle_type", nullable = false)
    private String vehicleType;

    @Column(nullable = false)
    private String status = "AVAILABLE";

    @Column(name = "is_active")
    private Boolean isActive = true;
}