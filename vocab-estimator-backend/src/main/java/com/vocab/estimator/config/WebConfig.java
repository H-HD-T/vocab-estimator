package com.vocab.estimator.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve chart images from the charts directory
        String chartDir = System.getProperty("user.dir") + "/charts/";
        registry.addResourceHandler("/charts/**")
                .addResourceLocations("file:" + chartDir);
    }
}
