package com.taprise.game

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class ForgeFragment : Fragment(), Refreshable {
    private lateinit var vm: GameViewModel
    private var container: LinearLayout? = null

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        vm = ViewModelProvider(requireActivity())[GameViewModel::class.java]
        val root = inflater.inflate(R.layout.fragment_forge, parent, false)
        container = root.findViewById(R.id.upgradeList)
        refresh()
        return root
    }

    override fun refresh() {
        val c = container ?: return
        c.removeAllViews()
        addSectionLabel(c, "Forge Upgrades")
        UPGRADES.forEachIndexed { i, u ->
            val lv = vm.state.upgrades[i]
            val cost = vm.engine.upgCost(i)
            val maxed = lv >= u.max
            val canBuy = vm.state.coins >= cost && !maxed
            addUpgradeCard(c, i, u, lv, cost, maxed, canBuy)
        }
    }

    private fun addSectionLabel(parent: LinearLayout, text: String) {
        val tv = TextView(context)
        tv.text = text
        tv.setTextColor(Color.parseColor("#3d4560"))
        tv.textSize = 10f
        tv.letterSpacing = 0.2f
        tv.setPadding(0, 0, 0, dpToPx(8))
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.setMargins(0, 0, 0, dpToPx(4))
        parent.addView(tv, lp)
    }

    private fun addUpgradeCard(parent: LinearLayout, idx: Int, u: UpgradeDef, lv: Int, cost: Long, maxed: Boolean, canBuy: Boolean) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10))
            background = if (canBuy) requireContext().getDrawable(R.drawable.card_gold_bg)
                         else requireContext().getDrawable(R.drawable.card_bg)
            alpha = if (maxed) 0.35f else 1f
        }
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.setMargins(0, 0, 0, dpToPx(6))

        // Icon
        val ico = TextView(context).apply { text = u.ico; textSize = 24f }
        row.addView(ico, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).also {
            it.setMargins(0, 0, dpToPx(10), 0)
        })

        // Body
        val body = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val name = TextView(context).apply { text = u.name; setTextColor(Color.parseColor("#d8dce8")); textSize = 12f; typeface = android.graphics.Typeface.DEFAULT_BOLD }
        val desc = TextView(context).apply { text = u.desc; setTextColor(Color.parseColor("#3d4560")); textSize = 11f }
        body.addView(name); body.addView(desc)
        row.addView(body, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        // Cost / Level
        val right = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; gravity = android.view.Gravity.END }
        val costTv = TextView(context).apply {
            text = if (maxed) "MAX" else vm.engine.fmt(cost)
            setTextColor(if (maxed) Color.parseColor("#4be8a0") else Color.parseColor("#e8b84b"))
            textSize = 13f; typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val lvTv = TextView(context).apply { text = "$lv/${u.max}"; setTextColor(Color.parseColor("#3d4560")); textSize = 9f }
        right.addView(costTv); right.addView(lvTv)
        row.addView(right, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        if (!maxed) row.setOnClickListener { vm.buyUpgrade(idx); refresh(); (activity as? MainActivity)?.updateAllStats() }
        parent.addView(row, lp)
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()
}

// ─── Combat Fragment ──────────────────────────────────────────────────────────
class CombatFragment : Fragment(), Refreshable {
    private lateinit var vm: GameViewModel
    private var container: LinearLayout? = null

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        vm = ViewModelProvider(requireActivity())[GameViewModel::class.java]
        val root = inflater.inflate(R.layout.fragment_combat, parent, false)
        container = root.findViewById(R.id.combatContainer)
        vm.statsUpdate.observe(viewLifecycleOwner) { refresh() }
        refresh()
        return root
    }

    override fun refresh() {
        val c = container ?: return
        c.removeAllViews()
        val s = vm.state
        val e = s.enemy ?: return

        addLabel(c, "Combat Arena")

        // Wave badge
        val waveTv = TextView(context).apply {
            text = (if (s.wave % 10 == 0) "👑 BOSS WAVE · " else "") + "Wave ${s.wave}"
            setTextColor(Color.parseColor("#e84b6b")); textSize = 11f
            setPadding(dpToPx(10), dpToPx(3), dpToPx(10), dpToPx(3))
        }
        c.addView(waveTv)

        // Fighters row
        val fightRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
        val lp3 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        // Hero side
        val heroCol = makeHeroCol(s)
        fightRow.addView(heroCol, lp3)

        // VS
        val vs = TextView(context).apply { text = "VS"; setTextColor(Color.parseColor("#e84b6b")); textSize = 16f; gravity = android.view.Gravity.CENTER }
        fightRow.addView(vs, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        // Enemy side
        val enemyCol = makeEnemyCol(e, s)
        fightRow.addView(enemyCol, lp3)

        val cardLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        cardLp.setMargins(0, 0, 0, dpToPx(8))
        val card = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12)); background = context.getDrawable(R.drawable.card_bg) }
        card.addView(fightRow)

        // Combat log
        val logBox = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6)); setBackgroundColor(Color.parseColor("#0f1219")) }
        s.combatLog.forEach { entry ->
            val tv = TextView(context).apply {
                text = entry.msg
                textSize = 11f
                setTextColor(when (entry.type) {
                    LogType.GOLD  -> Color.parseColor("#e8b84b")
                    LogType.RED   -> Color.parseColor("#e84b6b")
                    LogType.GREEN -> Color.parseColor("#4be8a0")
                    else          -> Color.parseColor("#3d4560")
                })
            }
            logBox.addView(tv)
        }
        card.addView(logBox)
        c.addView(card, cardLp)

        // Hero stats
        val statsRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("⚔️" to "${s.hero.atk} ATK", "🛡️" to "${s.hero.def} DEF", "💥" to "${s.hero.crit}% CRIT", "💀" to "${s.totalKills} KILLS").forEach { (ico, label) ->
            val statCard = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; gravity = android.view.Gravity.CENTER; setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6)); background = context.getDrawable(R.drawable.card_bg) }
            statCard.addView(TextView(context).apply { text = ico; textSize = 14f; gravity = android.view.Gravity.CENTER })
            statCard.addView(TextView(context).apply { text = label; textSize = 9f; setTextColor(Color.parseColor("#d8dce8")); gravity = android.view.Gravity.CENTER })
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp.setMargins(0, 0, dpToPx(4), 0)
            statsRow.addView(statCard, lp)
        }
        val statsLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        statsLp.setMargins(0, 0, 0, dpToPx(12))
        c.addView(statsRow, statsLp)

        // Hero Equipment
        addLabel(c, "Hero Equipment")
        HERO_UPGRADES.forEachIndexed { i, hu ->
            val bought = s.heroBoughtUpgrades[i]
            val canBuy = s.coins >= hu.cost && !bought
            addHeroUpgradeCard(c, i, hu, bought, canBuy)
        }
    }

    private fun makeHeroCol(s: GameState): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; gravity = android.view.Gravity.CENTER
            addView(TextView(context).apply { text = "🧙"; textSize = 34f; gravity = android.view.Gravity.CENTER })
            addView(TextView(context).apply { text = "Your Hero"; textSize = 10f; setTextColor(Color.parseColor("#d8dce8")); gravity = android.view.Gravity.CENTER })
            val bar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = s.hero.maxhp; progress = s.hero.hp; progressDrawable = context.getDrawable(R.drawable.hp_bar_hero)
                minimumHeight = dpToPx(8)
                val lp2 = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(6))
                lp2.setMargins(0, dpToPx(4), 0, 0)
                layoutParams = lp2
            }
            addView(bar)
            addView(TextView(context).apply { text = "${s.hero.hp}/${s.hero.maxhp}"; textSize = 9f; setTextColor(Color.parseColor("#3d4560")); gravity = android.view.Gravity.CENTER })
        }
    }

    private fun makeEnemyCol(e: Enemy, s: GameState): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; gravity = android.view.Gravity.CENTER
            addView(TextView(context).apply { text = e.ico; textSize = 34f; gravity = android.view.Gravity.CENTER })
            addView(TextView(context).apply { text = e.name; textSize = 10f; setTextColor(Color.parseColor("#d8dce8")); gravity = android.view.Gravity.CENTER })
            val bar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = e.hp; progress = s.enemyHP; progressDrawable = context.getDrawable(R.drawable.hp_bar_enemy)
                val lp2 = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(6))
                lp2.setMargins(0, dpToPx(4), 0, 0)
                layoutParams = lp2
            }
            addView(bar)
            addView(TextView(context).apply { text = "${maxOf(0, s.enemyHP)}/${e.hp}"; textSize = 9f; setTextColor(Color.parseColor("#3d4560")); gravity = android.view.Gravity.CENTER })
        }
    }

    private fun addHeroUpgradeCard(parent: LinearLayout, idx: Int, u: HeroUpgradeDef, bought: Boolean, canBuy: Boolean) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dpToPx(10), dpToPx(9), dpToPx(10), dpToPx(9))
            background = context.getDrawable(R.drawable.card_bg)
            alpha = if (!bought && !canBuy) 0.4f else 1f
        }
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.setMargins(0, 0, 0, dpToPx(6))
        val ico = TextView(context).apply { text = u.ico; textSize = 22f }
        row.addView(ico, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0, 0, dpToPx(10), 0) })
        val body = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        body.addView(TextView(context).apply { text = u.name; setTextColor(Color.parseColor("#d8dce8")); textSize = 11f; typeface = android.graphics.Typeface.DEFAULT_BOLD })
        body.addView(TextView(context).apply { text = u.desc; setTextColor(Color.parseColor("#3d4560")); textSize = 10f })
        row.addView(body, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val costTv = TextView(context).apply {
            text = if (bought) "✓" else vm.engine.fmt(u.cost)
            setTextColor(if (bought) Color.parseColor("#4be8a0") else Color.parseColor("#e84b6b"))
            textSize = 13f; typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        row.addView(costTv)
        if (!bought) row.setOnClickListener { vm.buyHeroUpgrade(idx); refresh(); (activity as? MainActivity)?.updateAllStats() }
        parent.addView(row, lp)
    }

    private fun addLabel(parent: LinearLayout, text: String) {
        val tv = TextView(context).apply {
            this.text = text; setTextColor(Color.parseColor("#3d4560")); textSize = 10f; letterSpacing = 0.2f
        }
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.setMargins(0, 0, 0, dpToPx(8))
        parent.addView(tv, lp)
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()
}
