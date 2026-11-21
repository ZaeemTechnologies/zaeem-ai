package com.zaeemtechnologies.ai

enum class Eligibility { UNVERIFIED, VERIFIED_FREE }
data class Provider(val id: String, val enabled: Boolean, val modelId: String?, val eligibility: Eligibility)

/** Fail-closed gate: a request may only be sent for an enabled provider with a verified-free model. */
object ProviderPolicy {
    fun canSend(p: Provider): Boolean =
        p.enabled && p.modelId != null && p.eligibility == Eligibility.VERIFIED_FREE
}
