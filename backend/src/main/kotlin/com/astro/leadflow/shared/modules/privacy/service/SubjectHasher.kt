package com.astro.leadflow.shared.modules.privacy.service

import com.astro.leadflow.shared.exception.BusinessException
import com.astro.leadflow.shared.modules.ingestion.service.NormalizationService
import com.astro.leadflow.shared.modules.privacy.config.PrivacyProperties
import com.astro.leadflow.shared.util.PiiMasker
import org.springframework.stereotype.Component
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Normaliza identificadores de titular e calcula o HMAC-SHA256 que substitui o valor em claro. */
@Component
class SubjectHasher(
    private val props: PrivacyProperties,
    private val normalization: NormalizationService
) {
    fun normalize(identifier: String): PrivacySubject {
        val raw = identifier.trim()
        if (raw.isEmpty()) throw BusinessException("Informe o telefone ou e-mail do titular.")
        return if (raw.contains('@')) PrivacySubject(raw.lowercase(), isEmail = true)
        else PrivacySubject(normalization.normalizePhone(raw), isEmail = false)
    }

    fun hash(subject: PrivacySubject): String {
        check(props.hashSecret.isNotBlank()) { "privacy.hash-secret não configurado." }
        val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(props.hashSecret.toByteArray(), "HmacSHA256")) }
        return mac.doFinal(subject.value.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    fun hint(subject: PrivacySubject): String =
        if (subject.isEmail) PiiMasker.maskEmail(subject.value) else PiiMasker.maskPhone(subject.value)
}
