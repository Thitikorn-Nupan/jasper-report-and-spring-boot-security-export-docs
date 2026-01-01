package com.ttknp.apiandjasper.helpers.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UsefulAuthHelper {

    public static UUID genNewUuID() {
        return UUID.randomUUID();
    }

    public static UUID convertStringToUuid(String uuid) {
        return UUID.fromString(uuid);
    }

    public static List<GrantedAuthority> convertRolesStringToGrantedAuthorityList(String rolesString) {
      return AuthorityUtils.createAuthorityList(rolesString)  ;
    }

    public static Key convertSecretKeyStringToKey(String secretKey) {
        byte[] decodedKey = Base64.getEncoder().encode(secretKey.getBytes());
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "HmacSHA256");
    }

    // Base64 Generate
    public static String convertStringToBase64String(String passwordString) {
        return Base64.getEncoder().encodeToString(passwordString.getBytes(StandardCharsets.UTF_8));
    }

    // Base64 Encoding
    public static Boolean validatePasswordStringWithPasswordBase64(String passwordString, String passwordBase64) {
        String endcodePasswordString = Base64.getEncoder().encodeToString(passwordString.getBytes()); // endcode password that user input // focus getByte()
        return endcodePasswordString.equals(passwordBase64);
    }

    // BCrypt Generate
    public static String convertStringToBCryptString(String passwordString) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.encode(passwordString);
    }

    // BCrypt Encoding
    public static Boolean validatePasswordStringWithPasswordBCrypt(String passwordString, String passwordBCrypt) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.matches(passwordString, passwordBCrypt);
    }

    // When converting an Object into a Map, Jackson provides multiple approaches. Jackson is a versatile library renowned for its excellent support of various conversion types, such as JSON or XML.
    public static Map<String, Object> convertObjectToMap(Object object) throws IllegalAccessException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.convertValue(object, new TypeReference<Map<String, Object>>() {});
    }


}
