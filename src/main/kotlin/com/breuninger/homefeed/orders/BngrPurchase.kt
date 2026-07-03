package com.breuninger.homefeed.orders

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

/**
 * Purchases snapshot the product name and price at purchase time (as order
 * lines do in reality) — no foreign key into the catalog, which also keeps
 * the orders domain independent of catalog.
 */
@Entity
@Table(name = "purchases")
class BngrPurchaseEntity(
    @Id val id: UUID,
    val userEmail: String,
    val productName: String,
    val priceCents: Int,
    val purchasedAt: Instant,
)

interface BngrPurchaseRepository : JpaRepository<BngrPurchaseEntity, UUID> {
    fun findByUserEmailOrderByPurchasedAtDesc(userEmail: String): List<BngrPurchaseEntity>
}
