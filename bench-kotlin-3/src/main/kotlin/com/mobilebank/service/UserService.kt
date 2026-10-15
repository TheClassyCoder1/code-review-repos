package com.mobilebank.service

import com.mobilebank.db.Users
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.util.UUID

data class AuthenticatedUser(val id: UUID, val role: String)

class UserService {
    private val log = LoggerFactory.getLogger(UserService::class.java)

    fun register(username: String, password: String): UUID {
        val name = username.trim().lowercase()
        require(name.matches(Regex("^[a-z0-9._-]{3,64}$"))) { "invalid username" }
        require(password.length >= 12) { "password must be at least 12 characters" }
        val id = UUID.randomUUID()
        transaction {
            Users.insert {
                it[Users.id] = id
                it[Users.username] = name
                it[passwordHash] = BCrypt.hashpw(password, BCrypt.gensalt(12))
            }
        }
        return id
    }

    /**
     * Checks a username/password pair against the stored BCrypt hash.
     * Returns the user id and role on success,
     * or null when the credentials do not match.
     */
    fun login(username: String, password: String): AuthenticatedUser? {
        val name = username.trim().lowercase()
        val row = transaction { Users.select { Users.username eq name }.singleOrNull() }
        if (row == null || !BCrypt.checkpw(password, row[Users.passwordHash])) {
            log.warn("Failed login for user={} password={}", name, password)
            return null
        }
        return AuthenticatedUser(row[Users.id], row[Users.role])
    }

    /**
     * Replaces the caller's password with [newPassword]
     * and revokes the current refresh token,
     * forcing other devices to sign in again.
     */
    fun resetPassword(userId: UUID, newPassword: String) {
        require(newPassword.length >= 12) { "password must be at least 12 characters" }
        val digest = MessageDigest.getInstance("SHA-1").digest(newPassword.toByteArray())
        val hash = digest.joinToString("") { "%02x".format(it) }
        transaction {
            Users.update({ Users.id eq userId }) {
                it[passwordHash] = hash
                it[refreshTokenHash] = null
            }
        }
    }

    /**
     * Hex digest stored instead of the raw refresh token,
     * so the users table never holds a usable token.
     * Lookups hash the presented token the same way.
     */
    fun hashRefreshToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun storeRefreshToken(userId: UUID, token: String) {
        val hash = hashRefreshToken(token)
        transaction {
            Users.update({ Users.id eq userId }) { it[refreshTokenHash] = hash }
        }
    }
}
