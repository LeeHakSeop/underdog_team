package aaa.predictive_maintenance_p.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class KakaoTokenStore {

    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH = 12;

    private final JdbcTemplate jdbcTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${kakao.oauth.token-encryption-key:}")
    private String tokenEncryptionKey;

    public KakaoTokenStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void ensureTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS kakao_oauth_connection (
                    user_id VARCHAR(100) PRIMARY KEY,
                    access_token_encrypted TEXT NOT NULL,
                    refresh_token_encrypted TEXT NOT NULL,
                    access_expires_at TIMESTAMP NOT NULL,
                    refresh_expires_at TIMESTAMP NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
        try {
            jdbcTemplate.update("""
                    INSERT INTO kakao_oauth_connection (
                        user_id, access_token_encrypted, refresh_token_encrypted,
                        access_expires_at, refresh_expires_at, updated_at
                    )
                    SELECT user_id, access_token_encrypted, refresh_token_encrypted,
                           access_expires_at, refresh_expires_at, updated_at
                    FROM kakao_oauth_token
                    WHERE token_id = 1
                    ON CONFLICT (user_id) DO NOTHING
                    """);
            jdbcTemplate.update("DELETE FROM kakao_oauth_token WHERE token_id = 1");
        } catch (DataAccessException ignored) {
            // 새 설치에는 이전 단일 계정 테이블이 없으므로 마이그레이션할 내용도 없다.
        }
    }

    public boolean encryptionConfigured() {
        return tokenEncryptionKey != null && !tokenEncryptionKey.isBlank();
    }

    public List<StoredToken> loadAll() {
        return jdbcTemplate.query("""
                SELECT user_id, access_token_encrypted, refresh_token_encrypted,
                       access_expires_at, refresh_expires_at
                FROM kakao_oauth_connection
                ORDER BY created_at, user_id
                """, (rs, rowNum) -> new StoredToken(
                rs.getString("user_id"),
                decrypt(rs.getString("access_token_encrypted")),
                decrypt(rs.getString("refresh_token_encrypted")),
                rs.getTimestamp("access_expires_at").toInstant(),
                rs.getTimestamp("refresh_expires_at").toInstant()
        ));
    }

    public Optional<StoredToken> load(String userId) {
        return loadAll().stream().filter(token -> token.userId().equals(userId)).findFirst();
    }

    public void save(StoredToken token) {
        requireEncryptionKey();
        jdbcTemplate.update("""
                INSERT INTO kakao_oauth_connection (
                    user_id, access_token_encrypted, refresh_token_encrypted,
                    access_expires_at, refresh_expires_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (user_id) DO UPDATE SET
                    access_token_encrypted = EXCLUDED.access_token_encrypted,
                    refresh_token_encrypted = EXCLUDED.refresh_token_encrypted,
                    access_expires_at = EXCLUDED.access_expires_at,
                    refresh_expires_at = EXCLUDED.refresh_expires_at,
                    updated_at = CURRENT_TIMESTAMP
                """,
                token.userId(), encrypt(token.accessToken()), encrypt(token.refreshToken()),
                Timestamp.from(token.accessExpiresAt()), Timestamp.from(token.refreshExpiresAt())
        );
    }

    public void delete(String userId) {
        jdbcTemplate.update("DELETE FROM kakao_oauth_connection WHERE user_id = ?", userId);
    }

    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM kakao_oauth_connection");
    }

    private String encrypt(String plainText) {
        requireEncryptionKey();
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array()
            );
        } catch (Exception error) {
            throw new IllegalStateException("카카오 토큰 암호화에 실패했습니다.", error);
        }
    }

    private String decrypt(String encoded) {
        requireEncryptionKey();
        try {
            byte[] combined = Base64.getDecoder().decode(encoded);
            if (combined.length <= IV_LENGTH) {
                throw new IllegalArgumentException("암호화된 토큰 형식이 올바르지 않습니다.");
            }
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception error) {
            throw new IllegalStateException("저장된 카카오 토큰을 복호화할 수 없습니다.", error);
        }
    }

    private SecretKeySpec encryptionKey() throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(tokenEncryptionKey.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(digest, "AES");
    }

    private void requireEncryptionKey() {
        if (!encryptionConfigured()) {
            throw new IllegalStateException("KAKAO_TOKEN_ENCRYPTION_KEY 환경변수가 필요합니다.");
        }
    }

    public record StoredToken(
            String userId,
            String accessToken,
            String refreshToken,
            Instant accessExpiresAt,
            Instant refreshExpiresAt
    ) {
    }
}
