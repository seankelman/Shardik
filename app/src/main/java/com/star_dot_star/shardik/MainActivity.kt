package com.star_dot_star.shardik

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.Toast
import com.star_dot_star.shardik.service.AppVpnService


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btn_start).setOnClickListener {
            val intent = VpnService.prepare(applicationContext)
            if (intent != null) {
                startActivityForResult(intent, 0)
            } else {
                onActivityResult(0, Activity.RESULT_OK, null)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Starting VpnService", Toast.LENGTH_LONG).show()
            val intent = Intent(this, AppVpnService::class.java)
            startService(intent)
        }
    }
}
