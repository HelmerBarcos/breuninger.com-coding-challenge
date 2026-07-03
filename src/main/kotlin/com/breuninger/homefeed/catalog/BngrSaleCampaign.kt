package com.breuninger.homefeed.catalog

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

/**
 * Campaign texts are localized as data (ADR-011): German base columns plus
 * nullable English translations - a missing translation falls back to German.
 */
@Entity
@Table(name = "sale_campaigns")
class BngrSaleCampaignEntity(
    @Id val id: UUID,
    val headline: String,
    val ctaLabel: String,
    val headlineEn: String?,
    val ctaLabelEn: String?,
    val imageUrl: String,
    val active: Boolean,
)

interface BngrSaleCampaignRepository : JpaRepository<BngrSaleCampaignEntity, UUID> {
    fun findFirstByActiveTrue(): BngrSaleCampaignEntity?
}
