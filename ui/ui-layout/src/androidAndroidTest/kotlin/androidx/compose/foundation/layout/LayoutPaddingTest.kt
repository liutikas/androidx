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

package androidx.compose.foundation.layout

import androidx.compose.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.test.filters.SmallTest
import androidx.ui.core.Alignment
import androidx.ui.core.InspectableParameter
import androidx.ui.core.Layout
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Modifier
import androidx.ui.core.Ref
import androidx.ui.core.onChildPositioned
import androidx.ui.core.onPositioned
import androidx.ui.core.positionInRoot
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@SmallTest
@RunWith(JUnit4::class)
class LayoutPaddingTest : LayoutTest() {

    /**
     * Tests that negative start padding is not allowed.
     */
    @Test(expected = IllegalArgumentException::class)
    fun negativeStartPadding_throws() {
        Modifier.padding(start = -1f.dp)
    }

    /**
     * Tests that negative top padding is not allowed.
     */
    @Test(expected = IllegalArgumentException::class)
    fun negativeTopPadding_throws() {
        Modifier.padding(top = -1f.dp)
    }

    /**
     * Tests that negative end padding is not allowed.
     */
    @Test(expected = IllegalArgumentException::class)
    fun negativeEndPadding_throws() {
        Modifier.padding(end = -1f.dp)
    }

    /**
     * Tests that negative bottom padding is not allowed.
     */
    @Test(expected = IllegalArgumentException::class)
    fun negativeBottomPadding_throws() {
        Modifier.padding(bottom = -1f.dp)
    }

    /**
     * Tests that the [padding]-all and [padding] factories return equivalent modifiers.
     */
    @Test
    fun allEqualToAbsoluteWithExplicitSides() {
        Assert.assertEquals(
            Modifier.padding(10.dp, 10.dp, 10.dp, 10.dp),
            Modifier.padding(10.dp)
        )
    }

    /**
     * Tests that the symmetrical-[padding] and [padding] factories return equivalent modifiers.
     */
    @Test
    fun symmetricEqualToAbsoluteWithExplicitSides() {
        Assert.assertEquals(
            Modifier.padding(10.dp, 20.dp, 10.dp, 20.dp),
            Modifier.padding(10.dp, 20.dp)
        )
    }

    /**
     * Tests the top-level [padding] modifier factory with a single "all sides" argument,
     * checking that a uniform padding of all sides is applied to a child when plenty of space is
     * available for both content and padding.
     */
    @Test
    fun paddingAllAppliedToChild() = with(density) {
        val padding = 10.dp
        testPaddingIsAppliedImplementation(padding) { child: @Composable () -> Unit ->
            TestBox(modifier = Modifier.padding(padding), body = child)
        }
    }

    /**
     * Tests the top-level [padding] modifier factory with a single [androidx.compose.foundation.layout
     * .InnerPadding] argument, checking that padding is applied to a child when plenty of space
     * is available for both content and padding.
     */
    @Test
    fun paddingInnerPaddingAppliedToChild() = with(density) {
        val padding = InnerPadding(start = 1.dp, top = 3.dp, end = 6.dp, bottom = 10.dp)
        testPaddingWithDifferentInsetsImplementation(
            padding.start, padding.top, padding.end, padding.bottom
        ) { child: @Composable () -> Unit ->
            TestBox(modifier = Modifier.padding(padding), body = child)
        }
    }

    /**
     * Tests the top-level [absolutePadding] modifier factory with different values for left, top,
     * right and bottom paddings, checking that this padding is applied as expected when plenty of
     * space is available for both the content and padding.
     */
    @Test
    fun absolutePaddingAppliedToChild() {
        val paddingLeft = 10.dp
        val paddingTop = 15.dp
        val paddingRight = 20.dp
        val paddingBottom = 30.dp
        val padding = Modifier.absolutePadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
        testPaddingWithDifferentInsetsImplementation(
            paddingLeft,
            paddingTop,
            paddingRight,
            paddingBottom
        ) { child: @Composable () -> Unit ->
            TestBox(modifier = padding, body = child)
        }
    }

    /**
     * Tests the result of the [padding] modifier factory when not enough space is
     * available to accommodate both the padding and the content. In this case, the padding
     * should still be applied, modifying the final position of the content by its left and top
     * paddings even if it would result in constraints that the child content is unable or
     * unwilling to satisfy.
     */
    @Test
    fun insufficientSpaceAvailable() = with(density) {
        val padding = 30.dp
        testPaddingWithInsufficientSpaceImplementation(padding) { child: @Composable () -> Unit ->
            TestBox(modifier = Modifier.padding(padding), body = child)
        }
    }

    @Test
    fun intrinsicMeasurements() = with(density) {
        val padding = 100.toDp()

        val latch = CountDownLatch(1)
        var error: Throwable? = null
        testIntrinsics(@Composable {
            TestBox(modifier = Modifier.padding(padding)) {
                Container(Modifier.aspectRatio(2f)) { }
            }
        }) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            // Spacing is applied on both sides of an axis
            val totalAxisSpacing = (padding * 2).toIntPx()

            // When the width/height is measured as 3 x the padding
            val testDimension = (padding * 3).toIntPx()
            // The actual dimension for the AspectRatio will be: test dimension - total padding
            val actualAspectRatioDimension = testDimension - totalAxisSpacing

            // When we measure the width first, the height will be half
            val expectedAspectRatioHeight = (actualAspectRatioDimension / 2f).roundToInt()
            // When we measure the height first, the width will be double
            val expectedAspectRatioWidth = actualAspectRatioDimension * 2

            // Add back the padding on both sides to get the total expected height
            val expectedTotalHeight = expectedAspectRatioHeight + totalAxisSpacing
            // Add back the padding on both sides to get the total expected height
            val expectedTotalWidth = expectedAspectRatioWidth + totalAxisSpacing

            try {
                // Min width.
                assertEquals(totalAxisSpacing, minIntrinsicWidth(0.dp.toIntPx()))
                assertEquals(expectedTotalWidth, minIntrinsicWidth(testDimension))
                assertEquals(totalAxisSpacing, minIntrinsicWidth(Constraints.Infinity))
                // Min height.
                assertEquals(totalAxisSpacing, minIntrinsicHeight(0.dp.toIntPx()))
                assertEquals(expectedTotalHeight, minIntrinsicHeight(testDimension))
                assertEquals(totalAxisSpacing, minIntrinsicHeight(Constraints.Infinity))
                // Max width.
                assertEquals(totalAxisSpacing, maxIntrinsicWidth(0.dp.toIntPx()))
                assertEquals(expectedTotalWidth, maxIntrinsicWidth(testDimension))
                assertEquals(totalAxisSpacing, maxIntrinsicWidth(Constraints.Infinity))
                // Max height.
                assertEquals(totalAxisSpacing, maxIntrinsicHeight(0.dp.toIntPx()))
                assertEquals(expectedTotalHeight, maxIntrinsicHeight(testDimension))
                assertEquals(totalAxisSpacing, maxIntrinsicHeight(Constraints.Infinity))
            } catch (t: Throwable) {
                error = t
            } finally {
                latch.countDown()
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        error?.let { throw it }

        Unit
    }

    @Test
    fun testRtlSupport() = with(density) {
        val sizeDp = 100.toDp()
        val size = sizeDp.toIntPx()
        val padding1Dp = 5.dp
        val padding2Dp = 10.dp
        val padding3Dp = 15.dp
        val padding1 = padding1Dp.toIntPx()
        val padding2 = padding2Dp.toIntPx()
        val padding3 = padding3Dp.toIntPx()

        val drawLatch = CountDownLatch(3)
        val childSize = Array(3) { IntSize(0, 0) }
        val childPosition = Array(3) { Offset(0f, 0f) }

        // ltr: P1 S P2 | S P3 | P1 S
        // rtl:    S P1 | P3 S | P2 S P1
        show {
            Row(Modifier.fillMaxSize().rtl) {
                Stack(
                    Modifier.padding(start = padding1Dp, end = padding2Dp)
                        .preferredSize(sizeDp, sizeDp)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.positionInRoot
                            drawLatch.countDown()
                        }
                ) {
                }

                Stack(
                    Modifier.padding(end = padding3Dp)
                        .preferredSize(sizeDp, sizeDp)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.positionInRoot
                            drawLatch.countDown()
                        }
                ) {
                }

                Stack(
                    Modifier.padding(start = padding1Dp)
                        .preferredSize(sizeDp, sizeDp)
                        .onPositioned { coordinates: LayoutCoordinates ->
                            childSize[2] = coordinates.size
                            childPosition[2] = coordinates.positionInRoot
                            drawLatch.countDown()
                        }
                ) {
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        val root = findOwnerView()
        waitForDraw(root)

        val rootWidth = root.width
//        S P1 | P3 S | P2 S P1
        assertEquals(Offset((rootWidth - padding1 - size).toFloat(), 0f), childPosition[0])
        assertEquals(IntSize(size, size), childSize[0])

        assertEquals(
            Offset((rootWidth - padding1 - padding2 - size * 2).toFloat(), 0f),
            childPosition[1]
        )
        assertEquals(IntSize(size, size), childSize[1])

        assertEquals(
            Offset((rootWidth - size * 3 - padding1 * 2 - padding2 - padding3).toFloat(), 0f),
            childPosition[2]
        )
        assertEquals(IntSize(size, size), childSize[2])
    }

    @Test
    fun testPaddingRtl_whenBetweenLayoutDirectionModifiers() = with(density) {
        val padding = 50
        val size = 300
        val paddingDp = padding.toDp()
        val latch = CountDownLatch(1)
        val resultPosition = Ref<Offset>()
        val resultSize = Ref<IntSize>()

        show {
            Column(Modifier.fillMaxSize().rtl) {
                Stack(Modifier
                    .preferredSize(size.toDp())
                    .ltr
                    .padding(start = paddingDp)
                    .rtl
                    .onChildPositioned {
                        resultPosition.value = it.positionInRoot
                        resultSize.value = it.size
                        latch.countDown()
                    }
                ) {
                    Stack(Modifier.fillMaxSize()) {}
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        val root = findOwnerView()
        waitForDraw(root)
        val rootWidth = root.width

        assertEquals(
            IntSize(size - padding, size).toSize(),
            resultSize.value?.toSize()
        )
        assertEquals(
            Offset((rootWidth - size + padding).toFloat(), 0f),
            resultPosition.value
        )
    }

    @Test
    fun testInspectableParameter() {
        val exclusions = listOf("nameFallback", "rtlAware")
        val modifier = Modifier.padding(10.dp, 20.dp, 30.dp, 40.dp) as InspectableParameter
        assertThat(modifier.nameFallback).isEqualTo("padding")
        assertThat(modifier.valueOverride).isNull()
        assertThat(modifier.inspectableElements.map { it.name }.toList())
            .containsExactlyElementsIn(modifier.javaClass.declaredFields
                .filter { !it.isSynthetic && !exclusions.contains(it.name) }
                .map { it.name })
    }

    @Test
    fun testInspectableParameterForAbsolute() {
        val modifier = Modifier.absolutePadding(10.dp, 20.dp, 30.dp, 40.dp) as InspectableParameter
        assertThat(modifier.nameFallback).isEqualTo("absolutePadding")
        assertThat(modifier.valueOverride).isNull()
        assertThat(modifier.inspectableElements.map { it.name }.toList())
            .containsExactly("left", "top", "right", "bottom")
    }

    @Test
    fun testInspectableParameterWithSameOverallValue() {
        val exclusions = listOf("nameFallback", "rtlAware")
        val modifier = Modifier.padding(40.dp) as InspectableParameter
        assertThat(modifier.nameFallback).isEqualTo("padding")
        assertThat(modifier.valueOverride).isEqualTo(40.dp)
        assertThat(modifier.inspectableElements.map { it.name }.toList())
            .containsExactlyElementsIn(modifier.javaClass.declaredFields
                .filter { !it.isSynthetic && !exclusions.contains(it.name) }
                .map { it.name })
    }

    private fun testPaddingIsAppliedImplementation(
        padding: Dp,
        paddingContainer: @Composable (@Composable () -> Unit) -> Unit
    ) = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()
        val paddingPx = padding.toIntPx()

        val drawLatch = CountDownLatch(1)
        var childSize = IntSize(-1, -1)
        var childPosition = Offset(-1f, -1f)
        show {
            Stack(Modifier.fillMaxSize()) {
                ConstrainedBox(
                    constraints = DpConstraints.fixed(sizeDp, sizeDp),
                    modifier = Modifier.gravity(Alignment.Center)
                ) {
                    val children = @Composable {
                        Container(Modifier.onPositioned { coordinates: LayoutCoordinates ->
                            childSize = coordinates.size
                            childPosition = coordinates.positionInRoot
                            drawLatch.countDown()
                        }) {
                        }
                    }
                    paddingContainer(children)
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        val innerSize = (size - paddingPx * 2)
        assertEquals(IntSize(innerSize, innerSize), childSize)
        val left = ((root.width - size) / 2f).roundToInt() + paddingPx
        val top = ((root.height - size) / 2f).roundToInt() + paddingPx
        assertEquals(
            Offset(left.toFloat(), top.toFloat()),
            childPosition
        )
    }

    private fun testPaddingWithDifferentInsetsImplementation(
        left: Dp,
        top: Dp,
        right: Dp,
        bottom: Dp,
        paddingContainer: @Composable ((@Composable () -> Unit) -> Unit)
    ) = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val drawLatch = CountDownLatch(1)
        var childSize = IntSize(-1, -1)
        var childPosition = Offset(-1f, -1f)
        show {
            Stack(Modifier.fillMaxSize()) {
                ConstrainedBox(
                    constraints = DpConstraints.fixed(sizeDp, sizeDp),
                    modifier = Modifier.gravity(Alignment.Center)
                ) {
                    val children = @Composable {
                        Container(Modifier.onPositioned { coordinates: LayoutCoordinates ->
                            childSize = coordinates.size
                            childPosition = coordinates.positionInRoot
                            drawLatch.countDown()
                        }) {
                        }
                    }
                    paddingContainer(children)
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        val paddingLeft = left.toIntPx()
        val paddingRight = right.toIntPx()
        val paddingTop = top.toIntPx()
        val paddingBottom = bottom.toIntPx()
        assertEquals(
            IntSize(
                size - paddingLeft - paddingRight,
                size - paddingTop - paddingBottom
            ),
            childSize
        )
        val viewLeft = ((root.width - size) / 2f).roundToInt() + paddingLeft
        val viewTop = ((root.height - size) / 2f).roundToInt() + paddingTop
        assertEquals(
            Offset(viewLeft.toFloat(), viewTop.toFloat()),
            childPosition
        )
    }

    private fun testPaddingWithInsufficientSpaceImplementation(
        padding: Dp,
        paddingContainer: @Composable (@Composable () -> Unit) -> Unit
    ) = with(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()
        val paddingPx = padding.toIntPx()

        val drawLatch = CountDownLatch(1)
        var childSize = IntSize(-1, -1)
        var childPosition = Offset(-1f, -1f)
        show {
            Stack(Modifier.fillMaxSize()) {
                ConstrainedBox(
                    constraints = DpConstraints.fixed(sizeDp, sizeDp),
                    modifier = Modifier.gravity(Alignment.Center)
                ) {
                    paddingContainer {
                        Container(Modifier.onPositioned { coordinates: LayoutCoordinates ->
                            childSize = coordinates.size
                            childPosition = coordinates.positionInRoot
                            drawLatch.countDown()
                        }) {
                        }
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        val root = findOwnerView()
        waitForDraw(root)

        assertEquals(IntSize(0, 0), childSize)
        val left = ((root.width - size) / 2f).roundToInt() + paddingPx
        val top = ((root.height - size) / 2f).roundToInt() + paddingPx
        assertEquals(Offset(left.toFloat(), top.toFloat()), childPosition)
    }

    /**
     * A trivial layout that applies a [Modifier] and measures/lays out a single child
     * with the same constraints it received.
     */
    @Composable
    private fun TestBox(modifier: Modifier = Modifier, body: @Composable () -> Unit) {
        Layout(children = body, modifier = modifier) { measurables, constraints ->
            require(measurables.size == 1) {
                "TestBox received ${measurables.size} children; must have exactly 1"
            }
            val placeable = measurables.first().measure(constraints)
            layout(
                placeable.width.coerceAtMost(constraints.maxWidth),
                placeable.height.coerceAtMost(constraints.maxHeight)
            ) {
                placeable.place(0, 0)
            }
        }
    }
}
