package vfunny.shortvideovfunnyapp.Lang.ui.ui.main

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.FragmentStatePagerAdapter
import vfunny.shortvideovfunnyapp.Post.model.Language

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter(
    private val context: Context,
    fm: FragmentManager
) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val fragmentList: MutableList<Fragment> = mutableListOf()

    override fun getItem(position: Int): Fragment {
        if (fragmentList.size > position) {
            return fragmentList[position]
        }

        val fragment = PlaceholderFragment.newInstance(position)
        fragmentList.add(fragment)
        return fragment
    }

    override fun getPageTitle(position: Int): CharSequence {
        return Language.getAllLanguages()[position].name
    }

    override fun getCount(): Int {
        return Language.getAllLanguages().size
    }
}