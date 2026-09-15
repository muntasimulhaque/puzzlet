package io.github.muntasimulhaque.puzzlet.ui

import io.github.muntasimulhaque.puzzlet.R

/** Spoken and printed picture names. The child taps the picture; the parent reads the word. */
internal fun sceneNameRes(sceneId: String): Int = when (sceneId) {
    "sail" -> R.string.scene_sail
    "house" -> R.string.scene_house
    "balloon" -> R.string.scene_balloon
    "fruit" -> R.string.scene_fruit
    "train" -> R.string.scene_train
    "castle" -> R.string.scene_castle
    "rocket" -> R.string.scene_rocket
    "lighthouse" -> R.string.scene_lighthouse
    "truck" -> R.string.scene_truck
    "plane" -> R.string.scene_plane
    "flowers" -> R.string.scene_flowers
    "icecream" -> R.string.scene_icecream
    "kite" -> R.string.scene_kite
    "windmill" -> R.string.scene_windmill
    "beach" -> R.string.scene_beach
    "mushroom" -> R.string.scene_mushroom
    else -> R.string.app_name
}
