package vn.specom.hrm.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import vn.specom.hrm.common.ApiMessageDto;

import java.time.Duration;

/**
 * For services other than IAM: asks {@code GET /api/v1/me/authorities} on the IAM service with the caller's own
 * token, so no service credential exists and IAM applies the same rules as for the FE. Results are cached per token
 * for {@code ttl} — that is the delay before a role change reaches this service.
 *
 * <p>Fails closed: when IAM cannot be reached the request fails (500), it is never let through without authorities.
 * Not retried — a retry storm on IAM would make an outage worse.
 */
public class RemoteAuthorityResolver implements AuthorityResolver {
    private static final ParameterizedTypeReference<ApiMessageDto<HrmPrincipal>> TYPE = new ParameterizedTypeReference<>() {
    };

    private final RestClient client;
    private final Cache<String, HrmPrincipal> cache;

    public RemoteAuthorityResolver(String iamBaseUrl, Duration ttl) {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(Duration.ofSeconds(2));
        rf.setReadTimeout(Duration.ofSeconds(3));
        this.client = RestClient.builder().baseUrl(iamBaseUrl).requestFactory(rf).build();
        this.cache = Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(10_000).build();
    }

    @Override
    public HrmPrincipal resolve(Jwt jwt) {
        return cache.get(jwt.getTokenValue(), token -> fetch(token));
    }

    private HrmPrincipal fetch(String token) {
        try {
            ApiMessageDto<HrmPrincipal> body = client.get()
                    .uri("/api/v1/me/authorities")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(TYPE);
            if (body == null || body.getData() == null) {
                throw new AuthenticationServiceException("IAM returned no authorities");
            }
            return body.getData();
        } catch (RestClientException e) {
            throw new AuthenticationServiceException("IAM authority lookup failed", e);
        }
    }
}
