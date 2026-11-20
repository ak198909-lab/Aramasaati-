package com.aramasaati.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Telefon her gelen aramada bu servise sorar.
 * Kural "engelle" diyorsa arama reddedilir; zil çalmaz, arayan kişi meşgul/ulaşılamıyor duyar.
 * SMS'lere hiç dokunulmaz — kişi mesaj atmaya devam edebilir.
 */
class CallBlockerService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart
        val incoming = callDetails.callDirection == Call.Details.DIRECTION_INCOMING

        val rule = if (incoming && !number.isNullOrBlank()) {
            try {
                RuleStore(this).findBlockingRule(number, Calendar.getInstance())
            } catch (e: Exception) {
                null
            }
        } else null

        val response = if (rule != null) {
            CallResponse.Builder()
                .setDisallowCall(true)      // aramayı kabul etme
                .setRejectCall(true)        // reddet (zil çalmaz)
                .setSkipCallLog(false)      // arama geçmişinde görünsün
                .setSkipNotification(true)  // "cevapsız arama" bildirimi çıkmasın
                .build()
        } else {
            CallResponse.Builder().build()  // normal şekilde çalsın
        }
        respondToCall(callDetails, response)

        if (rule != null && number != null) {
            val time = SimpleDateFormat("dd.MM HH:mm", Locale("tr")).format(Date())
            RuleStore(this).addLog("$time  ${rule.name}  ($number)")
            notifyBlocked(rule.name)
        }
    }

    private fun notifyBlocked(name: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Engellenen aramalar", NotificationManager.IMPORTANCE_LOW)
        )
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("Arama engellendi")
            .setContentText("$name izin verilen saat dışında aradı.")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(name.hashCode(), n)
    }

    companion object {
        private const val CHANNEL_ID = "blocked_calls"
    }
}
