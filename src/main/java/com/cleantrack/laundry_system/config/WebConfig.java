package com.cleantrack.laundry_system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves uploaded bank receipts straight from the "uploads" folder, so a receipt can be opened as soon as it
 * is uploaded without restarting the application. Receipts uploaded by older versions (saved under
 * src/main/resources/static/uploads) are still served from the classpath.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/", "classpath:/static/uploads/");
    }
}
