package com.jumkid.vault.config;

import com.jumkid.share.security.BearerTokenRequestFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${jwt.token.enable}")
    private boolean enableTokenCheck;

    @Value("${jwt.token.introspect.url}")
    private String tokenIntrospectUrl;

    private final RestTemplate restTemplate;

    @Autowired
    public SecurityConfig(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/v2/api-docs",
                                "/configuration/ui",
                                "/swagger-resources/**",
                                "/configuration/security",
                                "/swagger-ui.html",
                                "/webjars/**").permitAll() //whitelist
                .antMatchers("/**").authenticated() //any other request
                .and().sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .csrf().disable();  // enable this if the authorization service exposure to public

        http.addFilterBefore(new BearerTokenRequestFilter(enableTokenCheck, tokenIntrospectUrl, restTemplate),
                UsernamePasswordAuthenticationFilter.class);

        CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
        encodingFilter.setEncoding(StandardCharsets.UTF_8.toString());
        encodingFilter.setForceEncoding(true);
        http.addFilterBefore(encodingFilter, CsrfFilter.class);
    }

}
