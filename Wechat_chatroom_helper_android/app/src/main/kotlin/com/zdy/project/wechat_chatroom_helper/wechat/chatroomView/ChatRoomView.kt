package com.zdy.project.wechat_chatroom_helper.wechat.chatroomView

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.*
import cn.bingoogolapple.swipebacklayout.BGASwipeBackLayout2
import cn.bingoogolapple.swipebacklayout.MySwipeBackLayout
import com.zdy.project.wechat_chatroom_helper.io.model.ChatInfoModel
import com.zdy.project.wechat_chatroom_helper.LogUtils
import com.zdy.project.wechat_chatroom_helper.PageType
import com.zdy.project.wechat_chatroom_helper.io.AppSaveInfo
import com.zdy.project.wechat_chatroom_helper.utils.DeviceUtils
import com.zdy.project.wechat_chatroom_helper.utils.ScreenUtils
import com.zdy.project.wechat_chatroom_helper.wechat.dialog.WhiteListDialogBuilder
import com.zdy.project.wechat_chatroom_helper.wechat.manager.AvatarMaker
import com.zdy.project.wechat_chatroom_helper.wechat.plugins.RuntimeInfo
import com.zdy.project.wechat_chatroom_helper.wechat.plugins.hook.adapter.MainAdapter
import com.zdy.project.wechat_chatroom_helper.wechat.plugins.hook.message.MessageFactory
import network.ApiManager
import java.util.*


/**
 * Created by Mr.Zdy on 2017/8/27.
 */

class ChatRoomView(private val mContext: Context, mContainer: ViewGroup, private val pageType: Int) : ChatRoomContract.View {

    private lateinit var mPresenter: ChatRoomContract.Presenter
    private lateinit var swipeBackLayout: MySwipeBackLayout

    private val mainView: LinearLayout
    private val mRecyclerView: RecyclerView
    private lateinit var mToolbarContainer: ViewGroup
    private lateinit var mToolbar: Toolbar

    private lateinit var mAdapter: ChatRoomRecyclerViewAdapter

    private var uuid = "0"

    override val isShowing: Boolean get() = !swipeBackLayout.isOpen

    init {

        val params = ViewGroup.MarginLayoutParams(
                ViewGroup.MarginLayoutParams.MATCH_PARENT, ViewGroup.MarginLayoutParams.MATCH_PARENT)

        mainView = LinearLayout(mContext)
        mainView.layoutParams = ViewGroup.LayoutParams(ScreenUtils.getScreenWidth(mContext),
                ViewGroup.LayoutParams.MATCH_PARENT)
        mainView.orientation = LinearLayout.VERTICAL

        mRecyclerView = RecyclerView(mContext)
        mRecyclerView.id = android.R.id.list
        mRecyclerView.layoutManager = LinearLayoutManager(mContext)

        mainView.addView(initToolbar())
        mainView.addView(mRecyclerView)
        mainView.isClickable = true

        mainView.setBackgroundColor(Color.parseColor("#" + AppSaveInfo.helperColorInfo()))

        initSwipeBack()

        mContainer.addView(swipeBackLayout, params)

        uuid = DeviceUtils.getIMELCode(mContext)
        ApiManager.sendRequestForUserStatistics("init", uuid, Build.MODEL)

    }


    private fun initSwipeBack() {
        swipeBackLayout = MySwipeBackLayout(mContext)
        swipeBackLayout.attachToView(mainView, mContext)
        swipeBackLayout.setPanelSlideListener(object : BGASwipeBackLayout2.PanelSlideListener {
            override fun onPanelSlide(panel: View, slideOffset: Float) {

            }

            override fun onPanelOpened(panel: View) {
            }

            override fun onPanelClosed(panel: View) {

            }
        })
    }


    override fun setOnDialogItemClickListener(listener: ChatRoomRecyclerViewAdapter.OnDialogItemClickListener) {
        mAdapter.setOnDialogItemClickListener(listener)
    }


    override fun show() {
        LogUtils.log("TrackHelperCan'tOpen, ChatRoomView -> show no params")
        show(ScreenUtils.getScreenWidth(mContext))
    }

    override fun dismiss() {
        dismiss(0)
    }

    override fun show(offest: Int) {
        LogUtils.log("TrackHelperCan'tOpen, ChatRoomView -> show, offest = ${offest}, swipeBackLayout = ${swipeBackLayout}")
        swipeBackLayout.closePane()
    }

    override fun dismiss(offest: Int) {
        swipeBackLayout.openPane()
    }


    override fun init() {
        mAdapter = ChatRoomRecyclerViewAdapter(mContext)
        LogUtils.log("mRecyclerView = $mRecyclerView, mAdapter = $mAdapter")
        mRecyclerView.adapter = mAdapter
    }


    override fun refreshList(isForce: Boolean, data: Any?) {
        mainView.post {
            val newDatas =
                    if (pageType == PageType.CHAT_ROOMS) MessageFactory.getSpecChatRoom()
                    else MessageFactory.getSpecOfficial()

//            val oldDatas = mAdapter.data
//            val diffResult = DiffUtil.calculateDiff(DiffCallBack(oldDatas, newDatas), true)
//            diffResult.dispatchUpdatesTo(mAdapter)
            mAdapter.data = newDatas

            mAdapter.notifyDataSetChanged()
        }
        LogUtils.log("showMessageRefresh for all recycler view , pageType = " + PageType.printPageType(pageType))
    }

    class DiffCallBack(private var mOldDatas: ArrayList<ChatInfoModel>,
                       private var mNewDatas: ArrayList<ChatInfoModel>) : DiffUtil.Callback() {

        init {
            if (mOldDatas.size != 0 && mNewDatas.size != 0) {

                LogUtils.log("DiffCallBack, oldData = ${mOldDatas.joinToString { it.content.toString() + "\n" }}")
                LogUtils.log("DiffCallBack, newData = ${mNewDatas.joinToString { it.content.toString() + "\n" }}")
            }
        }

        override fun getOldListSize() = mOldDatas.size

        override fun getNewListSize() = mNewDatas.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                mOldDatas[oldItemPosition].field_username == mNewDatas[newItemPosition].field_username

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                mOldDatas[oldItemPosition] == mNewDatas[newItemPosition]

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {

            val oldItem = mOldDatas[oldItemPosition]
            val newItem = mNewDatas[newItemPosition]

            LogUtils.log("getChangePayload, oldItem = $oldItem, newItem = $newItem")

            return if (oldItem == newItem) null
            else {
                val bundle = Bundle()
                if (oldItem.content != newItem.content) bundle.putCharSequence("content", newItem.content)
                if (oldItem.conversationTime != newItem.conversationTime) bundle.putCharSequence("conversationTime", newItem.conversationTime)
                if (oldItem.unReadMuteCount != newItem.unReadMuteCount) bundle.putInt("unReadMuteCount", newItem.unReadMuteCount)
                if (oldItem.unReadCount != newItem.unReadCount) bundle.putInt("unReadCount", newItem.unReadCount)

                if (bundle.isEmpty) null else bundle
            }
        }
    }

    private fun initToolbar(): View {
        mToolbarContainer = RelativeLayout(mContext)

        mToolbar = Toolbar(mContext)

        val height = ScreenUtils.dip2px(mContext, 48f)

        mToolbar.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)

        mToolbar.setNavigationOnClickListener { dismiss() }
        mToolbar.setBackgroundColor(Color.parseColor("#" + AppSaveInfo.toolbarColorInfo()))
        mRecyclerView.setBackgroundColor(Color.parseColor("#" + AppSaveInfo.helperColorInfo()))

        when (pageType) {
            PageType.CHAT_ROOMS -> mToolbar.title = "群消息助手"
            PageType.OFFICIAL -> mToolbar.title = "服务号助手"
        }
        mToolbar.setTitleTextColor(-0x50506)

        val clazz: Class<*>
        try {
            clazz = Class.forName("android.widget.Toolbar")
            val mTitleTextView = clazz.getDeclaredField("mTitleTextView")
            mTitleTextView.isAccessible = true
            val textView = mTitleTextView.get(mToolbar) as TextView
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)

            val mNavButtonView = clazz.getDeclaredField("mNavButtonView")
            mNavButtonView.isAccessible = true
            val imageButton = mNavButtonView.get(mToolbar) as ImageButton
            val layoutParams = imageButton.layoutParams
            layoutParams.height = height
            imageButton.layoutParams = layoutParams

        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        }

        val imageView = ImageView(mContext)

        val params = RelativeLayout.LayoutParams(height, height)
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)

        imageView.layoutParams = params
        val padding = height / 8
        imageView.setPadding(padding, padding, padding, padding)
        imageView.setImageDrawable(AvatarMaker.handleAvatarDrawable(mContext, pageType, 0x00000000))

        imageView.setOnClickListener {
            val whiteListDialogBuilder = WhiteListDialogBuilder()
            when (pageType) {
                PageType.OFFICIAL -> whiteListDialogBuilder.pageType = PageType.OFFICIAL
                PageType.CHAT_ROOMS -> whiteListDialogBuilder.pageType = PageType.CHAT_ROOMS
            }
            val dialog = whiteListDialogBuilder.getWhiteListDialog(mContext)
            dialog.show()
            dialog.setOnDismissListener {
                when (pageType) {
                    PageType.OFFICIAL -> RuntimeInfo.officialViewPresenter.refreshList(false, Any())
                    PageType.CHAT_ROOMS -> RuntimeInfo.chatRoomViewPresenter.refreshList(false, Any())
                }

                MainAdapter.originAdapter.notifyDataSetChanged()
            }
        }

        mToolbarContainer.addView(mToolbar)
        mToolbarContainer.addView(imageView)

        return mToolbarContainer
    }

    override fun setPresenter(presenter: ChatRoomContract.Presenter) {
        mPresenter = presenter
    }
}
