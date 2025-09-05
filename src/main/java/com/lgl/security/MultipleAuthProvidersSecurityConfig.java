package com.lgl.security;

import static org.springframework.http.HttpMethod.GET;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.session.InvalidSessionStrategy;
import org.springframework.security.web.session.SimpleRedirectInvalidSessionStrategy;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@EnableWebSecurity
public class MultipleAuthProvidersSecurityConfig extends WebSecurityConfigurerAdapter {
	@Autowired
	CustomAuthenticationProvider customAuthProvider;

	@Autowired
	MyAuthenticationSuccessFailureHandler myAuthenticationSuccessFailureHandler;
	
	@Autowired 
	LoggedInSession loggedInSession;
	
	@Override
	public void configure(AuthenticationManagerBuilder auth) throws Exception {

		auth.authenticationProvider(customAuthProvider);
		
		/*
		auth.inMemoryAuthentication().withUser("memuser").password(passwordEncoder().encode("pass1")).roles("USER")
									 .and()
									 .withUser("admin").password(passwordEncoder().encode("pass1")).roles("ADMIN");
		*/
		// expose inMemoryUserDetailsManager as a UserDetailService bean
		auth.userDetailsService(inMemoryUserDetailsManager());
	}

	@Bean
	public InMemoryUserDetailsManager inMemoryUserDetailsManager()
	{
		List<UserDetails> userDetailsList = new ArrayList<>();
		userDetailsList.add(User.withUsername("memuser").password(passwordEncoder().encode("pass1"))
				.roles("USER").build());
		userDetailsList.add(User.withUsername("admin").password(passwordEncoder().encode("pass1"))
				.roles("ADMIN").build());
		userDetailsList.add(User.withUsername("employee").password(passwordEncoder().encode("pass1"))
				.roles("EMPLOYEE", "USER").build());
		userDetailsList.add(User.withUsername("manager").password(passwordEncoder().encode("pass1"))
				.roles("MANAGER", "ADMIN", "USER").build());

		return new InMemoryUserDetailsManager(userDetailsList);
	}
	
	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().antMatchers("/static/**", "/js/**", "/css/**");
		
		super.configure(web);
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		
		//http.httpBasic().and().authorizeRequests().antMatchers("/**").authenticated();
		http
			// set SameSite=None for JSESSIONID
			// Note: add after BasicAuthenticationFilter doesn't work for 302 redirect
			//.addFilterAfter(new SessionCookieFilter(), BasicAuthenticationFilter.class)
			/* 
			 * Note: cookie filter MUST be added before ChannelProcessingFilter 
			 * in order to work for 302 redirect as well
			 */
			//.addFilterBefore(new SessionCookieFilter(), BasicAuthenticationFilter.class)
			
			// use sessionRegistry bean to get all active users
		 	.sessionManagement()
		 	.maximumSessions(5).sessionRegistry(sessionRegistry());
		
		http
			// in case of invalid session, redirect to /api/session/invalid which return status 401
			.sessionManagement()
			.invalidSessionStrategy(new MyInvalidSessionStrategy("/api/session/invalid", loggedInSession)).and()
			//.invalidSessionUrl("/api/session/invalid").and()
			//.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED).and()
			.cors().and()
			.csrf().disable()
			.rememberMe().and()
			.authorizeRequests()
			.antMatchers("/*login*", "/index.html*", "/api/session/invalid", "/api/login/success", 
					"/qr-code", "/qr-code/sessionid", "/qr-code/loginStatus", "/api/login/activeUsers", "/api/login/myActiveUsers", 
					"/ws/**", "/stompTest").permitAll()
			.antMatchers(HttpMethod.POST, "/api/tutorials*").hasRole("ADMIN")
			.antMatchers(HttpMethod.PUT, "/api/tutorials*").hasRole("ADMIN")
			.antMatchers(HttpMethod.DELETE, "/api/tutorials*").hasRole("ADMIN")
			.anyRequest().authenticated()
			.and()
			
			// # 1. use httpBaic authentication
			//.httpBasic();
			
			// # 2. use spring security default login/logout page
			//.formLogin().defaultSuccessUrl("/").and().logout().logoutUrl("/logout");
		
			// # 3. copy the source to static html file on Vue side (mylogin.html)
			//.formLogin().loginProcessingUrl("/login").loginPage("/mylogin.html").defaultSuccessUrl("/")
			//.and().logout().logoutUrl("/logout").logoutSuccessUrl("/mylogin.html?logout");

			// # 4. create own vue login page on Vue side (Login.vue)
			.formLogin().loginProcessingUrl("/login").loginPage("/index.html")
			//.defaultSuccessUrl("/api/login/success")
			.successHandler(myAuthenticationSuccessFailureHandler)
			.failureHandler(myAuthenticationSuccessFailureHandler)
			//.failureUrl("/index.html?error")
			
			.and().logout().logoutUrl("/logout")
			//.logoutSuccessUrl("/index.html?logout");
			.addLogoutHandler((request, response, authentication) -> {
				// this logout handler will be called first, at which time the session id is still the orignial one before logged out
				loggedInSession.userLoggedOut(request, response, authentication);
			})
			.logoutSuccessHandler((request, response, authentication) -> {
				/*
				 * Note: at this point, current session is no longer valid after logout,, so we
				 * return a new session here (Response Cookie: JSESSIONID) otherwise, 
				 * redirect will get "invalid session id" error
				 */
				request.getSession(true);
                response.setStatus(HttpServletResponse.SC_OK);
            });

	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowCredentials(true);
		configuration.setAllowedOrigins(Arrays.asList("http://localhost:8080", "http://localhost:8082", "http://192.168.0.14:8080", "http://192.168.0.14:8082", "https://localhost:8443", "https://192.168.0.14:8443", "https://192.168.0.14:8082", "https://mygame-6zac4xqw4q-pd.a.run.app", "http://mygame-6zac4xqw4q-pd.a.run.app:8080"));
		configuration.setAllowedMethods(Arrays.asList("*"));
		configuration.addAllowedHeader("*");
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
	
	@Bean
	public SessionRegistry sessionRegistry() {
	    return new SessionRegistryImpl();
	}
	
	@Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

  /* @Bean
    public EmbeddedServletContainerCustomizer containerCustomizer() {

        return new EmbeddedServletContainerCustomizer() {
            @Override
            public void customize(ConfigurableEmbeddedServletContainer container) {

                ErrorPage error404Page = new ErrorPage(HttpStatus.NOT_FOUND, "/index.html");

                container.addErrorPages(error404Page);
            }
        };
    }
    */
}
