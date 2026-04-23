package com.astro.leadflow.shared.modules.inventory.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest
import java.net.URI

class VehiclePhotoUrlServiceTest {

    private lateinit var s3Presigner: S3Presigner
    private lateinit var vehiclePhotoUrlService: VehiclePhotoUrlService

    @BeforeEach
    fun setup() {
        s3Presigner = mockk()
        vehiclePhotoUrlService = VehiclePhotoUrlService(
            s3Presigner = s3Presigner,
            bucketName = "leadflow-files"
        )
    }

    @Test
    fun `buildAccessUrl should presign aws style urls using extracted key`() {
        val presignRequestSlot = slot<GetObjectPresignRequest>()
        val presignedRequest = mockk<PresignedGetObjectRequest>()

        every { s3Presigner.presignGetObject(capture(presignRequestSlot)) } returns presignedRequest
        every { presignedRequest.url() } returns URI.create(
            "http://localhost:4566/leadflow-files/vehicles/org/veh/photo.jpg?signature=test"
        ).toURL()

        val accessUrl = vehiclePhotoUrlService.buildAccessUrl(
            "https://leadflow-files.s3.us-east-1.amazonaws.com/vehicles/org/veh/photo.jpg"
        )

        assertEquals(
            "vehicles/org/veh/photo.jpg",
            presignRequestSlot.captured.getObjectRequest().key()
        )
        assertEquals(
            "leadflow-files",
            presignRequestSlot.captured.getObjectRequest().bucket()
        )
        assertEquals(
            "http://localhost:4566/leadflow-files/vehicles/org/veh/photo.jpg?signature=test",
            accessUrl
        )
    }

    @Test
    fun `buildAccessUrl should presign path style urls using extracted key`() {
        val presignRequestSlot = slot<GetObjectPresignRequest>()
        val presignedRequest = mockk<PresignedGetObjectRequest>()

        every { s3Presigner.presignGetObject(capture(presignRequestSlot)) } returns presignedRequest
        every { presignedRequest.url() } returns URI.create(
            "http://localhost:4566/leadflow-files/vehicles/org/veh/photo.jpg?signature=test"
        ).toURL()

        vehiclePhotoUrlService.buildAccessUrl(
            "http://localhost:4566/leadflow-files/vehicles/org/veh/photo.jpg?X-Amz-Signature=abc"
        )

        assertEquals(
            "vehicles/org/veh/photo.jpg",
            presignRequestSlot.captured.getObjectRequest().key()
        )
    }

    @Test
    fun `extractObjectKey should keep raw object keys unchanged`() {
        assertEquals(
            "vehicles/org/veh/photo.jpg",
            vehiclePhotoUrlService.extractObjectKey("vehicles/org/veh/photo.jpg")
        )
    }
}
