package OneCoin.Server.config;

import OneCoin.Server.config.auth.filter.JwtAuthenticationFilter;
import OneCoin.Server.config.auth.filter.JwtVerificationFilter;
import OneCoin.Server.config.auth.handler.*;
import OneCoin.Server.config.auth.jwt.JwtTokenizer;
import OneCoin.Server.config.auth.userdetails.Oauth2UserDetailService;
import OneCoin.Server.config.auth.utils.CustomAuthorityUtils;
import OneCoin.Server.user.mapper.UserMapper;
import OneCoin.Server.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity(debug = true)
public class SecurityConfig {
    private final JwtTokenizer jwtTokenizer;
    private final CustomAuthorityUtils customAuthorityUtils;
    private final UserService userService;
    private final UserMapper userMapper;
    @Value("${spring.client.ip}")
    private String clientURL;

    public SecurityConfig(JwtTokenizer jwtTokenizer, CustomAuthorityUtils customAuthorityUtils, UserService userService, UserMapper userMapper) {
        this.jwtTokenizer = jwtTokenizer;
        this.customAuthorityUtils = customAuthorityUtils;
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .headers().frameOptions().sameOrigin() // 동일 출처로부터 들어오는 request 만 페이지 렌더링을 허용
                .and()
                .csrf().disable()        // 일단 disable
                .cors(Customizer.withDefaults())    // corsConfigurationSource 에서 설정
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)     // 세션 생성하지 않음
                .and()
                .formLogin().disable()
                .httpBasic().disable()   // jwt 쓸거니 비활성화
                .exceptionHandling()
                .authenticationEntryPoint(new UserAuthenticationEntryPoint())  // 엔트리포인트 작업 추가
                .accessDeniedHandler(new UserAccessDeniedHandler())     // 접근 거부 핸들러 추가
                .and()
                .apply(new CustomFilterConfigurer())    // 커스텀 필터 적용
                .and()
                .authorizeHttpRequests(authorize -> authorize
                        .antMatchers(HttpMethod.GET, "/api/order/**").hasRole("USER")
                        .antMatchers(HttpMethod.POST, "/api/order/**").hasRole("USER")
                        .antMatchers(HttpMethod.GET, "/ws/chat/**").permitAll()
                        .antMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .antMatchers(HttpMethod.PATCH, "/api/users/**").hasRole("USER")
                        .antMatchers(HttpMethod.GET, "/api/users").permitAll()
                        .antMatchers(HttpMethod.GET, "/api/users/**").permitAll()
                        .antMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("USER")
                        .antMatchers(HttpMethod.POST, "/api/admin/**").hasRole("ADMIN")
                        .antMatchers(HttpMethod.DELETE, "/api/admin/**").hasRole("ADMIN")
                        .anyRequest().permitAll()
                )
                .oauth2Login()
                .successHandler(new UserOAuth2SuccessHandler(jwtTokenizer, customAuthorityUtils, userService, userMapper, clientURL))
                .userInfoEndpoint()     // Oauth2 로그인 성공 후 userInfo 엔드포인트(userInfo 받아옴)
                .userService(new Oauth2UserDetailService(userService, jwtTokenizer, customAuthorityUtils, userMapper));  // 리소스 서버에서 사용자 정보를 가져온 상태에서 추가로 진행
        return http.build();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    // Cors 정책 설정
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));   // 오리진 허용
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));  // 허용 메소드 설정
        configuration.setAllowedHeaders(List.of("*", "connection", "upgrade"));     // 요청 헤더 허용 설정
        configuration.setAllowCredentials(true);        // 크레덴셜 지원 설정
        configuration.setExposedHeaders(List.of("*"));       // 응답 헤더 허용 설정

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();   // CorsConfigurationSource 구현 클래스
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // 커스텀 필터 설정
    public class CustomFilterConfigurer extends AbstractHttpConfigurer<CustomFilterConfigurer, HttpSecurity> {
        @Override
        public void configure(HttpSecurity builder) throws Exception {
            AuthenticationManager authenticationManager = builder.getSharedObject(AuthenticationManager.class);

            JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(authenticationManager, jwtTokenizer, userService);  // JwtAuthenticationFilter 를 생성하면서 JwtAuthenticationFilter 에서 사용되는 AuthenticationManager 와 JwtTokenizer 를 DI
            jwtAuthenticationFilter.setFilterProcessesUrl("/api/auth/login");          // 로그인 url 변경

            // 성공, 실패 핸들러 추가, 일반적으로 인증을 위한 필터마다 성공, 실패 구현 클래스를 각각 생성할거라 DI 하지 않아도 무방
            jwtAuthenticationFilter.setAuthenticationSuccessHandler(new UserAuthenticationSuccessHandler());
            jwtAuthenticationFilter.setAuthenticationFailureHandler(new UserAuthenticationFailureHandler());

            JwtVerificationFilter jwtVerificationFilter = new JwtVerificationFilter(jwtTokenizer, customAuthorityUtils, userService);

            // 커스텀한 필터를 필터 체인에 추가
            builder.addFilter(jwtAuthenticationFilter)
                    .addFilterAfter(jwtVerificationFilter, JwtAuthenticationFilter.class)
                    .addFilterAfter(jwtVerificationFilter, OAuth2LoginAuthenticationFilter.class);
        }
    }
}
