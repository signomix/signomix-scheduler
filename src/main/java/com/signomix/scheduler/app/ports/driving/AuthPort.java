package com.signomix.scheduler.app.ports.driving;

import org.jboss.logging.Logger;

import com.signomix.common.Token;
import com.signomix.common.User;
import com.signomix.scheduler.app.logic.AuthLogic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AuthPort {

    @Inject
    Logger logger;

    @Inject
    AuthLogic authLogic;

    public User getUser(String token){
        return authLogic.getUserFromToken(token);
    }
}
