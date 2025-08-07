package com.catpuppyapp.puppygit.notification.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.catpuppyapp.puppygit.constants.IntentCons
import com.catpuppyapp.puppygit.notification.AutomationNotify
import com.catpuppyapp.puppygit.notification.HttpServiceExecuteNotify
import com.catpuppyapp.puppygit.notification.HttpServiceHoldNotify
import com.catpuppyapp.puppygit.notification.NormalNotify
import com.catpuppyapp.puppygit.notification.base.NotifyBase
import com.catpuppyapp.puppygit.activity.MainActivity
import com.catpuppyapp.puppygit.utils.AppModel
import com.catpuppyapp.puppygit.utils.forEachBetter
import kotlin.random.Random

object NotifyUtil {
    private val notifyList:List<NotifyBase> = listOf(
        //这创建的实例只是用来注册通知渠道的，通知id随便填个数就行，反正不用
        NormalNotify.create(1),
        HttpServiceHoldNotify.create(2),
        HttpServiceExecuteNotify.create(3),
        AutomationNotify.create(4),
    )

    /**
     * @param appContext better use application context, but activityContext or serviceContext should be fine although
     */
    fun initAllNotify(appContext: Context) {
        notifyList.forEachBetter {
            it.init(appContext)
        }
    }

    fun sendNotificationClickGoToSpecifiedPage(notify: NotifyBase, title:String, msg:String, startPage:Int, startRepoId:String) {
        notify.sendNotification(
            null,
            title,
            msg,
            createPendingIntentGoToSpecifiedPage(null, startPage, startRepoId)
        )
    }


    fun createPendingIntent(context: Context?, extras:Map<String, String>): PendingIntent {
        val context = context ?: AppModel.realAppContext

        val intent = Intent(context, MainActivity::class.java) // 替换为您的主活动
        //创建个新Activity并清掉之前的Activity，不然可能存在多个Activity，有点混乱
        //这个 or 是 bitwise，相当于 '1 | 0' 的 '|'
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        //添加参数
        extras.forEachBetter { k, v ->
            intent.putExtra(k, v)
        }

        //flag作用是如果pendingIntent已经存在，则取消之前的然后创建个新的，没验证，可能是
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun createPendingIntentGoToSpecifiedPage(context: Context?, startPage:Int, startRepoId:String):PendingIntent {
        return createPendingIntent(
            context,
            mapOf(
                IntentCons.ExtrasKey.startPage to startPage.toString(),
                IntentCons.ExtrasKey.startRepoId to startRepoId
            )
        )
    }

    fun genId():Int {
        //I want less than 50 as reserved notification Id,so dont use it
        return Random.nextInt(51, Int.MAX_VALUE)
    }

}
