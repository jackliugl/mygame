package com.lgl.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

@Component
public class MyAuthenticationSuccessFailureHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

	@Autowired 
	LoggedInSession loggedInSession;
	
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		
		String sessionID = RequestContextHolder.currentRequestAttributes().getSessionId();
		System.out.println("onAuthenticationSuccess - session ID: " + sessionID);
		response.setHeader(HttpHeaders.SET_COOKIE, String.format("JSESSIONID=%s; Path=/; Secure; HttpOnly; SameSite=None; Jack=test;", sessionID));
		
		loggedInSession.userLoggedIn();
		
		// you can redirect here, the above SET-COOKIE will be in the 302 response
		//response.sendRedirect("/");
	}

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		
		String sessionID = RequestContextHolder.currentRequestAttributes().getSessionId();
		System.out.println("onAuthenticationFailure - session ID: " + sessionID);
		response.setHeader(HttpHeaders.SET_COOKIE, String.format("JSESSIONID=%s; Path=/; Secure; HttpOnly; SameSite=None; Jack=test;", sessionID));
		response.sendRedirect("/index.html?error");
	}	
}
