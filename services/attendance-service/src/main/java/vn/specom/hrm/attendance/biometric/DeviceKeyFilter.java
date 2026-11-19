package vn.specom.hrm.attendance.biometric;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import vn.specom.hrm.attendance.common.EAttendanceError;
import vn.specom.hrm.attendance.common.EStatus;
import vn.specom.hrm.common.ApiMessageDto;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates a biometric device by its X-Device-Key (no Keycloak token: devices cannot log in). Unknown, deleted or
 * inactive devices get 401 ATT_024. The key itself is never logged.
 */
public class DeviceKeyFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Device-Key";

    /** Who is calling: a device of a company. */
    public record DevicePrincipal(Long deviceId, Long companyId) {
    }

    private final BiometricDeviceRepository devices;
    private final ObjectMapper mapper;

    public DeviceKeyFilter(BiometricDeviceRepository devices, ObjectMapper mapper) {
        this.devices = devices;
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String key = request.getHeader(HEADER);
        BiometricDevice d = key == null || key.isBlank() ? null : devices.findByApiKeyHashAndDeletedFalse(
                DeviceKeys.hash(key.trim())).filter(x -> DeviceKeys.same(x.getApiKeyHash(), DeviceKeys.hash(key.trim())))
                .filter(x -> x.getStatus() == EStatus.ACTIVE).orElse(null);
        if (d == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            mapper.writeValue(response.getOutputStream(), ApiMessageDto.builder().result(false)
                    .code(EAttendanceError.ATT_024.getCode()).message(EAttendanceError.ATT_024.getMessage()).build());
            return;
        }
        var auth = new UsernamePasswordAuthenticationToken(new DevicePrincipal(d.getId(), d.getCompanyId()), null,
                List.of(new SimpleGrantedAuthority("ROLE_BIOMETRIC_DEVICE")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
