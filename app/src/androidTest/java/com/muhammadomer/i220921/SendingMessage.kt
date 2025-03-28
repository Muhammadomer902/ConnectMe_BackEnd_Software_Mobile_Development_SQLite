package com.muhammadomer.i220921


import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.`is`
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SendingMessage {

    @Rule
    @JvmField
    var mActivityScenarioRule = ActivityScenarioRule(WelcomePage::class.java)

    @Test
    fun sendingMessage() {
        val appCompatEditText = onView(
            allOf(
                withId(R.id.Username),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.ScrollView")),
                        0
                    ),
                    3
                )
            )
        )
        appCompatEditText.perform(scrollTo(), replaceText("Muhammad Mouid"), closeSoftKeyboard())

        val appCompatEditText2 = onView(
            allOf(
                withId(R.id.Password),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.ScrollView")),
                        0
                    ),
                    4
                )
            )
        )
        appCompatEditText2.perform(scrollTo(), replaceText("123456"), closeSoftKeyboard())

        pressBack()

        val appCompatButton = onView(
            allOf(
                withId(R.id.myBtn), withText("Login"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.ScrollView")),
                        0
                    ),
                    6
                )
            )
        )
        appCompatButton.perform(scrollTo(), click())

        val appCompatEditText3 = onView(
            allOf(
                withId(R.id.Username), withText("Muhammad Mouid"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.ScrollView")),
                        0
                    ),
                    3
                )
            )
        )
        appCompatEditText3.perform(scrollTo(), replaceText("Muhammad Mouid Ali"))

        val appCompatEditText4 = onView(
            allOf(
                withId(R.id.Username), withText("Muhammad Mouid Ali"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.ScrollView")),
                        0
                    ),
                    3
                ),
                isDisplayed()
            )
        )
        appCompatEditText4.perform(closeSoftKeyboard())

        pressBack()

        val appCompatButton2 = onView(
            allOf(
                withId(R.id.myBtn), withText("Login"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.ScrollView")),
                        0
                    ),
                    6
                )
            )
        )
        appCompatButton2.perform(scrollTo(), click())

        val appCompatButton3 = onView(
            allOf(
                withId(R.id.DM),
                childAtPosition(
                    allOf(
                        withId(R.id.CommunityText),
                        childAtPosition(
                            withId(R.id.main),
                            0
                        )
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        appCompatButton3.perform(click())

        val appCompatEditText5 = onView(
            allOf(
                withId(R.id.Search),
                childAtPosition(
                    allOf(
                        withId(R.id.SearchBar),
                        childAtPosition(
                            withClassName(`is`("android.widget.RelativeLayout")),
                            2
                        )
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        appCompatEditText5.perform(replaceText("Muha"), closeSoftKeyboard())

        val appCompatButton4 = onView(
            allOf(
                withId(R.id.SearchLogo),
                childAtPosition(
                    allOf(
                        withId(R.id.SearchBar),
                        childAtPosition(
                            withClassName(`is`("android.widget.RelativeLayout")),
                            2
                        )
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        appCompatButton4.perform(click())

        val appCompatButton5 = onView(
            allOf(
                withId(R.id.SearchLogo),
                childAtPosition(
                    allOf(
                        withId(R.id.SearchBar),
                        childAtPosition(
                            withClassName(`is`("android.widget.RelativeLayout")),
                            2
                        )
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        appCompatButton5.perform(click())

        val recyclerView = onView(
            allOf(
                withId(R.id.dmRecyclerView),
                childAtPosition(
                    withClassName(`is`("android.widget.RelativeLayout")),
                    3
                )
            )
        )
        recyclerView.perform(actionOnItemAtPosition<ViewHolder>(0, click()))

        val appCompatEditText6 = onView(
            allOf(
                withId(R.id.messageInput),
                childAtPosition(
                    allOf(
                        withId(R.id.TextBar),
                        childAtPosition(
                            withClassName(`is`("android.widget.RelativeLayout")),
                            3
                        )
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        appCompatEditText6.perform(replaceText("Hi"), closeSoftKeyboard())

        pressBack()

        val appCompatImageView = onView(
            allOf(
                withId(R.id.sendButton),
                childAtPosition(
                    allOf(
                        withId(R.id.TextBar),
                        childAtPosition(
                            withClassName(`is`("android.widget.RelativeLayout")),
                            3
                        )
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        appCompatImageView.perform(click())
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
