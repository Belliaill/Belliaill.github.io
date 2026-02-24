package com.taprise.game

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

// ─── Daily Quests ─────────────────────────────────────────────────────────────
class DailyFragment : Fragment(), Refreshable {
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
        val s = vm.state
        vm.engine.updateQuests()

        addLabel(c, "Daily Quests")

        // Gems row
        val gemsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10)); background = context.getDrawable(R.drawable.card_bg)
        }
        gemsRow.addView(TextView(context).apply { text = "💎"; textSize = 22f })
        gemsRow.addView(TextView(context).apply {
            text = "Quest Gems earned"; setTextColor(Color.parseColor("#3d4560")); textSize = 12f
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also { it.setMargins(dp(8),0,0,0) })
        gemsRow.addView(TextView(context).apply {
            text = s.gems.toString(); setTextColor(Color.parseColor("#4be8a0")); textSize = 18f; typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        c.addView(gemsRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,0,0,dp(10)) })

        // Quest cards
        s.quests.forEachIndexed { i, q ->
            val pct = ((q.current.toDouble() / q.target) * 100).toInt().coerceIn(0, 100)
            val done = q.current >= q.target

            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(12), dp(12), dp(12))
                if (q.claimed) setBackgroundColor(Color.parseColor("#071e0e"))
                else background = context.getDrawable(R.drawable.card_bg)
            }

            val topRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.TOP }
            topRow.addView(TextView(context).apply { text = q.def.ico; textSize = 24f })
            val info = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            info.addView(TextView(context).apply { text = q.def.name; setTextColor(Color.parseColor("#d8dce8")); textSize = 12f; typeface = android.graphics.Typeface.DEFAULT_BOLD })
            info.addView(TextView(context).apply { text = q.desc; setTextColor(Color.parseColor("#3d4560")); textSize = 11f })
            info.addView(TextView(context).apply { text = "Reward: ${q.rewardType} +${q.rewardAmt}"; setTextColor(Color.parseColor("#4be8a0")); textSize = 10f })
            topRow.addView(info, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also { it.setMargins(dp(10),0,0,0) })
            card.addView(topRow)

            val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100; progress = pct; progressDrawable = context.getDrawable(R.drawable.xp_bar_drawable)
            }
            card.addView(progressBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(5)).also { it.setMargins(0,dp(8),0,dp(4)) })

            val statusTv = TextView(context).apply {
                text = "${q.current} / ${q.target}${if (q.claimed) " ✓" else ""}"
                setTextColor(Color.parseColor("#3d4560")); textSize = 10f; gravity = android.view.Gravity.END
            }
            card.addView(statusTv, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

            if (done && !q.claimed) {
                val claimBtn = Button(context).apply {
                    text = "🎁 Claim Reward"
                    setTextColor(Color.parseColor("#4be8a0")); textSize = 11f
                    setBackgroundColor(Color.parseColor("#071e0e"))
                    setOnClickListener { vm.claimQuest(i); refresh(); (activity as? MainActivity)?.updateAllStats() }
                }
                card.addView(claimBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,dp(8),0,0) })
            }

            c.addView(card, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,0,0,dp(8)) })
        }

        // Timer
        val refreshIn = Math.max(0L, Math.ceil((s.questsRefreshAt - System.currentTimeMillis()) / 1000.0).toLong())
        c.addView(TextView(context).apply {
            text = "Quests reset in: ${vm.engine.fmtTime(refreshIn)}"
            setTextColor(Color.parseColor("#3d4560")); textSize = 10f; gravity = android.view.Gravity.CENTER
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun addLabel(parent: LinearLayout, text: String) {
        val tv = TextView(context).apply { this.text = text; setTextColor(Color.parseColor("#3d4560")); textSize = 10f; letterSpacing = 0.2f }
        parent.addView(tv, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,0,0,dp(8)) })
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}

// ─── Prestige / Ascension ─────────────────────────────────────────────────────
class PrestigeFragment : Fragment(), Refreshable {
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
        val s = vm.state

        val wrap = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; gravity = android.view.Gravity.CENTER
            setPadding(dp(16), dp(10), dp(16), dp(10))
        }

        wrap.addView(TextView(context).apply {
            text = "Ascension"; setTextColor(Color.parseColor("#b87fff")); textSize = 26f
            typeface = android.graphics.Typeface.DEFAULT_BOLD; gravity = android.view.Gravity.CENTER
        })

        wrap.addView(TextView(context).apply {
            text = "✨"; textSize = 42f; gravity = android.view.Gravity.CENTER
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,dp(14),0,dp(14)) })

        wrap.addView(TextView(context).apply {
            text = "Reset your mortal progress and ascend to a higher plane.\nMarket, Guild ranks, and Hero level survive."
            setTextColor(Color.parseColor("#3d4560")); textSize = 12f; gravity = android.view.Gravity.CENTER
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,0,0,dp(16)) })

        val bonusCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.parseColor("#120a2a")); gravity = android.view.Gravity.CENTER
        }
        bonusCard.addView(TextView(context).apply {
            text = "Current: +${s.prestigeCount * 25}% · Next: +${(s.prestigeCount + 1) * 25}%"
            setTextColor(Color.parseColor("#b87fff")); textSize = 12f; typeface = android.graphics.Typeface.DEFAULT_BOLD; gravity = android.view.Gravity.CENTER
        })
        wrap.addView(bonusCard, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,0,0,dp(16)) })

        val ascendBtn = Button(context).apply {
            text = "ASCEND"
            isEnabled = s.level >= 20
            setTextColor(Color.WHITE); textSize = 16f; typeface = android.graphics.Typeface.DEFAULT_BOLD
            setBackgroundColor(if (s.level >= 20) Color.parseColor("#6d28d9") else Color.parseColor("#2a2a2a"))
            setOnClickListener { vm.doPrestige(); refresh(); (activity as? MainActivity)?.updateAllStats() }
        }
        wrap.addView(ascendBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)).also { it.setMargins(0,0,0,dp(10)) })

        wrap.addView(TextView(context).apply {
            text = if (s.level >= 20) "Ready! Upgrades reset. Hero level, Market & Quests survive."
                   else "Reach Level 20 (currently Level ${s.level})"
            setTextColor(Color.parseColor("#3d4560")); textSize = 11f; gravity = android.view.Gravity.CENTER
        })

        // Achievements
        val achLabel = TextView(context).apply {
            text = "TROPHIES"; setTextColor(Color.parseColor("#3d4560")); textSize = 10f; letterSpacing = 0.2f; gravity = android.view.Gravity.CENTER
        }
        wrap.addView(achLabel, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.setMargins(0,dp(20),0,dp(10)) })

        val achGrid = GridLayout(context).apply { columnCount = 2 }
        ACHS.forEachIndexed { i, a ->
            val unlocked = s.achievements[i]
            val cell = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL; gravity = android.view.Gravity.CENTER
                setPadding(dp(10), dp(10), dp(10), dp(10))
                setBackgroundColor(if (unlocked) Color.parseColor("#071e0e") else Color.parseColor("#13161f"))
            }
            val ico = TextView(context).apply {
                text = a.ico; textSize = 26f; gravity = android.view.Gravity.CENTER
                alpha = if (unlocked) 1f else 0.3f
            }
            cell.addView(ico)
            cell.addView(TextView(context).apply {
                text = a.name; textSize = 10f; typeface = android.graphics.Typeface.DEFAULT_BOLD; gravity = android.view.Gravity.CENTER
                setTextColor(if (unlocked) Color.parseColor("#4be8a0") else Color.parseColor("#d8dce8"))
            })
            cell.addView(TextView(context).apply {
                text = a.desc; textSize = 9f; setTextColor(Color.parseColor("#3d4560")); gravity = android.view.Gravity.CENTER
            })
            val lp = GridLayout.LayoutParams().apply { width = 0; columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); setMargins(dp(3),dp(3),dp(3),dp(3)) }
            achGrid.addView(cell, lp)
        }
        wrap.addView(achGrid)
        c.addView(wrap)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
