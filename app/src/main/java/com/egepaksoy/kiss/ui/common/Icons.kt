package com.egepaksoy.kiss.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** İnce çizgili, tek tip ikon seti. */
object KIcons {
    private fun stroke(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach {
                addPath(
                    pathData = addPathNodes(it),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.8f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    val Back = stroke("back", "M15 5l-7 7 7 7")
    val Phone = stroke("phone", "M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2z")
    val Torch = stroke("torch", "M8 2h8v4l-2 4v11h-4V10L8 6z", "M12 13v2")
    val Gear = stroke("gear",
        "M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6z",
        "M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z")
    val Backspace = stroke("del", "M21 5H9l-6 7 6 7h12z", "M17 9l-6 6", "M11 9l6 6")
    val Send = stroke("send", "M5 12h14", "M13 6l6 6-6 6")
    val Flip = stroke("flip", "M4 12a8 8 0 0 1 14-5.3L20 9", "M20 4v5h-5", "M20 12a8 8 0 0 1-14 5.3L4 15", "M4 20v-5h5")
    val Close = stroke("close", "M6 6l12 12", "M18 6L6 18")
    val Bolt = stroke("bolt", "M13 2L4 14h7l-1 8 9-12h-7z")
    val BoltOff = stroke("boltoff", "M13 2L4 14h7l-1 8 9-12h-7z", "M3 3l18 18")
    val Plus = stroke("plus", "M12 5v14", "M5 12h14")
    val Message = stroke("msg", "M4 5h16v11H9l-5 4z")
    val Trash = stroke("trash", "M4 7h16", "M9 7V4h6v3", "M6 7l1 13h10l1-13")
    val Search = stroke("search", "M11 4a7 7 0 1 0 0 14a7 7 0 1 0 0-14z", "M20 20l-4-4")
    val Chevron = stroke("chev", "M9 5l7 7-7 7")
    val Check = stroke("check", "M5 12l5 5 9-10")
    val Alarm = stroke("alarm", "M12 6a7 7 0 1 0 0 14a7 7 0 1 0 0-14z", "M12 10v3.5l2 1.5", "M4 5l3-2", "M20 5l-3-2")
    val Lock = stroke("lock", "M6 11h12v9H6z", "M8 11V8a4 4 0 0 1 8 0v3")
}
