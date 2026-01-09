package backend.tdms.com.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "vehicle_inspections")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class VehicleInspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Vehicle vehicle;

    @Column(name = "inspection_date", nullable = false)
    private LocalDate inspectionDate;

    @Column(name = "next_inspection_due", nullable = false)
    private LocalDate nextInspectionDue;

    @Column(name = "inspection_status", nullable = false)
    private String inspectionStatus; // PASSED, FAILED, PENDING

    @Column(name = "certificate_number")
    private String certificateNumber;

    @Column(name = "notes", length = 1000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_user_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User recordedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        
        // Auto-calculate next inspection due (6 months later)
        if (nextInspectionDue == null && inspectionDate != null) {
            nextInspectionDue = inspectionDate.plusMonths(6);
        }
    }
}