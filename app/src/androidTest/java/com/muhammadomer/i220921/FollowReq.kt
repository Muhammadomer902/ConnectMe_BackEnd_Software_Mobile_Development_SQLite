package com.muhammadomer.i220921


import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
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
class FollowReq {

    @Rule
    @JvmField
    var mActivityScenarioRule = ActivityScenarioRule(WelcomePage::class.java)

    @Test
    fun followReq() {
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
        appCompatEditText.perform(scrollTo(), replaceText("Muhammad"), closeSoftKeyboard())

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
                withId(R.id.Username), withText("Muhammad"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.ScrollView")),
                        0
                    ),
                    3
                )
            )
        )
        appCompatEditText3.perform(scrollTo(), replaceText("Muhammad Omer "))

        val appCompatEditText4 = onView(
            allOf(
                withId(R.id.Username), withText("Muhammad Omer "),
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
                withId(R.id.Search),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.main),
                        3
                    ),
                    1
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
                            0
                        )
                    ),
                    0
                )
            )
        )
        appCompatEditText5.perform(scrollTo(), replaceText("Muhammad "), closeSoftKeyboard())

        val appCompatButton4 = onView(
            allOf(
                withId(R.id.SearchLogo),
                childAtPosition(
                    allOf(
                        withId(R.id.SearchBar),
                        childAtPosition(
                            withClassName(`is`("android.widget.RelativeLayout")),
                            0
                        )
                    ),
                    1
                )
            )
        )
        appCompatButton4.perform(scrollTo(), click())

        val appCompatButton5 = onView(
            allOf(
                withId(R.id.SearchLogo),
                childAtPosition(
                    allOf(
                        withId(R.id.SearchBar),
                        childAtPosition(
                            withClassName(`is`("android.widget.RelativeLayout")),
                            0
                        )
                    ),
                    1
                )
            )
        )
        appCompatButton5.perform(scrollTo(), click())

        val appCompatButton6 = onView(
            allOf(
                withId(R.id.followButton), withText("Follow"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.searchedUsersRecyclerView),
                        0
                    ),
                    1
                )
            )
        )
        appCompatButton6.perform(scrollTo(), click())

        pressBack()
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
