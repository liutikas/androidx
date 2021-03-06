/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.integration.test.core.text

import androidx.compose.Composable
import androidx.compose.foundation.Box
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.mutableStateOf
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.compose.ui.graphics.Color
import androidx.ui.integration.test.RandomTextGenerator
import androidx.ui.integration.test.ToggleableTestCase
import androidx.ui.test.ComposeTestCase
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

class TextToggleTextTestCase(
    private val textGenerator: RandomTextGenerator,
    private val textLength: Int,
    private val width: Dp,
    private val fontSize: TextUnit
) : ComposeTestCase, ToggleableTestCase {

    val text = mutableStateOf(textGenerator.nextParagraph(length = textLength))

    @Composable
    override fun emitContent() {
        Box(
            modifier = Modifier.wrapContentSize(Alignment.Center).preferredWidth(width)
        ) {
            Text(text = text.value, color = Color.Black, fontSize = fontSize)
        }
    }

    override fun toggleState() {
        text.value = textGenerator.nextParagraph(length = textLength)
    }
}