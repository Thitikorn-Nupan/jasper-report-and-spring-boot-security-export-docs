package com.ttknp.apiandjasper.configs;

import com.ttknp.apiandjasper.configs.jwt.JwtRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSecurity(debug = true)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    private final JwtRequestFilter jwtRequestFilter;

    @Autowired
    public SecurityConfig(JwtRequestFilter jwtRequestFilter) {
        this.jwtRequestFilter = jwtRequestFilter;
    }

    @Bean
    @Order(1) // High priority for API , If you want to add new SecurityFilterChain don't forget @Order(2) ,@Order(3),...
    public SecurityFilterChain filterChainApi(HttpSecurity httpSecurity) throws Exception {
        log.info("Configuring filterChainApi");
        // work after run auth micro
        httpSecurity
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        httpSecurity
                .csrf()
                .disable();
        // all req authen
        httpSecurity
                .cors(cors -> cors.configurationSource(corsSecureConfig())) // *** Custom cors config on security
                .securityMatcher("/api/**")  // Note, it's importance for @Order(...) This chain only matches /api/**
                .authorizeHttpRequests((authorizationManagerRequestMatcherRegistry) -> {
                    authorizationManagerRequestMatcherRegistry.requestMatchers(HttpMethod.POST,"/api/login").permitAll(); // whoever can access
                    authorizationManagerRequestMatcherRegistry.requestMatchers(HttpMethod.POST,"/api/reads-report").permitAll();
                    authorizationManagerRequestMatcherRegistry.requestMatchers(HttpMethod.POST,"/api/auth/reads-report").hasRole("ADMIN"); // Note , hasRole(...) will looking to ROLE_* as ROLE_USER,ROLE_ADMIN
                    authorizationManagerRequestMatcherRegistry.requestMatchers(HttpMethod.POST,"/api/auth/reads-report-v2").hasRole("ADMIN"); // Note , hasRole(...) will looking to ROLE_* as ROLE_USER,ROLE_ADMIN
                    authorizationManagerRequestMatcherRegistry.requestMatchers(HttpMethod.POST,"/api/auth/principle").hasAnyRole("ADMIN","USER"); // Note , hasAnyRole(...) will looking to ROLE_* as ROLE_USER,ROLE_ADMIN
                    authorizationManagerRequestMatcherRegistry.requestMatchers(HttpMethod.GET,"/api/preview-report").hasRole("ADMIN"); // Note , hasRole(...) will looking to ROLE_* as ROLE_USER,ROLE_ADMIN
                    authorizationManagerRequestMatcherRegistry.anyRequest().authenticated(); // another authenticate all
                }).httpBasic();
        httpSecurity.addFilterBefore(this.jwtRequestFilter, BasicAuthenticationFilter.class);
        return httpSecurity.build();
    }

    // Example multiple SecurityFilterChain
    /** @Bean
    @Order(2) // Work as setup another module
    public SecurityFilterChain filterChainTest(HttpSecurity httpSecurity) throws Exception {
        log.info("Configuring filterChainTest");
        // test work after run auth micro
        httpSecurity
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        httpSecurity
                .csrf()
                .disable();
        // all req authen
        httpSecurity.securityMatcher("/test/**") // This chain only matches /test/**
                .authorizeHttpRequests((authorizationManagerRequestMatcherRegistry) -> {
                    authorizationManagerRequestMatcherRegistry.requestMatchers(HttpMethod.POST,"/test/login").permitAll();
                    authorizationManagerRequestMatcherRegistry.anyRequest().hasRole("ADMIN"); // ** authenticate all and have to be admin
                }).httpBasic();
        // Add a filter to validate the tokens with every request
        httpSecurity.addFilterBefore(this.jwtRequestFilter, BasicAuthenticationFilter.class);
        return httpSecurity.build();
    }


    @Bean
    @Order(3) // Work as setup another module
    public SecurityFilterChain filterChainServer(HttpSecurity httpSecurity) throws Exception {
        log.info("Configuring filterChainServer");
        // Test work after run auth micro
        httpSecurity
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        httpSecurity
                .csrf()
                .disable();
        // All req authen
        httpSecurity.securityMatcher("/server/**") // This chain only matches /server/**
                .authorizeHttpRequests((authorizationManagerRequestMatcherRegistry) -> {
                    authorizationManagerRequestMatcherRegistry.requestMatchers(HttpMethod.POST,"/server/login").permitAll();
                    // Note , hasAuthority(...) will looking to string without prefix!!
                    authorizationManagerRequestMatcherRegistry.anyRequest().hasAuthority("admin");
                }).httpBasic();
        // Add a filter to validate the tokens with every request
        httpSecurity.addFilterBefore(this.jwtRequestFilter, BasicAuthenticationFilter.class);
        return httpSecurity.build();
    }*/


    @Bean
    public CorsConfigurationSource corsSecureConfig() {
        log.debug("Configuring addCorsMappings (secure)");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        // Allow specific origins (replace with your Angular app's URL)
        config.setAllowedOrigins(List.of("http://localhost:4200","http://thitikorn-nupan.com"));
        // Allow all methods (GET, POST, PUT, DELETE, etc.)
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Allow all headers in the request
        config.setAllowedHeaders(Collections.singletonList("*")); // Note, set only Authorization won't work
        // **Crucially, expose the headers Angular needs to read**
        config.setExposedHeaders(Arrays.asList("Authorization", "File-Name"));
        // Allow credentials (e.g., cookies, authorization headers)
        config.setAllowCredentials(true);
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
