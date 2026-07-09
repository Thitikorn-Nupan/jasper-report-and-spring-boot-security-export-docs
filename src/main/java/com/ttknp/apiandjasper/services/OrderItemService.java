package com.ttknp.apiandjasper.services;

import com.ttknp.apiandjasper.entities.OrderItem;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRRtfExporter;
import net.sf.jasperreports.engine.export.JRXmlExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderItemService {

    private static final Logger log = LoggerFactory.getLogger(OrderItemService.class);
    private final JdbcTemplate jdbcTemplate;

    public OrderItemService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private <U> List<U> executeQuery(String sql, RowMapper<U> rowMapper) {
        return jdbcTemplate.query(sql, rowMapper);
    }

    private <U> List<U> executeQuery(String sql, RowMapper<U> rowMapper,Object ...params) {
        return jdbcTemplate.query(sql, rowMapper,params);
    }

    private List<OrderItem> getOrderItems() {
        String sql = "select * from order_items;";
        return executeQuery(sql, BeanPropertyRowMapper.newInstance(OrderItem.class));
    }

    private List<OrderItem> getOrderItemsForJasperReport() {
        String sql = "SELECT oi.oi_id , REPLACE(CONCAT('http://www.thitikorn-nupan.com/app/', oi.image_url ) , 'images', 'image-ecomerce')  AS image_url , oi.quantity , oi.price \n" +
                "FROM ecomerce_backend_modify.order_items AS oi\n" +
                "GROUP BY  oi.oi_id, oi.order_oid ;";
        return executeQuery(sql, BeanPropertyRowMapper.newInstance(OrderItem.class));
    }

    private List<OrderItem> getOrderItemsForJasperReportLimit10() {
        String sql = "SELECT oi.oi_id , REPLACE(CONCAT('http://www.thitikorn-nupan.com/app/', oi.image_url ) , 'images', 'image-ecomerce')  AS image_url , oi.quantity , oi.price \n" +
                "FROM ecomerce_backend_modify.order_items AS oi\n" +
                "GROUP BY  oi.oi_id, oi.order_oid LIMIT 10;"; //
        return executeQuery(sql, BeanPropertyRowMapper.newInstance(OrderItem.class));
    }

    private List<OrderItem> getOrderItemsForJasperReportWhereLikeDatetime(String datetime) {
        String sql = "SELECT oi.oi_id , REPLACE(CONCAT('http://www.thitikorn-nupan.com/app/', oi.image_url ) , 'images', 'image-ecomerce')  AS image_url , oi.quantity , oi.price ,\n" +
                "o.date_created , o.status_purchase\n" +
                "FROM ecomerce_backend_modify.order_items AS oi\n" +
                "JOIN orders AS o\n" +
                "ON\to.oid = oi.order_oid\n" +
                "WHERE o.date_created LIKE ?\n" +  // would be '%2024-10-08%'
                "GROUP BY oi.oi_id, oi.order_oid ;";
        log.debug("datetime: {}", datetime); // 2024-10-08
        return executeQuery(sql, BeanPropertyRowMapper.newInstance(OrderItem.class),"%"+datetime+"%");// i have to where like because this is datetime 2025-07-07 15:43:22.994000 format
    }

    public HashMap<String, byte[]> getOrderItemsHasMapReport(String fileType) {
        HashMap<String, byte[]> map = new HashMap<>(); // key is filename & value is file
        String fileName;
        if (fileType != null) {
            try {
                fileName = switch (fileType) {
                    case "CSV" -> "order_items.csv";  // Export to CSV
                    case "XLSX" -> "order_items.xlsx"; // Export to XLSX
                    case "HTML" -> "order_items.html"; // Export to HTML
                    case "XML" -> "order_items.xml"; // Export to XML
                    case "DOC" -> "order_items.doc"; // Export to DOC
                    case "PDF" -> "order_items.pdf";// Export to PDF
                    default -> "order_items.txt"; // Export to TXT
                };
                //  byte[] fileReport = ordersHistoryListJasperReportInBytesRootPath(fileType);
                byte[] fileReport = ordersHistoryListJasperReportInBytesFromRootPath(fileType);
                // log.debug("file report : {}", fileReport); // [102, 10, 54, 49, 57, 55, 51, 49, 48, 10, 37, 37, 69, 79, 70, 10 ,...]
                map.put(fileName, fileReport);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return map;
    }

    public byte[] getOrderItemsReportV2(String fileType) {
        if (fileType != null) {
            try {
                OrdersHistoryListJasperReportInBytesFromRootPathApplyThread ordersHistoryListJasperReportInBytesFromRootPathApplyThread = new OrdersHistoryListJasperReportInBytesFromRootPathApplyThread(fileType);
                ordersHistoryListJasperReportInBytesFromRootPathApplyThread.start();
                ordersHistoryListJasperReportInBytesFromRootPathApplyThread.join();
                return ordersHistoryListJasperReportInBytesFromRootPathApplyThread.fileReport;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("failed print report v2");
        }
    }

    // ***
    class OrdersHistoryListJasperReportInBytesFromRootPathApplyThread extends Thread {
        private byte[] fileReport;
        private final String fileType;

        public OrdersHistoryListJasperReportInBytesFromRootPathApplyThread(String fileType) {
            this.fileType = fileType;
        }

        public byte[] getFileReport() {
            return fileReport;
        }

        @Override
        public void run() {
            String resourceTemplateClassPath = "classpath:report/orders_history_list_basic_template.jrxml";
            List<OrderItem> orderItemsDataSource = getOrderItemsForJasperReport(); // getOrderItemsForJasperReport();
            // 0.1 Fix invalid url (Optional)
            orderItemsDataSource.forEach(orderItem -> {
                orderItem.setImageUrl(orderItem.getImageUrl().replaceAll(" ", "%20"));
            });
            Double totalPrice = 0d;
            for (OrderItem orderItem : orderItemsDataSource)  totalPrice += (orderItem.getPrice() * orderItem.getQuantity());
            // 1. Create Required Parameters For mapping parameter tags
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("title", "Orders History");
            parameters.put("logoUrl", "http://www.thitikorn-nupan.com/app/ecommerce/logo.png");
            parameters.put("datetimeCondition", " ");
            parameters.put("totalPrice", totalPrice);
            try {
                // 2. Create DataSource
                JRBeanCollectionDataSource beanCollectionDataSource = new JRBeanCollectionDataSource(orderItemsDataSource);
                // 2.2 Load Path Of Template
                String path = ResourceUtils.getFile(resourceTemplateClassPath).getAbsolutePath();
                // 3. Compile .jrmxl template, stored in JasperReport object
                JasperReport jasperReport = JasperCompileManager.compileReport(path);
                // 4. Fill Report - by passing complied .jrxml object, parameters, datasource
                JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, beanCollectionDataSource);
                // 5.Export Report - by using JasperExportManager
                fileReport = exportJasperReportBytes(jasperPrint, fileType);
            } catch (FileNotFoundException | JRException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public HashMap<String, byte[]> getOrderItemsHasMapReport(String fileType, String datetime) { // it's same getOrderItemsHasMapReport(fileType) but just have parameters
        HashMap<String, byte[]> map = new HashMap<>();
        String fileName;
        if (fileType != null) {
            try {
                fileName = switch (fileType) {
                    case "CSV" -> "order_items.csv";
                    case "XLSX" -> "order_items.xlsx";
                    case "HTML" -> "order_items.html";
                    case "XML" -> "order_items.xml";
                    case "DOC" -> "order_items.doc";
                    case "PDF" -> "order_items.pdf";
                    default -> "order_items.txt";
                };
                byte[] fileReport = ordersHistoryListJasperReportInBytesFromRootPath(fileType,datetime);
                map.put(fileName, fileReport);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return map;
    }


    private byte[] ordersHistoryListJasperReportInBytesFromAbsPath(String fileType) throws Exception {
        String resourceTemplateClassPath = "B:/jrxmls/orders_history_list_basic_template.jrxml";
        List<OrderItem> orderItemsDataSource = getOrderItemsForJasperReportLimit10(); // getOrderItemsForJasperReport();
        // 0.1 Fix invalid url (Optional)
        orderItemsDataSource.forEach(orderItem -> {
            orderItem.setImageUrl(orderItem.getImageUrl().replaceAll(" ", "%20"));
        });
        Double totalPrice = 0d;
        for (OrderItem orderItem : orderItemsDataSource)  totalPrice += (orderItem.getPrice() * orderItem.getQuantity());
        // 1. Create Required Parameters For mapping parameter tags
        Map<String, Object> parameters = new HashMap<>(); // ** can be null
        parameters.put("title", "Orders History TOP 10");
        parameters.put("logoUrl", "http://www.thitikorn-nupan.com/app/ecommerce/logo.png");
        parameters.put("datetimeCondition", " ");
        parameters.put("totalPrice", totalPrice);
        // 2. Create DataSource
        JRBeanCollectionDataSource beanCollectionDataSource = new JRBeanCollectionDataSource(orderItemsDataSource);
        // 2.2 Load Path Of Template
        // Optional: Use the File class to get the absolute path string
        // This is useful if you start with a relative path and need a guaranteed absolute one
        File reportFile = new File(resourceTemplateClassPath);
        String verifiedAbsolutePath = reportFile.getAbsolutePath(); // Ensures it is absolute
        // 3. Compile .jrmxl template, stored in JasperReport object
        JasperReport jasperReport = JasperCompileManager.compileReport(verifiedAbsolutePath);
        // 4. Fill Report - by passing complied .jrxml object, parameters, datasource
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, beanCollectionDataSource);
        // 5.Export Report - by using JasperExportManager
        return exportJasperReportBytes(jasperPrint, fileType);
    }

    private byte[] ordersHistoryListJasperReportInBytesFromRootPath(String fileType) throws Exception {
        String resourceTemplateClassPath = "classpath:report/orders_history_list_basic_template.jrxml";
        List<OrderItem> orderItemsDataSource = getOrderItemsForJasperReport(); // getOrderItemsForJasperReport();
        // 0.1 Fix invalid url (Optional)
        orderItemsDataSource.forEach(orderItem -> {
            orderItem.setImageUrl(orderItem.getImageUrl().replaceAll(" ", "%20"));
        });
        Double totalPrice = 0d;
        for (OrderItem orderItem : orderItemsDataSource)  totalPrice += (orderItem.getPrice() * orderItem.getQuantity());
        // 1. Create Required Parameters For mapping parameter tags
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("title", "Orders History");
        parameters.put("logoUrl", "http://www.thitikorn-nupan.com/app/ecommerce/logo.png");
        parameters.put("datetimeCondition", " ");
        parameters.put("totalPrice", totalPrice);
        // 2. Create DataSource
        JRBeanCollectionDataSource beanCollectionDataSource = new JRBeanCollectionDataSource(orderItemsDataSource);
        // 2.2 Load Path Of Template
        String path = ResourceUtils.getFile(resourceTemplateClassPath).getAbsolutePath();
        // 3. Compile .jrmxl template, stored in JasperReport object
        JasperReport jasperReport = JasperCompileManager.compileReport(path);
        // 4. Fill Report - by passing complied .jrxml object, parameters, datasource
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, beanCollectionDataSource);
        // 5.Export Report - by using JasperExportManager
        return exportJasperReportBytes(jasperPrint, fileType);
    }

    private byte[] ordersHistoryListJasperReportInBytesFromRootPath(String fileType, String datetime) throws Exception {
        String resourceTemplateClassPath = "classpath:report/orders_history_list_basic_template.jrxml";
        List<OrderItem> orderItemsDataSource = getOrderItemsForJasperReportWhereLikeDatetime(datetime);
        orderItemsDataSource.forEach(orderItem -> {
            orderItem.setImageUrl(orderItem.getImageUrl().replaceAll(" ", "%20"));
        });
        Double totalPrice = 0d;
        for (OrderItem orderItem : orderItemsDataSource)  totalPrice += (orderItem.getPrice() * orderItem.getQuantity());
        Map<String, Object> parameters = new HashMap<>(); // ** can be null
        parameters.put("title", "Orders History");
        parameters.put("logoUrl", "http://www.thitikorn-nupan.com/app/ecommerce/logo.png");
        parameters.put("datetimeCondition", datetime);
        parameters.put("totalPrice", totalPrice);
        JRBeanCollectionDataSource beanCollectionDataSource = new JRBeanCollectionDataSource(orderItemsDataSource);
        String path = ResourceUtils.getFile(resourceTemplateClassPath).getAbsolutePath();
        JasperReport jasperReport = JasperCompileManager.compileReport(path);
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, beanCollectionDataSource);
        return exportJasperReportBytes(jasperPrint, fileType);
    }

    // jasper Reports Util
    private byte[] exportJasperReportBytes(JasperPrint jasperPrint, String reportType) throws JRException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        switch (reportType) {
            case "CSV":
                // Export to CSV
                JRCsvExporter csvExporter = new JRCsvExporter();
                csvExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                csvExporter.setExporterOutput(new SimpleWriterExporterOutput(outputStream));
                csvExporter.exportReport();
                break;
            case "XLSX":
                // Export to XLSX
                JRXlsxExporter xlsxExporter = new JRXlsxExporter();
                xlsxExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                xlsxExporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
                xlsxExporter.exportReport();
                break;
            case "HTML":
                // Export to HTML
                HtmlExporter htmlExporter = new HtmlExporter();
                htmlExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                htmlExporter.setExporterOutput(new SimpleHtmlExporterOutput(outputStream));
                htmlExporter.exportReport();
                break;
            case "XML":
                // Export to XML
                JRXmlExporter xmlExporter = new JRXmlExporter();
                xmlExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                xmlExporter.setExporterOutput(new SimpleXmlExporterOutput(outputStream));
                xmlExporter.exportReport();
                break;
            case "DOC":
                // Export to DOCX (RTF format)
                JRRtfExporter docxExporter = new JRRtfExporter();
                docxExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                docxExporter.setExporterOutput(new SimpleWriterExporterOutput(outputStream));
                docxExporter.exportReport();
                break;
            case "PDF":
                JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream);
                break;
            default:
                break;
        }
        return outputStream.toByteArray();
    }
}
