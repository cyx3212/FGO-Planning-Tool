package com.ssttkkl.fgoplanningtool.ui.servantlist

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.ssttkkl.fgoplanningtool.R
import com.ssttkkl.fgoplanningtool.data.item.Item
import com.ssttkkl.fgoplanningtool.databinding.FragmentServantlistBinding
import com.ssttkkl.fgoplanningtool.resources.servant.Servant
import com.ssttkkl.fgoplanningtool.ui.MainActivity
import com.ssttkkl.fgoplanningtool.ui.servantfilter.ServantFilterFragment
import com.ssttkkl.fgoplanningtool.ui.utils.BackHandler
import com.ssttkkl.fgoplanningtool.ui.utils.CommonRecViewItemDecoration
import com.ssttkkl.fgoplanningtool.ui.utils.NoInterfaceImplException
import java.lang.Exception

class ServantListFragment : Fragment(),
        BackHandler,
        ServantFilterFragment.Callback,
        LifecycleOwner {
    interface OnClickServantListener {
        fun onClickServant(servantID: Int)
    }

    private var listener: OnClickServantListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = when {
            parentFragment is OnClickServantListener -> parentFragment as OnClickServantListener
            activity is OnClickServantListener -> activity as OnClickServantListener
            else -> throw NoInterfaceImplException(OnClickServantListener::class)
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    private val servantFilterFragment
        get() = childFragmentManager.findFragmentById(R.id.servantFilterFragment) as? ServantFilterFragment

    private lateinit var binding: FragmentServantlistBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentServantlistBinding.inflate(inflater, container, false)
        binding.viewModel = ViewModelProviders.of(this)[ServantListFragmentViewModel::class.java].apply {
            hiddenServantIDs.value = arguments?.getIntArray(KEY_HIDDEN)?.toHashSet() ?: setOf()
            start(context!!, parentFragment?.run { this::class.qualifiedName } ?: "")
            viewType.observe(this@ServantListFragment, Observer {
                onViewTypeChanged(it ?: return@Observer)
            })
            informClickServantEvent.observe(this@ServantListFragment, Observer {
                onInformClickServant(it ?: return@Observer)
            })
            isDefaultFilterState.observe(this@ServantListFragment, Observer {
                onIsDefaultFilterStateChanged()
            })
            servantFilterFragment?.originServantIDs = originServantIDs
        }
        binding.setLifecycleOwner(this)
        return binding.root
    }

    private lateinit var itemDecoration: CommonRecViewItemDecoration

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)

        itemDecoration = CommonRecViewItemDecoration(context!!)
        binding.recView.apply {
            adapter = ServantListAdapter(context!!, this@ServantListFragment, binding.viewModel!!)
            setHasFixedSize(true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.servantlist, menu)

        when (binding.viewModel?.viewType?.value) {
            ViewType.List -> menu.findItem(R.id.switchToListView)?.isVisible = false
            ViewType.Grid -> menu.findItem(R.id.switchToGridView)?.isVisible = false
        }

        menu.findItem(R.id.sortAndFilter)?.setIcon(if (binding.viewModel?.isDefaultFilterState?.value == true)
            R.drawable.ic_sort_white_24dp
        else
            R.drawable.ic_sort_coloraccent_bg_24dp)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sortAndFilter -> {
                if (binding.drawerlayout.isDrawerOpen(GravityCompat.END))
                    binding.drawerlayout.closeDrawer(GravityCompat.END)
                else
                    binding.drawerlayout.openDrawer(GravityCompat.END)
            }
            R.id.switchToListView -> binding.viewModel?.onClickSwitchToListView()
            R.id.switchToGridView -> binding.viewModel?.onClickSwitchToGridView()
            else -> super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onBackPressed(): Boolean {
        return if (binding.drawerlayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerlayout.closeDrawer(GravityCompat.END)
            true
        } else false
    }

    override fun onFilter(filtered: List<Servant>, isDefaultState: Boolean) {
        binding.viewModel?.onFiltered(filtered, isDefaultState)
    }

    override fun onRequestCostItems(servant: Servant): Collection<Item> {
        return binding.viewModel?.onRequestCostItems(servant) ?: listOf()
    }

    private fun onViewTypeChanged(viewType: ViewType) {
        when (viewType) {
            ViewType.Grid -> {
                binding.recView.layoutManager = FlexboxLayoutManager(context).apply {
                    flexWrap = FlexWrap.WRAP
                    justifyContent = JustifyContent.SPACE_AROUND
                }
                binding.recView.removeItemDecoration(itemDecoration)
            }
            ViewType.List -> {
                binding.recView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                binding.recView.addItemDecoration(itemDecoration)
            }
        }
        activity?.invalidateOptionsMenu()
    }

    private fun onInformClickServant(servantID: Int) {
        listener?.onClickServant(servantID)
    }

    private fun onIsDefaultFilterStateChanged() {
        (activity as? MainActivity)?.invalidateOptionsMenu()
    }

    var hiddenServantIDs: Set<Int>
        get() = arguments?.getIntArray(KEY_HIDDEN)?.toSet() ?: setOf()
        set(value) {
            (arguments
                    ?: Bundle().also { arguments = it }).putIntArray(KEY_HIDDEN, value.toIntArray())
            if (::binding.isInitialized)
                binding.viewModel?.hiddenServantIDs?.value = hiddenServantIDs
        }


    companion object {
        private const val KEY_HIDDEN = "hidden"
    }
}