package com.example.ui.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object AdManager {
    private const val TAG = "AdManager"

    // Test Ad Unit IDs provided by user
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    @Volatile
    private var isInitStarted = false

    private val _isSdkInitialized = MutableStateFlow(false)
    val isSdkInitialized: StateFlow<Boolean> = _isSdkInitialized.asStateFlow()

    /**
     * Initializes Google Mobile Ads strictly off the main UI thread so app startup
     * paints immediately with zero blank screen delay.
     */
    fun initialize(context: Context) {
        if (isInitStarted) return
        isInitStarted = true
        val appContext = context.applicationContext
        scope.launch {
            try {
                MobileAds.initialize(appContext) { status ->
                    Log.d(TAG, "MobileAds initialized on background thread: $status")
                    _isSdkInitialized.value = true
                    scope.launch(Dispatchers.Main) {
                        preloadInterstitial(appContext)
                        RewardedAdManager.markSdkInitialized(appContext)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing MobileAds", e)
            }
        }
    }

    fun preloadInterstitial(context: Context) {
        if (!_isSdkInitialized.value || interstitialAd != null || isInterstitialLoading) return
        isInterstitialLoading = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            TEST_INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded successfully")
                    interstitialAd = ad
                    isInterstitialLoading = false
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Interstitial ad failed to load: ${adError.message}")
                    interstitialAd = null
                    isInterstitialLoading = false
                }
            }
        )
    }

    fun showInterstitial(activity: Activity?, onAdClosed: (() -> Unit)? = null) {
        val currentAd = interstitialAd
        if (activity != null && currentAd != null) {
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad dismissed")
                    interstitialAd = null
                    preloadInterstitial(activity)
                    onAdClosed?.invoke()
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    Log.e(TAG, "Interstitial ad failed to show: ${adError.message}")
                    interstitialAd = null
                    preloadInterstitial(activity)
                    onAdClosed?.invoke()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad showed full screen")
                }
            }
            currentAd.show(activity)
        } else {
            Log.d(TAG, "Interstitial ad not ready, invoking continuation callback")
            if (activity != null && _isSdkInitialized.value) preloadInterstitial(activity)
            onAdClosed?.invoke()
        }
    }

    fun findActivity(context: Context): Activity? {
        var current = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }
}

@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = AdManager.TEST_BANNER_AD_UNIT_ID
) {
    val isSdkReady by AdManager.isSdkInitialized.collectAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Color(0xFF282828), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF3A3A3A), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "AdMob Banner",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF888888),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        // Defer AdView (Chromium WebView) inflation until MobileAds finishes initializing in background
        if (isSdkReady) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    AdView(context).apply {
                        setAdSize(AdSize.BANNER)
                        setAdUnitId(adUnitId)
                        adListener = object : AdListener() {
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                Log.w("BannerAdView", "Banner ad failed to load: ${error.message}")
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                    }
                }
            )
        }
    }
}
