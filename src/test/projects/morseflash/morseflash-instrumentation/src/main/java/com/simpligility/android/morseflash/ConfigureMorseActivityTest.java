/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.simpligility.android.morseflash;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.github.rtyley.android.screenshot.celebrity.Screenshots.poseForScreenshot;
import static com.github.rtyley.android.screenshot.celebrity.Screenshots.poseForScreenshotNamed;


@RunWith(AndroidJUnit4.class)
public class ConfigureMorseActivityTest {

    @Rule
    public ActivityTestRule<ConfigureMorseActivity> mActivityRule =
            new ActivityTestRule<ConfigureMorseActivity>(ConfigureMorseActivity.class);

    @Test
    @LargeTest
    public void testAppearance() throws Exception {
        poseForScreenshot();
        onView(withId(R.id.message)).perform(typeText("s"));
        poseForScreenshot();
        onView(withId(R.id.message)).perform(typeText("o"));
        poseForScreenshot();
        onView(withId(R.id.message)).perform(typeText("s"));
        poseForScreenshotNamed("ConfigureMorseActivity-SOS");
    }
}