package com.projects.instagram.config;

import com.projects.instagram.service.UserProfileService;
import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    public SecurityConfig() {
    } // no service injected here

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
                .requestMatchers("/static/**", "/uploads/**", "/webjars/**", "/favicon.ico");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource)) // 🔥 explicitly link it here
                .authorizeHttpRequests(auth -> auth
                                // allow OPTIONS requests for CORS preflight

                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                // websocket endpoints (handshake/info)
                                .requestMatchers("/ws", "/ws/**", "/ws/info").permitAll()

                                .requestMatchers("/api/**").permitAll()          // allow all api endpoints (for now)
                                .requestMatchers("/api/files/**").permitAll()    // allow access to file-serving endpoint
                                .requestMatchers("/static/**", "/uploads/**", "/webjars/**", "/favicon.ico").permitAll()      // static profile images
                                .requestMatchers("/api/posts").permitAll()
                                .requestMatchers("/api/posts/**").authenticated()
                                .requestMatchers("/api/users/*/recommendations").permitAll()
                                .requestMatchers("/api/users/*/recommendations/**").permitAll()
                                .requestMatchers("/users/*").permitAll()
                                .requestMatchers("/login").permitAll()

                                .requestMatchers("/users/user/*").permitAll()
                                .requestMatchers("/users/*/posts").permitAll()
                                .requestMatchers("/users/*/posts/*").permitAll()
                                .requestMatchers("/posts/**").permitAll()
                                .requestMatchers("/posts/*").permitAll()
                                .requestMatchers("/*/posts").permitAll()
                                .requestMatchers("/*/posts/**").permitAll()
                                .requestMatchers("/users/email/*").permitAll()
                                .requestMatchers("/api/posts/*/*").permitAll()
                                .requestMatchers("/api/users/*/messaged-users").permitAll()
                                .requestMatchers("/api/conversations").permitAll()
                                .requestMatchers("/api/conversations/*").permitAll()
                                .requestMatchers("/api/conversations/*/messages").permitAll()
                                .requestMatchers("/api/conversations/*/messages/*").permitAll()
                                .requestMatchers("/api/conversations/*/read/*").permitAll()
                                .requestMatchers("/api/conversations/*/messages/**").permitAll()
                                .requestMatchers("/api/messages/conversations/*").permitAll()
                                .requestMatchers("/api/conversations/*/read/*").permitAll()
                                .requestMatchers("/api/conversations/*/read/*").permitAll()
                                .requestMatchers("/api/users/*/conversations/unread-count").permitAll()


                                .requestMatchers("/api/notifications").permitAll()
                                .requestMatchers("/api/users/*/notifications").permitAll()
                                .requestMatchers("/api/users/*/notifications/**").permitAll()
                                .requestMatchers("/api/users/*/notifications/unread-count").permitAll()
                                .requestMatchers("/api/users/*/notifications/*/read").permitAll()
                                .requestMatchers("/api/users/*/notifications/read-all").permitAll()
                                .requestMatchers("/api/users/*/notifications/stream").permitAll()

                                .requestMatchers("/api/following/*").permitAll()
                                .requestMatchers("/api/following/**").permitAll()
                                .requestMatchers("/api/users/*/followings/**").permitAll()

                                .requestMatchers("/api/follower/*").permitAll()
                                .requestMatchers("/api/follower/**").permitAll()
                                .requestMatchers("/api/users/*/followers/**").permitAll()

                                // allow your public endpoints
                                .requestMatchers("/login", "/register", "/public/**", "/api/otp/**", "/users", "/posts").permitAll()

                                // Lock down follow endpoints (require authentication for POST & DELETE)
                                .requestMatchers(HttpMethod.POST, "/follow/**").authenticated()
//                            .requestMatchers(HttpMethod.POST, "/api/follow/**").authenticated()
                                .requestMatchers(HttpMethod.DELETE, "/api/follow/**").authenticated()
                                .requestMatchers(HttpMethod.DELETE, "/api/posts/*/delete/*").permitAll()
                                // --- PROTECT FOLLOW ROUTES (specific matchers) ---
                                // protect DELETE /api/follow/{targetId}/remove
                                .requestMatchers(HttpMethod.DELETE, "/api/follow/*/remove").permitAll()

                                .requestMatchers(HttpMethod.POST, "/api/follow/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/posts").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/posts").permitAll() // if uploads are public
                                .requestMatchers(HttpMethod.GET, "/api/files/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/files/users/**").permitAll()
                                .requestMatchers(HttpMethod.DELETE, "/api/follow/*/remove").permitAll()
                                .requestMatchers(HttpMethod.GET, "/users/*/followers/count").permitAll()
                                .requestMatchers(HttpMethod.GET, "/users/*/followers/count").permitAll()

                                // everything else needs authentication
                                .anyRequest().authenticated()
                );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:5500",
                "http://127.0.0.1:5500",
                "http://10.91.2.29:5173",
                "http://10.91.2.29:5173/instagram-clone",
                "https://social-media-frontend-nbdo.vercel.app"

        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


    // IMPORTANT: accept UserProfileService and PasswordEncoder as method parameters
    @Bean
    public AuthenticationManager authenticationManager(UserProfileService userProfileService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userProfileService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    UserDetailsService userDetailsService( ) {
        UserDetails user1 = User.withUsername("admin")
                .password(passwordEncoder().encode("Raj@24"))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(user1);
    }

}
