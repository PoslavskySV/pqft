package cc.redberry.qplatform.endpoints;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.io.*;
import java.security.Key;
import java.util.List;

import static cc.redberry.qplatform.endpoints.ServerUtil.HEADER_X_AUTH;

public final class Authenticator implements HandlerFilterFunction<ServerResponse, ServerResponse> {
    private static final Logger log = LoggerFactory.getLogger(Authenticator.class);
    private final String xAuthToken;
    private final Key jwtKey;

    private Authenticator(String xAuthToken, Key jwtKey) {
        this.xAuthToken = xAuthToken;
        this.jwtKey = jwtKey;
    }

    /**
     * Used to extract X-Auth token to make requests to other parts of the system.
     *
     * All the sub-services of the app must be deployed with the same X-Auth token.
     */
    public String getXAuthToken() {
        return xAuthToken;
    }

    @Override
    public Mono<ServerResponse> filter(ServerRequest request, HandlerFunction<ServerResponse> next) {
        if (jwtKey == null && xAuthToken == null)
            return next.handle(request);

        boolean jwtAuthorized = false;
        if (jwtKey != null) {
            try {
                List<String> values = request.headers().header(HttpHeaders.AUTHORIZATION);
                String val;
                if (values.size() == 1) {
                    val = values.get(0);
                    log.trace("Authorization header: {}", val);
                    if (val.startsWith("Bearer ")) {
                        val = val.substring(7);
                        Jwts.parser().setSigningKey(jwtKey).parseClaimsJws(val); // JWT token content is not used
                        jwtAuthorized = true;
                    } else
                        log.warn("Wrong authorization header structure: {}", val);
                } else
                    log.trace("Authorization header not found / multiple instances.");
            } catch (JwtException ex) {
                log.error("JWT authorization error", ex);
                return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        boolean xAuthAuthorized = false;
        if (xAuthToken != null) {
            List<String> values = request.headers().header(HEADER_X_AUTH);
            String val;
            if (values.size() == 1) {
                val = values.get(0);
                log.trace("X-Auth header: {}", val);
                if (!xAuthToken.equals(val)) {
                    log.error("X-Auth authorization error");
                    return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
                }
                xAuthAuthorized = true;
            } else
                log.trace("X-Auth header not found / multiple instances.");
        }

        if (xAuthAuthorized || jwtAuthorized)
            return next.handle(request);
        else {
            log.error("Authorization error.");
            return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    private static volatile Authenticator globalAuth = null;

    public static Authenticator getAuthenticator() {
        if (globalAuth == null)
            synchronized (Authenticator.class) {
                if (globalAuth == null) {
                    if (true) return new Authenticator(null, null); // fixme
                    String jwtKeyString = System.getenv("Q_AUTH_JWT_KEY");
                    String jwtKeyFile = System.getenv("Q_AUTH_JWT_KEY_FILE");

                    if (jwtKeyString != null && jwtKeyFile != null)
                        throw new IllegalArgumentException("Can't use both Q_AUTH_JWT_KEY and Q_AUTH_JWT_KEY_FILE.");

                    byte[] jwtKeyBytes = null;

                    if (jwtKeyString != null)
                        jwtKeyBytes = jwtKeyString.getBytes();

                    if (jwtKeyFile != null)
                        try (InputStream stream = new FileInputStream(jwtKeyFile)) {
                            jwtKeyBytes = StreamUtils.copyToByteArray(stream);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                    Key jwtKey = null;
                    if (jwtKeyBytes != null) {
                        jwtKey = Keys.hmacShaKeyFor(jwtKeyBytes);
                        log.info("JWT authentication enabled");
                    }

                    String xAuthKeyString = System.getenv("Q_AUTH_X_AUTH_TOKEN");
                    String xAuthKeyFile = System.getenv("Q_AUTH_X_AUTH_TOKEN_FILE");

                    if (xAuthKeyString != null && xAuthKeyFile != null)
                        throw new IllegalArgumentException("Can't use both Q_AUTH_X_AUTH_TOKEN and Q_AUTH_X_AUTH_TOKEN_FILE.");

                    if (xAuthKeyFile != null)
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(xAuthKeyFile)))) {
                            xAuthKeyString = reader.readLine();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                    if (xAuthKeyString == null)
                        xAuthKeyString = "qwerty";
                    if (jwtKey == null)
                        jwtKey = Keys.hmacShaKeyFor("qwertyqwertyqwertyqwertyqwertyqwertyqwertyqwerty".getBytes());

                    if (xAuthKeyString == null && jwtKey == null) {
                        log.error("No authentication is selected for API calls.");
                        System.exit(1);
                    }

                    if (xAuthKeyString != null)
                        log.info("X-Auth authentication enabled");

                    globalAuth = new Authenticator(xAuthKeyString, jwtKey);
                }
            }
        return globalAuth;
    }
}
