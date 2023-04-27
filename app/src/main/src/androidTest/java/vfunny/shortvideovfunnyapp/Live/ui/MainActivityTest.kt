import androidx.test.espresso.DataInteraction
import androidx.test.espresso.ViewInteraction
import androidx.test.filters.LargeTest
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent

import androidx.test.InstrumentationRegistry.getInstrumentation
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*

import vfunny.shortvideovfunnyapp.R

import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.IsInstanceOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anything
import org.hamcrest.Matchers.`is`

@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Rule
    @JvmField
    var mActivityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun mainActivityTest() {
        val supportVectorDrawablesButton = onView(
            allOf(
                withId(com.firebase.ui.auth.R.id.email_button), withText("Sign in with email"),
                childAtPosition(
                    allOf(
                        withId(com.firebase.ui.auth.R.id.btn_holder),
                        childAtPosition(
                            withId(com.google.android.material.R.id.container),
                            0
                        )
                    ),
                    0
                )
            )
        )
        supportVectorDrawablesButton.perform(scrollTo(), click())

        val textInputEditText = onView(
            allOf(
                withId(com.firebase.ui.auth.R.id.email),
                childAtPosition(
                    childAtPosition(
                        withId(com.firebase.ui.auth.R.id.email_layout),
                        0
                    ),
                    0
                )
            )
        )
        textInputEditText.perform(scrollTo(), replaceText(""), closeSoftKeyboard())

        val textInputEditText2 = onView(
            allOf(
                withId(com.firebase.ui.auth.R.id.email),
                childAtPosition(
                    childAtPosition(
                        withId(com.firebase.ui.auth.R.id.email_layout),
                        0
                    ),
                    0
                )
            )
        )
        textInputEditText2.perform(scrollTo(), click())

        val textInputEditText3 = onView(
            allOf(
                withId(com.firebase.ui.auth.R.id.email),
                childAtPosition(
                    childAtPosition(
                        withId(com.firebase.ui.auth.R.id.email_layout),
                        0
                    ),
                    0
                )
            )
        )
        textInputEditText3.perform(scrollTo(), replaceText("test1@gmail.com"), closeSoftKeyboard())

        val materialButton = onView(
            allOf(
                withId(com.firebase.ui.auth.R.id.button_next), withText("Next"),
                childAtPosition(
                    allOf(
                        withId(com.firebase.ui.auth.R.id.email_top_layout),
                        childAtPosition(
                            withClassName(`is`("android.widget.ScrollView")),
                            0
                        )
                    ),
                    2
                )
            )
        )
        materialButton.perform(scrollTo(), click())

        val textInputEditText4 = onView(
            allOf(
                withId(com.firebase.ui.auth.R.id.password),
                childAtPosition(
                    childAtPosition(
                        withId(com.firebase.ui.auth.R.id.password_layout),
                        0
                    ),
                    0
                )
            )
        )
        textInputEditText4.perform(scrollTo(), replaceText("test@123"), closeSoftKeyboard())

        val materialButton2 = onView(
            allOf(
                withId(com.firebase.ui.auth.R.id.button_done), withText("Sign in"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.ScrollView")),
                        0
                    ),
                    4
                )
            )
        )
        materialButton2.perform(scrollTo(), click())

        val constraintLayout = onView(
            allOf(
                withId(R.id.root),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.view_pager),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        constraintLayout.perform(click())

        val constraintLayout2 = onView(
            allOf(
                withId(R.id.root),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.view_pager),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        constraintLayout2.perform(click())

        val constraintLayout3 = onView(
            allOf(
                withId(R.id.root),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.view_pager),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        constraintLayout3.perform(click())

        val constraintLayout4 = onView(
            allOf(
                withId(R.id.root),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.view_pager),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        constraintLayout4.perform(click())
    }

    private fun childAtPosition(
        parentMatcher: Matcher<View>, position: Int
    ): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
