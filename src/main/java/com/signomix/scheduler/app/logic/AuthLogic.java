package com.signomix.scheduler.app.logic;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.db.AuthDaoIface;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.UserDaoIface;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Klasa zawierająca logikę biznesową dotyczącą autoryzacji.
 * 
 * @author Grzegorz
 */
@ApplicationScoped
public class AuthLogic {
    private static final Logger logger = Logger.getLogger(AuthLogic.class);

    // TODO: move to config
    private long sessionTokenLifetime = 30; // minutes
    private long permanentTokenLifetime = 10 * 365 * 24 * 60; // 10 years in minutes

    @Inject
    @DataSource("oltp")
    AgroalDataSource tsDs;

    AuthDaoIface authDao;
    UserDaoIface userDao;

    @ConfigProperty(name = "questdb.client.config")
    String questDbConfig;

    void onStart(@Observes StartupEvent ev) {
            authDao = new com.signomix.common.tsdb.AuthDao();
            authDao.setDatasource(tsDs, questDbConfig);
            userDao = new com.signomix.common.tsdb.UserDao();
            userDao.setDatasource(tsDs);
    }

    public String getUserId(String token) {
        return authDao.getUserId(token, sessionTokenLifetime, permanentTokenLifetime);
    }

    public User getUserFromToken(String token) {
        try {
            return userDao.getUser(getUserId(token));
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            return null;
        }
    }

}
