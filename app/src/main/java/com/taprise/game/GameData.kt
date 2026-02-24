package com.taprise.game

// ─── Upgrade Definitions ───────────────────────────────────────────────────
data class UpgradeDef(
    val name: String, val ico: String, val desc: String,
    val base: Long, val max: Int,
    val effect: (level: Int) -> UpgradeEffect
)
data class UpgradeEffect(val cpc: Long, val cps: Long, val mult: Double)

val UPGRADES = listOf(
    UpgradeDef("Lucky Finger","☝️","+1 coin/click",    10L,  10) { l -> UpgradeEffect(l.toLong(), 0, 1.0) },
    UpgradeDef("Coin Magnet", "🧲","+3 coins/click",   55L,  10) { l -> UpgradeEffect(l*3L, 0, 1.0) },
    UpgradeDef("Gold Vein",   "⛏️","+1 coin/sec",      90L,  10) { l -> UpgradeEffect(0, l.toLong(), 1.0) },
    UpgradeDef("Tap Frenzy",  "⚡","+10 coins/click",  320L, 10) { l -> UpgradeEffect(l*10L, 0, 1.0) },
    UpgradeDef("Coin Factory","🏭","+5 coins/sec",     650L, 10) { l -> UpgradeEffect(0, l*5L, 1.0) },
    UpgradeDef("Midas Touch", "👑","×2 all income/lv",2200L, 5) { l -> UpgradeEffect(0, 0, Math.pow(2.0, l.toDouble())) },
)

// ─── Hero Upgrades ─────────────────────────────────────────────────────────
data class HeroUpgradeDef(val name: String, val ico: String, val desc: String, val cost: Long, val stat: String, val amt: Int)
val HERO_UPGRADES = listOf(
    HeroUpgradeDef("Iron Sword",   "🗡️", "+5 ATK",        200L,  "atk",   5),
    HeroUpgradeDef("Leather Helm", "🪖", "+10 DEF",       400L,  "def",   10),
    HeroUpgradeDef("Vitality Gem", "💎", "+30 MaxHP",     600L,  "maxhp", 30),
    HeroUpgradeDef("Rune Blade",   "⚔️", "+15 ATK",      1500L,  "atk",   15),
    HeroUpgradeDef("Dragon Mail",  "🛡️", "+25 DEF",      3000L,  "def",   25),
    HeroUpgradeDef("Phoenix Core", "🔥", "+10% crit",    5000L,  "crit",  10),
)

// ─── Enemies ───────────────────────────────────────────────────────────────
data class EnemyDef(val name: String, val ico: String, val baseHP: Int, val baseAtk: Int, val baseDef: Int, val reward: Long, val boss: Boolean = false)
val ENEMY_POOL = listOf(
    EnemyDef("Goblin",      "👺", 30,  3,  2,   8),
    EnemyDef("Skeleton",    "💀", 50,  5,  1,   14),
    EnemyDef("Orc Brute",   "👹", 80,  8,  4,   22),
    EnemyDef("Dark Mage",   "🧙", 60,  12, 2,   30),
    EnemyDef("Stone Golem", "🗿", 120, 6,  10,  35),
    EnemyDef("Vampire",     "🧛", 100, 14, 5,   48),
    EnemyDef("Dragon",      "🐲", 300, 20, 12, 120, true),
)

// ─── Relics ────────────────────────────────────────────────────────────────
data class RelicDef(val id: String, val name: String, val ico: String, val flavor: String, val basePrice: Long, val vol: Double, val bias: Double)
val RELIC_DEFS = listOf(
    RelicDef("ember",  "Ember Crystal", "🔮", "Volatile ancient energy",  120,  0.18,  0.02),
    RelicDef("titan",  "Titan Shard",   "⚙️", "Heavy industrial piece",   450,  0.12,  0.01),
    RelicDef("moon",   "Moon Fragment", "🌙", "Rare celestial debris",    900,  0.22, -0.01),
    RelicDef("dragon", "Dragon Scale",  "🐉", "Near-mythical material",  2800,  0.27,  0.015),
    RelicDef("void",   "Void Essence",  "🌀", "Unstable dark matter",    7500,  0.35, -0.02),
)

// ─── Expeditions ───────────────────────────────────────────────────────────
data class ExpeditionDef(val name: String, val ico: String, val desc: String, val durationSec: Long, val rewards: List<String>, val minWave: Int)
val EXPEDITIONS_DEF = listOf(
    ExpeditionDef("Ancient Ruins",  "🏛️", "Explore forgotten temples for lost treasure.",  30,   listOf("coins:50","xp:20"),                1),
    ExpeditionDef("Cursed Forest",  "🌲", "Venture into the dark woods. Relics await.",   60,   listOf("coins:120","relic:ember"),           3),
    ExpeditionDef("Mountain Pass",  "⛰️", "Dangerous cliffs hide rare minerals.",         120,  listOf("coins:300","gems:2"),                5),
    ExpeditionDef("Sunken Temple",  "🌊", "Dive deep for legendary artifacts.",           300,  listOf("coins:800","relic:moon","xp:80"),    8),
    ExpeditionDef("Dragon's Lair",  "🔥", "Only the bravest dare enter. Huge rewards.",  600,  listOf("coins:2000","relic:dragon","gems:5"), 12),
)

// ─── Potion Recipes ────────────────────────────────────────────────────────
data class PotionRecipe(val name: String, val ico: String, val desc: String, val ingredients: List<String>, val effect: String, val durationSec: Long, val value: Double)
val POTION_RECIPES = listOf(
    PotionRecipe("Coin Elixir",  "🟡", "×2 coins for 60s",         listOf("ember","ember"),          "coinMult", 60,  2.0),
    PotionRecipe("Speed Brew",   "🟢", "×3 passive income 30s",    listOf("titan","ember"),           "cpsMult",  30,  3.0),
    PotionRecipe("Battle Tonic", "🔴", "+50% Hero ATK 90s",        listOf("moon","titan"),            "heroAtk",  90,  1.5),
    PotionRecipe("Shield Potion","🔵", "Hero takes no dmg 20s",    listOf("dragon","void"),           "shield",   20,  0.0),
    PotionRecipe("Grand Elixir", "🟣", "×5 all income 30s",        listOf("void","dragon","moon"),    "allMult",  30,  5.0),
)

// ─── Achievements ──────────────────────────────────────────────────────────
data class AchievementDef(val ico: String, val name: String, val desc: String, val check: (GameState) -> Boolean)
val ACHS = listOf(
    AchievementDef("🪙","First Tap",       "Click once",                  { it.clicks >= 1 }),
    AchievementDef("💯","Century",          "100 clicks",                  { it.clicks >= 100 }),
    AchievementDef("⚔️","First Blood",     "Defeat your first enemy",     { it.totalKills >= 1 }),
    AchievementDef("🐲","Dragon Slayer",    "Defeat the Dragon boss",      { it.bossKills >= 1 }),
    AchievementDef("🏛️","Explorer",        "Complete first expedition",   { it.totalExpeditions >= 1 }),
    AchievementDef("⚗️","Alchemist",       "Brew your first potion",      { it.totalBrewed >= 1 }),
    AchievementDef("💰","Rich!",            "Earn 1,000 coins",            { it.totalCoins >= 1000 }),
    AchievementDef("💎","Diamond Hands",    "Earn 100,000 total coins",    { it.totalCoins >= 100000 }),
    AchievementDef("📈","Investor",         "Buy your first relic",        { it.portfolio.isNotEmpty() }),
    AchievementDef("🎲","Quest Completer",  "Complete 5 daily quests",     { it.totalQuestsDone >= 5 }),
    AchievementDef("⬆️","Levelhead",        "Reach Level 5",               { it.level >= 5 }),
    AchievementDef("🏆","Veteran",          "Reach Level 10",              { it.level >= 10 }),
    AchievementDef("✨","Ascended",         "Ascend for the first time",   { it.prestigeCount >= 1 }),
    AchievementDef("🌊","Wave 10",          "Survive Wave 10",             { it.wave >= 10 }),
)

// ─── Quests ────────────────────────────────────────────────────────────────
data class QuestDef(val ico: String, val name: String, val descTemplate: String, val type: String, val targets: List<Int>, val reward: String)
val QUEST_POOL = listOf(
    QuestDef("👆","Tap Addict",      "Tap {n} times",         "clicks",  listOf(25,50,100,250),  "coins:50"),
    QuestDef("💰","Money Bags",      "Earn {n} coins",        "earn",    listOf(200,500,1500,5000),"xp:30"),
    QuestDef("⚔️","Monster Slayer", "Defeat {n} enemies",    "kills",   listOf(3,8,15,30),        "gems:1"),
    QuestDef("🏛️","Explorer",       "Complete {n} expedition","expdone", listOf(1,2,4,7),          "gems:2"),
    QuestDef("📈","Trader",          "Buy {n} relics",        "bought",  listOf(1,3,5,10),         "coins:200"),
    QuestDef("⚗️","Alchemist",      "Brew {n} potions",      "brewed",  listOf(1,2,4,6),           "xp:60"),
)

// ─── Live State Objects ────────────────────────────────────────────────────
data class Enemy(val name: String, val ico: String, val hp: Int, val atk: Int, val def: Int, val reward: Long, val isBoss: Boolean)
data class Hero(var hp: Int, var maxhp: Int, var atk: Int, var def: Int, var crit: Int)
data class Expedition(val def: ExpeditionDef, var active: Boolean = false, var endsAt: Long = 0, var done: Boolean = false)
data class MarketRelic(val def: RelicDef, var price: Double, val history: ArrayDeque<Double> = ArrayDeque<Double>().also { dq -> repeat(10) { dq.add(0.0) } }, var dir: Int = 0)
data class Quest(val def: QuestDef, val target: Int, var current: Long = 0, var claimed: Boolean = false, val rewardType: String, val rewardAmt: Int) {
    val desc: String get() = def.descTemplate.replace("{n}", target.toString())
}
data class Potion(val recipe: PotionRecipe)
data class ActiveEffect(val effect: String, val expiresAt: Long, val value: Double)
data class CombatLogEntry(val msg: String, val type: LogType)
enum class LogType { GOLD, RED, GREEN, NORMAL }
