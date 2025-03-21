package com.muhammadomer.i220921

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.widget.RelativeLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class WelcomePage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_welcome_page)

        val mainLayout = findViewById<RelativeLayout>(R.id.main)
        val fadeOut = AlphaAnimation(1f, 0f)
        fadeOut.duration = 1000
        fadeOut.fillAfter = true

        Handler(Looper.getMainLooper()).postDelayed({
            mainLayout.startAnimation(fadeOut)
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, LogInPage::class.java))
                finish()
            }, 1000)
        }, 3000)
    }
}
