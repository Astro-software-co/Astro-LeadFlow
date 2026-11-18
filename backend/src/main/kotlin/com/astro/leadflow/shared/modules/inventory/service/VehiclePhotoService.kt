package com.astro.leadflow.shared.modules.inventory.service

import com.astro.leadflow.shared.exception.BusinessException
import com.astro.leadflow.shared.exception.ResourceNotFoundException
import com.astro.leadflow.shared.modules.inventory.api.responses.VehiclePhotoResponse
import com.astro.leadflow.shared.modules.inventory.domain.VehiclePhoto
import com.astro.leadflow.shared.modules.inventory.domain.VehiclePhotoRepository
import com.astro.leadflow.shared.util.SecurityUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import java.util.UUID

@Service
class VehiclePhotoService(
    private val vehicleService: VehicleService,
    private val vehiclePhotoRepository: VehiclePhotoRepository,
    private val vehiclePhotoUrlService: VehiclePhotoUrlService,
    private val s3Client: S3Client,
    @Value("\${app.aws.s3.bucket}") private val bucketName: String
) {

    companion object {
        private val ALLOWED_TYPES = setOf("image/jpeg", "image/png", "image/webp")
        private const val MAX_SIZE_BYTES = 5 * 1024 * 1024L
        private const val MAX_PHOTOS_PER_VEHICLE = 20
    }

    @Transactional
    fun upload(vehicleId: UUID, file: MultipartFile, position: Int): VehiclePhotoResponse {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val vehicle = vehicleService.findVehicleInOrg(vehicleId, orgId)

        if (file.contentType !in ALLOWED_TYPES)
            throw BusinessException("Tipo de arquivo inválido. Permitidos: JPEG, PNG, WEBP.")

        if (file.size > MAX_SIZE_BYTES)
            throw BusinessException("Arquivo muito grande. Máximo permitido: 5MB.")

        if (vehicle.photos.size >= MAX_PHOTOS_PER_VEHICLE)
            throw BusinessException("Limite de $MAX_PHOTOS_PER_VEHICLE fotos por veículo atingido.")

        val key = "vehicles/${orgId}/${vehicleId}/${UUID.randomUUID()}.jpg"

        try {
            ensureBucketExists()
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.contentType)
                    .build(),
                RequestBody.fromBytes(file.bytes)
            )
        } catch (ex: Exception) {
            throw BusinessException("Não foi possível armazenar a foto do veículo. Verifique o serviço de arquivos local.")
        }

        vehicle.addPhoto(key, position)

        val savedPhoto = vehiclePhotoRepository.save(vehicle.photos.last())
        return toResponse(savedPhoto)
    }

    @Transactional(readOnly = true)
    fun listPhotos(vehicleId: UUID): List<VehiclePhotoResponse> {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val vehicle = vehicleService.findVehicleInOrg(vehicleId, orgId)
        return vehicle.photos
            .sortedBy { it.position }
            .map(::toResponse)
    }

    @Transactional
    fun delete(vehicleId: UUID, photoId: UUID) {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val vehicle = vehicleService.findVehicleInOrg(vehicleId, orgId)

        val photo = vehicle.photos.find { it.id == photoId }
            ?: throw ResourceNotFoundException("VehiclePhoto", photoId)

        val key = vehiclePhotoUrlService.extractObjectKey(photo.s3Url)
        s3Client.deleteObject(
            DeleteObjectRequest.builder().bucket(bucketName).key(key).build()
        )

        vehicle.photos.remove(photo)
        vehiclePhotoRepository.delete(photo)
    }

    @Transactional
    fun reorder(vehicleId: UUID, photoIds: List<UUID>): List<VehiclePhotoResponse> {
        val orgId = SecurityUtils.getCurrentOrganizationId()
        val vehicle = vehicleService.findVehicleInOrg(vehicleId, orgId)

        photoIds.forEachIndexed { index, photoId ->
            val photo = vehicle.photos.find { it.id == photoId }
                ?: throw ResourceNotFoundException("VehiclePhoto", photoId)
            photo.position = index
        }

        vehiclePhotoRepository.saveAll(vehicle.photos)

        return vehicle.photos
            .sortedBy { it.position }
            .map(::toResponse)
    }

    private fun toResponse(photo: VehiclePhoto): VehiclePhotoResponse =
        VehiclePhotoResponse(
            id = photo.id!!,
            s3Url = vehiclePhotoUrlService.buildAccessUrl(photo),
            position = photo.position
        )

    private fun ensureBucketExists() {
        try {
            s3Client.headBucket(
                HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build()
            )
        } catch (ex: S3Exception) {
            if (ex.statusCode() != 404) {
                throw ex
            }

            s3Client.createBucket(
                CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build()
            )
        }
    }
}
