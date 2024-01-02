package de.jepfa.yapm.ui.intro

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntroCustomLayoutFragment
import de.jepfa.yapm.R
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.STATE_WHATS_NEW_SHOWED_FOR_VERSION
import de.jepfa.yapm.util.DebugInfo.getVersionCode
import de.jepfa.yapm.util.DebugInfo.getVersionCodeForWhatsNew

// Taken from https://github.com/AppIntro/AppIntro
class WhatsNewActivity : IntroBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make sure you don't call setContentView!

        //TODO new fragment layout for each new minor App version (e.g 1.7.x -> 1.7)
        addSlide(
            AppIntroCustomLayoutFragment.newInstance(R.layout.fragment_whats_new_in_1_9
            ))
        addSlide(
            AppIntroCustomLayoutFragment.newInstance(R.layout.fragment_whats_new_in_1_8
            ))
        addSlide(
            AppIntroCustomLayoutFragment.newInstance(R.layout.fragment_whats_new_in_1_7
            ))

    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        PreferenceService.putInt(STATE_WHATS_NEW_SHOWED_FOR_VERSION, getVersionCodeForWhatsNew(this), this)
        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        PreferenceService.putInt(STATE_WHATS_NEW_SHOWED_FOR_VERSION, getVersionCodeForWhatsNew(this), this)
        finish()
    }
}