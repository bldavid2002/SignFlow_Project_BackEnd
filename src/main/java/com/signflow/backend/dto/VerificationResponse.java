package com.signflow.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerificationResponse {
    private boolean isSigned;
    private String signedBy;
    private String signature;
    private String message;
}
