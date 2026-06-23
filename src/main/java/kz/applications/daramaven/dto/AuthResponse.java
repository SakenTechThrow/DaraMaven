package kz.applications.daramaven.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;

    private String refreshToken;

    private String tokenType;

    private long expiresIn;

    public AuthResponse(String accessToken) {
        this.accessToken = accessToken;
        this.expiresIn = 0;
        this.refreshToken = null;
        this.tokenType = "Bearer";
    }
}
