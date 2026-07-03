package com.breuninger.homefeed.catalog

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

@Entity
@Table(name = "sale_campaigns")
class BngrSaleCampaignEntity(
    @Id val id: UUID,
    val headline: String,
    val ctaLabel: String,
    val imageUrl: String,
    val active: Boolean,
)

interface BngrSaleCampaignRepository : JpaRepository<BngrSaleCampaignEntity, UUID> {
    fun findFirstByActiveTrue(): BngrSaleCampaignEntity?
}
