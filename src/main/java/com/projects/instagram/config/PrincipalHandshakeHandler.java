// src/main/java/com/projects/instagram/config/PrincipalHandshakeHandler.java
package com.projects.instagram.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

public class PrincipalHandshakeHandler extends DefaultHandshakeHandler {
    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        Object p = attributes.get("principal");
        if (p instanceof Principal) {
            return (Principal) p;
        }
        return super.determineUser(request, wsHandler, attributes);
    }
}
