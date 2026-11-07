package com.subscribers.gudogging.domain.auth.external;

import com.subscribers.gudogging.domain.auth.application.exception.InvalidGoogleIdTokenException;
import com.subscribers.gudogging.domain.auth.domain.GoogleTokenVerifier;
import com.subscribers.gudogging.domain.auth.domain.GoogleUserInfo;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class GoogleIdTokenVerifierAdapter implements GoogleTokenVerifier {

    private static final String JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");
    private static final String EMAIL_CLAIM = "email";
    private static final String NAME_CLAIM = "name";

    private final JwtDecoder jwtDecoder;

    public GoogleIdTokenVerifierAdapter(@Value("${google.client-ids}") List<String> clientIds) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(),
            new JwtClaimValidator<>(JwtClaimNames.ISS, iss -> ISSUERS.contains(String.valueOf(iss))),
            new JwtClaimValidator<Collection<?>>(JwtClaimNames.AUD,
                aud -> aud.stream().anyMatch(clientIds::contains))
        ));
        this.jwtDecoder = decoder;
    }

    @Override
    public GoogleUserInfo verify(String idToken) {
        try {
            Jwt jwt = jwtDecoder.decode(idToken);
            return new GoogleUserInfo(
                jwt.getSubject(),
                jwt.getClaimAsString(EMAIL_CLAIM),
                jwt.getClaimAsString(NAME_CLAIM)
            );
        } catch (JwtException e) {
            throw new InvalidGoogleIdTokenException();
        }
    }
}
