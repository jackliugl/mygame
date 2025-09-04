package com.lgl.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface MyWebSocketService {

    // keep track of the associated service for each session
    public static Map<String, MyWebSocketService> sessionServiceMap = new ConcurrentHashMap<String, MyWebSocketService>();

    public void leave(String sessionID);
}
