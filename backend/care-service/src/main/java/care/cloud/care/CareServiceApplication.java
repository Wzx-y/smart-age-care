package care.cloud.care;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CareServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CareServiceApplication.class, args);
    }
}
