package com.xwray.groupie.example.viewbinding

import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.Group
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.OnItemClickListener
import com.xwray.groupie.OnItemLongClickListener
import com.xwray.groupie.Section
import com.xwray.groupie.TouchCallback
import com.xwray.groupie.example.core.InfiniteScrollListener
import com.xwray.groupie.example.core.Prefs
import com.xwray.groupie.example.core.SettingsActivity
import com.xwray.groupie.example.core.decoration.CarouselItemDecoration
import com.xwray.groupie.example.core.decoration.DebugItemDecoration
import com.xwray.groupie.example.core.decoration.SwipeTouchCallback
import com.xwray.groupie.example.viewbinding.databinding.ActivityMainBinding
import com.xwray.groupie.example.viewbinding.item.CardItem
import com.xwray.groupie.example.viewbinding.item.CarouselCardItem
import com.xwray.groupie.example.viewbinding.item.ColumnItem
import com.xwray.groupie.example.viewbinding.item.FullBleedCardItem
import com.xwray.groupie.example.viewbinding.item.HeaderItem
import com.xwray.groupie.example.viewbinding.item.HeartCardItem
import com.xwray.groupie.example.viewbinding.item.SmallCardItem
import com.xwray.groupie.example.viewbinding.item.SwipeToDeleteItem
import com.xwray.groupie.example.viewbinding.item.UpdatableItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val INSET_TYPE_KEY = "inset_type"
const val INSET = "inset"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var groupAdapter: GroupieAdapter
    private lateinit var prefs: Prefs

    private val gray: Int by lazy { ContextCompat.getColor(this, R.color.background) }
    private val betweenPadding: Int by lazy { resources.getDimensionPixelSize(R.dimen.padding_small) }
    private val rainbow200: IntArray by lazy { resources.getIntArray(R.array.rainbow_200) }
    private val rainbow500: IntArray by lazy { resources.getIntArray(R.array.rainbow_500) }

    private val infiniteLoadingSection: Section = Section(HeaderItem(R.string.infinite_loading))
    private val swipeSection: Section = Section(HeaderItem(R.string.swipe_to_delete))

    // Normally there's no need to hold onto a reference to this list, but for demonstration
    // purposes, we'll shuffle this list and post an update periodically
    private var updatableItems: ArrayList<UpdatableItem> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs.get(this)

        groupAdapter = GroupieAdapter().apply {
            setOnItemClickListener(onItemClickListener)
            setOnItemLongClickListener(onItemLongClickListener)
            spanCount = 12
        }
        populateAdapter()

        val layoutManager = GridLayoutManager(this, groupAdapter.spanCount).apply {
            spanSizeLookup = groupAdapter.spanSizeLookup
        }

        binding.recyclerView.also {
            it.layoutManager = layoutManager
            it.addItemDecoration(HeaderItemDecoration(gray, betweenPadding))
            it.addItemDecoration(InsetItemDecoration(gray, betweenPadding))
            it.addItemDecoration(DebugItemDecoration(it.context))
            it.adapter = groupAdapter
            it.addOnScrollListener(object : InfiniteScrollListener(layoutManager) {
                override fun onLoadMore(currentPage: Int) {
                    val color = rainbow200[currentPage % rainbow200.size]
                    for (i in 0..4) {
                        infiniteLoadingSection.add(CardItem(color))
                    }
                }
            })
            ItemTouchHelper(touchCallback).attachToRecyclerView(it)
        }
        binding.fab.setOnClickListener { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }
        prefs.registerListener(onSharedPrefChangeListener)

        binding.update.setOnClickListener { populateAdapter() }
    }

    private fun populateAdapter() {

        // Expandable group
        val expandableHeaderItem = ExpandableHeaderItem(
            ExpandableHeaderItemData(
                R.string.expanding_group,
                R.string.expanding_group_subtitle,
                headerId = 0
            )
        )
        groupAdapter.update(
            listOf(
                ExpandableGroup(
                    expandableHeaderItem,
                    true
                ).also { expandableGroup ->
                    for (i in 0..1) {
                        expandableGroup.add(CardItem(rainbow200[i]))
                    }
                }
            )
        )
    }

    private val onItemClickListener = OnItemClickListener { item, _ ->
        if (item is CardItem && item.text.isNotEmpty()) {
            Toast.makeText(this@MainActivity, item.text, Toast.LENGTH_SHORT).show()
        }
    }

    private val onItemLongClickListener = OnItemLongClickListener { item, _ ->
        if (item is CardItem && item.text.isNotEmpty()) {
            Toast.makeText(this@MainActivity, "Long clicked: " + item.text, Toast.LENGTH_SHORT).show()
            return@OnItemLongClickListener true
        }
        false
    }

    override fun onDestroy() {
        prefs.unregisterListener(onSharedPrefChangeListener)
        super.onDestroy()
    }

    private val touchCallback: TouchCallback by lazy {
        object : SwipeTouchCallback() {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val item = groupAdapter.getItem(viewHolder.adapterPosition)
                // Change notification to the adapter happens automatically when the section is
                // changed.
                swipeSection.remove(item)
            }
        }
    }

    private val onSharedPrefChangeListener = OnSharedPreferenceChangeListener { _, _ -> // This is pretty evil, try not to do this
        groupAdapter.notifyDataSetChanged()
    }
}
