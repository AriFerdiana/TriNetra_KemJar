package com.smartwaste.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;


@Component
public class AuthenticationEventListener {

    @Autowired
    private LoginAttemptService loginAttemptService;

    @EventListener
    public void authenticationFailed(AuthenticationFailureBadCredentialsEvent event) {
        String email = (String) event.getAuthentication().getPrincipal();
        loginAttemptService.loginFailed(email);
    }

    @EventListener
    public void authenticationSucceeded(AuthenticationSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof UserDetails) {
            UserDetails user = (UserDetails) event.getAuthentication().getPrincipal();
            loginAttemptService.loginSucceeded(user.getUsername());
        } else if (event.getAuthentication().getPrincipal() instanceof String) {
            loginAttemptService.loginSucceeded((String) event.getAuthentication().getPrincipal());
        }
    }
}
