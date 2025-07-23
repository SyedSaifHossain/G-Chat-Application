package com.syedsaifhossain.g_chatapplication

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMessagingService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "收到消息: ${remoteMessage.data}")

        // 处理接收到的消息
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "消息数据: ${remoteMessage.data}")
        }

        // 处理通知
        remoteMessage.notification?.let {
            Log.d(TAG, "消息通知: ${it.title} - ${it.body}")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Log.d(TAG, "新的FCM令牌: $token")

        // 这里可以将新令牌发送到服务器
        // sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        // TODO: 将令牌发送到你的服务器
        Log.d(TAG, "发送令牌到服务器: $token")
        }
}