package vn.specom.hrm.iam.me;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import vn.specom.hrm.common.Nulls;
import vn.specom.hrm.exception.ApiException;
import vn.specom.hrm.iam.constant.EIamError;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/**
 * The avatar is a file of the company's media library; IAM keeps only its id. Looking it up through media-service
 * with the caller's own token is also the ownership check (a file of another company answers 404).
 */
@Slf4j
@Component
public class AvatarMediaClient {
    static final long MAX_BYTES = 2L * 1024 * 1024;
    static final Set<String> TYPES = Set.of("image/jpeg", "image/png", "image/gif");

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MediaFile(String id, String companyId, String mimeType, long sizeBytes, String viewUrl) {
    }

    private final RestClient http;
    private final ObjectMapper mapper;

    public AvatarMediaClient(@Value("${hrm.media-service-url:http://localhost:8082}") String baseUrl,
                             ObjectMapper mapper) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Nulls.nonNull(Duration.ofSeconds(3)));
        f.setReadTimeout(Nulls.nonNull(Duration.ofSeconds(5)));
        this.http = RestClient.builder().baseUrl(Nulls.nonNull(baseUrl)).requestFactory(f).build();
        this.mapper = mapper;
    }

    /** The file, when it is a JPG/PNG/GIF of the company of at most 2 MB; otherwise IAM_024. */
    public MediaFile requireAvatar(Long fileId, Long companyId) {
        MediaFile f = fetch(fileId);
        if (f == null || !Objects.equals(f.companyId(), String.valueOf(companyId))
                || !TYPES.contains(f.mimeType()) || f.sizeBytes() > MAX_BYTES) {
            throw ApiException.badRequest(EIamError.IAM_024);
        }
        return f;
    }

    /** For display: the current link of the avatar, or null when it is gone or media-service is unreachable. */
    public String viewUrl(Long fileId) {
        if (fileId == null) {
            return null;
        }
        try {
            MediaFile f = fetch(fileId);
            return f == null ? null : f.viewUrl();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private MediaFile fetch(Long fileId) {
        try {
            JsonNode body = http.get().uri("/api/v1/media/files/{id}", fileId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + callerToken())
                    .retrieve().body(JsonNode.class);
            JsonNode data = body == null ? null : body.path("data");
            return data == null || data.isMissingNode() || data.isNull() ? null : mapper.convertValue(data, MediaFile.class);
        } catch (HttpClientErrorException.NotFound | HttpClientErrorException.Forbidden e) {
            // Not a file of the caller's company, or the caller may not open the media library.
            throw ApiException.badRequest(EIamError.IAM_024);
        } catch (RestClientException e) {
            log.warn("media-service call failed: {}", e.getClass().getSimpleName());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, EIamError.IAM_025);
        }
    }

    private static String callerToken() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getTokenValue();
        }
        throw ApiException.unauthorized(EIamError.AUTH_006);
    }
}
