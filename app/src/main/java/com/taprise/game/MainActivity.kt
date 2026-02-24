package com.taprise.game

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2

class MainActivity : AppCompatActivity() {

    private lateinit var vm: GameViewModel
    private lateinit var viewPager: ViewPager2

    // Header
    private lateinit var hCoins: TextView
    private lateinit var hGems: TextView
    private lateinit var prestigeGem: TextView
    private lateinit var statCpc: TextView
    private lateinit var statCps: TextView
    private lateinit var statWave: TextView
    private lateinit var lvLabel: TextView
    private lateinit var lvXp: TextView
    private lateinit var xpBar: ProgressBar
    private lateinit var cpsLabel: TextView
    private lateinit var tapBtn: Button
    private lateinit var toast: TextView

    private val tabs = listOf("Forge","Combat","Quests","Alchemy","Market","Daily","Ascend")
    private val tabIcons = listOf("⚒️","⚔️","🗺️","⚗️","📈","🎲","✨")
    private val tabViews = mutableListOf<LinearLayout>()

    private val toastHandler = Handler(Looper.getMainLooper())
    private var floaterHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.statusBarColor = Color.parseColor("#06070a")

        vm = ViewModelProvider(this)[GameViewModel::class.java]
        bindViews()
        setupTabs()
        setupViewPager()
        setupTapButton()
        observeViewModel()
        updateAllStats()
    }

    private fun bindViews() {
        hCoins     = findViewById(R.id.hCoins)
        hGems      = findViewById(R.id.hGems)
        prestigeGem= findViewById(R.id.prestigeGem)
        statCpc    = findViewById(R.id.statCpc)
        statCps    = findViewById(R.id.statCps)
        statWave   = findViewById(R.id.statWave)
        lvLabel    = findViewById(R.id.lvLabel)
        lvXp       = findViewById(R.id.lvXp)
        xpBar      = findViewById(R.id.xpBar)
        cpsLabel   = findViewById(R.id.cpsLabel)
        tapBtn     = findViewById(R.id.tapBtn)
        toast      = findViewById(R.id.toast)
    }

    private fun setupTabs() {
        val tabBar = findViewById<LinearLayout>(R.id.tabBar)
        tabBar.removeAllViews()
        tabs.forEachIndexed { i, label ->
            val tab = layoutInflater.inflate(R.layout.tab_item, tabBar, false) as LinearLayout
            tab.findViewById<TextView>(R.id.tabIcon).text = tabIcons[i]
            tab.findViewById<TextView>(R.id.tabLabel).text = label
            tab.setOnClickListener { viewPager.currentItem = i }
            tabBar.addView(tab)
            tabViews.add(tab)
        }
        updateTabSelection(0)
    }

    private fun updateTabSelection(selected: Int) {
        tabViews.forEachIndexed { i, tab ->
            val label = tab.findViewById<TextView>(R.id.tabLabel)
            val bg = if (i == selected) Color.parseColor("#13161f") else Color.TRANSPARENT
            tab.setBackgroundColor(bg)
            label.setTextColor(if (i == selected) Color.parseColor("#e8b84b") else Color.parseColor("#3d4560"))
        }
    }

    private fun setupViewPager() {
        viewPager = findViewById(R.id.viewPager)
        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 7
            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> ForgeFragment()
                1 -> CombatFragment()
                2 -> ExpeditionsFragment()
                3 -> AlchemyFragment()
                4 -> MarketFragment()
                5 -> DailyFragment()
                6 -> PrestigeFragment()
                else -> ForgeFragment()
            }
        }
        viewPager.isUserInputEnabled = false
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) { updateTabSelection(position) }
        })
    }

    private fun setupTapButton() {
        tapBtn.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(80).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                    if (event.action == MotionEvent.ACTION_UP) {
                        val earned = vm.onTap()
                        spawnFloater(event.rawX, event.rawY, "+${vm.engine.fmt(earned)}")
                    }
                }
            }
            true
        }
    }

    private fun observeViewModel() {
        vm.statsUpdate.observe(this) { updateAllStats() }
        vm.uiEvent.observe(this) { event ->
            when (event) {
                is UiEvent.Toast -> showToast(event.msg, event.type)
                is UiEvent.MarketUpdated -> refreshCurrentFragment()
                is UiEvent.ExpeditionsUpdated -> refreshCurrentFragment()
                is UiEvent.AlchemyUpdated -> refreshCurrentFragment()
            }
        }
    }

    private fun refreshCurrentFragment() {
        val frag = supportFragmentManager.findFragmentByTag("f${viewPager.currentItem}")
        (frag as? Refreshable)?.refresh()
    }

    fun updateAllStats() {
        val s = vm.state
        val (cpc, cps) = vm.engine.getStats()
        hCoins.text = vm.engine.fmt(s.coins)
        hGems.text = s.gems.toString()
        prestigeGem.text = s.prestigeCount.toString()
        statCpc.text = vm.engine.fmt(cpc)
        statCps.text = vm.engine.fmt(cps)
        statWave.text = s.wave.toString()
        lvLabel.text = "Level ${s.level}"
        val need = vm.engine.xpFor(s.level)
        lvXp.text = "${s.xp} / $need XP"
        xpBar.progress = ((s.xp.toDouble() / need) * 100).toInt().coerceIn(0, 100)
        cpsLabel.text = "+${vm.engine.fmt(cps)} coins/sec"
    }

    private fun showToast(msg: String, type: String) {
        val bg = when (type) {
            "bad"     -> Color.parseColor("#e84b6b")
            "info"    -> Color.parseColor("#4b9de8")
            "prestige"-> Color.parseColor("#b87fff")
            "combat"  -> Color.parseColor("#e84b6b")
            "quest"   -> Color.parseColor("#4be8a0")
            else      -> Color.parseColor("#4be8a0")
        }
        val tc = when (type) {
            "info","prestige" -> Color.WHITE
            else -> Color.parseColor("#071008")
        }
        toast.text = msg
        toast.setBackgroundColor(bg)
        toast.setTextColor(tc)
        toast.visibility = View.VISIBLE
        toast.alpha = 1f
        toastHandler.removeCallbacksAndMessages(null)
        toastHandler.postDelayed({
            toast.animate().alpha(0f).setDuration(400).withEndAction { toast.visibility = View.GONE }.start()
        }, 2200)
    }

    private fun spawnFloater(x: Float, y: Float, text: String) {
        val tv = TextView(this)
        tv.text = text
        tv.setTextColor(Color.parseColor("#e8b84b"))
        tv.textSize = 18f
        tv.setShadowLayer(8f, 0f, 0f, Color.parseColor("#e8b84b"))
        val rootView = window.decorView as ViewGroup
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.leftMargin = (x - 30).toInt()
        params.topMargin = (y - 40).toInt()
        (rootView as? FrameLayout)?.addView(tv, params)
        tv.animate()
            .translationY(-120f)
            .alpha(0f)
            .setDuration(900)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { rootView.removeView(tv) }
            .start()
    }
}

interface Refreshable { fun refresh() }
