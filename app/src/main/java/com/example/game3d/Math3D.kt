package com.example.game3d

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vec3(val x: Float, val y: Float, val z: Float)

data class ProjectedMarker(
    val id: String,
    val normX: Float,
    val normY: Float,
    val visible: Boolean,
    val distance: Float
)

data class WorldRenderSnapshot(
    val playerX: Float = 0f,
    val playerY: Float = 1.68f,
    val playerZ: Float = 9.2f,
    val yawDeg: Float = 0f,
    val pitchDeg: Float = -4f,
    val isCutscene: Boolean = false,
    val cutscenePhase: Int = 0,
    val isSeatedAtDesk: Boolean = false,
    val bankOwned: Boolean = false,
    val bankUpgraded: Boolean = false,
    val isUpgrading: Boolean = false,
    val atmActive: Boolean = false,
    val bankOpen: Boolean = false,
    val carShowroomUnlocked: Boolean = false,
    val counterCount: Int = 1,
    val takaStacksOnTray: Int = 3,
    val activeTaskTarget: TaskTarget = TaskTarget.OWNER,
    val targetedInteractable: InteractableType? = null,
    val hasActiveCustomer: Boolean = false,
    val customerShirtColor: Long = 0xFFE63946L,
    val customerHairColor: Long = 0xFF1A1A1AL,
    val queueCount: Int = 0,
    val isNightTime: Boolean = false
)

enum class TaskTarget {
    OWNER,
    UPGRADE_PAD,
    ATM_MACHINE,
    DOOR_SIGN,
    TELLER_DESK,
    CAR_SHOWROOM
}

enum class InteractableType {
    OWNER,
    UPGRADE_PAD,
    ATM_MACHINE,
    DOOR_SIGN,
    TELLER_DESK,
    CAR_SHOWROOM
}

object Math3D {
    fun clamp(v: Float, min: Float, max: Float): Float = v.coerceIn(min, max)

    fun distance2D(x1: Float, z1: Float, x2: Float, z2: Float): Float {
        val dx = x1 - x2
        val dz = z1 - z2
        return sqrt(dx * dx + dz * dz)
    }

    fun angleDiffDeg(a: Float, b: Float): Float {
        var d = (a - b) % 360f
        if (d > 180f) d -= 360f
        if (d < -180f) d += 360f
        return d
    }

    fun forwardXZ(yawDeg: Float): Pair<Float, Float> {
        val rad = Math.toRadians(yawDeg.toDouble())
        // yaw = 0 faces -Z (into the bank)
        val fx = sin(rad).toFloat()
        val fz = -cos(rad).toFloat()
        return fx to fz
    }

    fun rightXZ(yawDeg: Float): Pair<Float, Float> {
        val rad = Math.toRadians(yawDeg.toDouble())
        val rx = cos(rad).toFloat()
        val rz = sin(rad).toFloat()
        return rx to rz
    }
}
