package backend.tdms.com.service;

import backend.tdms.com.dto.DailyCheckResponseDTO;
import backend.tdms.com.dto.DriverCheckDTO;
import backend.tdms.com.model.DailyVehicleCheck;
import backend.tdms.com.model.Driver;
import backend.tdms.com.model.User;
import backend.tdms.com.model.Vehicle;
import backend.tdms.com.repository.DailyVehicleCheckRepository;
import backend.tdms.com.repository.DriverRepository;
import backend.tdms.com.repository.UserRepository;
import backend.tdms.com.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyVehicleCheckService {

    private final DailyVehicleCheckRepository checkRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;

    /**
     * Driver submits daily vehicle check
     */
    @Transactional
    public DailyCheckResponseDTO submitCheck(DriverCheckDTO dto) {
        log.info("Driver {} submitting check for vehicle {}", dto.getDriverId(), dto.getVehicleId());

        Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId())
            .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        Driver driver = driverRepository.findById(dto.getDriverId())
            .orElseThrow(() -> new RuntimeException("Driver not found"));

        // Check if vehicle already checked today
        if (checkRepository.isVehicleCheckedToday(dto.getVehicleId(), LocalDate.now())) {
            log.warn("Vehicle {} already checked today", vehicle.getPlateNo());
            // Allow but log warning - maybe driver noticed new issue
        }

        DailyVehicleCheck check = new DailyVehicleCheck();
        check.setVehicle(vehicle);
        check.setDriver(driver);
        check.setCheckDate(dto.getCheckDate() != null ? dto.getCheckDate() : LocalDate.now());
        check.setCheckLocation(dto.getCheckLocation());

        // Set all checks
        check.setTiresOk(dto.getTiresOk());
        check.setLightsOk(dto.getLightsOk());
        check.setBrakesOk(dto.getBrakesOk());
        check.setMirrorsOk(dto.getMirrorsOk());
        check.setWindshieldOk(dto.getWindshieldOk());
        check.setWipersOk(dto.getWipersOk());
        check.setBodyDamage(dto.getBodyDamage());
        check.setCleanlinessOk(dto.getCleanlinessOk());

        // Safety equipment
        check.setFireExtinguisher(dto.getFireExtinguisher());
        check.setFirstAidKit(dto.getFirstAidKit());
        check.setWarningTriangle(dto.getWarningTriangle());

        // Fluids
        check.setOilLevelOk(dto.getOilLevelOk());
        check.setCoolantLevelOk(dto.getCoolantLevelOk());
        check.setFuelLevel(dto.getFuelLevel());
        
        // Problems
        check.setHasProblems(dto.getHasProblems() != null ? dto.getHasProblems() : false);
        check.setProblemsDescription(dto.getProblemsDescription());
        check.setIsSafeToDrive(dto.getIsSafeToDrive() != null ? dto.getIsSafeToDrive() : true);
        check.setUrgencyLevel(dto.getUrgencyLevel());
        check.setDriverNotes(dto.getDriverNotes());

        // Determine if action required
        if (Boolean.TRUE.equals(check.getHasProblems())) {
            check.setActionRequired(true);
            check.setFollowUpNeeded(true);
        }

        DailyVehicleCheck saved = checkRepository.save(check);

        log.info("Daily check saved for vehicle {} by driver {}: Status = {}", 
            vehicle.getPlateNo(), driver.getNames(), saved.getOverallStatus());

        return convertToDTO(saved);
    }

    /**
     * Get today's checks
     */
    public List<DailyCheckResponseDTO> getTodaysChecks() {
        List<DailyVehicleCheck> checks = checkRepository.findTodaysChecks(LocalDate.now());
        return checks.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get checks with problems (last 7 days)
     */
    public List<DailyCheckResponseDTO> getChecksWithProblems() {
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
        List<DailyVehicleCheck> checks = checkRepository.findChecksWithProblems(sevenDaysAgo);
        return checks.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get unreviewed problems
     */
    public List<DailyCheckResponseDTO> getUnreviewedProblems() {
        List<DailyVehicleCheck> checks = checkRepository.findUnreviewedProblems();
        return checks.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get urgent checks
     */
    public List<DailyCheckResponseDTO> getUrgentChecks() {
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
        List<DailyVehicleCheck> checks = checkRepository.findUrgentChecks(sevenDaysAgo);
        return checks.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get latest check for vehicle
     */
    public DailyCheckResponseDTO getLatestCheck(Long vehicleId) {
        DailyVehicleCheck check = checkRepository.findLatestCheckByVehicleId(vehicleId)
            .orElseThrow(() -> new RuntimeException("No checks found for this vehicle"));
        return convertToDTO(check);
    }

    /**
     * Get check history for vehicle
     */
    public List<DailyCheckResponseDTO> getVehicleCheckHistory(Long vehicleId, int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        LocalDate endDate = LocalDate.now();
        
        List<DailyVehicleCheck> checks = checkRepository.findVehicleChecksByDateRange(
            vehicleId, startDate, endDate
        );
        
        return checks.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Manager reviews a check
     */
    @Transactional
    public DailyCheckResponseDTO reviewCheck(Long checkId, String managerNotes, String actionTaken) {
        DailyVehicleCheck check = checkRepository.findById(checkId)
            .orElseThrow(() -> new RuntimeException("Check not found"));

        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User reviewer = userRepository.findByEmail(currentUserEmail).orElse(null);

        check.setReviewedBy(reviewer);
        check.setReviewedAt(java.time.LocalDateTime.now());
        check.setManagerNotes(managerNotes);
        check.setActionTaken(actionTaken);

        DailyVehicleCheck updated = checkRepository.save(check);

        log.info("Check {} reviewed by {}", checkId, currentUserEmail);

        return convertToDTO(updated);
    }

    /**
     * Get dashboard summary
     */
    public Map<String, Object> getDashboardSummary() {
        List<DailyVehicleCheck> todaysChecks = checkRepository.findTodaysChecks(LocalDate.now());
        List<DailyVehicleCheck> unreviewedProblems = checkRepository.findUnreviewedProblems();
        List<DailyVehicleCheck> urgentChecks = checkRepository.findUrgentChecks(LocalDate.now().minusDays(7));

        long goodCount = todaysChecks.stream()
            .filter(c -> "GOOD".equals(c.getOverallStatus()))
            .count();

        long issuesCount = todaysChecks.stream()
            .filter(c -> "HAS_ISSUES".equals(c.getOverallStatus()))
            .count();

        long urgentCount = todaysChecks.stream()
            .filter(c -> "URGENT".equals(c.getOverallStatus()))
            .count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalChecksToday", todaysChecks.size());
        summary.put("goodChecks", goodCount);
        summary.put("checksWithIssues", issuesCount);
        summary.put("urgentChecks", urgentCount);
        summary.put("unreviewedProblemsCount", unreviewedProblems.size());
        summary.put("unreviewedProblems", unreviewedProblems.stream().map(this::convertToDTO).collect(Collectors.toList()));
        summary.put("urgentChecksList", urgentChecks.stream().map(this::convertToDTO).collect(Collectors.toList()));

        return summary;
    }

    /**
     * Convert to DTO
     */
    private DailyCheckResponseDTO convertToDTO(DailyVehicleCheck check) {
        DailyCheckResponseDTO dto = new DailyCheckResponseDTO();
        dto.setId(check.getId());
        dto.setVehicleId(check.getVehicle().getId());
        dto.setVehiclePlateNo(check.getVehicle().getPlateNo());
        dto.setVehicleType(check.getVehicle().getVehicleType());
        dto.setDriverId(check.getDriver().getId());
        dto.setDriverName(check.getDriver().getNames());
        dto.setDriverPhone(check.getDriver().getPhoneNumber());
        dto.setCheckDate(check.getCheckDate());
        dto.setCheckLocation(check.getCheckLocation());
        dto.setOverallStatus(check.getOverallStatus());

        // All checks
        dto.setTiresOk(check.getTiresOk());
        dto.setLightsOk(check.getLightsOk());
        dto.setBrakesOk(check.getBrakesOk());
        dto.setMirrorsOk(check.getMirrorsOk());
        dto.setWindshieldOk(check.getWindshieldOk());
        dto.setWipersOk(check.getWipersOk());
        dto.setBodyDamage(check.getBodyDamage());
        dto.setCleanlinessOk(check.getCleanlinessOk());
        dto.setFireExtinguisher(check.getFireExtinguisher());
        dto.setFirstAidKit(check.getFirstAidKit());
        dto.setWarningTriangle(check.getWarningTriangle());
        dto.setOilLevelOk(check.getOilLevelOk());
        dto.setCoolantLevelOk(check.getCoolantLevelOk());
        dto.setFuelLevel(check.getFuelLevel());

        // Problems
        dto.setHasProblems(check.getHasProblems());
        dto.setProblemsDescription(check.getProblemsDescription());
        dto.setIsSafeToDrive(check.getIsSafeToDrive());
        dto.setUrgencyLevel(check.getUrgencyLevel());
        dto.setActionRequired(check.getActionRequired());
        dto.setActionTaken(check.getActionTaken());
        dto.setFollowUpNeeded(check.getFollowUpNeeded());

        // Review
        dto.setReviewed(check.getReviewedBy() != null);
        if (check.getReviewedBy() != null) {
            dto.setReviewedByEmail(check.getReviewedBy().getEmail());
        }
        dto.setReviewedAt(check.getReviewedAt());
        dto.setManagerNotes(check.getManagerNotes());

        dto.setCreatedAt(check.getCreatedAt());
        dto.setDriverNotes(check.getDriverNotes());

        return dto;
    }
}