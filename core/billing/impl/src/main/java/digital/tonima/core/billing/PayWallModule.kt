package digital.tonima.core.billing

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import digital.tonima.paywall.core.PayWallConfig
import digital.tonima.paywall.core.PayWallManager
import digital.tonima.paywall.play.PayWallManagerImpl
import javax.inject.Singleton

/**
 * Wires a single [PayWallManager] shared by [BillingManagerImpl] and
 * [SubscriptionManagerImpl], replacing the two independent BillingClient
 * instances the native implementation used to create (one per product type).
 */
@Module
@InstallIn(SingletonComponent::class)
object PayWallModule {
    @Provides
    @Singleton
    fun providePayWallConfig(): PayWallConfig =
        PayWallConfig(
            inAppProductIds = setOf(PRODUCT_ID_REMOVE_ADS),
            subscriptionProductIds = setOf(MONTHLY_SUBSCRIPTION_PLAN),
        )

    @Provides
    @Singleton
    fun providePayWallManager(
        @ApplicationContext context: Context,
        config: PayWallConfig,
    ): PayWallManager = PayWallManagerImpl(context, config)
}
