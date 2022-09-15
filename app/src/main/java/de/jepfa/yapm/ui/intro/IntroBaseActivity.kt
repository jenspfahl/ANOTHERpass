package de.jepfa.yapm.ui.intro

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroCustomLayoutFragment
import de.jepfa.yapm.R
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.STATE_INTRO_SHOWED

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