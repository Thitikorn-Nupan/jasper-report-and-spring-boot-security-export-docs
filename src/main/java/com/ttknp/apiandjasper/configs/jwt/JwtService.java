package com.ttknp.apiandjasper.configs.jwt;

import com.ttknp.apiandjasper.entities.LoginModel;
import com.ttknp.apiandjasper.helpers.auth.UsefulAuthHelper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@PropertySource(value="classpath:info/jwt.properties", ignoreResourceNotFound=true)
public class JwtService {

    private final Long validityPeriod;
    private final String secretKey;

    @Autowired
    public JwtService(Environment environment) {
        this.secretKey = environment.getProperty("jwt.secret.key");
        this.validityPeriod = Long.parseLong(Objects.requireNonNull(environment.getProperty("jwt.validity.period")));
    }

    // for generate token
    public  <T extends LoginModel> JwtBuilder generateToken(String id, T object) {
        Map<String, Object> claims = new HashMap<>(); // for payload
        Map<String,Object> user = new HashMap<>();

        user.put("username", object.getUsername());
        user.put("password", object.getPassword());
        user.put("email", object.getEmail());
        user.put("createBy", object.getCreateBy());
        user.put("role", object.getRole());
        claims.put("user", user);

        // Let's set the JWT Claims
        JwtBuilder jwtBuilder = Jwts.builder();
        jwtBuilder.setId(id); // can be null
        jwtBuilder.setSubject(object.getUsername()); // Subject is combination of the username
        jwtBuilder.setClaims(claims); // data for payload
        jwtBuilder.setIssuedAt(new Date());
        jwtBuilder.setExpiration(new Date(System.currentTimeMillis() + validityPeriod)); // plus 1 hour
        // The SignatureAlgorithm.HS512 is JWT signature algorithm we will be using to sign the token , We will sign our JWT with our ApiKey secret (256 bits)
        jwtBuilder.signWith(SignatureAlgorithm.HS512, UsefulAuthHelper.convertSecretKeyStringToKey(secretKey));
        /**
        // payload will be
        {
              "exp": 1747921802,
              "user": {
                "uid": "59aef92e-3d50-45d2-bdfb-19051c6df16f",
                "password": "12345",
                "createBy": "Admin",
                "role": "ADMIN",
                "email": "Admin@hotmail.com",
                "username": "Admin"
              },
              "iat": 1747918202
        }
        */
        return jwtBuilder;
    }

    // Decode a token
    public Claims getClaimsFromToken(String token) {
        Key key = UsefulAuthHelper.convertSecretKeyStringToKey(secretKey);
        return Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(token)
                .getBody();
    }

    public String getSubjectFromToken(String token) {
        Key key = UsefulAuthHelper.convertSecretKeyStringToKey(secretKey);
        return Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
