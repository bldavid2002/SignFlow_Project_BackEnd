package com.signflow.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = S3Config.class)
@TestPropertySource(properties = {
        "aws.s3.credentials.access-key=dummy-test-access-key",
        "aws.s3.credentials.secret-key=dummy-test-secret-key",
        "aws.s3.region=eu-central-1"
})
public class S3ConfigTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void shouldCreateS3ClientBean(){
        boolean hasS3Client = context.containsBean("s3Client");
        assertThat(hasS3Client).isTrue();

        S3Client s3Client = context.getBean(S3Client.class);
        assertThat(s3Client).isNotNull();
    }
}
