package org.tiltedwindmills.fantasy.zealots;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"org.tiltedwindmills.fantasy.zealots", "org.tiltedwindmills.fantasy.mfl.services" })
public class ZealotsStatisticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZealotsStatisticsApplication.class, args);
    }
}
