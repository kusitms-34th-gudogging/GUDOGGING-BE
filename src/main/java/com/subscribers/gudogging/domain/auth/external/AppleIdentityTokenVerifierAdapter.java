package com.subscribers.gudogging.domain.auth.external;

import com.subscribers.gudogging.domain.auth.application.exception.InvalidAppleIdTokenException;
import com.subscribers.gudogging.domain.auth.domain.AppleTokenVerifier;
import com.subscribers.gudogging.domain.auth.domain.AppleUserInfo;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class AppleIdentityTokenVerifierAdapter implements AppleTokenVerifier {

    private static final String JWK_SET_URI = "https://appleid.apple.com/auth/keys";
    private static final String ISSUER = "https://appleid.apple.com";
    private static final String EMAIL_CLAIM = "email";

    private final JwtDecoder jwtDecoder;

    public AppleIdentityTokenVerifierAdapter(@Value("${apple.client-id}") String clientId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI)
            .jwsAlgorithm(SignatureAlgorithm.RS256)
            .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(),
            new JwtClaimValidator<>(JwtClaimNames.ISS, iss -> ISSUER.equals(String.valueOf(iss))),
            new JwtClaimValidator<Collection<?>>(JwtClaimNames.AUD, aud -> aud.contains(clientId))
        ));
        this.jwtDecoder = decoder;
    }

    @Override
    public AppleUserInfo verify(String identityToken) {
        try {
            Jwt jwt = jwtDecoder.decode(identityToken);
            return new AppleUserInfo(jwt.getSubject(), jwt.getClaimAsString(EMAIL_CLAIM));
        } catch (JwtException e) {
            throw new InvalidAppleIdTokenException();
        }
    }
}
