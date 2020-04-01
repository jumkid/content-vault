package com.jumkid.vault.security;

import com.jumkid.share.security.TokenRequestFilter;
import com.jumkid.share.security.jwt.TokenUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CommonRequestFilter extends TokenRequestFilter {

    @Autowired
    public CommonRequestFilter(RestTemplate restTemplate, TokenUser tokenUser) {
        super(restTemplate, tokenUser);
    }

}
