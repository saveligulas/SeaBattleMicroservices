package sg.spring.seabattle.gateway.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class BasicAuthorizationFilter extends OncePerRequestFilter {
    private final AuthorizationService authorizationService;

    public BasicAuthorizationFilter(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // quick fix to allow requests to auth without validating
        if (!request.getRequestURI().contains("api/v1/auth")) {
            String userId = request.getHeader("User-ID");
            if (userId == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User-ID Header not set");
                return;
            }

            if (!authorizationService.isAuthenticated(userId)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not authenticated");
                return;
            }

            User user = new User(userId, "", Collections.emptyList());
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(token);
        }

        filterChain.doFilter(request, response);
    }
}
