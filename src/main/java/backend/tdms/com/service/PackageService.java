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

    private static final BigDecimal BASE_PRICE_PER_KG = new BigDecimal("1000.00");
    private static final BigDecimal MINIMUM_PACKAGE_PRICE = new BigDecimal("2000.00");

    /**
     * Book a package for delivery
     */
    @Transactional
    public PackageResponseDTO bookPackage(PackageBookingDTO dto) {
        log.info("Booking package from {} to trip {}", dto.getSenderNames(), dto.getDailyTripId());

        DailyTrip dailyTrip = dailyTripRepository.findById(dto.getDailyTripId())
            .orElseThrow(() -> new RuntimeException("Trip not found"));

        LocalDate today = LocalDate.now();
        LocalDate maxBookingDate = today.plusDays(2);

        if (dailyTrip.getTripDate().isBefore(today)) {
            throw new RuntimeException("Cannot book package for past trips");
        }

        if (dailyTrip.getTripDate().isAfter(maxBookingDate)) {
            throw new RuntimeException("Can only book packages up to 2 days in advance");
        }

        if (dto.getReceiverIdNumber() == null || dto.getReceiverIdNumber().trim().isEmpty()) {
            throw new RuntimeException("Receiver National ID is required");
        }

        String currentUserEmail = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        User bookedByUser = userRepository.findByEmail(currentUserEmail).orElse(null);

        BigDecimal price = calculatePackagePrice(dto.getPackageWeight(), dailyTrip);
        LocalDateTime expectedArrival = calculateExpectedArrival(dailyTrip);

        Package pkg = new Package();

        // Sender info
        pkg.setSenderNames(dto.getSenderNames());
        pkg.setSenderPhone(dto.getSenderPhone());
        pkg.setSenderEmail(dto.getSenderEmail());
        pkg.setSenderIdNumber(dto.getSenderIdNumber());
        pkg.setSenderAddress(dto.getSenderAddress());

        // Receiver info
        pkg.setReceiverNames(dto.getReceiverNames());
        pkg.setReceiverPhone(dto.getReceiverPhone());
        pkg.setReceiverEmail(dto.getReceiverEmail());
        pkg.setReceiverIdNumber(dto.getReceiverIdNumber());
        pkg.setReceiverAddress(dto.getReceiverAddress());

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

        Package savedPackage = packageRepository.save(pkg);

        log.info("Package booked successfully: {}", savedPackage.getTrackingNumber());

        sendBookingNotifications(savedPackage, dailyTrip);

        return convertToDTO(savedPackage);
    }

    /**
     * Send notifications when package is booked
     */
    private void sendBookingNotifications(Package pkg, DailyTrip trip) {
        String travelDate = trip.getTripDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String departureTime = trip.getTimeSlot().getDepartureTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        String expectedArrivalFormatted = pkg.getExpectedArrivalTime()
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String departureInfo = travelDate + " at " + departureTime;

        if (pkg.getSenderEmail() != null && !pkg.getSenderEmail().isEmpty()) {
            packageEmailService.sendPackageSentEmail(
                pkg.getSenderEmail(),
                pkg.getSenderNames(),
                pkg.getTrackingNumber(),
                pkg.getReceiverNames(),
                trip.getRoute().getOrigin(),
                trip.getRoute().getDestination(),
                departureInfo,
                expectedArrivalFormatted,
                pkg.getSenderAddress(),
                pkg.getReceiverAddress()
            );
        }

        if (pkg.getReceiverEmail() != null && !pkg.getReceiverEmail().isEmpty()) {
            packageEmailService.sendPackageIncomingEmail(
                pkg.getReceiverEmail(),
                pkg.getReceiverNames(),
                pkg.getTrackingNumber(),
                pkg.getSenderNames(),
                pkg.getSenderPhone(),
                trip.getRoute().getOrigin(),
                trip.getRoute().getDestination(),
                departureInfo,
                expectedArrivalFormatted,
                pkg.getPackageDescription() != null ? pkg.getPackageDescription() : "Package",
                pkg.getSenderAddress(),
                pkg.getReceiverAddress()
            );
        }

        pkg.setSenderNotifiedSent(true);
        pkg.setReceiverNotifiedSent(true);
        packageRepository.save(pkg);

        log.info("Booking notifications sent for package: {}", pkg.getTrackingNumber());
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

        pkg.setPackageStatus("ARRIVED");
        pkg.setActualArrivalTime(LocalDateTime.now());

        Package savedPackage = packageRepository.save(pkg);

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

        if (pkg.getReceiverEmail() != null && !pkg.getReceiverEmail().isEmpty()) {
            packageEmailService.sendPackageArrivedEmail(
                pkg.getReceiverEmail(),
                pkg.getReceiverNames(),
                pkg.getTrackingNumber(),
                arrivalTime,
                collectionLocation,
                pkg.getReceiverAddress()
            );
        }

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

        if (!"ARRIVED".equals(pkg.getPackageStatus())) {
            throw new RuntimeException("Package must have arrived to be collected. Current status: " +
                pkg.getPackageStatus());
        }

        if (!pkg.getReceiverIdNumber().equals(dto.getReceiverIdNumber())) {
            throw new RuntimeException("Invalid National ID. Package can only be collected by the designated receiver.");
        }

        pkg.setPackageStatus("COLLECTED");
        pkg.setCollectedAt(LocalDateTime.now());
        pkg.setCollectedByName(dto.getCollectedByName());
        pkg.setCollectedById(dto.getReceiverIdNumber());

        Package savedPackage = packageRepository.save(pkg);

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

        if (pkg.getSenderEmail() != null && !pkg.getSenderEmail().isEmpty()) {
            packageEmailService.sendPackageDeliveredEmail(
                pkg.getSenderEmail(),
                pkg.getSenderNames(),
                pkg.getTrackingNumber(),
                pkg.getReceiverNames(),
                collectionTime,
                pkg.getSenderAddress(),
                pkg.getReceiverAddress()
            );
        }

        pkg.setSenderNotifiedDelivered(true);
        packageRepository.save(pkg);

        log.info("Delivery notifications sent for package: {}", pkg.getTrackingNumber());
    }

    /**
     * Calculate package price
     */
    private BigDecimal calculatePackagePrice(Double weight, DailyTrip trip) {
        if (weight == null || weight <= 0) {
            return MINIMUM_PACKAGE_PRICE;
        }

        BigDecimal calculatedPrice = BASE_PRICE_PER_KG.multiply(new BigDecimal(weight));

        BigDecimal ticketPrice = trip.getRoute().getPrice();
        BigDecimal premium = ticketPrice.multiply(new BigDecimal("0.30"));
        calculatedPrice = calculatedPrice.add(premium);

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

    public List<PackageResponseDTO> getArrivedPackages() {
        return packageRepository.findByPackageStatus("ARRIVED").stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public List<PackageResponseDTO> getInTransitPackages() {
        return packageRepository.findByPackageStatus("IN_TRANSIT").stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public List<PackageResponseDTO> getCollectedPackages() {
        return packageRepository.findByPackageStatus("COLLECTED").stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get packages sent by current logged-in user
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
            .sorted((p1, p2) -> p2.getBookingDate().compareTo(p1.getBookingDate()))
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get packages where current user is the receiver
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
            .sorted((p1, p2) -> p2.getBookingDate().compareTo(p1.getBookingDate()))
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get package statistics for current user
     */
    public Map<String, Object> getMyPackageStatistics() {
        List<PackageResponseDTO> sentPackages = getMySentPackages();
        List<PackageResponseDTO> receivedPackages = getMyReceivedPackages();

        Map<String, Object> stats = new HashMap<>();

        stats.put("totalSent", sentPackages.size());
        stats.put("sentInTransit", sentPackages.stream()
            .filter(p -> "IN_TRANSIT".equals(p.getPackageStatus())).count());
        stats.put("sentArrived", sentPackages.stream()
            .filter(p -> "ARRIVED".equals(p.getPackageStatus())).count());
        stats.put("sentCollected", sentPackages.stream()
            .filter(p -> "COLLECTED".equals(p.getPackageStatus())).count());

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
     * Convert Package entity to DTO
     */
    private PackageResponseDTO convertToDTO(Package pkg) {
        PackageResponseDTO dto = new PackageResponseDTO();
        dto.setId(pkg.getId());
        dto.setTrackingNumber(pkg.getTrackingNumber());

        dto.setSenderNames(pkg.getSenderNames());
        dto.setSenderPhone(pkg.getSenderPhone());
        dto.setSenderAddress(pkg.getSenderAddress());

        dto.setReceiverNames(pkg.getReceiverNames());
        dto.setReceiverPhone(pkg.getReceiverPhone());
        dto.setReceiverIdNumber(pkg.getReceiverIdNumber());
        dto.setReceiverAddress(pkg.getReceiverAddress());

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