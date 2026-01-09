package backend.tdms.com.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "routes_vehicles")
public class RouteVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // This table defines which vehicles are assigned to which routes
    // System will only use these vehicles for auto-generating trips
}