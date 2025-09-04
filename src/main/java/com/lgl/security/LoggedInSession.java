package com.lgl.security;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;

@Component
public class LoggedInSession {

	@Autowired
	InMemoryUserDetailsManager inMemoryUserDetailsManager;
	
	@Autowired
	AuthenticationManager authenManager;
	
	// shared map to store all logged-in users
	static Map<String, Authentication> loggedInSessionUserMap = new ConcurrentHashMap<String, Authentication>();

	public void userLoggedIn() {

		String sessionID = RequestContextHolder.currentRequestAttributes().getSessionId();
		Authentication authInfo = SecurityContextHolder.getContext().getAuthentication();

		System.out.println(String.format("userLoggedIn - sessionID: %s, username: %s", sessionID, authInfo.getName()));
		
		loggedInSessionUserMap.put(sessionID, authInfo);
	}

	public void userLoggedOut(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {

		String sessionID = RequestContextHolder.currentRequestAttributes().getSessionId();
		Authentication authInfo = SecurityContextHolder.getContext().getAuthentication();
		
		if (loggedInSessionUserMap.containsKey(sessionID)) {
			String userName = (authInfo != null) ? authInfo.getName() : "";
			
			Assert.isTrue(userName.equals(loggedInSessionUserMap.get(sessionID).getName()), () -> "username doesn't match!");
			
			System.out.println(String.format("userLoggedOut - sessionID: %s, username: %s", sessionID, userName));
	
			loggedInSessionUserMap.remove(sessionID);
		}		
	}
	
	public void userInvalidSession(HttpServletRequest request) {

		//String sessionID = RequestContextHolder.currentRequestAttributes().getSessionId();
		String sessionID = request.getRequestedSessionId();

		if (loggedInSessionUserMap.containsKey(sessionID)) {
			String userName = loggedInSessionUserMap.get(sessionID).getName();
			
			System.out.println(String.format("userInvalidSession - sessionID: %s, username: %s", sessionID, userName));
	
			loggedInSessionUserMap.remove(sessionID);
		}
	}

	public String userLoggedInByQRCode(String qrSessionID) {

		String sessionID = RequestContextHolder.currentRequestAttributes().getSessionId();
		Authentication authInfo = SecurityContextHolder.getContext().getAuthentication();

		System.out.println(String.format("userLoggedInByQRCode - sessionID: %s, username: %s, qrSessionID: %s", sessionID, authInfo.getName(), qrSessionID));

		loggedInSessionUserMap.put(qrSessionID, authInfo);
		
		return authInfo.getName();
	}
	
	/*
	 * check if current session id is already logged in, if yes, updated security context and return true
	 * otherwise return false
	 */
	public boolean checkIfQRLoggedIn() {

		String sessionID = RequestContextHolder.currentRequestAttributes().getSessionId();
		Authentication authInfo = SecurityContextHolder.getContext().getAuthentication();

		String userName = (authInfo != null) ? authInfo.getName() : "";
		if (!userName.equals("") && !userName.equalsIgnoreCase("anonymousUser")) {
			// current session already has user info
			System.out.println(String.format("checkIfQRLoggedIn - current session %s already logged in as %s.", sessionID, userName));
			return true;
		}

		Authentication qrAuthInfo = loggedInSessionUserMap.get(sessionID);
		if (qrAuthInfo != null) {
			
			// option 1 - set Authentication object directly (it works, but Spring doesn't track this session properly)
			//SecurityContextHolder.getContext().setAuthentication(qrAuthInfo);
			
			// option 2 - look up username and authorities
			UserDetails user = inMemoryUserDetailsManager.loadUserByUsername(qrAuthInfo.getName());
			Authentication authentication =  new UsernamePasswordAuthenticationToken(user.getUsername(), "", user.getAuthorities());
			SecurityContextHolder.getContext().setAuthentication(authentication);
			
			System.out.println(String.format("checkIfQRLoggedIn - sessionID: %s, SecurityContext updated to username: %s", sessionID, qrAuthInfo.getName()));
			return true;
		}
		
		return false;
	}
	
	public List<String> allLoggedInUsers() {

		return loggedInSessionUserMap.values().stream().map(x -> x.getName()).collect(Collectors.toList());
	}
}
