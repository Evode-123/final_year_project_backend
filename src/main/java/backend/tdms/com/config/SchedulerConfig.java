package backend.tdms.com.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulerConfig {
    // This enables the @Scheduled annotation in TripGenerationService
}