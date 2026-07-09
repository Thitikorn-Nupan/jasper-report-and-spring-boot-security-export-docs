package com.ttknp.apiandjasper.controllers;

import com.ttknp.apiandjasper.configs.jwt.JwtService;
import com.ttknp.apiandjasper.entities.FileType;
import com.ttknp.apiandjasper.entities.LoginModel;
import com.ttknp.apiandjasper.entities.LoginRequest;
import com.ttknp.apiandjasper.entities.LoginResponse;
import com.ttknp.apiandjasper.helpers.auth.UsefulAuthHelper;
import com.ttknp.apiandjasper.helpers.jwt.JwtSpringSecurityContextHelper;
import com.ttknp.apiandjasper.services.OrderItemService;
import io.jsonwebtoken.JwtBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.CONTENT_DISPOSITION;

@RestController
@RequestMapping(value = "/api/")
public class ApiControl {

    private static final Logger log = LoggerFactory.getLogger(ApiControl.class);
    private final JwtService jwtService;
    private final OrderItemService orderItemService;

    @Autowired
    public ApiControl(JwtService jwtService, OrderItemService orderItemService) {
        this.jwtService = jwtService;
        this.orderItemService = orderItemService;
    }

    /// LoginRequest for client req
    @PostMapping("/login")
    private LoginResponse demoLogin(@RequestBody LoginRequest loginRequest) {
        LoginResponse loginResponse = new LoginResponse();
        // getModels() stores password as BCrypt
        getModels().forEach((LoginModel loginModelTemp) -> {
            // find by username
            if (loginModelTemp.getUsername().equals(loginRequest.getUsername())) {
                if ( UsefulAuthHelper.validatePasswordStringWithPasswordBCrypt(loginRequest.getPassword(), loginModelTemp.getPassword())) { // check password string with password bcrypt from database
                    JwtBuilder jwtBuilder = jwtService.generateToken(null, loginModelTemp); /// Generate token and set all details as claims,issue&expired token,... by LoginModel
                    loginResponse.setToken(jwtBuilder.compact());
                } else {
                    loginResponse.setToken(null);
                }
            }
        });
        return loginResponse;
    }

    @PostMapping("/auth/principle")
    private Object getAuthPrinciple() {
        return JwtSpringSecurityContextHelper.getPrincipal();
    }

    @PostMapping("/reads-report")
    private ResponseEntity<Resource> readsOrderItemsAsReport(@RequestBody FileType fileType)  {
        HashMap<String,byte[]> map = orderItemService.getOrderItemsHasMapReport(fileType.getFileExtension());
        if (!map.isEmpty()) {
            List<String> keySet = map.keySet().stream().toList();
            ByteArrayResource resource = new ByteArrayResource(map.get(keySet.get(0)));
            return ResponseEntity.ok()
                    .header(CONTENT_DISPOSITION, "attachment; filename=\"" + keySet.get(0) + "\"")
                    .header("File-Name", keySet.get(0)) // Note, custom headers won't work until you  .cors(cors -> cors.configurationSource(corsConfigurationSource())) on filterChain
                    .contentLength(resource.contentLength())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        }
        else {
            throw new RuntimeException("File Download Failed");
        }
    }

    @GetMapping("/preview-report") // For pdf only
    private ResponseEntity<Resource>  previewOrderItemsAsReport()  {
        FileType fileType = new FileType();
        fileType.setFileExtension("PDF");
        HashMap<String,byte[]> map = orderItemService.getOrderItemsHasMapReport(fileType.getFileExtension());
        if (!map.isEmpty()) {
            List<String> keySet = map.keySet().stream().toList();
            ByteArrayResource resource = new ByteArrayResource(map.get(keySet.get(0)));
            return ResponseEntity.ok()
                    // Set Content-Disposition to inline for preview
                    .header(CONTENT_DISPOSITION, "inline; filename=\"" + keySet.get(0) + "\"")
                    .header("File-Name", keySet.get(0))
                    .contentLength(resource.contentLength())
                    .contentType(MediaType.APPLICATION_PDF) // change only contentType for review report
                    .body(resource);
        }
        else {
            throw new RuntimeException("Preview File Failed");
        }
    }

    @PostMapping("/auth/reads-report")
    private ResponseEntity<Resource> readsOrderItemsAsReportAuth(@RequestBody FileType fileType)  {
        HashMap<String,byte[]> map = orderItemService.getOrderItemsHasMapReport(fileType.getFileExtension());
        if (!map.isEmpty()) {
            List<String> keySet = map.keySet().stream().toList();
            ByteArrayResource resource = new ByteArrayResource(map.get(keySet.get(0)));
            return ResponseEntity.ok()
                    .header(CONTENT_DISPOSITION, "attachment; filename=\"" + keySet.get(0) + "\"")
                    .header("File-Name", keySet.get(0)) // Note, custom headers won't work until you  .cors(cors -> cors.configurationSource(corsConfigurationSource())) on filterChain
                    .contentLength(resource.contentLength())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        }
        else {
            throw new RuntimeException("File Download Failed");
        }
    }

    @PostMapping("/auth/reads-report-v2")
    private ResponseEntity<Resource> readsOrderItemsAsReportAuthV2(@RequestBody FileType fileType)  {
        byte[] file = orderItemService.getOrderItemsReportV2(fileType.getFileExtension());
        String fileName = switch (fileType.getFileExtension()) {
            case "CSV" -> "order_items.csv";  // Export to CSV
            case "XLSX" -> "order_items.xlsx"; // Export to XLSX
            case "HTML" -> "order_items.html"; // Export to HTML
            case "XML" -> "order_items.xml"; // Export to XML
            case "DOC" -> "order_items.doc"; // Export to DOC
            case "PDF" -> "order_items.pdf";// Export to PDF
            default -> "order_items.txt"; // Export to TXT
        };
        if (file != null) {
            ByteArrayResource resource = new ByteArrayResource(file);
            return ResponseEntity.ok()
                    .header(CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .header("File-Name", fileName)
                    .contentLength(resource.contentLength())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        }
        else {
            throw new RuntimeException("File Download Failed");
        }
    }

    private List<LoginModel> getModels() {
        List<LoginModel> models = new ArrayList<>();
        LoginModel loginAdminModel = new LoginModel();
        LoginModel loginUserModel = new LoginModel();
        loginAdminModel.setUsername("admin");
        loginAdminModel.setEmail("admin@hotmail.com");
        loginAdminModel.setRole("ROLE_ADMIN"); // ** required work for hasRole(...) need a prefix as ROLE_*
        loginAdminModel.setCreateBy("ADMIN");
        loginAdminModel.setPassword(UsefulAuthHelper.convertStringToBCryptString("1"));
        loginUserModel.setUsername("user");
        loginUserModel.setEmail("user@hotmail.com");
        loginUserModel.setRole("ROLE_USER"); // ** required work for hasRole(...) need a prefix as ROLE_*
        loginUserModel.setCreateBy("ADMIN");
        loginUserModel.setPassword(UsefulAuthHelper.convertStringToBCryptString("1"));
        models.add(loginAdminModel);
        models.add(loginUserModel);
        return models;
    }

}
