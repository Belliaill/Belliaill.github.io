package com.taprise.game

import kotlin.math.*
import kotlin.random.Random

class GameEngine(val state: GameState) {

    // ─── Helpers ─────────────────────────────────────────────────────────────
    fun xpFor(lvl: Int): Long = floor(100.0 * 1.42.pow(lvl - 1)).toLong()
    fun upgCost(i: Int): Long = floor(UPGRADES[i].base * 2.25.pow(state.upgrades[i])).toLong()
    fun clamp(v: Double, lo: Double, hi: Double) = max(lo, min(hi, v))
    fun rand(a: Int, b: Int) = Random.nextInt(a, b + 1)

    fun fmt(n: Long): String = when {
        n >= 1_000_000_000_000L -> "%.1fT".format(n / 1_000_000_000_000.0)
        n >= 1_000_000_000L     -> "%.1fB".format(n / 1_000_000_000.0)
        n >= 1_000_000L         -> "%.1fM".format(n / 1_000_000.0)
        n >= 1_000L             -> "%.1fK".format(n / 1_000.0)
        else                    -> n.toString()
    }

    fun fmtTime(sec: Long): String = when {
        sec < 60   -> "${sec}s"
        sec < 3600 -> "${sec / 60}m ${sec % 60}s"
        else       -> "${sec / 3600}h ${(sec % 3600) / 60}m"
    }

    // ─── Stats ───────────────────────────────────────────────────────────────
    fun getStats(): Pair<Long, Long> {
        var cpc = 1L; var cps = 0L; var mult = 1.0
        UPGRADES.forEachIndexed { i, u ->
            val lv = state.upgrades[i]
            if (lv == 0) return@forEachIndexed
            val e = u.effect(lv)
            cpc += e.cpc; cps += e.cps; mult *= e.mult
        }
        val now = System.currentTimeMillis()
        var coinMult = 1.0; var cpsMult = 1.0; var allMult = 1.0
        state.activeEffects["coinMult"]?.takeIf { it.expiresAt > now }?.let { coinMult = it.value }
        state.activeEffects["cpsMult"]?.takeIf { it.expiresAt > now }?.let { cpsMult = it.value }
        state.activeEffects["allMult"]?.takeIf { it.expiresAt > now }?.let { allMult = it.value }
        val finalCpc = floor(cpc * mult * state.prestigeMult * coinMult * allMult).toLong()
        val finalCps = floor(cps * mult * state.prestigeMult * cpsMult * allMult).toLong()
        return Pair(finalCpc, finalCps)
    }

    fun portfolioValue(): Long =
        state.relics.sumOf { r -> r.price * (state.portfolio[r.def.id] ?: 0) }.toLong()

    // ─── XP / Level ──────────────────────────────────────────────────────────
    fun gainXP(amt: Long): Boolean {
        state.xp += amt
        val need = xpFor(state.level)
        if (state.xp >= need) {
            state.xp -= need
            state.level++
            return true // leveled up
        }
        return false
    }

    // ─── Tap ─────────────────────────────────────────────────────────────────
    fun handleClick(): Long {
        val (cpc, _) = getStats()
        state.coins += cpc
        state.totalCoins += cpc
        state.clicks++
        state.questProgress.clicks++
        state.questProgress.earn += cpc
        gainXP(1 + floor(state.level * 0.18).toLong())
        checkAchievements()
        updateQuests()
        return cpc
    }

    // ─── Upgrades ────────────────────────────────────────────────────────────
    fun buyUpgrade(i: Int): Boolean {
        val cost = upgCost(i)
        if (state.coins < cost || state.upgrades[i] >= UPGRADES[i].max) return false
        state.coins -= cost
        state.upgrades[i]++
        gainXP(5)
        checkAchievements()
        return true
    }

    fun buyHeroUpgrade(i: Int): Boolean {
        val u = HERO_UPGRADES[i]
        if (state.coins < u.cost || state.heroBoughtUpgrades[i]) return false
        state.coins -= u.cost
        state.heroBoughtUpgrades[i] = true
        when (u.stat) {
            "maxhp" -> { state.hero.maxhp += u.amt; state.hero.hp = min(state.hero.hp + u.amt, state.hero.maxhp) }
            "crit"  -> state.hero.crit = max(0, min(75, state.hero.crit + u.amt))
            "atk"   -> state.hero.atk += u.amt
            "def"   -> state.hero.def += u.amt
        }
        return true
    }

    // ─── Combat ──────────────────────────────────────────────────────────────
    fun spawnEnemy() {
        val isBoss = state.wave % 10 == 0
        val pool = ENEMY_POOL.filter { it.boss == isBoss }
        val base = pool[rand(0, pool.size - 1)]
        val scale = 1.0 + state.wave * 0.18
        state.enemy = Enemy(
            name = base.name, ico = base.ico,
            hp = floor(base.baseHP * scale).toInt(),
            atk = floor(base.baseAtk * scale).toInt(),
            def = floor(base.baseDef * scale).toInt(),
            reward = floor(base.reward * scale).toLong(),
            isBoss = isBoss
        )
        state.enemyHP = state.enemy!!.hp
    }

    fun addLog(msg: String, type: LogType) {
        state.combatLog.addFirst(CombatLogEntry(msg, type))
        if (state.combatLog.size > 8) state.combatLog.removeLast()
    }

    /** Returns combat result message */
    fun combatTick(): CombatResult {
        if (state.enemy == null) spawnEnemy()
        val enemy = state.enemy!!
        val now = System.currentTimeMillis()
        val shielded = state.activeEffects["shield"]?.expiresAt?.let { it > now } ?: false
        var heroAtkMult = 1.0
        state.activeEffects["heroAtk"]?.takeIf { it.expiresAt > now }?.let { heroAtkMult = it.value }

        val isCrit = rand(1, 100) <= state.hero.crit
        val dmgToEnemy = max(1, floor((state.hero.atk * heroAtkMult - enemy.def) * (if (isCrit) 2 else 1)).toInt())
        state.enemyHP = max(0, state.enemyHP - dmgToEnemy)
        addLog("${if (isCrit) "⚡ CRIT! " else ""}You hit ${enemy.name} for $dmgToEnemy dmg", LogType.GOLD)

        if (state.enemyHP > 0 && !shielded) {
            val dmgToHero = max(1, enemy.atk - state.hero.def)
            state.hero.hp = max(0, state.hero.hp - dmgToHero)
            addLog("${enemy.name} hits you for $dmgToHero dmg", LogType.RED)
        } else if (shielded && state.enemyHP > 0) {
            addLog("🛡️ Shield absorbed damage!", LogType.GREEN)
        }

        if (state.enemyHP <= 0) {
            val reward = enemy.reward
            state.coins += reward; state.totalCoins += reward
            state.questProgress.earn += reward
            state.questProgress.kills++
            state.totalKills++
            val bossKilled = enemy.isBoss
            if (bossKilled) { state.bossKills++; state.gems += 2 }
            if (state.wave % 5 == 0 && !enemy.isBoss) {
                val drop = RELIC_DEFS[rand(0, 2)]
                state.relicInventory[drop.id] = (state.relicInventory[drop.id] ?: 0) + 1
                addLog("💎 Relic drop: ${drop.name}", LogType.GREEN)
            }
            gainXP(10 + state.wave * 2L)
            state.wave++
            spawnEnemy()
            updateQuests(); checkAchievements()
            return CombatResult.EnemyDefeated(reward, bossKilled)
        }
        if (state.hero.hp <= 0) {
            addLog("💀 You were defeated! Resting...", LogType.RED)
            state.hero.hp = floor(state.hero.maxhp * 0.4).toInt()
            state.coins = floor(state.coins * 0.9).toLong()
            if (state.wave > 1) state.wave = max(1, state.wave - 1)
            spawnEnemy()
            return CombatResult.HeroDefeated
        }
        return CombatResult.Ongoing
    }

    // ─── Expeditions ─────────────────────────────────────────────────────────
    fun startExpedition(i: Int): Boolean {
        val exp = state.expeditions[i]
        if (exp.active || state.wave < exp.def.minWave) return false
        exp.active = true
        exp.endsAt = System.currentTimeMillis() + exp.def.durationSec * 1000
        exp.done = false
        return true
    }

    fun claimExpedition(i: Int): Boolean {
        val exp = state.expeditions[i]
        if (!exp.done) return false
        exp.def.rewards.forEach { r ->
            val parts = r.split(":")
            val type = parts[0]; val valStr = parts[1]
            when (type) {
                "coins" -> { val v = valStr.toLong(); state.coins += v; state.totalCoins += v; state.questProgress.earn += v }
                "xp"    -> gainXP(valStr.toLong())
                "gems"  -> state.gems += valStr.toInt()
                "relic" -> state.relicInventory[valStr] = (state.relicInventory[valStr] ?: 0) + 1
            }
        }
        state.totalExpeditions++
        state.questProgress.expdone++
        exp.active = false; exp.done = false; exp.endsAt = 0
        updateQuests(); checkAchievements()
        return true
    }

    fun checkExpeditions(): List<Int> {
        val now = System.currentTimeMillis()
        val completed = mutableListOf<Int>()
        state.expeditions.forEachIndexed { i, exp ->
            if (exp.active && !exp.done && exp.endsAt > 0 && now >= exp.endsAt) {
                exp.done = true
                completed.add(i)
            }
        }
        return completed
    }

    // ─── Alchemy ─────────────────────────────────────────────────────────────
    fun addToSlot(relicId: String): Boolean {
        val emptyIdx = state.slots.indexOfFirst { it == null }
        if (emptyIdx == -1) return false
        if ((state.relicInventory[relicId] ?: 0) < 1) return false
        state.relicInventory[relicId] = (state.relicInventory[relicId] ?: 0) - 1
        state.slots[emptyIdx] = relicId
        return true
    }

    fun clearSlot(idx: Int) {
        state.slots[idx]?.let { id ->
            state.relicInventory[id] = (state.relicInventory[id] ?: 0) + 1
            state.slots[idx] = null
        }
    }

    fun brew(): PotionRecipe? {
        val filled = state.slots.filterNotNull()
        if (filled.size < 2) return null
        val match = POTION_RECIPES.find { p ->
            p.ingredients.size == filled.size &&
            p.ingredients.sorted().joinToString() == filled.sorted().joinToString()
        } ?: return null
        state.slots.fill(null)
        state.potions.add(Potion(match))
        state.totalBrewed++
        state.questProgress.brewed++
        updateQuests(); checkAchievements()
        return match
    }

    fun usePotion(idx: Int): Potion? {
        if (idx >= state.potions.size) return null
        val p = state.potions.removeAt(idx)
        val exp = System.currentTimeMillis() + p.recipe.durationSec * 1000
        state.activeEffects[p.recipe.effect] = ActiveEffect(p.recipe.effect, exp, p.recipe.value)
        return p
    }

    // ─── Market ──────────────────────────────────────────────────────────────
    fun buyRelic(id: String): Long {
        val r = state.relics.find { it.def.id == id } ?: return -1
        val cost = r.price.toLong()
        if (state.coins < cost) return -1
        state.coins -= cost
        state.portfolio[id] = (state.portfolio[id] ?: 0) + 1
        state.questProgress.bought++
        gainXP(3)
        updateQuests(); checkAchievements()
        return cost
    }

    fun sellRelic(id: String): Long {
        if ((state.portfolio[id] ?: 0) == 0) return -1
        val r = state.relics.find { it.def.id == id } ?: return -1
        val val_ = r.price.toLong()
        state.coins += val_; state.totalCoins += val_; state.questProgress.earn += val_
        state.portfolio[id] = (state.portfolio[id] ?: 0) - 1
        if (state.portfolio[id] == 0) state.portfolio.remove(id)
        gainXP(2)
        updateQuests()
        return val_
    }

    fun marketTick() {
        state.relics.forEach { r ->
            val pull = (r.def.basePrice - r.price) / r.def.basePrice * 0.08
            val rnd = (Random.nextDouble() - 0.5) * 2 * r.def.vol
            val ch = rnd + r.def.bias + pull
            r.price = max(r.def.basePrice * 0.3, r.price * (1 + ch))
            r.history.removeFirst()
            r.history.addLast(r.price)
            r.dir = if (ch > 0) 1 else if (ch < 0) -1 else 0
        }
    }

    // ─── Quests ──────────────────────────────────────────────────────────────
    fun generateQuests() {
        val shuffled = QUEST_POOL.shuffled().take(3)
        state.quests.clear()
        state.quests.addAll(shuffled.map { q ->
            val target = q.targets[rand(0, q.targets.size - 1)]
            val parts = q.reward.split(":")
            Quest(q, target, rewardType = parts[0], rewardAmt = parts[1].toInt())
        })
        state.questProgress = QuestProgress()
        state.questsRefreshAt = System.currentTimeMillis() + 24 * 3600 * 1000
    }

    fun updateQuests() {
        if (state.quests.isEmpty() || System.currentTimeMillis() > state.questsRefreshAt) {
            generateQuests()
        }
        val prog = state.questProgress
        state.quests.filter { !it.claimed }.forEach { q ->
            q.current = min(q.target.toLong(), when (q.def.type) {
                "clicks"  -> prog.clicks
                "earn"    -> prog.earn
                "kills"   -> prog.kills.toLong()
                "expdone" -> prog.expdone.toLong()
                "bought"  -> prog.bought.toLong()
                "brewed"  -> prog.brewed.toLong()
                else      -> 0L
            })
        }
    }

    fun claimQuest(i: Int): Quest? {
        val q = state.quests.getOrNull(i) ?: return null
        if (q.claimed || q.current < q.target) return null
        q.claimed = true
        state.totalQuestsDone++
        when (q.rewardType) {
            "coins" -> { state.coins += q.rewardAmt; state.totalCoins += q.rewardAmt }
            "xp"    -> gainXP(q.rewardAmt.toLong())
            "gems"  -> state.gems += q.rewardAmt
        }
        checkAchievements()
        return q
    }

    // ─── Prestige ────────────────────────────────────────────────────────────
    fun canPrestige() = state.level >= 20

    fun doPrestige(): Boolean {
        if (!canPrestige()) return false
        state.prestigeCount++
        state.coins = 0; state.totalCoins = 0; state.clicks = 0
        state.level = 1; state.xp = 0
        state.upgrades.fill(0)
        checkAchievements()
        return true
    }

    // ─── Achievements ────────────────────────────────────────────────────────
    fun checkAchievements(): List<Int> {
        val newlyUnlocked = mutableListOf<Int>()
        ACHS.forEachIndexed { i, a ->
            if (!state.achievements[i] && a.check(state)) {
                state.achievements[i] = true
                newlyUnlocked.add(i)
            }
        }
        return newlyUnlocked
    }

    // ─── Passive Ticks ───────────────────────────────────────────────────────
    fun passiveCoinTick(): Long {
        val (_, cps) = getStats()
        if (cps > 0) {
            state.coins += cps; state.totalCoins += cps; state.questProgress.earn += cps
        }
        return cps
    }

    fun heroRegenTick() {
        if (state.hero.hp < state.hero.maxhp) {
            state.hero.hp = min(state.hero.maxhp, state.hero.hp + 1)
        }
    }

    fun cleanExpiredEffects() {
        val now = System.currentTimeMillis()
        state.activeEffects.entries.removeAll { it.value.expiresAt < now }
    }
}

sealed class CombatResult {
    data class EnemyDefeated(val reward: Long, val isBoss: Boolean) : CombatResult()
    object HeroDefeated : CombatResult()
    object Ongoing : CombatResult()
}
