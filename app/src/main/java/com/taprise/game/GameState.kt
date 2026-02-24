package com.taprise.game

class GameState {
    // Core
    var coins: Long = 0
    var totalCoins: Long = 0
    var clicks: Long = 0
    var gems: Int = 0
    var level: Int = 1
    var xp: Long = 0
    var prestigeCount: Int = 0
    val prestigeMult: Double get() = 1.0 + prestigeCount * 0.25

    // Upgrades
    val upgrades = IntArray(UPGRADES.size)
    val heroBoughtUpgrades = BooleanArray(HERO_UPGRADES.size)

    // Achievements
    val achievements = BooleanArray(ACHS.size)

    // Combat
    var wave: Int = 1
    val hero = Hero(hp = 80, maxhp = 80, atk = 12, def = 5, crit = 5)
    var enemy: Enemy? = null
    var enemyHP: Int = 0
    val combatLog = ArrayDeque<CombatLogEntry>()
    var totalKills: Int = 0
    var bossKills: Int = 0

    // Expeditions
    val expeditions = EXPEDITIONS_DEF.map { Expedition(it) }.toMutableList()
    var totalExpeditions: Int = 0

    // Market
    val relics = RELIC_DEFS.map { d ->
        MarketRelic(d, d.basePrice.toDouble()).also { r ->
            r.history.clear()
            repeat(10) { r.history.add(d.basePrice.toDouble()) }
        }
    }.toMutableList()
    val portfolio = mutableMapOf<String, Int>()
    val relicInventory = mutableMapOf<String, Int>()

    // Alchemy
    val slots = arrayOfNulls<String>(3)
    val potions = mutableListOf<Potion>()
    val activeEffects = mutableMapOf<String, ActiveEffect>()
    var totalBrewed: Int = 0

    // Quests
    val quests = mutableListOf<Quest>()
    var questProgress = QuestProgress()
    var questsRefreshAt: Long = 0
    var totalQuestsDone: Int = 0
}

data class QuestProgress(
    var clicks: Long = 0,
    var earn: Long = 0,
    var kills: Int = 0,
    var expdone: Int = 0,
    var bought: Int = 0,
    var brewed: Int = 0
)
