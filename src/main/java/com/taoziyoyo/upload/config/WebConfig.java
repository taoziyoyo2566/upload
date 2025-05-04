package com.taoziyoyo.upload.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Add simple URL mappings without controller code
        registry.addViewController("/").setViewName("redirect:/files");
        registry.addViewController("/privacy").setViewName("privacy");
        registry.addViewController("/terms").setViewName("terms");
        registry.addViewController("/contact").setViewName("contact");
    }
}