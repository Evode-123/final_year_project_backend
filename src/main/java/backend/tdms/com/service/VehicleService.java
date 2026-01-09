package backend.tdms.com.service;

import backend.tdms.com.dto.VehicleDTO;
import backend.tdms.com.model.Vehicle;
import backend.tdms.com.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    @Transactional
    public Vehicle createVehicle(VehicleDTO dto) {
        if (vehicleRepository.existsByPlateNo(dto.getPlateNo())) {
            throw new RuntimeException("Vehicle with this plate number already exists");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setPlateNo(dto.getPlateNo());
        vehicle.setCapacity(dto.getCapacity());
        vehicle.setVehicleType(dto.getVehicleType());
        vehicle.setStatus("AVAILABLE");
        vehicle.setIsActive(true);

        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public Vehicle updateVehicle(Long id, VehicleDTO dto) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        vehicle.setPlateNo(dto.getPlateNo());
        vehicle.setCapacity(dto.getCapacity());
        vehicle.setVehicleType(dto.getVehicleType());
        vehicle.setStatus(dto.getStatus());
        vehicle.setIsActive(dto.getIsActive());

        return vehicleRepository.save(vehicle);
    }

    public List<Vehicle> getAllActiveVehicles() {
        return vehicleRepository.findByIsActiveTrue();
    }

    public List<Vehicle> getAvailableVehicles() {
        return vehicleRepository.findByStatus("AVAILABLE");
    }

    public Vehicle getVehicleById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
    }

    @Transactional
    public void deleteVehicle(Long id) {
        Vehicle vehicle = getVehicleById(id);
        vehicle.setIsActive(false);
        vehicleRepository.save(vehicle);
    }
}