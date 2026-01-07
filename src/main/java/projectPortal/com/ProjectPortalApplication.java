package projectPortal.com;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class 	ProjectPortalApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProjectPortalApplication.class, args);
	}

}
