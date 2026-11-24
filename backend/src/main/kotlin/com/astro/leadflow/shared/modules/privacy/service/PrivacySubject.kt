package com.astro.leadflow.shared.modules.privacy.service

/** Identificador de titular já normalizado: telefone em E.164 ou e-mail em minúsculas. */
data class PrivacySubject(val value: String, val isEmail: Boolean) {
    override fun toString() = "PrivacySubject(***)"
}
