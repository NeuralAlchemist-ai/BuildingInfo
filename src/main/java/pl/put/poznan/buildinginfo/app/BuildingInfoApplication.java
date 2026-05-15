package pl.put.poznan.buildinginfo.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Building Info Spring Boot application.
 * Starts an embedded Tomcat server and registers all REST endpoints.
 */
@SpringBootApplication(scanBasePackages = "pl.put.poznan.buildinginfo")
public class BuildingInfoApplication {

    public static void main(String[] args) {
        SpringApplication.run(BuildingInfoApplication.class, args);
    }
}
