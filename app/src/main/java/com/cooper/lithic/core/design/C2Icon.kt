package com.cooper.lithic.core.design

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cooper.lithic.R

/** UI-C1 vectors selectively reused; Search/Info use conventional, locally authored symbols. */
enum class C2Glyph { Now, Review, Settings, Project, Subscription, Back, Add, More, Close, Chevron, Image, ImageError, Search, Info, Computer, Book, Audio, Wellness }

@Composable
fun C2Icon(kind: C2Glyph, modifier: Modifier = Modifier, description: String? = null) {
    val resource = when (kind) {
        C2Glyph.Now -> R.drawable.ic_ui_now
        C2Glyph.Review -> R.drawable.ic_ui_review
        C2Glyph.Settings -> R.drawable.ic_ui_settings
        C2Glyph.Project -> R.drawable.ic_ui_project
        C2Glyph.Subscription -> R.drawable.ic_ui_subscription
        C2Glyph.Back -> R.drawable.ic_ui_back
        C2Glyph.Add -> R.drawable.ic_ui_add
        C2Glyph.More -> R.drawable.ic_ui_more
        C2Glyph.Close -> R.drawable.ic_ui_close
        C2Glyph.Chevron -> R.drawable.ic_ui_chevron_right
        C2Glyph.Image -> R.drawable.ic_ui_image
        C2Glyph.ImageError -> R.drawable.ic_ui_image_error
        C2Glyph.Search -> R.drawable.ic_ui_search
        C2Glyph.Info -> R.drawable.ic_ui_info
        C2Glyph.Computer -> R.drawable.ic_ui_computer
        C2Glyph.Book -> R.drawable.ic_ui_book
        C2Glyph.Audio -> R.drawable.ic_ui_audio
        C2Glyph.Wellness -> R.drawable.ic_ui_wellness
    }
    Icon(painterResource(resource), description, modifier.size(24.dp))
}
