package com.apsmkimo.go2radiolive.data.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoveAdsPolicyTest {
    @Test
    fun purchasedUnacknowledgedGrantsAndMustBeAcknowledged() {
        val decision = RemoveAdsPolicy.decide(
            purchases = listOf(purchase(state = RemoveAdsPolicy.STATE_PURCHASED, acknowledged = false)),
            querySucceeded = true,
            previousEntitled = false,
            previousPending = false,
        )
        assertTrue(decision.entitled)
        assertFalse(decision.pending)
        assertEquals(listOf("token"), decision.acknowledgeTokens)
    }

    @Test
    fun purchasedAcknowledgedGrantsWithoutAnotherAck() {
        val decision = RemoveAdsPolicy.decide(
            purchases = listOf(purchase(state = RemoveAdsPolicy.STATE_PURCHASED, acknowledged = true)),
            querySucceeded = true,
            previousEntitled = false,
            previousPending = false,
        )
        assertTrue(decision.entitled)
        assertTrue(decision.acknowledgeTokens.isEmpty())
    }

    @Test
    fun pendingDoesNotGrant() {
        val decision = RemoveAdsPolicy.decide(
            purchases = listOf(purchase(state = RemoveAdsPolicy.STATE_PENDING, acknowledged = false)),
            querySucceeded = true,
            previousEntitled = false,
            previousPending = false,
        )
        assertFalse(decision.entitled)
        assertTrue(decision.pending)
        assertTrue(decision.acknowledgeTokens.isEmpty())
    }

    @Test
    fun purchasedWinsOverAPendingRecord() {
        val decision = RemoveAdsPolicy.decide(
            purchases = listOf(
                purchase(state = RemoveAdsPolicy.STATE_PENDING, acknowledged = false, token = "pending"),
                purchase(state = RemoveAdsPolicy.STATE_PURCHASED, acknowledged = false, token = "bought"),
            ),
            querySucceeded = true,
            previousEntitled = false,
            previousPending = true,
        )
        assertTrue(decision.entitled)
        assertFalse(decision.pending)
        assertEquals(listOf("bought"), decision.acknowledgeTokens)
    }

    @Test
    fun emptySuccessfulQueryRevokesEntitlement() {
        val decision = RemoveAdsPolicy.decide(
            purchases = emptyList(),
            querySucceeded = true,
            previousEntitled = true,
            previousPending = false,
        )
        assertFalse(decision.entitled)
        assertFalse(decision.pending)
    }

    @Test
    fun failedQueryKeepsTheCachedEntitlement() {
        val kept = RemoveAdsPolicy.decide(
            purchases = emptyList(),
            querySucceeded = false,
            previousEntitled = true,
            previousPending = false,
        )
        assertTrue(kept.entitled)
        assertTrue(kept.acknowledgeTokens.isEmpty())

        val stillOff = RemoveAdsPolicy.decide(
            purchases = listOf(purchase(state = RemoveAdsPolicy.STATE_PURCHASED, acknowledged = true)),
            querySucceeded = false,
            previousEntitled = false,
            previousPending = true,
        )
        assertFalse(stillOff.entitled)
        assertTrue(stillOff.pending)
    }

    @Test
    fun otherProductsAndUnspecifiedStateDoNotGrant() {
        val decision = RemoveAdsPolicy.decide(
            purchases = listOf(
                purchase(productId = "other", state = RemoveAdsPolicy.STATE_PURCHASED, acknowledged = true),
                purchase(state = RemoveAdsPolicy.STATE_UNSPECIFIED, acknowledged = false),
            ),
            querySucceeded = true,
            previousEntitled = true,
            previousPending = false,
        )
        assertFalse(decision.entitled)
        assertFalse(decision.pending)
    }

    @Test
    fun selectsTheFirstPricedOfferThatHasAToken() {
        val selected = RemoveAdsPolicy.selectOffer(
            listOf(
                RemoveAdsPolicy.PricedOffer(formattedPrice = "", offerToken = "blank-price"),
                RemoveAdsPolicy.PricedOffer(formattedPrice = "US$0.99", offerToken = ""),
                RemoveAdsPolicy.PricedOffer(formattedPrice = "US$1.99", offerToken = "token"),
            ),
        )
        assertEquals("US$1.99", selected?.formattedPrice)
        assertEquals("token", selected?.offerToken)
        assertNull(RemoveAdsPolicy.selectOffer(emptyList()))
    }

    @Test
    fun blankPriceIsNotShown() {
        assertEquals("US$1.99", RemoveAdsPolicy.displayPrice(" US$1.99 "))
        assertNull(RemoveAdsPolicy.displayPrice(null))
        assertNull(RemoveAdsPolicy.displayPrice("  "))
    }

    @Test
    fun debugOverrideOnlyCountsInDebugBuilds() {
        assertTrue(RemoveAdsPolicy.effectiveEntitled(owned = false, debugOverride = true, debugBuild = true))
        assertFalse(RemoveAdsPolicy.effectiveEntitled(owned = false, debugOverride = true, debugBuild = false))
        assertTrue(RemoveAdsPolicy.effectiveEntitled(owned = true, debugOverride = false, debugBuild = false))
    }

    private fun purchase(
        productId: String = RemoveAdsPolicy.PRODUCT_ID,
        state: Int,
        acknowledged: Boolean,
        token: String = "token",
    ) = RemoveAdsPolicy.PurchaseView(
        productIds = listOf(productId),
        state = state,
        acknowledged = acknowledged,
        purchaseToken = token,
    )
}
