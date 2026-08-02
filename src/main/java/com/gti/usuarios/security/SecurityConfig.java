package com.gti.usuarios.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final IntegracaoApiKeyFilter integracaoApiKeyFilter;

    public SecurityConfig(JwtFilter jwtFilter, IntegracaoApiKeyFilter integracaoApiKeyFilter) {
        this.jwtFilter = jwtFilter;
        this.integracaoApiKeyFilter = integracaoApiKeyFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/", "/*.html", "/*.css", "/*.js",
                                "/ativos/**", "/chamados/**", "/usuarios/**", "/configuracoes/**").permitAll()
                        .requestMatchers("/api/integracao/preview/**").hasRole("TI")
                        .requestMatchers("/api/integracao/**").permitAll()
                        .requestMatchers("/api/usuarios/**", "/api/tipos-ativo/**",
                                "/api/dashboard/**", "/api/relatorios/**").hasRole("TI")
                        .requestMatchers(HttpMethod.POST,   "/api/ativos/**").hasRole("TI")
                        .requestMatchers(HttpMethod.PUT,    "/api/ativos/**").hasRole("TI")
                        .requestMatchers(HttpMethod.DELETE, "/api/ativos/**").hasRole("TI")
                        .requestMatchers(HttpMethod.GET,    "/api/ativos/*/impacto-exclusao").hasRole("TI")
                        .requestMatchers("/api/ativos/**", "/api/chamados/**").authenticated()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(integracaoApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public FilterRegistrationBean<IntegracaoApiKeyFilter> integracaoApiKeyFilterRegistration(IntegracaoApiKeyFilter filter) {
        FilterRegistrationBean<IntegracaoApiKeyFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false); // desabilita o registro automático; usado só via addFilterBefore
        return reg;
    }

    @Bean
    public FilterRegistrationBean<JwtFilter> jwtFilterRegistration(JwtFilter filter) {
        FilterRegistrationBean<JwtFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false); // desabilita o registro automático; usado só via addFilterBefore
        return reg;
    }
}