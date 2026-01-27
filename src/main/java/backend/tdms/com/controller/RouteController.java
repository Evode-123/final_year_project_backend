package backend.tdms.com.controller;

import backend.tdms.com.dto.RouteDTO;
import backend.tdms.com.model.Route;
import backend.tdms.com.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/routes")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER','RECEPTIONIST','OTHER_USER')")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @PostMapping
    public ResponseEntity<Route> createRoute(@RequestBody RouteDTO routeDTO) {
        Route route = routeService.createRoute(routeDTO);
        return new ResponseEntity<>(route, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Route>> getAllActiveRoutes() {
        List<Route> routes = routeService.getAllActiveRoutes();
        return ResponseEntity.ok(routes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Route> getRouteById(@PathVariable Long id) {
        Route route = routeService.getRouteById(id);
        return ResponseEntity.ok(route);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Route> updateRoute(@PathVariable Long id, @RequestBody RouteDTO routeDTO) {
        Route route = routeService.updateRoute(id, routeDTO);
        return ResponseEntity.ok(route);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        routeService.deleteRoute(id);
        return ResponseEntity.noContent().build();
    }
}