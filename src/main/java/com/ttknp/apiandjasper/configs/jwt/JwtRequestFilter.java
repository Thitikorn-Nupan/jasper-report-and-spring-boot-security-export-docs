package com.ttknp.apiandjasper.configs.jwt;

import com.ttknp.apiandjasper.entities.LoginModel;
import com.ttknp.apiandjasper.helpers.auth.UsefulAuthHelper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Map;

/**
 * The JwtRequestFilter extends the Spring Web Filter OncePerRequestFilter class.
 * For any incoming request this Filter class gets executed.
 * It checks if the request has a valid JWT token.
 * If it has a valid JWT Token then it sets the Authentication in the context,
 * to specify that the current user is authenticated.
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter { // Here, this filter class extends the OncePerRequestFilter class to guarantee(v. รับประกัน) a single execution per request.

    private static final Logger log = LoggerFactory.getLogger(JwtRequestFilter.class);
    private final JwtService jwtService;

    @Autowired
    public JwtRequestFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    // Do every time in security's api your custom filter's doFilterInternal method will still be executed even after you configure permitAll() for specific endpoints.
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = request.getHeader("Authorization");
        Map<String, Object> user = null;
        Claims claims = null;

        if (token != null) {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7); // split Bearer // token = token.split(" ")[1]; same
                try {
                    claims = jwtService.getClaimsFromToken(token);
                    // log.debug("claims: {}", claims.get("user")); // {uid=59aef92e-3d50-45d2-bdfb-19051c6df16f, password=MQ==, createBy=Admin, role=ADMIN, email=Admin@hotmail.com, username=Admin}
                    user = UsefulAuthHelper.convertObjectToMap(claims.get("user"));
                    log.debug("user {}", user);
                } catch (IllegalAccessException | SignatureException | ExpiredJwtException e) {
                    StringBuilder stringBuilder = getErrorStringBuilder(request, e, 403, "Error while parsing token");
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter()
                            .print(stringBuilder);
                    response.getWriter()
                            .flush();
                    return;
                }

                String username = (String) user.get("username");
                String email = (String) user.get("email");
                String role = (String) user.get("role");
                String createBy = (String) user.get("createBy");

                LoginModel loginModel = new LoginModel();
                loginModel.setUsername(username);
                loginModel.setEmail(email);
                loginModel.setRole(role);
                loginModel.setCreateBy(createBy);

                log.debug("authenticating user {}", loginModel);
                // *** JwtAuthenticateToken very useful with abs clas
                // abs class work will be a data after SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticateToken(loginModel,null,claims,token));
                // JwtAuthenticateToken authentication = new JwtAuthenticateToken(loginModel,null,claims,token);
                // can reduce
                SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticateToken(loginModel, null, claims, token));
            }
        }

        /**if (token == null || !token.startsWith("Bearer ")) { // if you doesn't want permitAll(...) just add return;
            StringBuilder stringBuilder = getErrorStringBuilder(request, new RuntimeException(), 401, "Error authenticating token");
            response.setStatus(401);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter()
                    .print(stringBuilder);
            response.getWriter()
                    .flush();
            return;
        }*/
        chain.doFilter(request, response); // keep continute req/res if you want your permitAll(...) working!
    }


    /**
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Return true if the filter should NOT be applied
        String servletPath = request.getServletPath();
        return servletPath.equals("/api/login") || servletPath.startsWith("/test/login");
    }
    */

    private static StringBuilder getErrorStringBuilder(HttpServletRequest request, Exception e, int status, String message) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        stringBuilder.append("\"requestURL\": \"" + request.getRequestURL().toString() + "\",");
        stringBuilder.append("\"requestMethod\": \"" + request.getMethod() + "\",");
        stringBuilder.append("\"errorMessage\": \"" + e.getMessage() + "\",");
        stringBuilder.append("\"errorClassName\": \"" + e.getClass().getName() + "\",");
        stringBuilder.append("\"status\":" + status + ",");
        stringBuilder.append("\"message\": \"" + message + "\"");
        stringBuilder.append("}");
        return stringBuilder;
    }

}