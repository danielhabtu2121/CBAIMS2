package com.bank.cims.config;

import com.bank.cims.model.User;
import com.bank.cims.repository.UserRepository;
import com.bank.cims.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtTokenAuthenticationFilter extends OncePerRequestFilter {

    @Autowired private TokenService tokenService;
    @Autowired private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && !authHeader.isBlank()) {
            TokenService.TokenClaims claims = tokenService.validateAndExtract(authHeader);
            if (claims != null) {
                String userId = claims.userId != null && !claims.userId.isBlank() ? claims.userId : claims.username;
                User user = userRepository.findById(userId)
                        .or(() -> userRepository.findByUsername(userId))
                        .orElse(null);
                if (user != null && user.isActive()) {
                    request.setAttribute("authenticatedUser", user);
                    request.setAttribute("authenticatedUserId", user.getUsername());
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user.getUsername(),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}

