package com.breuninger.homefeed.catalog

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

@Entity
@Table(name = "products")
class BngrProductEntity(
    @Id val id: UUID,
    val name: String,
    val brand: String,
    val priceCents: Int,
    val imageUrl: String,
)

interface BngrProductRepository : JpaRepository<BngrProductEntity, UUID> {
    fun findByOrderByNameAsc(page: Pageable): List<BngrProductEntity>
}
