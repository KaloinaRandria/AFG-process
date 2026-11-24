package afg.process.afgprocess;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class AfgProcessApplication {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Indian/Antananarivo"));
    }

    public static void main(String[] args) {
        SpringApplication.run(AfgProcessApplication.class, args);
    }

}
