package com.wire.android.notification.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.parseIntoQualifiedID
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ConnectionRequestNotificationDismissReceiver : BroadcastReceiver() {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    override fun onReceive(context: Context, intent: Intent) {
        val requesterUserId: String? = intent.getStringExtra(EXTRA_CONNECTION_REQUESTER_USER_ID)
        appLogger.i("ConnectionRequestNotificationDismissReceiver: onReceive, requesterUserId: $requesterUserId")
        val userId: QualifiedID? =
            intent.getStringExtra(EXTRA_RECEIVER_USER_ID)?.parseIntoQualifiedID()

        GlobalScope.launch(dispatcherProvider.io()) {
            val sessionScope =
                if (userId != null) {
                    coreLogic.getSessionScope(userId)
                } else {
                    val currentSession = coreLogic.globalScope { session.currentSession() }
                    if (currentSession is CurrentSessionResult.Success) {
                        coreLogic.getSessionScope(currentSession.authSession.session.userId)
                    } else {
                        null
                    }
                }

            sessionScope?.let { scope ->
                requesterUserId?.parseIntoQualifiedID()?.let {
                    scope.conversations.markConnectionRequestAsNotified(it)
                }
            }
        }
    }

    companion object {
        private const val EXTRA_CONNECTION_REQUESTER_USER_ID = "connection_requester_user_id_extra"
        private const val EXTRA_RECEIVER_USER_ID = "user_id_extra"

        fun newIntent(context: Context, connectionRequesterUserId: String?, userId: String?): Intent =
            Intent(context, ConnectionRequestNotificationDismissReceiver::class.java).apply {
                putExtra(EXTRA_CONNECTION_REQUESTER_USER_ID, connectionRequesterUserId)
                putExtra(EXTRA_RECEIVER_USER_ID, userId)
            }
    }
}
