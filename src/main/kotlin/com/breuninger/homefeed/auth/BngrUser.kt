package com.breuninger.homefeed.auth

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

/** The three legal genders in Germany: männlich / weiblich / divers. */
enum class BngrGender { MALE, FEMALE, DIVERSE }

@Entity
@Table(name = "users")
class BngrUserEntity(
    @Id val id: UUID,
    @Column(unique = true) val email: String,
    val firstName: String,
    val lastName: String,
    @Enumerated(EnumType.STRING) val gender: BngrGender,
    val passwordHash: String,
)

interface BngrUserRepository : JpaRepository<BngrUserEntity, UUID> {
    fun findByEmail(email: String): BngrUserEntity?

    fun existsByEmail(email: String): Boolean
}
