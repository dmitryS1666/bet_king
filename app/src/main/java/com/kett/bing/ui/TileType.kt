package com.kett.bing.ui

import com.kett.bing.R

enum class TileType(val resId: Int) {
    EAGLE(R.drawable.ic_eagle),
    TURTLE(R.drawable.ic_turtle),
    FISH(R.drawable.ic_fish);

    companion object {
        fun random() = values().random()
    }
}