package com.taprise.game

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GameViewModel : ViewModel() {

    val state = GameState()
    val engine = GameEngine(state)

    private val _uiEvent = MutableLiveData<UiEvent>()
    val uiEvent: LiveData<UiEvent> = _uiEvent

    private val _statsUpdate = MutableLiveData<Unit>()
    val statsUpdate: LiveData<Unit> = _statsUpdate

    private val handler = Handler(Looper.getMainLooper())

    private val passiveTick = object : Runnable {
        override fun run() {
            val cps = engine.passiveCoinTick()
            if (cps > 0) _statsUpdate.value = Unit
            handler.postDelayed(this, 1000)
        }
    }

    private val combatTick = object : Runnable {
        override fun run() {
            val result = engine.combatTick()
            when (result) {
                is CombatResult.EnemyDefeated -> {
                    val msg = if (result.isBoss) "🐲 Boss defeated! +2 Gems!" else "✅ Enemy defeated! +${engine.fmt(result.reward)}"
                    val type = if (result.isBoss) "combat" else "info"
                    _uiEvent.value = UiEvent.Toast(msg, type)
                }
                CombatResult.HeroDefeated -> _uiEvent.value = UiEvent.Toast("💀 Defeated! Resting...", "bad")
                CombatResult.Ongoing -> {}
            }
            _statsUpdate.value = Unit
            handler.postDelayed(this, 2500)
        }
    }

    private val marketTick = object : Runnable {
        override fun run() {
            engine.marketTick()
            _uiEvent.value = UiEvent.MarketUpdated
            handler.postDelayed(this, 4000)
        }
    }

    private val expeditionTick = object : Runnable {
        override fun run() {
            val done = engine.checkExpeditions()
            done.forEach { i ->
                _uiEvent.value = UiEvent.Toast("🗺️ ${state.expeditions[i].def.name} returned!", "quest")
            }
            if (done.isNotEmpty()) _uiEvent.value = UiEvent.ExpeditionsUpdated
            handler.postDelayed(this, 5000)
        }
    }

    private val regenTick = object : Runnable {
        override fun run() {
            engine.heroRegenTick()
            engine.cleanExpiredEffects()
            _statsUpdate.value = Unit
            handler.postDelayed(this, 5000)
        }
    }

    init {
        engine.spawnEnemy()
        engine.generateQuests()
        handler.post(passiveTick)
        handler.post(combatTick)
        handler.post(marketTick)
        handler.post(expeditionTick)
        handler.post(regenTick)
    }

    fun onTap(): Long {
        val earned = engine.handleClick()
        val newAchs = engine.checkAchievements()
        newAchs.forEach { i -> _uiEvent.value = UiEvent.Toast("🏆 Achievement: ${ACHS[i].name}", "info") }
        _statsUpdate.value = Unit
        return earned
    }

    fun buyUpgrade(i: Int) {
        if (engine.buyUpgrade(i)) {
            _uiEvent.value = UiEvent.Toast("⚒️ ${UPGRADES[i].name} upgraded!", "info")
            _statsUpdate.value = Unit
        } else {
            _uiEvent.value = UiEvent.Toast("Not enough coins!", "bad")
        }
    }

    fun buyHeroUpgrade(i: Int) {
        if (engine.buyHeroUpgrade(i)) {
            _uiEvent.value = UiEvent.Toast("⚔️ ${HERO_UPGRADES[i].name} equipped!", "info")
            _statsUpdate.value = Unit
        } else {
            _uiEvent.value = UiEvent.Toast("Not enough coins!", "bad")
        }
    }

    fun startExpedition(i: Int) {
        if (engine.startExpedition(i)) {
            _uiEvent.value = UiEvent.Toast("🗺️ ${state.expeditions[i].def.name} begun!", "info")
            _uiEvent.value = UiEvent.ExpeditionsUpdated
        }
    }

    fun claimExpedition(i: Int) {
        if (engine.claimExpedition(i)) {
            _uiEvent.value = UiEvent.Toast("✅ Expedition complete!", "quest")
            _statsUpdate.value = Unit
            _uiEvent.value = UiEvent.ExpeditionsUpdated
        }
    }

    fun addToSlot(relicId: String) {
        if (!engine.addToSlot(relicId)) _uiEvent.value = UiEvent.Toast("No $relicId in inventory!", "bad")
        _uiEvent.value = UiEvent.AlchemyUpdated
    }

    fun clearSlot(idx: Int) { engine.clearSlot(idx); _uiEvent.value = UiEvent.AlchemyUpdated }

    fun brew() {
        val result = engine.brew()
        if (result != null) {
            _uiEvent.value = UiEvent.Toast("⚗️ Brewed: ${result.name}!", "info")
            _statsUpdate.value = Unit
        } else {
            _uiEvent.value = UiEvent.Toast("No recipe for these ingredients!", "bad")
        }
        _uiEvent.value = UiEvent.AlchemyUpdated
    }

    fun usePotion(idx: Int) {
        val p = engine.usePotion(idx)
        if (p != null) {
            _uiEvent.value = UiEvent.Toast("🧪 ${p.recipe.name} activated!", "info")
            _statsUpdate.value = Unit
        }
        _uiEvent.value = UiEvent.AlchemyUpdated
    }

    fun buyRelic(id: String) {
        val cost = engine.buyRelic(id)
        if (cost >= 0) {
            _uiEvent.value = UiEvent.Toast("Bought relic for ${engine.fmt(cost)} 🛒", "info")
            _statsUpdate.value = Unit; _uiEvent.value = UiEvent.MarketUpdated
        } else {
            _uiEvent.value = UiEvent.Toast("Not enough coins!", "bad")
        }
    }

    fun sellRelic(id: String) {
        val val_ = engine.sellRelic(id)
        if (val_ >= 0) {
            _uiEvent.value = UiEvent.Toast("Sold for ${engine.fmt(val_)}!", "info")
            _statsUpdate.value = Unit; _uiEvent.value = UiEvent.MarketUpdated
        } else {
            _uiEvent.value = UiEvent.Toast("Nothing to sell!", "bad")
        }
    }

    fun claimQuest(i: Int) {
        val q = engine.claimQuest(i)
        if (q != null) {
            _uiEvent.value = UiEvent.Toast("🎲 Quest done! ${q.rewardType}: +${q.rewardAmt}", "quest")
            _statsUpdate.value = Unit
        }
    }

    fun doPrestige() {
        if (engine.doPrestige()) {
            _uiEvent.value = UiEvent.Toast("✨ Ascended to Prestige ${state.prestigeCount}! +${state.prestigeCount * 25}% bonus!", "prestige")
            _statsUpdate.value = Unit
        }
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
    }
}

sealed class UiEvent {
    data class Toast(val msg: String, val type: String) : UiEvent()
    object MarketUpdated : UiEvent()
    object ExpeditionsUpdated : UiEvent()
    object AlchemyUpdated : UiEvent()
}
