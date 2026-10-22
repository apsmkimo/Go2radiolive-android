package com.apsmkimo.go2radiolive.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.apsmkimo.go2radiolive.BuildConfig
import com.apsmkimo.go2radiolive.data.local.RemoveAdsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RemoveAdsUiState(
    val entitled: Boolean = false,
    val pending: Boolean = false,
    val productAvailable: Boolean = false,
    val formattedPrice: String? = null,
    val detailsSettled: Boolean = false,
    val billingReady: Boolean = false,
    val purchaseInProgress: Boolean = false,
    val debugOverride: Boolean = false,
)

/**
 * Play Billing 8 for the non-consumable [RemoveAdsPolicy.PRODUCT_ID] product.
 * The client auto-reconnects after a service drop. A failed setup still retries
 * with capped backoff. Each successful connection queries INAPP purchases,
 * acknowledges unacknowledged PURCHASED items, and does not grant entitlement
 * while a purchase is PENDING. No server. Billing failures stay off the UI.
 */
class BillingManager(
    context: Context,
    private val onEntitlement: (Boolean) -> Unit,
) : PurchasesUpdatedListener {

    private val appContext = context.applicationContext
    private val store = RemoveAdsStore(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val purchaseMutex = Mutex()

    // SMCPKG_SUPPORT>>>Cursor114
    // private val client: BillingClient = BillingClient.newBuilder(appContext)
    //     .setListener(this)
    //     .enablePendingPurchases(
    //         PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
    //     )
    //     .build()
    private val client: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .enableAutoServiceReconnection()
        .build()
    // SMCPKG_SUPPORT<<<Cursor114

    private val _state = MutableStateFlow(RemoveAdsUiState())
    val state: StateFlow<RemoveAdsUiState> = _state.asStateFlow()

    private var started = false
    private var connecting = false
    private var reconnectAttempt = 0
    private var reconnectJob: Job? = null

    private var owned = false
    private var debugOverride = false
    private var pendingPurchase = false
    private var productAvailable = false
    private var formattedPrice: String? = null
    private var detailsSettled = false
    private var billingReady = false
    private var purchaseInProgress = false
    private var productDetails: ProductDetails? = null
    private var offerToken: String? = null

    fun start() {
        if (started) return
        started = true
        val snapshot = store.readBlocking()
        owned = snapshot.entitled
        debugOverride = BuildConfig.DEBUG && snapshot.debugOverride
        publish()
        connect()
    }

    fun refreshPurchases() {
        scope.launch {
            if (client.isReady) {
                queryPurchases()
            } else if (!connecting) {
                reconnectJob?.cancel()
                reconnectJob = null
                reconnectAttempt = 0
                connect()
            }
        }
    }

    fun restore() {
        refreshPurchases()
    }

    fun buy(activity: Activity) {
        scope.launch { launchPurchase(activity) }
    }

    fun setDebugOverride(enabled: Boolean) {
        if (!BuildConfig.DEBUG) return
        debugOverride = enabled
        publish()
        scope.launch { runCatching { store.setDebugOverride(enabled) } }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        scope.launch {
            purchaseInProgress = false
            publish()
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                // The callback list is not a full inventory, so it can grant or
                // mark pending, and a follow-up query decides refunds.
                applyPurchaseSnapshot(purchases.orEmpty(), allowRevoke = false)
            }
            if (
                result.responseCode == BillingClient.BillingResponseCode.OK ||
                result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED
            ) {
                queryPurchases()
            }
        }
    }

    private suspend fun launchPurchase(activity: Activity) {
        if (owned || pendingPurchase || purchaseInProgress) return
        val details = productDetails ?: return
        if (!client.isReady) {
            refreshPurchases()
            return
        }
        val token = offerToken
        if (token.isNullOrBlank()) return
        // SMCPKG_SUPPORT>>>Cursor114
        // val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
        //     .setProductDetails(details)
        //     .build()
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(token)
            .build()
        // SMCPKG_SUPPORT<<<Cursor114
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        purchaseInProgress = true
        publish()
        val launched = runCatching { client.launchBillingFlow(activity, flowParams) }.getOrNull()
        if (launched == null) {
            purchaseInProgress = false
            publish()
            return
        }
        if (launched.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            purchaseInProgress = false
            publish()
            queryPurchases()
            return
        }
        if (launched.responseCode != BillingClient.BillingResponseCode.OK) {
            purchaseInProgress = false
            publish()
        }
    }

    private fun connect() {
        if (client.isReady || connecting) return
        connecting = true
        val listener = object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                scope.launch {
                    connecting = false
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        reconnectAttempt = 0
                        billingReady = true
                        publish()
                        queryProductDetails()
                        queryPurchases()
                    } else {
                        billingReady = false
                        if (!productAvailable) {
                            detailsSettled = true
                            formattedPrice = null
                            productDetails = null
                            offerToken = null
                        }
                        publish()
                        scheduleReconnect()
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                scope.launch {
                    connecting = false
                    billingReady = false
                    publish()
                    // SMCPKG_SUPPORT>>>Cursor114
                    // scheduleReconnect()
                    // enableAutoServiceReconnection() calls startConnection again.
                    // Failed setup still uses scheduleReconnect().
                    // SMCPKG_SUPPORT<<<Cursor114
                }
            }
        }
        val startedConnection = runCatching { client.startConnection(listener) }
        if (startedConnection.isFailure) {
            connecting = false
            billingReady = false
            if (!productAvailable) detailsSettled = true
            publish()
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        val exponent = reconnectAttempt.coerceAtMost(5)
        reconnectAttempt += 1
        val delayMs = 1_000L shl exponent
        reconnectJob = scope.launch {
            delay(delayMs)
            connect()
        }
    }

    private suspend fun queryProductDetails() {
        if (!client.isReady) return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(RemoveAdsPolicy.PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        val result = runCatching { client.queryProductDetails(params) }.getOrNull()
        val ok = result != null &&
            result.billingResult.responseCode == BillingClient.BillingResponseCode.OK
        if (!ok) {
            if (!productAvailable) {
                productDetails = null
                offerToken = null
                formattedPrice = null
                productAvailable = false
                detailsSettled = true
                publish()
            }
            return
        }
        val details = result.productDetailsList.orEmpty()
            .firstOrNull { it.productId == RemoveAdsPolicy.PRODUCT_ID }
        // SMCPKG_SUPPORT>>>Cursor114
        // val price = RemoveAdsPolicy.displayPrice(details?.oneTimePurchaseOfferDetails?.formattedPrice)
        val offer = details?.let { RemoveAdsPolicy.selectOffer(offersOf(it)) }
        val price = RemoveAdsPolicy.displayPrice(offer?.formattedPrice)
        // SMCPKG_SUPPORT<<<Cursor114
        if (details == null || offer == null || price == null) {
            productDetails = null
            offerToken = null
            formattedPrice = null
            productAvailable = false
            detailsSettled = true
            publish()
            return
        }
        productDetails = details
        offerToken = offer.offerToken
        formattedPrice = price
        productAvailable = true
        detailsSettled = true
        publish()
    }

    private fun offersOf(details: ProductDetails): List<RemoveAdsPolicy.PricedOffer> {
        val listed = details.oneTimePurchaseOfferDetailsList.orEmpty().map { offer ->
            RemoveAdsPolicy.PricedOffer(
                formattedPrice = offer.formattedPrice,
                offerToken = offer.offerToken.orEmpty(),
            )
        }
        if (listed.isNotEmpty()) return listed
        val single = details.oneTimePurchaseOfferDetails ?: return emptyList()
        return listOf(
            RemoveAdsPolicy.PricedOffer(
                formattedPrice = single.formattedPrice,
                offerToken = single.offerToken.orEmpty(),
            ),
        )
    }

    private suspend fun queryPurchases() {
        if (!client.isReady) return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val result = runCatching { client.queryPurchasesAsync(params) }.getOrNull()
        val succeeded = result != null &&
            result.billingResult.responseCode == BillingClient.BillingResponseCode.OK
        if (!succeeded) return
        applyPurchaseSnapshot(result.purchasesList, allowRevoke = true)
    }

    private suspend fun applyPurchaseSnapshot(purchases: List<Purchase>, allowRevoke: Boolean) {
        purchaseMutex.withLock {
            val decision = RemoveAdsPolicy.decide(
                purchases = purchases.map { purchase ->
                    RemoveAdsPolicy.PurchaseView(
                        productIds = purchase.products,
                        state = purchase.purchaseState,
                        acknowledged = purchase.isAcknowledged,
                        purchaseToken = purchase.purchaseToken,
                    )
                },
                querySucceeded = true,
                previousEntitled = owned,
                previousPending = pendingPurchase,
            )
            val entitled = if (allowRevoke) decision.entitled else owned || decision.entitled
            val pending = if (allowRevoke) {
                decision.pending
            } else if (entitled) {
                false
            } else {
                decision.pending || pendingPurchase
            }
            if (owned != entitled) {
                owned = entitled
                runCatching { store.setEntitled(entitled) }
            }
            pendingPurchase = pending
            publish()
            if (decision.entitled) {
                for (token in decision.acknowledgeTokens) {
                    acknowledge(token)
                }
            }
        }
    }

    private suspend fun acknowledge(token: String) {
        if (!client.isReady || token.isBlank()) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(token)
            .build()
        runCatching { client.acknowledgePurchase(params) }
    }

    private fun publish() {
        val entitled = RemoveAdsPolicy.effectiveEntitled(
            owned = owned,
            debugOverride = debugOverride,
            debugBuild = BuildConfig.DEBUG,
        )
        _state.value = RemoveAdsUiState(
            entitled = entitled,
            pending = pendingPurchase && !entitled,
            productAvailable = productAvailable,
            formattedPrice = formattedPrice,
            detailsSettled = detailsSettled,
            billingReady = billingReady,
            purchaseInProgress = purchaseInProgress,
            debugOverride = BuildConfig.DEBUG && debugOverride,
        )
        onEntitlement(entitled)
    }
}
