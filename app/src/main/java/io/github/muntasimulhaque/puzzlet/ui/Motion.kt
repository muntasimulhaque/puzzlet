package io.github.muntasimulhaque.puzzlet.ui

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * The app's whole motion vocabulary, in one place (D-084). Before this
 * file every surface picked its own numbers: the chooser rose in 220 ms,
 * the picture panel in 200, the celebration simply appeared, the held
 * piece grew in 130. Each number was fine on its own and the app as a
 * whole had no voice, which is what a child feels as "cheap" without
 * being able to name it.
 *
 * Three moves are all this app needs. A surface arrives in one quick
 * gesture; a tap is answered the same frame it lands, so nothing queues
 * behind an exit; and anything that settles under a finger settles on a
 * spring, never on a curve, because a spring can be interrupted and
 * carried on from where it was.
 *
 * These durations are the app's own frame, not a rule about time. The
 * system's animation scale still governs them: Compose scales every
 * tween and spring by the accessibility setting, so a child who runs
 * with animations off gets the same states, instantly.
 */
internal object Motion {
    /** One quick arrival: a plate, a panel, a scrim. */
    const val ARRIVE_MS = 220

    /** A held piece grows under the finger in this long. */
    const val LIFT_MS = 140

    /** The ring that answers a piece clicking home. */
    const val PULSE_MS = 380

    /** The one arrival curve: fast to leave, slow to land, never a bounce. */
    val arrive: Easing = LinearOutSlowInEasing

    /** How the world settles: a spring that can be interrupted mid flight. */
    fun <T> settle(stiffness: Float = 380f, damping: Float = 0.82f): SpringSpec<T> =
        spring(stiffness = stiffness, dampingRatio = damping)
}
