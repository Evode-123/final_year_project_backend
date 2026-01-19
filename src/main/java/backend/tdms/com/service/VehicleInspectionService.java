package backend.tdms.com.service;

import backend.tdms.com.dto.RecordInspectionDTO;
import backend.tdms.com.dto.VehicleInspectionDTO;
import backend.tdms.com.model.User;
import backend.tdms.com.model.Vehicle;
import backend.tdms.com.model.VehicleInspection;
import backend.tdms.com.repository.UserRepository;
import backend.tdms.com.repository.VehicleInspectionRepository;
import backend.tdms.com.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleInspectionService {

    private final VehicleInspectionRepository inspectionRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    /**
     * Record a new government inspection
     */
    @Transactional
    public VehicleInspectionDTO recordInspection(RecordInspectionDTO dto) {
        log.info("Recording inspection for vehicle ID: {}", dto.getVehicleId());

        Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId())
            .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        // Get current user
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User recordedBy = userRepository.findByEmail(currentUserEmail).orElse(null);

        // Create inspection record
        VehicleInspection inspection = new VehicleInspection();
        inspection.setVehicle(vehicle);
        inspection.setInspectionDate(dto.getInspectionDate());
        inspection.setInspectionStatus(dto.getInspectionStatus());
        inspection.setCertificateNumber(dto.getCertificateNumber());
        inspection.setNotes(dto.getNotes());
        inspection.setRecordedBy(recordedBy);

        // Next inspection is automatically calculated in @PrePersist (+ 6 months)

        VehicleInspection saved = inspectionRepository.save(inspection);

        log.info("Inspection recorded for vehicle {}: Status = {}, Next due = {}", 
            vehicle.getPlateNo(), saved.getInspectionStatus(), saved.getNextInspectionDue());

        return convertToDTO(saved);
    }

    /**
     * Get latest inspection for a vehicle
     */
    public VehicleInspectionDTO getLatestInspection(Long vehicleId) {
        // Check if vehicle exists
        vehicleRepository.findById(vehicleId)
            .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        VehicleInspection inspection = inspectionRepository.findLatestByVehicleId(vehicleId)
            .orElseThrow(() -> new RuntimeException("No inspection records found for this vehicle"));

        return convertToDTO(inspection);
    }

    /**
     * Get all inspections for a vehicle
     */
    public List<VehicleInspectionDTO> getVehicleInspectionHistory(Long vehicleId) {
        List<VehicleInspection> inspections = inspectionRepository.findByVehicleId(vehicleId);
        return inspections.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get vehicles due for inspection soon (within 30 days)
     */
    public List<VehicleInspectionDTO> getVehiclesDueSoon() {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysLater = today.plusDays(30);

        List<VehicleInspection> dueSoon = inspectionRepository.findDueSoon(today, thirtyDaysLater);

        return dueSoon.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get overdue vehicles
     */
    public List<VehicleInspectionDTO> getOverdueVehicles() {
        LocalDate today = LocalDate.now();
        List<VehicleInspection> overdue = inspectionRepository.findOverdue(today);

        return overdue.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get dashboard summary
     */
    public java.util.Map<String, Object> getDashboardSummary() {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysLater = today.plusDays(30);

        List<VehicleInspection> dueSoon = inspectionRepository.findDueSoon(today, thirtyDaysLater);
        List<VehicleInspection> overdue = inspectionRepository.findOverdue(today);
        
        long totalVehicles = vehicleRepository.count();
        long inspected = totalVehicles - overdue.size();

        java.util.Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("totalVehicles", totalVehicles);
        summary.put("inspectedVehicles", inspected);
        summary.put("dueSoonCount", dueSoon.size());
        summary.put("overdueCount", overdue.size());
        summary.put("dueSoonVehicles", dueSoon.stream().map(this::convertToDTO).collect(Collectors.toList()));
        summary.put("overdueVehicles", overdue.stream().map(this::convertToDTO).collect(Collectors.toList()));

        return summary;
    }

    /**
     * Convert entity to DTO (with null safety)
     */
    private VehicleInspectionDTO convertToDTO(VehicleInspection inspection) {
        VehicleInspectionDTO dto = new VehicleInspectionDTO();
        dto.setId(inspection.getId());
        
        // ✅ Add null check for vehicle
        if (inspection.getVehicle() != null) {
            dto.setVehicleId(inspection.getVehicle().getId());
            dto.setVehiclePlateNo(inspection.getVehicle().getPlateNo());
        }
        
        dto.setInspectionDate(inspection.getInspectionDate());
        dto.setNextInspectionDue(inspection.getNextInspectionDue());
        dto.setInspectionStatus(inspection.getInspectionStatus());
        dto.setCertificateNumber(inspection.getCertificateNumber());
        dto.setNotes(inspection.getNotes());

        if (inspection.getRecordedBy() != null) {
            dto.setRecordedByEmail(inspection.getRecordedBy().getEmail());
        }

        // ✅ Calculate days until due with null check
        if (inspection.getNextInspectionDue() != null) {
            LocalDate today = LocalDate.now();
            long daysUntilDue = ChronoUnit.DAYS.between(today, inspection.getNextInspectionDue());
            dto.setDaysUntilDue((int) daysUntilDue);

            // Determine urgency
            if (inspection.getNextInspectionDue().isBefore(today)) {
                dto.setUrgency("OVERDUE");
            } else if (daysUntilDue <= 30) {
                dto.setUrgency("DUE_SOON");
            } else {
                dto.setUrgency("OK");
            }
        } else {
            dto.setUrgency("NEVER_INSPECTED");
            dto.setDaysUntilDue(0);
        }

        return dto;
    }
}