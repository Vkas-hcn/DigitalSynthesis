package com.forget.profit.digital.synthesis.wx

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.forget.profit.digital.synthesis.R
import com.forget.profit.digital.synthesis.databinding.ActivityOneBinding
import android.view.animation.RotateAnimation
import android.view.animation.LinearInterpolator
import android.content.Intent
import com.forget.profit.digital.synthesis.MainActivity

class OneActivity : AppCompatActivity() {
    val binding by lazy { ActivityOneBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.one)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        startRotation()
    }

    private fun startRotation() {
        val rotate = RotateAnimation(
            0f, 360f,
            RotateAnimation.RELATIVE_TO_SELF, 0.5f,
            RotateAnimation.RELATIVE_TO_SELF, 0.5f
        )
        rotate.duration = 2000
        rotate.interpolator = LinearInterpolator()
        rotate.fillAfter = true

        binding.imgLoad.startAnimation(rotate)

        rotate.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation) {}

            override fun onAnimationRepeat(animation: android.view.animation.Animation) {}

            override fun onAnimationEnd(animation: android.view.animation.Animation) {
                val intent = Intent(this@OneActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        })
    }
}
