package com.apsmkimo.go2radiolive.data.billing

/**
 * Pure rules for the non-consumable `remove_ads` product.
 * A successful purchase query is the source of truth: PURCHASED grants
 * entitlement (and must be acknowledged), PENDING does not, and a purchase
 * that is no longer returned is treated as refunded. A failed query keeps
 * the previous entitlement so ads stay off while Play is unreachable.
 */
internal object RemoveAdsPolicy {
    const val PRODUCT_ID = "remove_ads"

    /** Matches [com.android.billingclient.api.Purchase.PurchaseState]. */
    const val STATE_UNSPECIFIED = 0
    const val STATE_PURCHASED = 1
    const val STATE_PENDING = 2

    data class PurchaseView(
        val productIds: List<String>,
        val state: Int,
        val acknowledged: Boolean,
        val purchaseToken: String,
    )

    data class Decision(
        val entitled: Boolean,
        val pending: Boolean,
        val acknowledgeTokens: List<String>,
    )

    fun decide(
        purchases: List<PurchaseView>,
        querySucceeded: Boolean,
        previousEntitled: Boolean,
        previousPending: Boolean,
    ): Decision {
        if (!querySucceeded) {
            return Decision(
                entitled = previousEntitled,
                pending = previousPending,
                acknowledgeTokens = emptyList(),
            )
        }
        val ours = purchases.filter { PRODUCT_ID in it.productIds }
        val purchased = ours.filter { it.state == STATE_PURCHASED }
        val pending = ours.any { it.state == STATE_PENDING }
        return Decision(
            entitled = purchased.isNotEmpty(),
            pending = pending && purchased.isEmpty(),
            acknowledgeTokens = purchased
                .filterNot { it.acknowledged }
                .map { it.purchaseToken }
                .filter { it.isNotBlank() },
        )
    }

    data class PricedOffer(
        val formattedPrice: String,
        val offerToken: String,
    )

    /**
     * Play Billing 8 one-time products can expose several offers. The buy flow
     * needs a non-blank offer token, and the card shows Play's formatted price.
     */
    fun selectOffer(offers: List<PricedOffer>): PricedOffer? {
        return offers.firstOrNull { offer ->
            offer.formattedPrice.isNotBlank() && offer.offerToken.isNotBlank()
        }
    }

    /** Play formats this string for the user's currency. Blank means unusable. */
    fun displayPrice(formattedPrice: String?): String? {
        val price = formattedPrice?.trim().orEmpty()
        return price.ifBlank { null }
    }

    fun effectiveEntitled(owned: Boolean, debugOverride: Boolean, debugBuild: Boolean): Boolean {
        return owned || (debugBuild && debugOverride)
    }
}
