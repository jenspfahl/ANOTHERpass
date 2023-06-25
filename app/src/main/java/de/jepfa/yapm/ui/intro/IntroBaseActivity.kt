package de.jepfa.yapm.ui.intro

import android.os.Bundle
import com.github.appintro.AppIntro
import de.jepfa.yapm.R

// Taken from https://github.com/AppIntro/AppIntro
abstract class IntroBaseActivity : AppIntro() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make sure you don't call setContentView!

        setIndicatorColor(getColor(R.color.colorAltAccent), getColor(R.color.colorPrimaryDark))
        setBackArrowColor(getColor(R.color.colorAccent))
        setColorSkipButton(getColor(R.color.colorAccent))
        setColorDoneText(getColor(R.color.colorAccent))
        setNextArrowColor(getColor(R.color.colorAccent))


    }

}