package me.sathish.runs_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class RunsAppApplication {

    public static void main(final String[] args) {
        System.out.println("RunsAppApplication.main() called");
        SpringApplication.run(RunsAppApplication.class, args);
    }

}
