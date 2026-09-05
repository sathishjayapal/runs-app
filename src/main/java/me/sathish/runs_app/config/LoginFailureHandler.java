package me.sathish.runs_app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                       AuthenticationException exception) throws IOException, ServletException {

        String errorCode = "INVALID_CREDENTIALS";
        String errorMessage = "Invalid username or password. Please try again.";

        if (exception instanceof UsernameNotFoundException) {
            log.warn("Login failed: user not found for request from {}", request.getRemoteAddr());
            // Don't leak that user doesn't exist - use generic message
        } else if (exception instanceof BadCredentialsException) {
            log.warn("Login failed: bad credentials for request from {}", request.getRemoteAddr());
        } else {
            log.error("Login failed with unexpected exception: {}", exception.getClass().getSimpleName(), exception);
            errorCode = "SERVER_ERROR";
            errorMessage = "Unable to process login. Please try again later.";
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", errorCode);
        errorResponse.put("message", errorMessage);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
