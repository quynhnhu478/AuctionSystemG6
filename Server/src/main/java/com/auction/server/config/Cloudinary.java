package com.auction.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
@Configuration
public class Cloudinary {
    @Bean
    public com.cloudinary.Cloudinary getCloudinary(){
        Map config = new HashMap();
        config.put("cloud_name", "dt28irsgx");
        config.put("api_key", "152316636556563");
        config.put("api_secret", "gcKW3rppVIQDdcucfGQmiFQpEKI");
        config.put("secure", true);
        return new com.cloudinary.Cloudinary(config);
    }

}
