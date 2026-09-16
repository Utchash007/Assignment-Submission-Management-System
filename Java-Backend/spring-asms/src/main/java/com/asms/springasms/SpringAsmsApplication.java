package com.asms.springasms;

import com.asms.springasms.config.DotenvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SpringAsmsApplication {

    static {
        DotenvLoader.load();
    }

    public static void main(String[] args) {
        DotenvLoader.load();
        SpringApplication.run(SpringAsmsApplication.class, args);
    }

}
