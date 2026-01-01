package com.ttknp.apiandjasper.configs.jwt;

import com.ttknp.apiandjasper.entities.LoginModel;
import com.ttknp.apiandjasper.helpers.auth.UsefulAuthHelper;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import java.util.Date;
import java.util.function.Function;

// @Service // Note !! you can call JwtAuthenticateToken class tru SecurityContextHolder as : (JwtAuthenticateToken) SecurityContextHolder.getContext().getAuthentication() it will give this class
public class JwtAuthenticateToken extends AbstractAuthenticationToken {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticateToken.class);
    private final LoginModel loginModel;
    private final Object credentials;
    private final Claims claim;
    private final String token;
    private final String subject;

    public JwtAuthenticateToken(LoginModel loginModel, Object credentials, Claims claims, String token) {
        super(UsefulAuthHelper.convertRolesStringToGrantedAuthorityList(loginModel.getRole())); // role
        this.loginModel = loginModel;
        this.credentials = credentials; // can be null
        this.subject = null;
        this.claim = claims;
        this.token = token;
        super.setAuthenticated(true);
    }

    public JwtAuthenticateToken(Claims claims, String token) {
        super(null);
        this.credentials = null;
        this.loginModel = null;
        this.subject = null;
        this.claim = claims;
        this.token = token;
        super.setAuthenticated(true);
    }

    public Claims getClaim() {
        return claim;
    }

    public String getToken() {
        return token;
    }

    public String getSubject() {
        return subject;
    }

    // not working
    public String getSubjectFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public Date getIssueFromToken(String token) {
        return getClaimFromToken(token, Claims::getIssuedAt);
    }

    public Date getExpirationFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    private  <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        // log.debug("this.getClaim() {}", this.getClaim()); // {exp=1747977424, user={password=MQ==, createBy=Admin, role=ADMIN, uuid=59aef92e-3d50-45d2-bdfb-19051c6df16f, email=Admin@hotmail.com, username=Admin}, iat=1747973824}
        // log.debug("claimsResolver.apply(claim) {}", claimsResolver.apply(claim));
        return claimsResolver.apply(claim);
    }

    /**
    ก่อนอื่นเราต้องรู้จักกับของ 5 อย่างที่สำคัญของ AutheticationToken นั้นก็คือ
    Principal -> เป็นของที่เราเอาไว้ระบุตัวตนเช่น name , email, id
    GrantedAuthorities -> เป็นของที่เอาไว้บอกสิทธิ์การเข้าถึงเช่น Roles
    isAuthenticated -> คือ Flag ที่เอาไว้บอกว่า Authenticated แล้ว
    Detail -> เป็นที่เก็บข้อมูลเพิ่มเติม คำอธิบายเพิ่มเติม
    Credentials -> password
    */
    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public Object getPrincipal() {
        return this.loginModel;
    }

}
