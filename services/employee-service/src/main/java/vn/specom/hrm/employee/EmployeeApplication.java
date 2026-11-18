package vn.specom.hrm.employee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// Scans vn.specom.hrm so the shared beans of hrm-common (PermissionChecker, exception handler, trace filter) load.
@SpringBootApplication(scanBasePackages = "vn.specom.hrm")
@ConfigurationPropertiesScan
public class EmployeeApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmployeeApplication.class, args);
    }
}
