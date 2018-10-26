package com.shuyu.github.kotlin.module.base

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.databinding.ViewDataBinding
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import com.shuyu.commonrecycler.BindSuperAdapter
import com.shuyu.commonrecycler.BindSuperAdapterManager
import com.shuyu.commonrecycler.listener.OnItemClickListener
import com.shuyu.commonrecycler.listener.OnLoadingListener
import com.shuyu.github.kotlin.ui.holder.base.BindCustomLoadMoreFooter
import com.shuyu.github.kotlin.ui.holder.base.BindCustomRefreshHeader
import com.shuyu.github.kotlin.ui.holder.base.BindingDataRecyclerManager
import javax.inject.Inject

/**
 * Created by guoshuyu
 * Date: 2018-10-19
 */
abstract class BaseListFragment<T : ViewDataBinding, R : BaseViewModel> : BaseFragment<T>(), OnItemClickListener, OnLoadingListener {

    protected var normalAdapterManager by autoCleared<BindingDataRecyclerManager>()

    private lateinit var baseViewModel: R

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    var adapter by autoCleared<BindSuperAdapter>()

    override fun onCreateView(mainView: View?) {
        normalAdapterManager = BindingDataRecyclerManager()
        baseViewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(getViewModelClass())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initList()

        getViewModel().loading.observe(this, Observer {
            when (it) {
                LoadState.RefreshDone -> {
                    refreshComplete()
                }
                LoadState.LoadMoreDone -> {
                    loadMoreComplete()
                }
                LoadState.Refresh -> {
                    ///刷新时清空旧数据
                    adapter?.dataList?.clear()
                    adapter?.notifyDataSetChanged()
                }
            }
        })

        getViewModel().dataList.observe(this, Observer { items ->
            items?.apply {
                if (items.size > 0) {
                    adapter?.dataList?.addAll(items)
                    adapter?.notifyItemRangeChanged(adapter!!.dataList!!.size, (adapter!!.dataList!!.size + items.size) - 1)
                }
            }
        })

        getViewModel().needMore.observe(this, Observer { it ->
            it?.apply {
                normalAdapterManager?.setNoMore(!it)
            }
        })

        showRefresh()
    }

    /**
     * item点击
     */
    override fun onItemClick(context: Context, position: Int) {

    }

    /**
     * 刷新
     */
    override fun onRefresh() {
        adapter?.dataList?.clear()
        getViewModel().refresh()
    }

    /**
     * 加载更多
     */
    override fun onLoadMore() {
        getViewModel().loadMore()
    }

    /**
     * 当前 recyclerView，为空即不走 @link[initList] 的初始化
     */
    abstract fun getRecyclerView(): RecyclerView?

    /**
     * 绑定Item
     */
    abstract fun bindHolder(manager: BindSuperAdapterManager)

    /**
     * ViewModel Class
     */
    abstract fun getViewModelClass(): Class<R>

    /**
     * ViewModel
     */
    open fun getViewModel(): R = baseViewModel

    /**
     * 是否需要下拉刷新
     */
    open fun enableRefresh(): Boolean = false

    /**
     * 是否需要下拉刷新
     */
    open fun enableLoadMore(): Boolean = false


    open fun refreshComplete() {
        normalAdapterManager?.refreshComplete()
    }

    open fun loadMoreComplete() {
        normalAdapterManager?.loadMoreComplete()
    }

    open fun showRefresh() {
        normalAdapterManager?.setRefreshing(true)
    }

    open fun isLoading(): Boolean = getViewModel().isLoading()


    fun initList() {
        if (activity != null && getRecyclerView() != null) {
            normalAdapterManager?.setPullRefreshEnabled(enableRefresh())
                    ?.setLoadingMoreEnabled(enableLoadMore())
                    ?.setOnItemClickListener(this)
                    ?.setLoadingListener(this)
                    ?.setRefreshHeader(BindCustomRefreshHeader(activity!!))
                    ?.setFootView(BindCustomLoadMoreFooter(activity!!))
                    ?.setLoadingMoreEmptyEnabled(false)
            normalAdapterManager?.apply {
                bindHolder(this)
                adapter = BindSuperAdapter(activity as Context, this, arrayListOf())
                getRecyclerView()?.layoutManager = LinearLayoutManager(activity!!)
                getRecyclerView()?.adapter = adapter
            }
        }
    }
}