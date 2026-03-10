package digital.tonima.core.billing

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode.OK
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchaseState.PURCHASED
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.paulrybitskyi.hiltbinder.BindType
import com.paulrybitskyi.hiltbinder.BindType.Component.SINGLETON
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import logcat.LogPriority
import logcat.logcat
import javax.inject.Inject
import javax.inject.Singleton

private const val PRODUCT_ID_REMOVE_ADS = "remove_ads_premium"

@Singleton
@BindType(installIn = SINGLETON, to = BillingManager::class)
class BillingManagerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : BillingManager {
        private val _isProUser = MutableStateFlow(false)
        override val isProUser = _isProUser.asStateFlow()

        private val purchasesUpdatedListener =
            PurchasesUpdatedListener { billingResult, purchases ->
                if (billingResult.responseCode == OK && purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            }

        private var billingClient: BillingClient =
            BillingClient
                .newBuilder(context)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases(
                    PendingPurchasesParams
                        .newBuilder()
                        .enableOneTimeProducts()
                        .build(),
                ).build()

        override fun connect() {
            connectInternal(null)
        }

        private fun connectInternal(onConnected: (() -> Unit)? = null) {
            if (billingClient.isReady) {
                onConnected?.invoke()
                return
            }

            billingClient.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (billingResult.responseCode == OK) {
                            logcat { "Billing client setup finished." }
                            queryPurchases()
                            onConnected?.invoke()
                        } else {
                            logcat(LogPriority.ERROR) { "Billing client setup failed: ${billingResult.debugMessage}" }
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        logcat(LogPriority.WARN) { "Billing service disconnected. Retrying..." }
                        // Evitamos reconexão automática recursiva aqui
                    }
                },
            )
        }

        private fun queryPurchases() {
            val inAppParams =
                QueryPurchasesParams
                    .newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()

            billingClient.queryPurchasesAsync(inAppParams) { _, inAppPurchases ->
                val hasInAppPremium =
                    inAppPurchases.any {
                        it.products.contains(
                            PRODUCT_ID_REMOVE_ADS,
                        ) && it.isAcknowledged
                    }
                _isProUser.value = hasInAppPremium
            }
        }

        override fun refresh() {
            queryPurchases()
        }

        override fun launchPurchaseFlow(activity: Activity) {
            if (!billingClient.isReady) {
                logcat(LogPriority.ERROR) {
                    "Billing client not ready. Attempting to reconnect."
                }
                connectInternal {
                    launchPurchaseFlow(activity)
                }
                return
            }

            val inAppProduct =
                QueryProductDetailsParams.Product
                    .newBuilder()
                    .setProductId(PRODUCT_ID_REMOVE_ADS)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()

            val inAppParams =
                QueryProductDetailsParams.newBuilder()
                    .setProductList(listOf(inAppProduct))
                    .build()

            billingClient.queryProductDetailsAsync(inAppParams) { billingResult, queryProductDetailsResult ->
                if (billingResult.responseCode == OK && queryProductDetailsResult.productDetailsList.isNotEmpty()) {
                    val productDetails = queryProductDetailsResult.productDetailsList[0]
                    launchBillingFlow(activity, productDetails)
                } else {
                    logcat(LogPriority.ERROR) {
                        "In-app product details not found or error. Response: ${billingResult.responseCode}"
                    }
                    Toast.makeText(
                        context,
                        "Não foi possível encontrar o item na loja. Verifique sua conexão.",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }

        private fun launchBillingFlow(
            activity: Activity,
            productDetails: com.android.billingclient.api.ProductDetails,
        ) {
            val productDetailsParamsList = mutableListOf<BillingFlowParams.ProductDetailsParams>()

            val productParamsBuilder =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)

            productDetailsParamsList.add(productParamsBuilder.build())

            val billingFlowParams =
                BillingFlowParams
                    .newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()

            billingClient.launchBillingFlow(activity, billingFlowParams)
        }

        private fun handlePurchase(purchase: Purchase) {
            if (purchase.purchaseState == PURCHASED && !purchase.isAcknowledged) {
                val acknowledgePurchaseParams =
                    AcknowledgePurchaseParams
                        .newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()

                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == OK) {
                        logcat { "Purchase acknowledged successfully. Updating pro status." }
                        _isProUser.value = true
                        queryPurchases()
                    }
                }
            }
        }
    }
