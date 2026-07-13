package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;


import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;


@Service
public class VnPayService {


    @Value("${vnpay.pay-url}")
    private String vnpPayUrl;


    @Value("${vnpay.tmn-code}")
    private String vnpTmnCode;


    @Value("${vnpay.hash-secret}")
    private String vnpHashSecret;


    @Value("${vnpay.return-url}")
    private String vnpReturnUrl;


    private static final DateTimeFormatter VNPAY_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");


    @PostConstruct
    public void debugConfig() {
        System.out.println("========== VNPAY CONFIG ==========");
        System.out.println("PAY_URL = [" + vnpPayUrl + "]");
        System.out.println("TMN_CODE = [" + vnpTmnCode + "]");
        System.out.println("TMN_CODE_LENGTH = " + vnpTmnCode.trim().length());
        System.out.println("HASH_SECRET_MASKED = [" + mask(vnpHashSecret) + "]");
        System.out.println("HASH_SECRET_LENGTH_RAW = " + vnpHashSecret.length());
        System.out.println("HASH_SECRET_LENGTH_TRIM = " + vnpHashSecret.trim().length());
        System.out.println("RETURN_URL = [" + vnpReturnUrl + "]");
        System.out.println("==================================");
    }


    public String createPaymentUrl(String txnRef,
                                   BigDecimal amount,
                                   HttpServletRequest request) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDateTime expire = now.plusMinutes(15);


        Map<String, String> params = new TreeMap<>();


        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", vnpTmnCode.trim());


        String vnpAmount = amount
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString();


        params.put("vnp_Amount", vnpAmount);
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);


        // Không dùng tiếng Việt có dấu, không dùng ký tự đặc biệt
        params.put("vnp_OrderInfo", "Thanh toan dat phong " + txnRef);


        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", vnpReturnUrl.trim());
        params.put("vnp_IpAddr", getClientIp(request));
        params.put("vnp_CreateDate", now.format(VNPAY_DATE_FORMAT));
        params.put("vnp_ExpireDate", expire.format(VNPAY_DATE_FORMAT));


        String hashData = buildHashData(params);
        String secureHash = hmacSHA512(vnpHashSecret.trim(), hashData);


        String queryUrl = buildQueryUrl(params);


        String paymentUrl = vnpPayUrl.trim()
                + "?"
                + queryUrl
                + "&vnp_SecureHash="
                + secureHash;


        System.out.println("========== VNPAY DEBUG ==========");
        System.out.println("HASH_DATA = " + hashData);
        System.out.println("QUERY_URL = " + queryUrl);
        System.out.println("SECURE_HASH = " + secureHash);
        System.out.println("PAYMENT_URL = " + paymentUrl);
        System.out.println("PAYMENT_URL_LENGTH = " + paymentUrl.length());
        System.out.println("=================================");


        return paymentUrl;
    }


    public boolean verifyReturnData(Map<String, String> params) {
        String vnpSecureHash = params.get("vnp_SecureHash");


        if (vnpSecureHash == null || vnpSecureHash.isBlank()) {
            return false;
        }


        Map<String, String> data = new TreeMap<>(params);
        data.remove("vnp_SecureHash");
        data.remove("vnp_SecureHashType");


        String hashData = buildHashData(data);
        String calculatedHash = hmacSHA512(vnpHashSecret.trim(), hashData);


        System.out.println("========== VNPAY RETURN DEBUG ==========");
        System.out.println("RETURN_HASH_DATA = " + hashData);
        System.out.println("VNPAY_HASH = " + vnpSecureHash);
        System.out.println("CALCULATED_HASH = " + calculatedHash);
        System.out.println("VALID = " + calculatedHash.equalsIgnoreCase(vnpSecureHash));
        System.out.println("========================================");


        return calculatedHash.equalsIgnoreCase(vnpSecureHash);
    }


    private String buildHashData(Map<String, String> params) {
        StringBuilder hashData = new StringBuilder();


        for (Map.Entry<String, String> entry : params.entrySet()) {
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue();


            if (fieldValue != null && !fieldValue.isBlank()) {
                if (hashData.length() > 0) {
                    hashData.append("&");
                }


                hashData.append(encode(fieldName))
                        .append("=")
                        .append(encode(fieldValue));
            }
        }


        return hashData.toString();
    }


    private String buildQueryUrl(Map<String, String> params) {
        StringBuilder queryUrl = new StringBuilder();


        for (Map.Entry<String, String> entry : params.entrySet()) {
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue();


            if (fieldValue != null && !fieldValue.isBlank()) {
                if (queryUrl.length() > 0) {
                    queryUrl.append("&");
                }


                queryUrl.append(encode(fieldName))
                        .append("=")
                        .append(encode(fieldValue));
            }
        }


        return queryUrl.toString();
    }


    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }


    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");


            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA512"
            );


            hmac512.init(secretKeySpec);


            byte[] bytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));


            StringBuilder hash = new StringBuilder();


            for (byte b : bytes) {
                hash.append(String.format("%02x", b));
            }


            return hash.toString();
        } catch (Exception e) {
            throw new RuntimeException("Cannot create VNPay secure hash", e);
        }
    }


    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");


        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }


        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }


        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            return "127.0.0.1";
        }


        return ip;
    }


    private String mask(String value) {
        if (value == null || value.trim().length() < 8) {
            return "INVALID";
        }


        String trimmed = value.trim();


        return trimmed.substring(0, 4)
                + "****"
                + trimmed.substring(trimmed.length() - 4);
    }
}

