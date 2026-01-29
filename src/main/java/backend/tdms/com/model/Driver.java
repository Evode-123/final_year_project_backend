package backend.tdms.com.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "drivers")
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String names;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "license_no", unique = true, nullable = false)
    private String licenseNo;

    @Column(name = "id_number", unique = true)
    private String idNumber;

    @Column(name = "license_expiry_date")
    private LocalDate licenseExpiryDate;

    @Column(length = 500)
    private String address;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_vehicle_id", unique = true)
    private Vehicle assignedVehicle;

    // ✅ NEW: Permanent link to User account (survives phone/email changes)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, ON_LEAVE, BACKUP

    @Column(name = "hired_date")
    private LocalDate hiredDate;

    @Column(name = "is_backup")
    private Boolean isBackup = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}