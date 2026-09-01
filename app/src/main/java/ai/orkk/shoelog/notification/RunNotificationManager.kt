package ai.orkk.shoelog.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ai.orkk.shoelog.MainActivity
import ai.orkk.shoelog.R
import ai.orkk.shoelog.data.repository.UnassignedRunNotifier
import ai.orkk.shoelog.domain.Exercise

object NotificationPolicy {
    fun shouldNotify(newExercises: List<Exercise>, autoAssignedIds: Set<String>): Boolean =
        newExercises.any { it.id !in autoAssignedIds }
}

class RunNotificationManager(private val context: Context) : UnassignedRunNotifier {
    init {
        createChannel()
    }

    fun canNotify(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    override fun notifyUnassigned(exerciseIds: Set<String>) {
        if (exerciseIds.isEmpty() || !canNotify()) return
        val firstId = exerciseIds.first()
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_EXERCISE_ID, firstId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("러닝화 배정이 필요해요")
            .setContentText("새 달리기 ${exerciseIds.size}개에 착용한 러닝화를 선택해 주세요.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "미배정 달리기",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "새 달리기에 러닝화를 배정하도록 알려줍니다."
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private companion object {
        const val CHANNEL_ID = "unassigned_runs"
        const val NOTIFICATION_ID = 4101
    }
}
