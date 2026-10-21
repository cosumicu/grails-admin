package grailsadmin

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

class AdminAccountService {
    private static final String LONG_PASSWORD_PREFIX = '{sha256-bcrypt}'
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12)
    private final String dummyHash = encoder.encode(UUID.randomUUID().toString())

    boolean configured() {
        AdminAccount.withTransaction([readOnly: true]) { AdminAccount.countBySlot('primary') > 0 }
    }

    void createInitialAccount(String username, char[] password, char[] confirmation,
                              boolean acceptNonRecommendedPassword = false) {
        try {
            String name = username?.trim()
            if (!name || name.length() > 100 || !(name ==~ /[A-Za-z0-9][A-Za-z0-9._@-]*/))
                throw new IllegalArgumentException('Username must contain 1-100 letters, digits, dots, underscores, @ or hyphens')
            if (!password) throw new IllegalArgumentException('Password cannot be empty')
            if (!confirmation || !Arrays.equals(password, confirmation))
                throw new IllegalArgumentException('Passwords do not match')
            int bytes = StandardCharsets.UTF_8.encode(java.nio.CharBuffer.wrap(password)).remaining()
            if ((password.length < 12 || bytes > 72) && !acceptNonRecommendedPassword)
                throw new IllegalArgumentException('Confirm the password warning before creating the account')
            AdminAccount.withTransaction {
                if (AdminAccount.countBySlot('primary')) throw new IllegalStateException('Grails Admin account already exists')
                new AdminAccount(slot: 'primary', username: name,
                    passwordHash: bytes > 72
                        ? LONG_PASSWORD_PREFIX + encoder.encode(longPasswordInput(new String(password)))
                        : encoder.encode(java.nio.CharBuffer.wrap(password)),
                    sessionVersion: UUID.randomUUID().toString()).save(failOnError: true, flush: true)
            }
        } finally {
            if (password) Arrays.fill(password, '\u0000' as char)
            if (confirmation) Arrays.fill(confirmation, '\u0000' as char)
        }
    }

    AdminAccount verify(String username, String password) {
        if (!username || !password) return null
        AdminAccount.withTransaction {
            AdminAccount account = AdminAccount.findBySlot('primary')
            if (!account || account.username != username) {
                encoder.matches(password, dummyHash)
                return null
            }
            boolean longHash = account.passwordHash.startsWith(LONG_PASSWORD_PREFIX)
            String storedHash = longHash ? account.passwordHash.substring(LONG_PASSWORD_PREFIX.length()) : account.passwordHash
            if (!longHash && password.getBytes(StandardCharsets.UTF_8).length > 72) return null
            String input = longHash ? longPasswordInput(password) : password
            if (!encoder.matches(input, storedHash)) return null
            if (encoder.upgradeEncoding(storedHash)) {
                account.passwordHash = (longHash ? LONG_PASSWORD_PREFIX : '') + encoder.encode(input)
                account.save(failOnError: true, flush: true)
            }
            account
        }
    }

    private static String longPasswordInput(String password) {
        byte[] digest = MessageDigest.getInstance('SHA-256').digest(password.getBytes(StandardCharsets.UTF_8))
        Base64.encoder.encodeToString(digest)
    }

    AdminAccount currentAccount(String version) {
        if (!version) return null
        AdminAccount.withTransaction([readOnly: true]) {
            AdminAccount account = AdminAccount.findBySlot('primary')
            account?.sessionVersion == version ? account : null
        }
    }
}
