/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.integration.test

import androidx.test.filters.MediumTest
import androidx.ui.test.assertMeasureSizeIsPositive
import androidx.ui.test.assertNoPendingChanges
import androidx.ui.integration.test.foundation.RectsInColumnSharedModelTestCase
import androidx.ui.test.ExperimentalTesting
import androidx.ui.test.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Ensure correctness of [RectsInColumnSharedModelTestCase].
 */
@MediumTest
@RunWith(Parameterized::class)
@OptIn(ExperimentalTesting::class)
class RectsInColumnSharedModelTest(private val numberOfRectangles: Int) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun initParameters(): Array<Any> = arrayOf(1, 10)
    }

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun toggleRectangleColor_compose() {
        val testCase = RectsInColumnSharedModelTestCase(numberOfRectangles)
        composeTestRule
            .forGivenTestCase(testCase)
            .performTestWithEventsControl {
                doFrame()
                assertNoPendingChanges()
                assertMeasureSizeIsPositive()
                testCase.toggleState()
                doFrame()
                assertNoPendingChanges()
            }
    }
}