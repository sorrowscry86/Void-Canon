package org.voidcat.voidcanon.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Small original line icons; avoid shipping an entire extended icon font/library. */
object CanonIcons {
    private fun icon(name: String, path: String, mirrored: Boolean = false): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f, autoMirror = mirrored).apply {
            addPath(PathParser().parsePathString(path).toNodes(), fill = null, stroke = SolidColor(Color.White),
                strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }.build()

    val World = icon("World", "M21 12A9 9 0 1 1 3 12A9 9 0 1 1 21 12M3 12H21M12 3C7 8 7 16 12 21C17 16 17 8 12 3M5 6.5C9 9 15 9 19 6.5M5 17.5C9 15 15 15 19 17.5")
    val Images = icon("Images", "M5 3H21V19H5ZM5 15L10 10L14 14L17 11L21 15M2 7V22H17M16 7H16.01")
    val Play = icon("Play", "M7 3L21 12L7 21Z")
    val Book = icon("Book", "M12 5C9 3 5 3 2 4V20C5 19 9 19 12 21C15 19 19 19 22 20V4C19 3 15 3 12 5ZM12 5V21M5 8H9M5 12H9M15 8H19M15 12H19")
    val Box = icon("Backup", "M3 7L12 3L21 7L12 11ZM3 7V18L12 22L21 18V7M12 11V22M7 5L16 9")
    val Offline = icon("Offline", "M21 12A9 9 0 1 1 3 12A9 9 0 1 1 21 12M7 12L10.5 15.5L17 9")
    val Right = icon("Next", "M9 5L16 12L9 19", true)
    val Back = icon("Back", "M19 12H5M12 5L5 12L12 19", true)
    val Edit = icon("Edit", "M15 4L20 9M4 20L9 19L21 7C22 6 18 2 17 3L5 15Z")
    val Person = icon("Character", "M16 7A4 4 0 1 1 8 7A4 4 0 1 1 16 7M4 21V19C4 12 20 12 20 19V21")
    val Place = icon("Location", "M19 9C19 15 12 22 12 22C12 22 5 15 5 9A7 7 0 1 1 19 9M14.5 9A2.5 2.5 0 1 1 9.5 9A2.5 2.5 0 1 1 14.5 9")
    val Upload = icon("Export", "M12 16V3M7 8L12 3L17 8M4 15V21H20V15")
    val Download = icon("Restore", "M12 3V16M7 11L12 16L17 11M4 18V21H20V18")
    val ImageAdd = icon("Import image", "M20 14V21H3V4H13M3 17L8 12L13 17M17 6H23M20 3V9M8 8H8.01")
    val History = icon("History", "M3 10A9 9 0 1 1 5 18M3 4V10H9M12 7V12L16 14")
    val Save = icon("Save", "M4 3H17L21 7V21H3V3ZM7 3V9H17V3M7 21V14H17V21")
    val Send = icon("Send", "M3 3L22 12L3 21L7 12ZM7 12H22", true)
    val Alert = icon("Notice", "M21 12A9 9 0 1 1 3 12A9 9 0 1 1 21 12M12 7V13M12 17H12.01")
}
