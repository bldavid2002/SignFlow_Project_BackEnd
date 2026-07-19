package com.signflow.backend.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.security.KeyStore;

@Configuration
public class KeyConfig {

    @Value("${signature.keystore.path}")
    private String keystorePath;

    @Value("${signature.keystore.password}")
    private String keyStorePassword;

    @Bean
    public KeyStore signatureKeyStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(keystorePath)){
            if (is == null) {
                throw new RuntimeException("Keystore file not found in classpath: " + keystorePath);
            }
                keyStore.load(is, keyStorePassword.toCharArray());
            return keyStore;
        }

    }

}
