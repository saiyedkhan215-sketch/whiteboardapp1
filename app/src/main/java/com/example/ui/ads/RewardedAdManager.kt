package com.example.ui.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WhiteboardRewardItem(
    private val rewardAmount: Int = 100,
    private val rewardType: String = "VIP_TOOLS"
) : RewardItem {
    override fun getAmount(): Int = rewardAmount
    override fun getType(): String = rewardType
}

object RewardedAdManager {
    private const val TAG = "RewardedAdManager"

    // Official Google AdMob Rewarded Test Ad Unit ID provided by user
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private var rewardedAd: RewardedAd? = null
    private var isInitialized = false

    private val _isAdLoaded = MutableStateFlow(false)
    val isAdLoaded: StateFlow<Boolean> = _isAdLoaded.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _totalRewardsEarned = MutableStateFlow(0)
    val totalRewardsEarned: StateFlow<Int> = _totalRewardsEarned.asStateFlow()

    private val _isVipUnlocked = MutableStateFlow(false)
    val isVipUnlocked: StateFlow<Boolean> = _isVipUnlocked.asStateFlow()

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            MobileAds.initialize(context) { status ->
                Log.d(TAG, "MobileAds initialized: $status")
                isInitialized = true
                loadAd(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MobileAds", e)
        }
    }

    fun loadAd(context: Context, onLoaded: (() -> Unit)? = null, onFailed: ((String) -> Unit)? = null) {
        if (_isLoading.value) return
        _isLoading.value = true

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            TEST_REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded ad loaded successfully.")
                    rewardedAd = ad
                    _isAdLoaded.value = true
                    _isLoading.value = false
                    onLoaded?.invoke()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Rewarded ad failed to load: ${loadAdError.message} (code: ${loadAdError.code})")
                    rewardedAd = null
                    _isAdLoaded.value = false
                    _isLoading.value = false
                    onFailed?.invoke(loadAdError.message)
                }
            }
        )
    }

    fun showAd(
        activity: Activity,
        onUserEarnedReward: (RewardItem) -> Unit,
        onAdClosed: (() -> Unit)? = null
    ) {
        val currentAd = rewardedAd
        if (currentAd != null) {
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    Log.d(TAG, "Rewarded ad clicked.")
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad dismissed.")
                    rewardedAd = null
                    _isAdLoaded.value = false
                    onAdClosed?.invoke()
                    // Preload next ad
                    loadAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
                    rewardedAd = null
                    _isAdLoaded.value = false
                    Toast.makeText(activity, "Ad playback error: ${adError.message}", Toast.LENGTH_SHORT).show()
                    onAdClosed?.invoke()
                    loadAd(activity)
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad displayed on screen.")
                }
            }

            currentAd.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                _totalRewardsEarned.value += rewardItem.amount
                _isVipUnlocked.value = true
                onUserEarnedReward(rewardItem)
            }
        } else {
            // Ad not ready yet - show progress toast and attempt rapid load or fallback simulation
            Toast.makeText(activity, "Loading test rewarded ad (${TEST_REWARDED_AD_UNIT_ID})...", Toast.LENGTH_SHORT).show()
            loadAd(
                context = activity,
                onLoaded = {
                    rewardedAd?.let { freshAd ->
                        freshAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                rewardedAd = null
                                _isAdLoaded.value = false
                                onAdClosed?.invoke()
                                loadAd(activity)
                            }
                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                rewardedAd = null
                                _isAdLoaded.value = false
                                onAdClosed?.invoke()
                                loadAd(activity)
                            }
                        }
                        freshAd.show(activity) { rewardItem ->
                            _totalRewardsEarned.value += rewardItem.amount
                            _isVipUnlocked.value = true
                            onUserEarnedReward(rewardItem)
                        }
                    }
                },
                onFailed = { error ->
                    Log.w(TAG, "Live ad request failed ($error), granting fallback reward for testing in emulator")
                    val fallbackReward = WhiteboardRewardItem(100, "VIP_TEST_REWARD")
                    _totalRewardsEarned.value += fallbackReward.amount
                    _isVipUnlocked.value = true
                    Toast.makeText(activity, "Test ad simulated (offline): Reward of ${fallbackReward.amount} VIP points granted!", Toast.LENGTH_LONG).show()
                    onUserEarnedReward(fallbackReward)
                    onAdClosed?.invoke()
                }
            )
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
