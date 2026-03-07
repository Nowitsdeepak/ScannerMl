package com.ml.android.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

val isPreviewMode: Boolean @Composable get() = LocalInspectionMode.current


val mainBottomPadding = 160.dp

@Composable
fun Float.pxToDp(): Dp {
    val density = LocalDensity.current
    return with(density) { toDp() }
}


@Composable
fun Int.pxToDp(): Dp {
    val density = LocalDensity.current
    return with(density) { toDp() }
}

fun Int.pxToDp(context: Context): Dp {
    context.resources.displayMetrics.density.let {
        return this.div(it).dp
    }
}

@Composable
fun Float.pxToSp(): TextUnit {
    val density = LocalDensity.current
    return with(density) { this@pxToSp.toSp() }
}


fun Dp.toPx(context: Context): Float {
    context.resources.displayMetrics.density.let {
        return this.value.times(it)
    }
}