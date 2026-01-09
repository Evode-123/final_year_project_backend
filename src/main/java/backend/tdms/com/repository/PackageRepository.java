package backend.tdms.com.repository;

import backend.tdms.com.model.DailyTrip;
import backend.tdms.com.model.Package;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PackageRepository extends JpaRepository<Package, Long> {

    Optional<Package> findByTrackingNumber(String trackingNumber);

    List<Package> findByDailyTrip(DailyTrip dailyTrip);

    List<Package> findBySenderPhone(String senderPhone);

    List<Package> findByReceiverPhone(String receiverPhone);

    List<Package> findByPackageStatus(String packageStatus);

    @Query("SELECT p FROM Package p WHERE p.senderPhone = :phone OR p.receiverPhone = :phone")
    List<Package> findByPhone(@Param("phone") String phone);

    @Query("SELECT p FROM Package p WHERE p.packageStatus = 'ARRIVED' ORDER BY p.actualArrivalTime DESC")
    List<Package> findArrivedPackages();

    @Query("SELECT p FROM Package p WHERE p.packageStatus = 'IN_TRANSIT' ORDER BY p.expectedArrivalTime ASC")
    List<Package> findInTransitPackages();

    @Query("SELECT p FROM Package p WHERE p.dailyTrip.id = :tripId AND p.packageStatus IN ('IN_TRANSIT', 'ARRIVED')")
    List<Package> findActivePackagesByTrip(@Param("tripId") Long tripId);

    @Query("SELECT COUNT(p) FROM Package p WHERE p.dailyTrip.id = :tripId AND p.packageStatus != 'CANCELLED'")
    Long countPackagesByTrip(@Param("tripId") Long tripId);

    @Query("SELECT p FROM Package p WHERE p.packageStatus = 'ARRIVED' " +
           "AND p.actualArrivalTime < CURRENT_TIMESTAMP " +
           "AND p.receiverNotifiedArrived = false")
    List<Package> findArrivedButNotNotified();
}