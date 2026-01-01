package com.ttknp.apiandjasper.configs.mvc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
/**
     Spring WebMvcConfigurer enables developers to customize the default MVC setups in Spring applications
     Request Mapping: Spring MVC maps requests to controllers using logical default patterns. Controller methods are annotated with @RequestMapping, and by default, their method names are used to match URLs.
     DispatcherServlet: This is the main component of Spring MVC. By accepting all incoming requests and forwarding them to the relevant controllers, it serves as a front controller.
     Model Binding: To make processing form submissions easier, Spring MVC automatically ties request parameters to method parameters.
     View Resolution: Spring MVC's default view resolution is intended to be adaptable. It finds the relevant view templates by combining configuration and convention.
*/
@Configuration
public class CustomWebMVCCorsNormalConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CustomWebMVCCorsNormalConfig.class);

    /// it's same @CrossOrigin() , Note, it's not the same on secure cors
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        log.debug("Configuring addCorsMappings (normal)");
        registry.addMapping("/**") // Allow all req start with /
                .allowedOrigins("http://localhost:4200","http://thitikorn-nupan.com") // Allow origins
                .allowedMethods("GET","POST","PUT","DELETE","OPTIONS") // Allow HTTP methods
                .allowedHeaders("*") // Allow all headers
                .exposedHeaders("Authorization","File-Name") // ** Importance when use httpClient in angular for getting custom headers
                .allowCredentials(true); // Allow credentials (e.g., cookies, authorization headers)

    }

}
