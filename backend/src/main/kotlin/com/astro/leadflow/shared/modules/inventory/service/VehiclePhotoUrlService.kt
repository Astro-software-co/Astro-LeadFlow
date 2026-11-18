package com.astro.leadflow.shared.modules.inventory.service

import com.astro.leadflow.shared.modules.inventory.domain.VehiclePhoto
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.net.URI
import java.time.Duration

@Service
class VehiclePhotoUrlService(
    private val s3Presigner: S3Presigner,
    @Value("\${app.aws.s3.bucket}") private val bucketName: String
) {

    companion object {
        private val URL_EXPIRATION: Duration = Duration.ofHours(12)
    }

    fun buildAccessUrl(photo: VehiclePhoto): String =
        buildAccessUrl(photo.s3Url)

    fun buildAccessUrl(storedReference: String): String {
        val key = extractObjectKey(storedReference)
        val presignedRequest = s3Presigner.presignGetObject(
            GetObjectPresignRequest.builder()
                .signatureDuration(URL_EXPIRATION)
                .getObjectRequest(
                    GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build()
                )
                .build()
        )

        return presignedRequest.url().toString()
    }

    fun extractObjectKey(storedReference: String): String {
        val trimmedReference = storedReference.trim()
        if (trimmedReference.isBlank()) {
            throw IllegalArgumentException("Referencia de foto vazia.")
        }

        val parsedUri = runCatching { URI.create(trimmedReference) }.getOrNull()
        val rawPath = when {
            parsedUri == null -> trimmedReference
            parsedUri.scheme.isNullOrBlank() -> parsedUri.path ?: trimmedReference
            else -> parsedUri.path ?: trimmedReference
        }

        val normalizedPath = rawPath.trim().trimStart('/')
        return if (normalizedPath.startsWith("$bucketName/")) {
            normalizedPath.removePrefix("$bucketName/")
        } else {
            normalizedPath
        }
    }
}
