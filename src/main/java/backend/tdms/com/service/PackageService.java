package backend.tdms.com.service;

import backend.tdms.com.dto.PackageBookingDTO;
import backend.tdms.com.dto.PackageCollectionDTO;
import backend.tdms.com.dto.PackageResponseDTO;
import backend.tdms.com.model.DailyTrip;
import backend.tdms.com.model.Package;
import backend.tdms.com.model.User;
import backend.tdms.com.repository.DailyTripRepository;
import backend.tdms.com.repository.PackageRepository;
import backend.tdms.com.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PackageService {

    private final PackageRepository packageRepository;
    private final DailyTripRepository dailyTripRepository;
    private final UserRepository userRepository;
    private final PackageEmailService packageEmailService;

    // Base price per kg
    private static final BigDecimal BASE_PRICE_PER_KG = new BigDecimal("1000.00"); // RWF
    private static final BigDecimal MINIMUM_PACKAGE_PRICE = new BigDecimal("2000.00"); // RWF

    /**
     * Book a package for delivery
     */
    @Transactional
    public PackageResponseDTO bookPackage(PackageBookingDTO dto) {
        log.info("Booking package from {} to trip {}", dto.getSenderNames(), dto.getDailyTripId());

        // Validate and get trip
        DailyTrip dailyTrip = dailyTripRepository.findById(dto.getDailyTripId())
            .orElseThrow(() -> new RuntimeException("Trip not found"));

        // Validate trip date
        LocalDate today = LocalDate.now();
        LocalDate maxBookingDate = today.plusDays(2);

        if (dailyTrip.getTripDate().isBefore(today)) {
            throw new RuntimeException("Cannot book package for past trips");
        }

        if (dailyTrip.getTripDate().isAfter(maxBookingDate)) {
            throw new RuntimeException("Can only book packages up to 2 days in advance");
        }

        // Validate receiver ID
        if (dto.getReceiverIdNumber() == null || dto.getReceiverIdNumber().trim().isEmpty()) {
            throw new RuntimeException("Receiver National ID is required");
        }

        // Get current user
        String currentUserEmail = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        User bookedByUser = userRepository.findByEmail(currentUserEmail).orElse(null);

        // Calculate price
        BigDecimal price = calculatePackagePrice(dto.getPackageWeight(), dailyTrip);

        // Calculate expected arrival
        LocalDateTime expectedArrival = calculateExpectedArrival(dailyTrip);

        // Create package
        Package pkg = new Package();
        
        // Sender info
        pkg.setSenderNames(dto.getSenderNames());
        pkg.setSenderPhone(dto.getSenderPhone());
        pkg.setSenderEmail(dto.getSenderEmail());
        pkg.setSenderIdNumber(dto.getSenderIdNumber());
        
        // Receiver info
        pkg.setReceiverNames(dto.getReceiverNames());
        pkg.setReceiverPhone(dto.getReceiverPhone());
        pkg.setReceiverEmail(dto.getReceiverEmail());
        pkg.setReceiverIdNumber(dto.getReceiverIdNumber());
        
        // Package details
        pkg.setPackageDescription(dto.getPackageDescription());
        pkg.setPackageWeight(dto.getPackageWeight());
        pkg.setPackageValue(dto.getPackageValue());
        pkg.setIsFragile(dto.getIsFragile() != null ? dto.getIsFragile() : false);
        
        // Trip and payment
        pkg.setDailyTrip(dailyTrip);
        pkg.setPrice(price);
        pkg.setPaymentMethod(dto.getPaymentMethod());
        pkg.setPaymentStatus("PAID");
        pkg.setPackageStatus("IN_TRANSIT");
        pkg.setExpectedArrivalTime(expectedArrival);
        pkg.setBookedBy(bookedByUser);

        // Save package
        Package savedPackage = packageRepository.save(pkg);

        log.info("Package booked successfully: {}", savedPackage.getTrackingNumber());

        // Send notifications asynchronously
        sendBookingNotifications(savedPackage, dailyTrip);

        // Return DTO to avoid Hibernate proxy serialization issues
        return convertToDTO(savedPackage);
    }

    /**
     * Send notifications when package is booked
     */
    private void sendBookingNotifications(Package pkg, DailyTrip trip) {
        // Format dates and times for notifications
        String travelDate = trip.getTripDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String departureTime = trip.getTimeSlot().getDepartureTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        String expectedArrivalFormatted = pkg.getExpectedArrivalTime()
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        
        // Create full departure info: "07/01/2026 at 08:00"
        String departureInfo = travelDate + " at " + departureTime;

        // Notify sender via Email
        if (pkg.getSenderEmail() != null && !pkg.getSenderEmail().isEmpty()) {
            packageEmailService.sendPackageSentEmail(
                pkg.getSenderEmail(),
                pkg.getSenderNames(),
                pkg.getTrackingNumber(),
                pkg.getReceiverNames(),
                trip.getRoute().getOrigin(),
                trip.getRoute().getDestination(),
                departureInfo,  // Now includes departure time
                expectedArrivalFormatted
            );
        }

        // Notify receiver via Email
        if (pkg.getReceiverEmail() != null && !pkg.getReceiverEmail().isEmpty()) {
            packageEmailService.sendPackageIncomingEmail(
                pkg.getReceiverEmail(),
                pkg.getReceiverNames(),
                pkg.getTrackingNumber(),
                pkg.getSenderNames(),
                pkg.getSenderPhone(),
                trip.getRoute().getOrigin(),
                trip.getRoute().getDestination(),
                departureInfo,  // Now includes departure time
                expectedArrivalFormatted,
                pkg.getPackageDescription() != null ? pkg.getPackageDescription() : "Package"
            );
        }

        // Update notification flags
        pkg.setSenderNotifiedSent(true);
        pkg.setReceiverNotifiedSent(true);
        packageRepository.save(pkg);

        log.info("Booking notifications sent for package: {} - Departure: {}, Expected arrival: {}", 
            pkg.getTrackingNumber(), departureInfo, expectedArrivalFormatted);
    }

    /**
     * Mark package as arrived and notify receiver
     */
    @Transactional
    public PackageResponseDTO markPackageAsArrived(Long packageId) {
        Package pkg = packageRepository.findById(packageId)
            .orElseThrow(() -> new RuntimeException("Package not found"));

        if (!"IN_TRANSIT".equals(pkg.getPackageStatus())) {
            throw new RuntimeException("Package must be in transit to mark as arrived");
        }

        // Update package status
        pkg.setPackageStatus("ARRIVED");
        pkg.setActualArrivalTime(LocalDateTime.now());

        Package savedPackage = packageRepository.save(pkg);

        // Send arrival notifications
        sendArrivalNotifications(savedPackage);

        log.info("Package marked as arrived: {}", pkg.getTrackingNumber());

        return convertToDTO(savedPackage);
    }

    /**
     * Send notifications when package arrives
     */
    private void sendArrivalNotifications(Package pkg) {
        String arrivalTime = pkg.getActualArrivalTime()
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String collectionLocation = pkg.getDailyTrip().getRoute().getDestination();

        // Notify receiver via Email
        if (pkg.getReceiverEmail() != null && !pkg.getReceiverEmail().isEmpty()) {
            packageEmailService.sendPackageArrivedEmail(
                pkg.getReceiverEmail(),
                pkg.getReceiverNames(),
                pkg.getTrackingNumber(),
                arrivalTime,
                collectionLocation
            );
        }

        // Update notification flag
        pkg.setReceiverNotifiedArrived(true);
        packageRepository.save(pkg);

        log.info("Arrival notifications sent for package: {}", pkg.getTrackingNumber());
    }

    /**
     * Mark package as collected and notify sender
     */
    @Transactional
    public PackageResponseDTO collectPackage(PackageCollectionDTO dto) {
        Package pkg = packageRepository.findById(dto.getPackageId())
            .orElseThrow(() -> new RuntimeException("Package not found"));

        // Validate status
        if (!"ARRIVED".equals(pkg.getPackageStatus())) {
            throw new RuntimeException("Package must have arrived to be collected. Current status: " + 
                pkg.getPackageStatus());
        }

        // Validate receiver ID
        if (!pkg.getReceiverIdNumber().equals(dto.getReceiverIdNumber())) {
            throw new RuntimeException("Invalid National ID. Package can only be collected by the designated receiver.");
        }

        // Update package
        pkg.setPackageStatus("COLLECTED");
        pkg.setCollectedAt(LocalDateTime.now());
        pkg.setCollectedByName(dto.getCollectedByName());
        pkg.setCollectedById(dto.getReceiverIdNumber());

        Package savedPackage = packageRepository.save(pkg);

        // Send delivery confirmation
        sendDeliveryNotifications(savedPackage);

        log.info("Package collected: {} by {}", pkg.getTrackingNumber(), dto.getCollectedByName());

        return convertToDTO(savedPackage);
    }

    /**
     * Send notifications when package is delivered
     */
    private void sendDeliveryNotifications(Package pkg) {
        String collectionTime = pkg.getCollectedAt()
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        // Notify sender via Email
        if (pkg.getSenderEmail() != null && !pkg.getSenderEmail().isEmpty()) {
            packageEmailService.sendPackageDeliveredEmail(
                pkg.getSenderEmail(),
                pkg.getSenderNames(),
                pkg.getTrackingNumber(),
                pkg.getReceiverNames(),
                collectionTime
            );
        }

        // Update notification flag
        pkg.setSenderNotifiedDelivered(true);
        packageRepository.save(pkg);

        log.info("Delivery notifications sent for package: {}", pkg.getTrackingNumber());
    }

    /**
     * Calculate package price based on weight and route
     */
    private BigDecimal calculatePackagePrice(Double weight, DailyTrip trip) {
        if (weight == null || weight <= 0) {
            return MINIMUM_PACKAGE_PRICE;
        }

        // Base calculation: weight × price per kg
        BigDecimal calculatedPrice = BASE_PRICE_PER_KG.multiply(new BigDecimal(weight));

        // Add 30% of ticket price as premium
        BigDecimal ticketPrice = trip.getRoute().getPrice();
        BigDecimal premium = ticketPrice.multiply(new BigDecimal("0.30"));
        calculatedPrice = calculatedPrice.add(premium);

        // Ensure minimum price
        if (calculatedPrice.compareTo(MINIMUM_PACKAGE_PRICE) < 0) {
            return MINIMUM_PACKAGE_PRICE;
        }

        return calculatedPrice.setScale(0, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Calculate expected arrival time
     */
    private LocalDateTime calculateExpectedArrival(DailyTrip trip) {
        LocalDateTime departureDateTime = LocalDateTime.of(
            trip.getTripDate(),
            trip.getTimeSlot().getDepartureTime()
        );

        Integer durationMinutes = trip.getRoute().getDurationMinutes() != null ?
            trip.getRoute().getDurationMinutes() : 120;

        return departureDateTime.plusMinutes(durationMinutes);
    }

    /**
     * Get package by tracking number
     */
    public PackageResponseDTO getPackageByTrackingNumber(String trackingNumber) {
        Package pkg = packageRepository.findByTrackingNumber(trackingNumber)
            .orElseThrow(() -> new RuntimeException("Package not found with tracking number: " + trackingNumber));
        return convertToDTO(pkg);
    }

    /**
     * Get all packages for a specific trip
     */
    public List<PackageResponseDTO> getPackagesForTrip(Long dailyTripId) {
        DailyTrip trip = dailyTripRepository.findById(dailyTripId)
            .orElseThrow(() -> new RuntimeException("Trip not found"));

        return packageRepository.findByDailyTrip(trip).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get packages by sender phone
     */
    public List<PackageResponseDTO> getPackagesBySender(String senderPhone) {
        return packageRepository.findBySenderPhone(senderPhone).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get packages by receiver phone
     */
    public List<PackageResponseDTO> getPackagesByReceiver(String receiverPhone) {
        return packageRepository.findByReceiverPhone(receiverPhone).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get packages by status
     */
    public List<PackageResponseDTO> getPackagesByStatus(String status) {
        return packageRepository.findByPackageStatus(status).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get packages that have arrived but not yet collected
     */
    public List<PackageResponseDTO> getArrivedPackages() {
        return packageRepository.findByPackageStatus("ARRIVED").stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get packages in transit
     */
    public List<PackageResponseDTO> getInTransitPackages() {
        return packageRepository.findByPackageStatus("IN_TRANSIT").stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get collected packages
     */
    public List<PackageResponseDTO> getCollectedPackages() {
        return packageRepository.findByPackageStatus("COLLECTED").stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    // PackageService.java - ADD THESE NEW METHODS

    /**
     * ✅ NEW: Get packages sent by current logged-in user
     * Uses user's phone number from their profile
     */
    public List<PackageResponseDTO> getMySentPackages() {
        String currentUserEmail = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        
        User currentUser = userRepository.findByEmail(currentUserEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (currentUser.getPhone() == null || currentUser.getPhone().trim().isEmpty()) {
            throw new RuntimeException("User phone number not found in profile. Please update your profile.");
        }

        log.info("Getting sent packages for user: {} with phone: {}", 
            currentUser.getEmail(), currentUser.getPhone());

        return packageRepository.findBySenderPhone(currentUser.getPhone()).stream()
            .sorted((p1, p2) -> p2.getBookingDate().compareTo(p1.getBookingDate())) // Newest first
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * ✅ NEW: Get packages where current user is the receiver
     * Uses user's phone number from their profile
     */
    public List<PackageResponseDTO> getMyReceivedPackages() {
        String currentUserEmail = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        
        User currentUser = userRepository.findByEmail(currentUserEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (currentUser.getPhone() == null || currentUser.getPhone().trim().isEmpty()) {
            throw new RuntimeException("User phone number not found in profile. Please update your profile.");
        }

        log.info("Getting received packages for user: {} with phone: {}", 
            currentUser.getEmail(), currentUser.getPhone());

        return packageRepository.findByReceiverPhone(currentUser.getPhone()).stream()
            .sorted((p1, p2) -> p2.getBookingDate().compareTo(p1.getBookingDate())) // Newest first
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * ✅ NEW: Get package statistics for current user
     * Returns total sent, received, in-transit, arrived, collected
     */
    public Map<String, Object> getMyPackageStatistics() {
        List<PackageResponseDTO> sentPackages = getMySentPackages();
        List<PackageResponseDTO> receivedPackages = getMyReceivedPackages();

        Map<String, Object> stats = new HashMap<>();
        
        // Sent package stats
        stats.put("totalSent", sentPackages.size());
        stats.put("sentInTransit", sentPackages.stream()
            .filter(p -> "IN_TRANSIT".equals(p.getPackageStatus())).count());
        stats.put("sentArrived", sentPackages.stream()
            .filter(p -> "ARRIVED".equals(p.getPackageStatus())).count());
        stats.put("sentCollected", sentPackages.stream()
            .filter(p -> "COLLECTED".equals(p.getPackageStatus())).count());
        
        // Received package stats
        stats.put("totalReceived", receivedPackages.size());
        stats.put("receivedInTransit", receivedPackages.stream()
            .filter(p -> "IN_TRANSIT".equals(p.getPackageStatus())).count());
        stats.put("receivedArrived", receivedPackages.stream()
            .filter(p -> "ARRIVED".equals(p.getPackageStatus())).count());
        stats.put("receivedCollected", receivedPackages.stream()
            .filter(p -> "COLLECTED".equals(p.getPackageStatus())).count());
        
        log.info("Package statistics - Sent: {}, Received: {}", 
            sentPackages.size(), receivedPackages.size());

        return stats;
    }

    /**
     * Cancel package before departure
     */
    @Transactional
    public PackageResponseDTO cancelPackage(Long packageId, String reason) {
        Package pkg = packageRepository.findById(packageId)
            .orElseThrow(() -> new RuntimeException("Package not found"));

        if ("COLLECTED".equals(pkg.getPackageStatus())) {
            throw new RuntimeException("Cannot cancel collected package");
        }

        if ("CANCELLED".equals(pkg.getPackageStatus())) {
            throw new RuntimeException("Package already cancelled");
        }

        // Check if trip hasn't departed yet
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tripDeparture = LocalDateTime.of(
            pkg.getDailyTrip().getTripDate(),
            pkg.getDailyTrip().getTimeSlot().getDepartureTime()
        );

        if (now.isAfter(tripDeparture)) {
            throw new RuntimeException("Cannot cancel package after trip departure");
        }

        pkg.setPackageStatus("CANCELLED");
        pkg.setCancelledAt(LocalDateTime.now());
        pkg.setCancellationReason(reason);
        pkg.setPaymentStatus("REFUNDED");

        Package savedPackage = packageRepository.save(pkg);

        log.info("Package cancelled: {}", pkg.getTrackingNumber());

        return convertToDTO(savedPackage);
    }

    /**
     * Convert Package to DTO
     */
    private PackageResponseDTO convertToDTO(Package pkg) {
        PackageResponseDTO dto = new PackageResponseDTO();
        dto.setId(pkg.getId());
        dto.setTrackingNumber(pkg.getTrackingNumber());
        
        dto.setSenderNames(pkg.getSenderNames());
        dto.setSenderPhone(pkg.getSenderPhone());
        
        dto.setReceiverNames(pkg.getReceiverNames());
        dto.setReceiverPhone(pkg.getReceiverPhone());
        dto.setReceiverIdNumber(pkg.getReceiverIdNumber());
        
        dto.setPackageDescription(pkg.getPackageDescription());
        dto.setPackageWeight(pkg.getPackageWeight());
        dto.setPackageValue(pkg.getPackageValue());
        dto.setIsFragile(pkg.getIsFragile());
        
        dto.setOrigin(pkg.getDailyTrip().getRoute().getOrigin());
        dto.setDestination(pkg.getDailyTrip().getRoute().getDestination());
        dto.setTravelDate(pkg.getDailyTrip().getTripDate());
        dto.setDepartureTime(pkg.getDailyTrip().getTimeSlot().getDepartureTime());
        dto.setVehiclePlateNo(pkg.getDailyTrip().getVehicle().getPlateNo());
        
        dto.setPackageStatus(pkg.getPackageStatus());
        dto.setPrice(pkg.getPrice());
        dto.setPaymentStatus(pkg.getPaymentStatus());
        dto.setPaymentMethod(pkg.getPaymentMethod());
        dto.setBookingDate(pkg.getBookingDate());
        dto.setExpectedArrivalTime(pkg.getExpectedArrivalTime());
        dto.setActualArrivalTime(pkg.getActualArrivalTime());
        dto.setCollectedAt(pkg.getCollectedAt());
        dto.setCollectedByName(pkg.getCollectedByName());
        
        return dto;
    }
}