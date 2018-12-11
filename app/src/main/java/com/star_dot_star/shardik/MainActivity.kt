package com.star_dot_star.shardik

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.Toast
import com.star_dot_star.shardik.service.AppVpnService


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()

        findViewById<Button>(R.id.btn_start).setOnClickListener {
            val intent = VpnService.prepare(applicationContext)
            if (intent != null) {
                startActivityForResult(intent, 0)
            } else {
                onActivityResult(0, Activity.RESULT_OK, null)
            }
        }
    }

    private fun createNotificationChannel() {
        // Channels are only supported in Oreo and up:
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        notificationManager?.createNotificationChannel(NotificationChannel(NOTIFICATION_CHANNEL,  NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Starting VpnService", Toast.LENGTH_LONG).show()
            val intent = Intent(this, AppVpnService::class.java)
            startService(intent)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL = "99"
        const val NOTIFICATION_CHANNEL_NAME = "General"
    }
}
