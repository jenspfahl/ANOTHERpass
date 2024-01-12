package de.jepfa.yapm.ui.intro

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntroCustomLayoutFragment
import de.jepfa.yapm.R
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.STATE_INTRO_SHOWED

// Taken from https://github.com/AppIntro/AppIntro
class IntroActivity : IntroBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make sure you don't call setContentView!

        addSlide(
            AppIntroCustomLayoutFragment.newInstance(R.layout.fragment_intro_1
            ))

        addSlide(Intro2Fragment())

        addSlide(
            AppIntroCustomLayoutFragment.newInstance(R.layout.fragment_intro_3
            ))

        addSlide(
            AppIntroCustomLayoutFragment.newInstance(R.layout.fragment_intro_4
            ))

        addSlide(
            AppIntroCustomLayoutFragment.newInstance(R.layout.fragment_intro_5
            ))

    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        PreferenceService.putBoolean(STATE_INTRO_SHOWED, true, this)
        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        PreferenceService.putBoolean(STATE_INTRO_SHOWED, true, this)
        finish()
    }
}