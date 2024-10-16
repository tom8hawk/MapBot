package ru.tom8hawk.mapbot.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.tom8hawk.mapbot.MapBot;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
public class InitDataValidator {


    private final byte[] secretKey;

    @Autowired
    public InitDataValidator(MapBot mapBot) {
        this.secretKey = hmacH256(mapBot.getBotToken().getBytes(StandardCharsets.UTF_8), "WebAppData".getBytes());
    }

    public boolean checkData(String initData) {
        if (initData == null || initData.isBlank()) {
            return false;
        }

        String parsedInitData = URLDecoder.decode(initData, StandardCharsets.UTF_8);
        String[] hashContainer = new String[1];
        List<String> sortedUrlDecoded = extractAndSortData(parsedInitData, hashContainer);

        if (hashContainer[0] == null) {
            return false;
        }

        return validateHash(sortedUrlDecoded, hashContainer[0]);
    }

    private boolean validateHash(List<String> sortedData, String originalHash) {
        if (secretKey == null) {
            return false;
        }

        byte[] dataHash = hmacH256(String.join("\n", sortedData).getBytes(StandardCharsets.UTF_8), secretKey);
        if (dataHash == null) {
            return false;
        }

        String computedHashString = bytesToHex(dataHash);
        return Objects.equals(originalHash, computedHashString);
    }

    private List<String> extractAndSortData(String initData, String[] hashContainer) {
        return Arrays.stream(initData.split("&"))
                .map(s -> extractHash(s, hashContainer))
                .filter(s -> !s.startsWith("hash="))
                .map(s -> URLDecoder.decode(s, StandardCharsets.UTF_8))
                .sorted()
                .toList();
    }

    private String extractHash(String param, String[] hashContainer) {
        if (param.startsWith("hash=")) {
            hashContainer[0] = param.substring(5);
        }
        return param;
    }

    private byte[] hmacH256(byte[] data, byte[] key) {
        try {
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "HmacSHA256");
            sha256HMAC.init(secretKeySpec);
            return sha256HMAC.doFinal(data);
        } catch (Exception e) {
            return null;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}