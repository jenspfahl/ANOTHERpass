package de.jepfa.yapm.ui.intro

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import com.github.appintro.AppIntro
import de.jepfa.yapm.R

// Taken from https://github.com/AppIntro/AppIntro
abstract class IntroBaseActivity : AppIntro() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
        // Make sure you don't call setContentView!

        setIndicatorColor(getColor(R.color.colorAltAccent), getColor(R.color.colorPrimaryDark))
        setBackArrowColor(getColor(R.color.colorAccent))
        setColorSkipButton(getColor(R.color.colorAccent))
        setColorDoneText(getColor(R.color.colorAccent))
        setNextArrowColor(getColor(R.color.colorAccent))


    }

}