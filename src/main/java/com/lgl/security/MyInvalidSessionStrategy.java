package com.lgl.security;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.session.InvalidSessionStrategy;

public class MyInvalidSessionStrategy implements InvalidSessionStrategy {

	private LoggedInSession loggedInSession = null;

	private String destinationUrl = "";

	private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

	public MyInvalidSessionStrategy(String invalidSessionUrl, LoggedInSession loggedInSession) {
		this.destinationUrl = invalidSessionUrl;
		this.loggedInSession = loggedInSession;
	}

	@Override
	public void onInvalidSessionDetected(HttpServletRequest request, HttpServletResponse response) throws IOException {

		// needs to get old session id before creating new session
		loggedInSession.userInvalidSession(request);

		request.getSession();
		this.redirectStrategy.sendRedirect(request, response, this.destinationUrl);
	}
}
