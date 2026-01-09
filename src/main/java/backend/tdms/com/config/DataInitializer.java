package backend.tdms.com.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import backend.tdms.com.model.Role;
import backend.tdms.com.repository.RoleRepository;
import backend.tdms.com.service.AuthenticationService;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuthenticationService authenticationService;

    @Override
    public void run(String... args) throws Exception {
        // Initialize roles
        String[] roles = {"ROLE_DRIVER", "ROLE_MANAGER", "ROLE_OTHER_USER", "ROLE_RECEPTIONIST", "ROLE_ADMIN"};
        for (String roleName : roles) {
            if (!roleRepository.findByName(roleName).isPresent()) {
                Role role = new Role();
                role.setName(roleName);
                roleRepository.save(role);
            }
        }

        // Create admin user
        authenticationService.createAdminOnStartup();
    }
}