package backend.tdms.com;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
		System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║   🚀 TDMS Application Started Successfully!            ║");
        System.out.println("║   ✅ Payment Auto-Verification: ENABLED                 ║");
        System.out.println("║   ⏰ Checking payments every 15 seconds                ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
	}

}
