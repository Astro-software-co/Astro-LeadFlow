package com.astro.leadflow.shared.modules.inventory.domain

import com.astro.leadflow.shared.modules.iam.domain.Organization
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "vehicles")
class Vehicle(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    val organization: Organization,

    @Column(nullable = false)
    var brand: String,

    @Column(nullable = false)
    var model: String,

    @Column(nullable = false)
    var year: Int,

    @Column
    var color: String? = null,

    @Column(name = "license_plate")
    var licensePlate: String? = null,

    @Column(name = "sale_price", nullable = false)
    var salePrice: BigDecimal,

    @Column(name = "cost_price")
    var costPrice: BigDecimal? = null,

    @Column(name = "mileage_km")
    var mileageKm: Int? = null,

    @Column(name = "ipva_paid", nullable = false)
    var ipvaPaid: Boolean = false,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var category: VehicleCategory,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: VehicleStatus = VehicleStatus.AVAILABLE,

    @OneToMany(
        mappedBy = "vehicle",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @OrderBy("position ASC")
    var photos: MutableList<VehiclePhoto> = mutableListOf(),

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun onUpdate() { updatedAt = LocalDateTime.now() }

    fun isAvailable(): Boolean = status == VehicleStatus.AVAILABLE

    fun belongsToOrganization(organizationId: UUID): Boolean =
        organization.id == organizationId

    fun addPhoto(s3Url: String, position: Int = photos.size) {
        photos.add(VehiclePhoto(vehicle = this, s3Url = s3Url, position = position))
    }
}