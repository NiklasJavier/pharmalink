package de.jklein.pharmalinkclient.security;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import java.io.Serializable;

@Component
@SessionScope
public class UserSession implements Serializable {

    private static final long serialVersionUID = 2L;

    private String jwt;
    private String username;
    private String theme;

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public boolean isLoggedIn() {
        return jwt != null && !jwt.isEmpty();
    }
}