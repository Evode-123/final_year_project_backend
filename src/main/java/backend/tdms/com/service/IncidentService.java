package backend.tdms.com.service;

import backend.tdms.com.dto.*;
import backend.tdms.com.model.*;
import backend.tdms.com.model.Incident.*;
import backend.tdms.com.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final DailyTripRepository dailyTripRepository;
    private final DriverRepository driverRepository;

    /**
     * Get driver's scheduled trips (today and upcoming)
     * Uses driver's assigned vehicle to find trips
     * 
     * ✅ FIXED: Now properly links User → Driver
     */
    public List<DailyTripResponseDTO> getDriverScheduledTrips() {
        String currentUserEmail = SecurityContextHolder.getContext()
            .getAuthentication().getName();

        User user = userRepository.findByEmail(currentUserEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // ✅ Use the direct user → driver link instead of phone matching
        Driver driver = driverRepository.findByUser(user)
            .orElseThrow(() -> new RuntimeException("Driver record not found for user: " + user.getEmail()));

        if (driver.getAssignedVehicle() == null) {
            throw new RuntimeException("No vehicle assigned to driver: " + driver.getNames());
        }

        // Get today and future trips for driver's vehicle
        LocalDate today = LocalDate.now();
        List<DailyTrip> trips = dailyTripRepository.findAll().stream()
            .filter(trip -> trip.getVehicle() != null && 
                          trip.getVehicle().getId().equals(driver.getAssignedVehicle().getId()) &&
                          !trip.getTripDate().isBefore(today))
            .sorted((t1, t2) -> {
                int dateCompare = t1.getTripDate().compareTo(t2.getTripDate());
                if (dateCompare != 0) return dateCompare;
                return t1.getTimeSlot().getDepartureTime().compareTo(t2.getTimeSlot().getDepartureTime());
            })
            .collect(Collectors.toList());

        log.info("Found {} scheduled trips for driver: {} (Vehicle: {})", 
            trips.size(), driver.getNames(), driver.getAssignedVehicle().getPlateNo());

        return trips.stream()
            .map(this::convertTripToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Report a new incident (typically by driver)
     */
    @Transactional
    public IncidentResponseDTO reportIncident(IncidentReportDTO dto) {
        String currentUserEmail = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        
        User reportedBy = userRepository.findByEmail(currentUserEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Incident incident = new Incident();
        
        // Set daily trip if provided
        if (dto.getDailyTripId() != null) {
            DailyTrip dailyTrip = dailyTripRepository.findById(dto.getDailyTripId())
                .orElseThrow(() -> new RuntimeException("Daily trip not found"));
            incident.setDailyTrip(dailyTrip);
            incident.setVehicle(dailyTrip.getVehicle());
            
            // Try to find assigned driver
            driverRepository.findByAssignedVehicle(dailyTrip.getVehicle())
                .ifPresent(incident::setDriver);
        }

        incident.setReportedBy(reportedBy);
        incident.setIncidentType(dto.getIncidentType());
        incident.setSeverity(dto.getSeverity());
        incident.setDescription(dto.getDescription());
        incident.setLocation(dto.getLocation());
        incident.setIncidentTime(dto.getIncidentTime() != null ? 
            dto.getIncidentTime() : LocalDateTime.now());
        incident.setDelayMinutes(dto.getDelayMinutes());
        incident.setRequiresMaintenance(dto.getRequiresMaintenance() != null ? 
            dto.getRequiresMaintenance() : false);
        incident.setAffectsSchedule(dto.getAffectsSchedule() != null ? 
            dto.getAffectsSchedule() : false);
        incident.setPassengersAffected(dto.getPassengersAffected() != null ? 
            dto.getPassengersAffected() : false);
        incident.setAffectedPassengerCount(dto.getAffectedPassengerCount());
        incident.setStatus(IncidentStatus.REPORTED);
        incident.setHasUnviewedStatusUpdate(false); // Reporter knows about it
        incident.setLastStatusChangeAt(LocalDateTime.now());

        Incident savedIncident = incidentRepository.save(incident);

        log.info("Incident reported: Type={}, Severity={}, Reporter={}", 
            incident.getIncidentType(), 
            incident.getSeverity(), 
            reportedBy.getEmail());

        return convertToResponseDTO(savedIncident);
    }

    /**
     * Update incident status and resolution
     */
    @Transactional
    public IncidentResponseDTO updateIncident(Long incidentId, IncidentUpdateDTO dto) {
        String currentUserEmail = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        
        User user = userRepository.findByEmail(currentUserEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Incident incident = incidentRepository.findById(incidentId)
            .orElseThrow(() -> new RuntimeException("Incident not found"));

        boolean statusChanged = false;

        // Update status
        if (dto.getStatus() != null && !dto.getStatus().equals(incident.getStatus())) {
            IncidentStatus oldStatus = incident.getStatus();
            incident.setStatus(dto.getStatus());
            statusChanged = true;
            
            // Mark as unviewed for reporter (so they see the badge)
            incident.setHasUnviewedStatusUpdate(true);
            incident.setLastStatusChangeAt(LocalDateTime.now());
            
            // If marking as resolved
            if (dto.getStatus() == IncidentStatus.RESOLVED && 
                oldStatus != IncidentStatus.RESOLVED) {
                incident.setResolvedBy(user);
                incident.setResolvedAt(LocalDateTime.now());
            }
            
            log.info("Incident {} status changed from {} to {} by {}", 
                incidentId, oldStatus, dto.getStatus(), user.getEmail());
        }

        // Update resolution notes
        if (dto.getResolutionNotes() != null && !dto.getResolutionNotes().trim().isEmpty()) {
            incident.setResolutionNotes(dto.getResolutionNotes());
            if (!statusChanged) {
                // Even if status didn't change, notes were added
                incident.setHasUnviewedStatusUpdate(true);
                incident.setLastStatusChangeAt(LocalDateTime.now());
            }
        }

        Incident updated = incidentRepository.save(incident);

        log.info("Incident {} updated by {}", incidentId, user.getEmail());

        return convertToResponseDTO(updated);
    }

    /**
     * Mark incident as viewed by reporter (clears the badge)
     */
    @Transactional
    public void markIncidentAsViewed(Long incidentId) {
        String currentUserEmail = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        
        User user = userRepository.findByEmail(currentUserEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Incident incident = incidentRepository.findById(incidentId)
            .orElseThrow(() -> new RuntimeException("Incident not found"));

        // Only reporter can mark as viewed
        if (!incident.getReportedBy().getId().equals(user.getId())) {
            throw new RuntimeException("You can only mark your own incidents as viewed");
        }

        incident.setHasUnviewedStatusUpdate(false);
        incidentRepository.save(incident);
        
        log.info("Incident {} marked as viewed by reporter {}", incidentId, user.getEmail());
    }

    /**
     * Get count of incidents with unviewed updates for current user
     */
    public Long getUnviewedIncidentsCount() {
        String currentUserEmail = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        
        User user = userRepository.findByEmail(currentUserEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));

        return incidentRepository.countByReportedByIdAndHasUnviewedStatusUpdate(
            user.getId(), true);
    }

    /**
     * Get all incidents
     */
    public List<IncidentResponseDTO> getAllIncidents() {
        return incidentRepository.findTop50ByOrderByCreatedAtDesc().stream()
            .map(this::convertToResponseDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get unresolved incidents
     */
    public List<IncidentResponseDTO> getUnresolvedIncidents() {
        return incidentRepository.findUnresolvedIncidents().stream()
            .map(this::convertToResponseDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get critical incidents
     */
    public List<IncidentResponseDTO> getCriticalIncidents() {
        return incidentRepository.findActiveCriticalIncidents().stream()
            .map(this::convertToResponseDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get today's incidents
     */
    public List<IncidentResponseDTO> getTodayIncidents() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        
        return incidentRepository.findTodayIncidents(startOfDay, endOfDay).stream()
            .map(this::convertToResponseDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get incidents for a specific trip
     */
    public List<IncidentResponseDTO> getIncidentsByTrip(Long dailyTripId) {
        return incidentRepository.findByDailyTripIdOrderByIncidentTimeDesc(dailyTripId).stream()
            .map(this::convertToResponseDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get ALL incidents reported by current user (driver)
     * ✅ CORRECT: Each driver sees ONLY their own incidents
     */
    public List<IncidentResponseDTO> getMyIncidents() {
        String currentUserEmail = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        
        User user = userRepository.findByEmail(currentUserEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));

        return incidentRepository.findByReportedByIdOrderByCreatedAtDesc(user.getId()).stream()
            .map(this::convertToResponseDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get RESOLVED incidents reported by current user
     * ✅ CORRECT: Only shows resolved incidents for current driver
     */
    public List<IncidentResponseDTO> getMyResolvedIncidents() {
        String currentUserEmail = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        
        User user = userRepository.findByEmail(currentUserEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));

        return incidentRepository.findByReportedByIdAndStatusOrderByResolvedAtDesc(
            user.getId(), IncidentStatus.RESOLVED).stream()
            .map(this::convertToResponseDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get PENDING (unresolved) incidents reported by current user
     * ✅ CORRECT: Only shows pending incidents for current driver
     */
    public List<IncidentResponseDTO> getMyPendingIncidents() {
        String currentUserEmail = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        
        User user = userRepository.findByEmail(currentUserEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));

        List<IncidentStatus> pendingStatuses = List.of(
            IncidentStatus.REPORTED,
            IncidentStatus.ACKNOWLEDGED,
            IncidentStatus.IN_PROGRESS
        );

        return incidentRepository.findByReportedByIdAndStatusInOrderByCreatedAtDesc(
            user.getId(), pendingStatuses).stream()
            .map(this::convertToResponseDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get incident statistics
     */
    public IncidentStatisticsDTO getStatistics() {
        IncidentStatisticsDTO stats = new IncidentStatisticsDTO();
        
        stats.setTotalIncidents(incidentRepository.count());
        stats.setReportedIncidents(incidentRepository.countByStatus(IncidentStatus.REPORTED));
        stats.setResolvedIncidents(incidentRepository.countByStatus(IncidentStatus.RESOLVED));
        stats.setCriticalIncidents(incidentRepository.countBySeverity(IncidentSeverity.CRITICAL));
        stats.setDelayIncidents((long) incidentRepository.findAll().stream()
            .filter(i -> i.getDelayMinutes() != null && i.getDelayMinutes() > 0)
            .count());
        stats.setTotalDelayMinutes(incidentRepository.sumTotalDelayMinutes());
        stats.setAverageDelayMinutes(incidentRepository.averageDelayMinutes());
        
        return stats;
    }

    /**
     * Get incident by ID
     */
    public IncidentResponseDTO getIncidentById(Long id) {
        Incident incident = incidentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Incident not found"));
        return convertToResponseDTO(incident);
    }

    /**
     * Convert DailyTrip to DTO
     */
    private DailyTripResponseDTO convertTripToDTO(DailyTrip trip) {
        DailyTripResponseDTO dto = new DailyTripResponseDTO();
        dto.setDailyTripId(trip.getId());
        dto.setTripDate(trip.getTripDate().toString());
        
        if (trip.getRoute() != null) {
            dto.setOrigin(trip.getRoute().getOrigin());
            dto.setDestination(trip.getRoute().getDestination());
        }
        
        if (trip.getTimeSlot() != null) {
            dto.setDepartureTime(trip.getTimeSlot().getDepartureTime().toString());
        }
        
        if (trip.getVehicle() != null) {
            dto.setVehiclePlateNo(trip.getVehicle().getPlateNo());
        }
        
        dto.setStatus(trip.getStatus());
        
        return dto;
    }

    /**
     * Convert Incident to DTO
     */
    private IncidentResponseDTO convertToResponseDTO(Incident incident) {
        IncidentResponseDTO dto = new IncidentResponseDTO();
        
        dto.setId(incident.getId());
        
        if (incident.getDailyTrip() != null) {
            dto.setDailyTripId(incident.getDailyTrip().getId());
            
            if (incident.getDailyTrip().getRoute() != null) {
                dto.setTripRoute(incident.getDailyTrip().getRoute().getOrigin() + 
                    " → " + incident.getDailyTrip().getRoute().getDestination());
            }
            
            if (incident.getDailyTrip().getTripDate() != null) {
                dto.setTripDate(incident.getDailyTrip().getTripDate().toString());
            }
            
            if (incident.getDailyTrip().getTimeSlot() != null && 
                incident.getDailyTrip().getTimeSlot().getDepartureTime() != null) {
                dto.setTripTime(incident.getDailyTrip().getTimeSlot().getDepartureTime().toString());
            }
        }
        
        if (incident.getVehicle() != null) {
            dto.setVehiclePlateNo(incident.getVehicle().getPlateNo());
        }
        
        if (incident.getDriver() != null) {
            dto.setDriverName(incident.getDriver().getNames());
        }
        
        dto.setIncidentType(incident.getIncidentType());
        dto.setSeverity(incident.getSeverity());
        dto.setDescription(incident.getDescription());
        dto.setLocation(incident.getLocation());
        dto.setIncidentTime(incident.getIncidentTime());
        dto.setDelayMinutes(incident.getDelayMinutes());
        dto.setStatus(incident.getStatus());
        
        if (incident.getReportedBy() != null) {
            String firstName = incident.getReportedBy().getFirstName() != null ? 
                incident.getReportedBy().getFirstName() : "";
            String lastName = incident.getReportedBy().getLastName() != null ? 
                incident.getReportedBy().getLastName() : "";
            dto.setReportedByName(firstName + " " + lastName);
            dto.setReportedByEmail(incident.getReportedBy().getEmail());
        }
        
        dto.setReportedAt(incident.getCreatedAt());
        dto.setResolutionNotes(incident.getResolutionNotes());
        
        if (incident.getResolvedBy() != null) {
            String firstName = incident.getResolvedBy().getFirstName() != null ? 
                incident.getResolvedBy().getFirstName() : "";
            String lastName = incident.getResolvedBy().getLastName() != null ? 
                incident.getResolvedBy().getLastName() : "";
            dto.setResolvedByName(firstName + " " + lastName);
        }
        
        dto.setResolvedAt(incident.getResolvedAt());
        dto.setRequiresMaintenance(incident.getRequiresMaintenance());
        dto.setAffectsSchedule(incident.getAffectsSchedule());
        dto.setPassengersAffected(incident.getPassengersAffected());
        dto.setAffectedPassengerCount(incident.getAffectedPassengerCount());
        
        // ✅ NEW: Include frontend tracking fields
        dto.setHasUnviewedStatusUpdate(incident.getHasUnviewedStatusUpdate());
        dto.setLastStatusChangeAt(incident.getLastStatusChangeAt());
        
        return dto;
    }
}