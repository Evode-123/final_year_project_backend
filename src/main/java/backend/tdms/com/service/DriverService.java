package backend.tdms.com.service;

import backend.tdms.com.dto.DriverDTO;
import backend.tdms.com.dto.DriverVehicleAssignmentDTO;
import backend.tdms.com.model.Driver;
import backend.tdms.com.model.Role;
import backend.tdms.com.model.User;
import backend.tdms.com.model.Vehicle;
import backend.tdms.com.repository.DriverRepository;
import backend.tdms.com.repository.RoleRepository;
import backend.tdms.com.repository.UserRepository;
import backend.tdms.com.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int PASSWORD_LENGTH = 12;

    @Transactional
    public Driver createDriver(DriverDTO dto) {
        // Validate unique fields
        if (driverRepository.existsByLicenseNo(dto.getLicenseNo())) {
            throw new RuntimeException("Driver with this license number already exists");
        }

        if (dto.getIdNumber() != null && driverRepository.existsByIdNumber(dto.getIdNumber())) {
            throw new RuntimeException("Driver with this ID number already exists");
        }

        // Check if email is provided for creating user account
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            // Check if email already exists
            if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
                throw new RuntimeException("A user with this email already exists");
            }

            // Create user account for the driver WITH PROFILE ALREADY COMPLETED
            String plainPassword = createDriverUserAccount(dto.getEmail(), dto.getNames(), dto.getPhoneNumber());
            
            // Send credentials email
            emailService.sendDriverCredentialsEmail(
                dto.getEmail(), 
                dto.getNames(),
                plainPassword
            );
        }

        // Create driver record
        Driver driver = new Driver();
        driver.setNames(dto.getNames());
        driver.setPhoneNumber(dto.getPhoneNumber());
        driver.setLicenseNo(dto.getLicenseNo());
        driver.setIdNumber(dto.getIdNumber());
        driver.setLicenseExpiryDate(dto.getLicenseExpiryDate());
        driver.setAddress(dto.getAddress());
        driver.setStatus("ACTIVE");
        driver.setIsBackup(dto.getIsBackup() != null ? dto.getIsBackup() : false);
        driver.setHiredDate(dto.getHiredDate() != null ? dto.getHiredDate() : LocalDate.now());

        Driver savedDriver = driverRepository.save(driver);
        log.info("Driver created: {} (License: {})", savedDriver.getNames(), savedDriver.getLicenseNo());

        return savedDriver;
    }

    /**
     * Creates a user account for the driver with ROLE_DRIVER
     * Profile is automatically completed with driver information
     * Returns the plain text password for email sending
     */
    private String createDriverUserAccount(String email, String driverName, String phoneNumber) {
        String randomPassword = generateRandomPassword();
        
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(randomPassword));
        user.setMustChangePassword(true); // Driver must change password on first login
        user.setProfileCompleted(true);   // ✅ FIXED: Profile is already completed!
        user.setEnabled(true);

        // Split driver name into first and last name
        String[] nameParts = driverName.trim().split("\\s+", 2);
        user.setFirstName(nameParts[0]);
        user.setLastName(nameParts.length > 1 ? nameParts[1] : "");
        
        // Set phone number
        user.setPhone(phoneNumber);

        // Assign ROLE_DRIVER
        Role driverRole = roleRepository.findByName("ROLE_DRIVER")
                .orElseThrow(() -> new RuntimeException("Driver role not found"));
        user.setRoles(new HashSet<>(Collections.singleton(driverRole)));

        userRepository.save(user);
        
        log.info("Driver user account created for: {} (Profile completed automatically)", email);
        return randomPassword; // Return plain password for email
    }

    private String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            password.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return password.toString();
    }

    @Transactional
    public Driver updateDriver(Long id, DriverDTO dto) {
        Driver driver = driverRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Driver not found"));

        // Check license number uniqueness if changed
        if (!driver.getLicenseNo().equals(dto.getLicenseNo())) {
            if (driverRepository.existsByLicenseNo(dto.getLicenseNo())) {
                throw new RuntimeException("Driver with this license number already exists");
            }
        }

        driver.setNames(dto.getNames());
        driver.setPhoneNumber(dto.getPhoneNumber());
        driver.setLicenseNo(dto.getLicenseNo());
        driver.setIdNumber(dto.getIdNumber());
        driver.setLicenseExpiryDate(dto.getLicenseExpiryDate());
        driver.setAddress(dto.getAddress());
        driver.setStatus(dto.getStatus());
        driver.setIsBackup(dto.getIsBackup());
        driver.setHiredDate(dto.getHiredDate());

        Driver updatedDriver = driverRepository.save(driver);
        log.info("Driver updated: {}", updatedDriver.getNames());

        return updatedDriver;
    }

    @Transactional
    public Driver assignDriverToVehicle(DriverVehicleAssignmentDTO dto) {
        Driver driver = driverRepository.findById(dto.getDriverId())
            .orElseThrow(() -> new RuntimeException("Driver not found"));

        Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId())
            .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        // Check if driver is backup
        if (driver.getIsBackup()) {
            throw new RuntimeException("Cannot assign backup driver to a vehicle permanently");
        }

        // Check if driver already has a vehicle
        if (driver.getAssignedVehicle() != null) {
            throw new RuntimeException("Driver already assigned to vehicle: " + 
                driver.getAssignedVehicle().getPlateNo());
        }

        // Check if vehicle already has a driver
        driverRepository.findByAssignedVehicle(vehicle).ifPresent(existingDriver -> {
            throw new RuntimeException("Vehicle already has an assigned driver: " + 
                existingDriver.getNames());
        });

        // Assign driver to vehicle
        driver.setAssignedVehicle(vehicle);
        Driver savedDriver = driverRepository.save(driver);

        log.info("Driver {} assigned to vehicle {}", 
            driver.getNames(), vehicle.getPlateNo());

        return savedDriver;
    }

    @Transactional
    public Driver unassignDriverFromVehicle(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
            .orElseThrow(() -> new RuntimeException("Driver not found"));

        if (driver.getAssignedVehicle() == null) {
            throw new RuntimeException("Driver is not assigned to any vehicle");
        }

        String vehiclePlateNo = driver.getAssignedVehicle().getPlateNo();
        driver.setAssignedVehicle(null);
        Driver savedDriver = driverRepository.save(driver);

        log.info("Driver {} unassigned from vehicle {}", 
            driver.getNames(), vehiclePlateNo);

        return savedDriver;
    }

    public List<DriverDTO> getAllDrivers() {
        return driverRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public List<DriverDTO> getActiveDrivers() {
        return driverRepository.findByStatus("ACTIVE").stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public List<DriverDTO> getBackupDrivers() {
        return driverRepository.findByIsBackupTrue().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public List<DriverDTO> getAvailableDriversForAssignment() {
        return driverRepository.findAvailableDriversForAssignment().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public DriverDTO getDriverById(Long id) {
        Driver driver = driverRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Driver not found"));
        return convertToDTO(driver);
    }

    public DriverDTO getDriverByVehicle(Long vehicleId) {
        Driver driver = driverRepository.findByAssignedVehicleId(vehicleId)
            .orElseThrow(() -> new RuntimeException("No driver assigned to this vehicle"));
        return convertToDTO(driver);
    }

    @Transactional
    public void deleteDriver(Long id) {
        Driver driver = driverRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Driver not found"));

        if (driver.getAssignedVehicle() != null) {
            throw new RuntimeException("Cannot delete driver assigned to a vehicle. Unassign first.");
        }

        driver.setStatus("INACTIVE");
        driverRepository.save(driver);
        log.info("Driver deactivated: {}", driver.getNames());
    }

    @Transactional
    public Driver changeDriverStatus(Long id, String status) {
        Driver driver = driverRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Driver not found"));

        driver.setStatus(status);
        Driver updatedDriver = driverRepository.save(driver);

        log.info("Driver {} status changed to {}", driver.getNames(), status);
        return updatedDriver;
    }

    private DriverDTO convertToDTO(Driver driver) {
        DriverDTO dto = new DriverDTO();
        dto.setId(driver.getId());
        dto.setNames(driver.getNames());
        dto.setPhoneNumber(driver.getPhoneNumber());
        dto.setLicenseNo(driver.getLicenseNo());
        dto.setIdNumber(driver.getIdNumber());
        dto.setLicenseExpiryDate(driver.getLicenseExpiryDate());
        dto.setAddress(driver.getAddress());
        dto.setStatus(driver.getStatus());
        dto.setIsBackup(driver.getIsBackup());
        dto.setHiredDate(driver.getHiredDate());

        if (driver.getAssignedVehicle() != null) {
            dto.setAssignedVehicleId(driver.getAssignedVehicle().getId());
            dto.setAssignedVehiclePlateNo(driver.getAssignedVehicle().getPlateNo());
        }

        return dto;
    }
}