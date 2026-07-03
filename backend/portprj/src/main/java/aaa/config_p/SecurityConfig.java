package aaa.config_p;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    /**
     * Spring Security 기본 설정
     *
     * 현재(개발 단계)
     *  - CSRF 비활성화
     *  - CORS 허용
     *  - 모든 API 접근 허용
     *
     * 추후(로그인 구현 시)
     *  - JWT Filter 추가
     *  - Role(권한) 설정
     *  - 인증이 필요한 API 설정
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // CSRF(Cross Site Request Forgery) 공격 방지 기능
                // REST API 개발 시에는 보통 사용하지 않으므로 비활성화
                .csrf(csrf -> csrf.disable())

                // CORS 정책 적용
                // 아래 corsConfigurationSource() 설정을 사용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 요청별 접근 권한 설정
                .authorizeHttpRequests(auth ->
                        auth
                                // 현재는 개발 단계이므로 모든 API 허용
                                .requestMatchers("/api/**").permitAll()

                                // 그 외 요청도 모두 허용
                                .anyRequest().permitAll()
                );

        return http.build();
    }


    /**
     * CORS(Cross-Origin Resource Sharing) 설정
     *
     * Vue(5173) → Spring Boot(8080)
     * 서로 다른 포트에서 통신할 수 있도록 허용
     */

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        // 허용할 Origin (Vue 개발 서버)
        config.setAllowedOrigins(List.of("http://localhost:5173"));

        // 허용할 HTTP Method
        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "OPTIONS"
        ));

        // 모든 Header 허용
        config.setAllowedHeaders(List.of("*"));

        // Session / Cookie / JWT 등 인증정보 전달 허용
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        // 모든 요청에 위 CORS 정책 적용
        source.registerCorsConfiguration("/**", config);

        return source;
    }

}