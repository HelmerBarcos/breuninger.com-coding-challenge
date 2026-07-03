package com.breuninger.homefeed.auth

import com.breuninger.homefeed.orders.BngrMockPurchaseFactory
import com.breuninger.homefeed.orders.BngrPurchaseRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

class BngrEmailTakenException(email: String) : RuntimeException("A user with email '$email' already exists")

data class BngrRegistration(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val gender: BngrGender,
)

/**
 * Registration seeds mock purchases in the same transaction (auth → orders is
 * the single allowed inter-domain dependency, ADR-001): every user has an
 * order history from day one, so the protected feed module is demonstrable
 * without purchase endpoints.
 */
@Service
class BngrRegisterUserService(
    private val users: BngrUserRepository,
    private val purchases: BngrPurchaseRepository,
    private val mockPurchases: BngrMockPurchaseFactory,
    private val passwordEncoder: PasswordEncoder,
) {

    @Transactional
    fun register(registration: BngrRegistration): BngrUserEntity {
        if (users.existsByEmail(registration.email)) throw BngrEmailTakenException(registration.email)
        val user = users.save(
            BngrUserEntity(
                id = UUID.randomUUID(),
                email = registration.email,
                firstName = registration.firstName,
                lastName = registration.lastName,
                gender = registration.gender,
                passwordHash = passwordEncoder.encode(registration.password),
            ),
        )
        purchases.saveAll(mockPurchases.createFor(user.email))
        return user
    }
}
