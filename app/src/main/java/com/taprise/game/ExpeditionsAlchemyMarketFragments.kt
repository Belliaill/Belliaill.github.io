package com.taprise.game

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import kotlin.math.max
import kotlin.math.min

// ─── Expeditions ──────────────────────────────────────────────────────────────
class ExpeditionsFragment : Fragment(), Refreshable {
    private lateinit var vm: GameViewModel
    private var container: LinearLayout? = null

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        vm = ViewModelProvider(requireActivity())[GameViewModel::class.java]
        val root = inflater.inflate(R.layout.fragment_generic, parent, false)
        container = root.findViewById(R.id.container)
        vm.statsUpdate.observe(viewLifecycleOwner) { refresh() }
        refresh()
        return root
    }

    override fun refresh() {
        val c = container ?: return
        c.removeAllViews()
        addLabel(c, "Expeditions")
        val now = System.currentTimeMillis()
        vm.state.expeditions.forEachIndexed { i, exp ->
            val locked = vm.state.wave < exp.def.minWave
            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                background = context.getDrawable(R.drawable.card_bg)
            }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, 0, dp(8))

            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
            val ico = TextView(context).apply { text = if (locked) "🔒" else exp.def.ico; textSize = 28f }
            row.addView(ico, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,0,dp(10),0) })

            val info = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            val nameTv = "${exp.def.name}${if (locked) " (Wave ${exp.def.minWave})" else ""}"
            info.addView(TextView(context).apply { text = nameTv; setTextColor(Color.parseColor("#d8dce8")); textSize = 12f; typeface = android.graphics.Typeface.DEFAULT_BOLD })
            info.addView(TextView(context).apply { text = exp.def.desc; setTextColor(Color.parseColor("#3d4560")); textSize = 11f })
            val rewardsRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            exp.def.rewards.forEach { r ->
                val tv = TextView(context).apply {
                    text = r; setTextColor(Color.parseColor("#e8b84b")); textSize = 10f
                    setPadding(dp(7), dp(2), dp(7), dp(2)); setBackgroundColor(Color.parseColor("#1a1505"))
                }
                rewardsRow.addView(tv, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,dp(4),dp(4),0) })
            }
            info.addView(rewardsRow)
            row.addView(info, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            card.addView(row)

            when {
                exp.done -> {
                    val btn = Button(context).apply {
                        text = "🎁 Claim Rewards"
                        setTextColor(Color.parseColor("#4be8a0")); textSize = 11f
                        setBackgroundColor(Color.parseColor("#0a1e12"))
                        setOnClickListener { vm.claimExpedition(i); refresh(); (activity as? MainActivity)?.updateAllStats() }
                    }
                    card.addView(btn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,dp(8),0,0) })
                }
                exp.active -> {
                    val remaining = max(0, Math.ceil((exp.endsAt - now) / 1000.0).toLong())
                    val pct = min(100, ((1 - (exp.endsAt - now).toDouble() / (exp.def.durationSec * 1000)) * 100).toInt().coerceAtLeast(0))
                    val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                        max = 100; progress = pct; progressDrawable = context.getDrawable(R.drawable.xp_bar_drawable)
                    }
                    val timerTv = TextView(context).apply { text = "⏳ ${vm.engine.fmtTime(remaining)} remaining"; setTextColor(Color.parseColor("#4b9de8")); textSize = 11f }
                    card.addView(timerTv, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,dp(8),0,0) })
                    card.addView(progressBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(4)).also { it.setMargins(0,dp(4),0,0) })
                }
                else -> {
                    val btn = Button(context).apply {
                        text = "→ Start (${vm.engine.fmtTime(exp.def.durationSec)})"
                        isEnabled = !locked
                        setTextColor(if (locked) Color.parseColor("#3d4560") else Color.parseColor("#4b9de8")); textSize = 11f
                        setBackgroundColor(Color.parseColor("#080e18"))
                        if (!locked) setOnClickListener { vm.startExpedition(i); refresh() }
                    }
                    card.addView(btn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,dp(8),0,0) })
                }
            }
            c.addView(card, lp)
        }
    }

    private fun addLabel(parent: LinearLayout, text: String) {
        val tv = TextView(context).apply { this.text = text; setTextColor(Color.parseColor("#3d4560")); textSize = 10f; letterSpacing = 0.2f }
        parent.addView(tv, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,0,0,dp(8)) })
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}

// ─── Alchemy ──────────────────────────────────────────────────────────────────
class AlchemyFragment : Fragment(), Refreshable {
    private lateinit var vm: GameViewModel
    private var container: LinearLayout? = null

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        vm = ViewModelProvider(requireActivity())[GameViewModel::class.java]
        val root = inflater.inflate(R.layout.fragment_generic, parent, false)
        container = root.findViewById(R.id.container)
        refresh()
        return root
    }

    override fun refresh() {
        val c = container ?: return
        c.removeAllViews()
        val s = vm.state
        val now = System.currentTimeMillis()

        addLabel(c, "Alchemy Lab")

        // Active effects
        val activeEffects = s.activeEffects.entries.filter { it.value.expiresAt > now }
        if (activeEffects.isNotEmpty()) {
            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            activeEffects.forEach { (k, v) ->
                val tv = TextView(context).apply {
                    text = "$k ${Math.ceil((v.expiresAt - now) / 1000.0).toLong()}s"
                    setTextColor(Color.parseColor("#a04be8")); textSize = 10f
                    setPadding(dp(8), dp(2), dp(8), dp(2)); setBackgroundColor(Color.parseColor("#140a20"))
                }
                row.addView(tv, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,0,dp(4),0) })
            }
            c.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,0,0,dp(10)) })
        }

        // Cauldron card
        val cauldron = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; gravity = android.view.Gravity.CENTER
            setPadding(dp(14), dp(14), dp(14), dp(14)); background = context.getDrawable(R.drawable.card_bg)
        }
        cauldron.addView(TextView(context).apply { text = "🪄"; textSize = 44f; gravity = android.view.Gravity.CENTER })

        // Slots
        val slotsRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER }
        s.slots.forEachIndexed { i, slotId ->
            val relic = slotId?.let { id -> RELIC_DEFS.find { it.id == id } }
            val slot = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL; gravity = android.view.Gravity.CENTER
                setPadding(dp(4), dp(10), dp(4), dp(10)); minimumHeight = dp(64)
                setBackgroundColor(Color.parseColor("#140a20"))
            }
            slot.addView(TextView(context).apply {
                text = relic?.ico ?: "+"; textSize = if (relic != null) 24f else 20f; gravity = android.view.Gravity.CENTER
                setTextColor(if (relic != null) Color.WHITE else Color.parseColor("#3d4560"))
            })
            if (relic != null) slot.addView(TextView(context).apply { text = relic.name.split(" ")[0]; setTextColor(Color.parseColor("#a04be8")); textSize = 9f; gravity = android.view.Gravity.CENTER })
            slot.setOnClickListener { if (slotId != null) { vm.clearSlot(i); refresh() } }
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp.setMargins(dp(4), 0, dp(4), 0)
            slotsRow.addView(slot, lp)
        }
        cauldron.addView(slotsRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,dp(10),0,dp(10)) })

        val brewBtn = Button(context).apply {
            text = "⚗️ Brew Potion"
            val filled = s.slots.count { it != null }
            isEnabled = filled >= 2
            setTextColor(if (filled >= 2) Color.parseColor("#a04be8") else Color.parseColor("#3d4560")); textSize = 11f
            setBackgroundColor(Color.parseColor("#140a20"))
            setOnClickListener { vm.brew(); refresh() }
        }
        cauldron.addView(brewBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        c.addView(cauldron, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,0,0,dp(10)) })

        // Relic Inventory
        addLabel(c, "Relic Inventory")
        val invRelics = s.relicInventory.entries.filter { it.value > 0 }
        if (invRelics.isEmpty()) {
            c.addView(TextView(context).apply { text = "No relics yet. Complete expeditions or reach wave milestones."; setTextColor(Color.parseColor("#3d4560")); textSize = 12f })
        } else {
            val grid = GridLayout(context).apply { columnCount = 3 }
            invRelics.forEach { (id, qty) ->
                val relic = RELIC_DEFS.find { it.id == id } ?: return@forEach
                val cell = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL; gravity = android.view.Gravity.CENTER
                    setPadding(dp(6), dp(10), dp(6), dp(10)); background = context.getDrawable(R.drawable.card_bg)
                }
                cell.addView(TextView(context).apply { text = relic.ico; textSize = 22f; gravity = android.view.Gravity.CENTER })
                cell.addView(TextView(context).apply { text = relic.name.split(" ")[0]; setTextColor(Color.parseColor("#d8dce8")); textSize = 9f; gravity = android.view.Gravity.CENTER })
                cell.addView(TextView(context).apply { text = "×$qty"; setTextColor(Color.parseColor("#3d4560")); textSize = 9f; gravity = android.view.Gravity.CENTER })
                cell.setOnClickListener { vm.addToSlot(id); refresh() }
                val lp = GridLayout.LayoutParams().apply { width = 0; columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); setMargins(dp(2),dp(2),dp(2),dp(2)) }
                grid.addView(cell, lp)
            }
            c.addView(grid, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }

        // Potion inventory
        if (s.potions.isNotEmpty()) {
            addLabel(c, "Your Potions")
            s.potions.forEachIndexed { i, pot ->
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(dp(10), dp(10), dp(10), dp(10)); background = context.getDrawable(R.drawable.card_bg)
                }
                row.addView(TextView(context).apply { text = pot.recipe.ico; textSize = 22f })
                val body = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
                body.addView(TextView(context).apply { text = pot.recipe.name; setTextColor(Color.parseColor("#d8dce8")); textSize = 11f })
                body.addView(TextView(context).apply { text = pot.recipe.desc; setTextColor(Color.parseColor("#3d4560")); textSize = 10f })
                row.addView(body, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also { it.setMargins(dp(10),0,0,0) })
                val useBtn = TextView(context).apply { text = "USE"; setTextColor(Color.parseColor("#a04be8")); textSize = 11f; typeface = android.graphics.Typeface.DEFAULT_BOLD }
                row.addView(useBtn)
                row.setOnClickListener { vm.usePotion(i); refresh() }
                c.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,0,0,dp(6)) })
            }
        }

        // Recipes
        addLabel(c, "Known Recipes")
        POTION_RECIPES.forEach { p ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(10), dp(10), dp(10)); background = context.getDrawable(R.drawable.card_bg)
            }
            row.addView(TextView(context).apply { text = p.ico; textSize = 22f })
            val body = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            body.addView(TextView(context).apply { text = p.name; setTextColor(Color.parseColor("#d8dce8")); textSize = 11f })
            body.addView(TextView(context).apply { text = p.desc; setTextColor(Color.parseColor("#3d4560")); textSize = 10f })
            val ingIcos = p.ingredients.mapNotNull { id -> RELIC_DEFS.find { it.id == id }?.ico }.joinToString(" + ")
            body.addView(TextView(context).apply { text = "🧪 $ingIcos"; setTextColor(Color.parseColor("#a04be8")); textSize = 10f })
            row.addView(body, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also { it.setMargins(dp(10),0,0,0) })
            c.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,0,0,dp(6)) })
        }
    }

    private fun addLabel(parent: LinearLayout, text: String) {
        val tv = TextView(context).apply { this.text = text; setTextColor(Color.parseColor("#3d4560")); textSize = 10f; letterSpacing = 0.2f }
        parent.addView(tv, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,dp(12),0,dp(8)) })
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}

// ─── Market ───────────────────────────────────────────────────────────────────
class MarketFragment : Fragment(), Refreshable {
    private lateinit var vm: GameViewModel
    private var container: LinearLayout? = null

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        vm = ViewModelProvider(requireActivity())[GameViewModel::class.java]
        val root = inflater.inflate(R.layout.fragment_generic, parent, false)
        container = root.findViewById(R.id.container)
        vm.uiEvent.observe(viewLifecycleOwner) { if (it is UiEvent.MarketUpdated) refresh() }
        refresh()
        return root
    }

    override fun refresh() {
        val c = container ?: return
        c.removeAllViews()
        val s = vm.state

        addLabel(c, "Relics Market")

        // Portfolio summary
        val pv = vm.engine.portfolioValue()
        val totalOwned = s.portfolio.values.sum()
        val summaryCard = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; setPadding(dp(12), dp(10), dp(12), dp(10))
            background = context.getDrawable(R.drawable.card_bg)
        }
        listOf(
            "Portfolio" to vm.engine.fmt(pv),
            "Owned" to totalOwned.toString(),
            "Net Worth" to vm.engine.fmt(s.coins + pv)
        ).forEach { (label, value) ->
            val stat = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; gravity = android.view.Gravity.CENTER }
            stat.addView(TextView(context).apply { text = value; setTextColor(Color.parseColor("#d8dce8")); textSize = 16f; typeface = android.graphics.Typeface.DEFAULT_BOLD; gravity = android.view.Gravity.CENTER })
            stat.addView(TextView(context).apply { text = label; setTextColor(Color.parseColor("#3d4560")); textSize = 8f; letterSpacing = 0.1f; gravity = android.view.Gravity.CENTER })
            summaryCard.addView(stat, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        c.addView(summaryCard, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,0,0,dp(8)) })

        // Relic list
        s.relics.forEach { r ->
            val price = r.price.toLong()
            val firstPrice = r.history.firstOrNull() ?: r.def.basePrice.toDouble()
            val pct = ((r.price - firstPrice) / firstPrice * 100)
            val col = if (r.dir > 0) Color.parseColor("#4be8a0") else if (r.dir < 0) Color.parseColor("#e84b6b") else Color.parseColor("#3d4560")
            val arrow = if (r.dir > 0) "▲" else if (r.dir < 0) "▼" else "─"
            val owned = s.portfolio[r.def.id] ?: 0

            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(12), dp(12), dp(12))
                background = context.getDrawable(R.drawable.card_bg)
            }
            val topRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
            topRow.addView(TextView(context).apply { text = r.def.ico; textSize = 24f })
            val info = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            info.addView(TextView(context).apply { text = r.def.name; setTextColor(Color.parseColor("#d8dce8")); textSize = 12f; typeface = android.graphics.Typeface.DEFAULT_BOLD })
            info.addView(TextView(context).apply { text = r.def.flavor; setTextColor(Color.parseColor("#3d4560")); textSize = 10f })
            topRow.addView(info, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also { it.setMargins(dp(10),0,0,0) })
            val priceCol = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; gravity = android.view.Gravity.END }
            priceCol.addView(TextView(context).apply { text = vm.engine.fmt(price); setTextColor(col); textSize = 16f; typeface = android.graphics.Typeface.DEFAULT_BOLD })
            priceCol.addView(TextView(context).apply { text = "$arrow ${"%.1f".format(kotlin.math.abs(pct))}%"; setTextColor(col); textSize = 10f; typeface = android.graphics.Typeface.DEFAULT_BOLD })
            topRow.addView(priceCol)
            card.addView(topRow)

            // Sparkline (simplified text representation)
            val sparkTv = TextView(context).apply {
                text = buildSparkline(r.history.toList())
                setTextColor(col); textSize = 8f; letterSpacing = 0.05f
            }
            card.addView(sparkTv, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,dp(4),0,dp(6)) })

            // Buy/Sell buttons
            val btnRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
            val buyBtn = Button(context).apply {
                text = "Buy ${vm.engine.fmt(price)}"
                isEnabled = s.coins >= price
                setTextColor(Color.parseColor("#4be8a0")); textSize = 10f
                setBackgroundColor(Color.parseColor("#071e0e"))
                setOnClickListener { vm.buyRelic(r.def.id); refresh(); (activity as? MainActivity)?.updateAllStats() }
            }
            val sellBtn = Button(context).apply {
                text = "Sell ${vm.engine.fmt(price)}"
                isEnabled = owned > 0
                setTextColor(Color.parseColor("#e84b6b")); textSize = 10f
                setBackgroundColor(Color.parseColor("#1e0707"))
                setOnClickListener { vm.sellRelic(r.def.id); refresh(); (activity as? MainActivity)?.updateAllStats() }
            }
            btnRow.addView(buyBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also { it.setMargins(0,0,dp(4),0) })
            btnRow.addView(sellBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            if (owned > 0) {
                val ownedTv = TextView(context).apply { text = "×$owned"; setTextColor(Color.parseColor("#4b9de8")); textSize = 12f; typeface = android.graphics.Typeface.DEFAULT_BOLD }
                btnRow.addView(ownedTv, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(dp(6),0,0,0) })
            }
            card.addView(btnRow)
            c.addView(card, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,0,0,dp(8)) })
        }
    }

    private fun buildSparkline(history: List<Double>): String {
        if (history.isEmpty()) return ""
        val min = history.minOrNull() ?: 0.0
        val max = history.maxOrNull() ?: 0.0
        val range = max - min
        val chars = listOf('▁','▂','▃','▄','▅','▆','▇','█')
        return history.joinToString("") { v ->
            val idx = if (range == 0.0) 3 else ((v - min) / range * 7).toInt().coerceIn(0, 7)
            chars[idx].toString()
        }
    }

    private fun addLabel(parent: LinearLayout, text: String) {
        val tv = TextView(context).apply { this.text = text; setTextColor(Color.parseColor("#3d4560")); textSize = 10f; letterSpacing = 0.2f }
        parent.addView(tv, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,0,0,dp(8)) })
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
