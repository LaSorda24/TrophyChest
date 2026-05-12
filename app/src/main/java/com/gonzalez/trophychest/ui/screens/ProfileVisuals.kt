package com.gonzalez.trophychest.ui.screens

import com.gonzalez.trophychest.R
import com.gonzalez.trophychest.data.UserDefaults

internal fun profileAvatarRes(profileImageId: Int): Int {
    return when (profileImageId.coerceIn(UserDefaults.MIN_IMAGE_ID, UserDefaults.MAX_IMAGE_ID)) {
        1 -> R.drawable.avatar_profile_1
        2 -> R.drawable.avatar_profile_2
        3 -> R.drawable.avatar_profile_3
        4 -> R.drawable.avatar_profile_4
        else -> R.drawable.avatar_profile_5
    }
}

internal fun profileBannerRes(bannerImageId: Int): Int {
    return when (bannerImageId.coerceIn(UserDefaults.MIN_IMAGE_ID, UserDefaults.MAX_IMAGE_ID)) {
        1 -> R.drawable.banner_profile_1
        2 -> R.drawable.banner_profile_2
        3 -> R.drawable.banner_profile_3
        4 -> R.drawable.banner_profile_4
        else -> R.drawable.banner_profile_5
    }
}
