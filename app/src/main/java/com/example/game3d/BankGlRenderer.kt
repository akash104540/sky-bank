package com.example.game3d

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import java.nio.ByteBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class BankGlRenderer(
    private val onMarkersUpdated: (List<ProjectedMarker>) -> Unit
) : GLSurfaceView.Renderer {

    @Volatile
    var worldSnapshot: WorldRenderSnapshot = WorldRenderSnapshot()

    private var program = 0
    private var aPositionLoc = 0
    private var aNormalLoc = 0
    private var uMvpLoc = 0
    private var uModelLoc = 0
    private var uColorLoc = 0
    private var uLightDirLoc = 0
    private var uEmissiveLoc = 0
    private var uPatternLoc = 0
    private var uTimeLoc = 0

    private lateinit var cubeMesh: Mesh
    private lateinit var cylinderMesh: Mesh
    private lateinit var sphereMesh: Mesh
    private lateinit var arrowMesh: Mesh

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var viewportWidth = 1
    private var viewportHeight = 1
    private val startTimeMs = System.currentTimeMillis()
    private var frameCounter = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        aPositionLoc = GLES20.glGetAttribLocation(program, "aPosition")
        aNormalLoc = GLES20.glGetAttribLocation(program, "aNormal")
        uMvpLoc = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        uModelLoc = GLES20.glGetUniformLocation(program, "uModelMatrix")
        uColorLoc = GLES20.glGetUniformLocation(program, "uColor")
        uLightDirLoc = GLES20.glGetUniformLocation(program, "uLightDir")
        uEmissiveLoc = GLES20.glGetUniformLocation(program, "uEmissive")
        uPatternLoc = GLES20.glGetUniformLocation(program, "uPattern")
        uTimeLoc = GLES20.glGetUniformLocation(program, "uTime")

        cubeMesh = MeshBuilder.createUnitCube()
        cylinderMesh = MeshBuilder.createCylinder(18)
        sphereMesh = MeshBuilder.createSphere(10, 16)
        arrowMesh = MeshBuilder.createChevronArrow()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width.coerceAtLeast(1)
        viewportHeight = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
        val aspect = viewportWidth.toFloat() / viewportHeight.toFloat()
        Matrix.perspectiveM(projMatrix, 0, 56f, aspect, 0.1f, 120f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val snap = worldSnapshot
        val timeSec = (System.currentTimeMillis() - startTimeMs) / 1000f

        if (snap.isNightTime) {
            GLES20.glClearColor(0.08f, 0.13f, 0.24f, 1f)
        } else {
            GLES20.glClearColor(0.45f, 0.74f, 0.94f, 1f)
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        GLES20.glUseProgram(program)

        val cam = computeCamera(snap, timeSec)
        Matrix.setLookAtM(viewMatrix, 0, cam[0], cam[1], cam[2], cam[3], cam[4], cam[5], 0f, 1f, 0f)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        GLES20.glUniform3f(uLightDirLoc, 0.42f, 0.85f, 0.50f)
        GLES20.glUniform1f(uTimeLoc, timeSec)

        drawSkyAndGround(snap)
        drawDhakaCityAndPark(timeSec)
        drawBankStructure(snap)
        drawBankInteriorProps(snap, timeSec)
        drawCarShowroom(snap, timeSec)
        drawCharacters(snap, timeSec)
        drawFloorGuides(snap, timeSec)
        drawTranslucentGlass(snap, timeSec)

        frameCounter++
        if (frameCounter % 2 == 0) {
            emitProjectedMarkers(snap, timeSec)
        }
    }

    private fun computeCamera(snap: WorldRenderSnapshot, t: Float): FloatArray {
        return when {
            snap.isCutscene && snap.cutscenePhase == 0 -> {
                // Inside bank hall showing upset customer, desks & vault (Screenshot 1)
                val drift = sin(t * 0.45f) * 0.4f
                floatArrayOf(-3.2f + drift, 2.35f, 0.5f, 1.4f, 1.35f, -5.8f)
            }
            snap.isCutscene && snap.cutscenePhase == 1 -> {
                // Outside bank looking at SKY BANK facade + stressed owner (Screenshot 2)
                val drift = sin(t * 0.5f) * 0.35f
                floatArrayOf(0.8f + drift, 2.0f, 10.6f, -0.8f, 2.3f, 2.8f)
            }
            snap.isSeatedAtDesk -> {
                // First-person seated at Teller Desk looking across monitor & customer (Screenshots 6-8)
                floatArrayOf(-0.2f, 1.56f, -4.45f, -0.2f, 1.35f, -1.6f)
            }
            else -> {
                val yawRad = Math.toRadians(snap.yawDeg.toDouble())
                val pitchRad = Math.toRadians(snap.pitchDeg.toDouble())
                val fx = (cos(pitchRad) * sin(yawRad)).toFloat()
                val fy = sin(pitchRad).toFloat()
                val fz = (-cos(pitchRad) * cos(yawRad)).toFloat()
                floatArrayOf(
                    snap.playerX, snap.playerY, snap.playerZ,
                    snap.playerX + fx * 5f, snap.playerY + fy * 5f, snap.playerZ + fz * 5f
                )
            }
        }
    }

    private fun drawSkyAndGround(snap: WorldRenderSnapshot) {
        // Distant grass/park ground
        drawBox(0f, -0.12f, 10f, 90f, 0.2f, 90f, 0.24f, 0.55f, 0.28f, pattern = 0)
        // Brick plaza sidewalk outside bank
        drawBox(0f, -0.01f, 6.8f, 22f, 0.04f, 5.6f, 0.62f, 0.42f, 0.34f, pattern = 2)
        // Dhaka City Asphalt Street
        drawBox(0f, -0.02f, 12.6f, 70f, 0.03f, 5.6f, 0.19f, 0.20f, 0.23f)
        // Road lane dashes
        for (ix in -24..24 step 4) {
            drawBox(ix.toFloat(), 0.005f, 12.6f, 1.8f, 0.01f, 0.18f, 0.95f, 0.85f, 0.30f, emissive = 0.1f)
        }
        // Bank marble lobby floor
        drawBox(0f, 0.0f, -3.0f, 18.2f, 0.05f, 14.2f, 0.82f, 0.84f, 0.86f, pattern = 1)
    }

    private fun drawDhakaCityAndPark(t: Float) {
        // Lush park trees in front of bank across the street
        val treePositions = listOf(
            -10f to 18.5f, -5.5f to 21f, 4.5f to 19.5f, 11f to 18f,
            -14f to 12f, 14.5f to 11f, -13f to -4f, 14f to -5f
        )
        for ((tx, tz) in treePositions) {
            drawCylinder(tx, 1.4f, tz, 0.42f, 2.8f, 0.42f, 0.40f, 0.26f, 0.16f)
            drawSphere(tx, 3.3f, tz, 2.5f, 2.2f, 2.5f, 0.16f, 0.54f, 0.22f)
            drawSphere(tx + 0.6f, 2.8f, tz - 0.3f, 1.7f, 1.6f, 1.7f, 0.20f, 0.62f, 0.25f)
        }
        // Park wooden benches
        for (bx in listOf(-6.5f, 5.5f)) {
            drawBox(bx, 0.38f, 16.5f, 2.0f, 0.1f, 0.6f, 0.55f, 0.32f, 0.16f)
            drawBox(bx, 0.72f, 16.78f, 2.0f, 0.5f, 0.08f, 0.55f, 0.32f, 0.16f)
        }
        // Dhaka high-rise skyline silhouettes
        val buildings = listOf(
            floatArrayOf(-18f, 29f, 6f, 18f, 6f, 0.35f, 0.42f, 0.52f),
            floatArrayOf(-9f, 32f, 7f, 24f, 6f, 0.28f, 0.36f, 0.48f),
            floatArrayOf(2f, 34f, 8f, 28f, 7f, 0.32f, 0.40f, 0.50f),
            floatArrayOf(13f, 30f, 7f, 21f, 6f, 0.38f, 0.44f, 0.54f),
            floatArrayOf(22f, 26f, 6f, 16f, 6f, 0.30f, 0.38f, 0.46f),
            floatArrayOf(-24f, 4f, 6f, 17f, 14f, 0.36f, 0.41f, 0.49f),
            floatArrayOf(24f, 3f, 6f, 20f, 14f, 0.33f, 0.39f, 0.48f)
        )
        for (b in buildings) {
            drawBox(b[0], b[3] * 0.5f, b[1], b[2], b[3], b[4], b[5], b[6], b[7])
            // Lit window bands
            for (wy in 3 until b[3].toInt() - 1 step 3) {
                drawBox(b[0], wy.toFloat(), b[1] - b[4] * 0.51f, b[2] * 0.78f, 0.7f, 0.08f, 0.72f, 0.88f, 0.96f, emissive = 0.2f)
            }
        }
    }

    private fun drawBankStructure(snap: WorldRenderSnapshot) {
        val upgraded = snap.bankUpgraded
        val wallR = if (upgraded) 0.09f else 0.16f
        val wallG = if (upgraded) 0.38f else 0.29f
        val wallB = if (upgraded) 0.28f else 0.23f

        // Back wall
        drawBox(0f, 2.3f, -10.0f, 18.2f, 4.6f, 0.35f, wallR, wallG, wallB, pattern = 3)
        // Left & Right stone/emerald walls with window cutouts (lower + upper + pillars)
        drawBox(-9.0f, 0.55f, -3.0f, 0.35f, 1.1f, 14.2f, wallR, wallG, wallB)
        drawBox(-9.0f, 4.0f, -3.0f, 0.35f, 1.2f, 14.2f, wallR, wallG, wallB)
        drawBox(9.0f, 0.55f, -3.0f, 0.35f, 1.1f, 14.2f, wallR, wallG, wallB)
        drawBox(9.0f, 4.0f, -3.0f, 0.35f, 1.2f, 14.2f, wallR, wallG, wallB)
        for (pz in listOf(-9.8f, -5.2f, -0.8f, 3.8f)) {
            drawBox(-9.0f, 2.3f, pz, 0.45f, 4.6f, 0.75f, 0.28f, 0.30f, 0.31f)
            drawBox(9.0f, 2.3f, pz, 0.45f, 4.6f, 0.75f, 0.28f, 0.30f, 0.31f)
        }

        // Ceiling with warm recessed lights
        drawBox(0f, 4.65f, -3.0f, 18.6f, 0.25f, 14.5f, 0.86f, 0.86f, 0.84f)
        for (lx in listOf(-5.5f, 0f, 5.5f)) {
            for (lz in listOf(-7.0f, -2.5f, 1.8f)) {
                drawBox(lx, 4.50f, lz, 1.1f, 0.05f, 0.55f, 1.0f, 0.97f, 0.88f, emissive = 0.75f)
            }
        }

        // Front Stone Columns flanking glass storefront
        drawBox(-7.4f, 2.4f, 4.05f, 3.2f, 4.8f, 0.55f, 0.62f, 0.61f, 0.58f)
        drawBox(7.4f, 2.4f, 4.05f, 3.2f, 4.8f, 0.55f, 0.62f, 0.61f, 0.58f)
        // Front header beam / cornice
        drawBox(0f, 4.45f, 4.10f, 18.4f, 1.45f, 0.75f, 0.22f, 0.24f, 0.26f)
        if (upgraded) {
            // Golden trim stripe after upgrade
            drawBox(0f, 3.82f, 4.48f, 18.4f, 0.14f, 0.08f, 0.96f, 0.74f, 0.18f, emissive = 0.25f)
        }

        // 3D Block Letters "SKY BANK" on front roof header!
        draw3DText("SKY BANK", 0f, 4.52f, 4.55f, letterScale = 0.20f, r = 0.96f, g = 0.78f, b = 0.24f, emissive = 0.20f)

        // Back Wall Dark Signboard + "SKY BANK"
        drawBox(0f, 2.75f, -9.75f, 5.2f, 1.35f, 0.15f, 0.09f, 0.12f, 0.16f)
        draw3DText("SKY BANK", 0f, 2.75f, -9.60f, letterScale = 0.11f, r = 0.90f, g = 0.95f, b = 1.0f, emissive = 0.30f)

        // Massive Steel Vault Door on Back-Right Wall (Screenshot 1 & 4)
        drawBox(5.4f, 1.75f, -9.72f, 2.8f, 3.1f, 0.22f, 0.20f, 0.22f, 0.25f)
        drawBox(5.4f, 1.75f, -9.58f, 2.35f, 2.65f, 0.18f, 0.48f, 0.52f, 0.56f)
        // Vault locking wheel & horizontal steel bars
        drawBox(5.4f, 2.3f, -9.45f, 1.9f, 0.10f, 0.10f, 0.75f, 0.78f, 0.80f)
        drawBox(5.4f, 1.2f, -9.45f, 1.9f, 0.10f, 0.10f, 0.75f, 0.78f, 0.80f)
        drawCylinder(5.8f, 1.75f, -9.42f, 0.65f, 0.12f, 0.65f, 0.82f, 0.70f, 0.24f, rotX = 90f)
    }

    private fun drawBankInteriorProps(snap: WorldRenderSnapshot, t: Float) {
        // Main Player Teller Desk at (-0.2, 0, -3.8)
        drawTellerCounter(-0.2f, -3.8f, isMain = true, takaStacks = snap.takaStacksOnTray, isHighlighted = snap.targetedInteractable == InteractableType.TELLER_DESK)

        // Additional Teller Counters
        if (snap.counterCount >= 2) drawTellerCounter(3.3f, -3.8f, isMain = false, takaStacks = 2, isHighlighted = false)
        if (snap.counterCount >= 3) drawTellerCounter(-3.7f, -3.8f, isMain = false, takaStacks = 2, isHighlighted = false)
        if (snap.counterCount >= 4) drawTellerCounter(6.4f, -3.8f, isMain = false, takaStacks = 3, isHighlighted = false)

        // ATM Machine near Front-Right Window (x = 6.4, z = 2.0)
        val atmHighlight = snap.targetedInteractable == InteractableType.ATM_MACHINE
        val atmGlow = if (atmHighlight) 0.45f else 0.0f
        drawBox(6.4f, 0.95f, 2.0f, 0.95f, 1.9f, 0.85f, 0.55f, 0.57f, 0.59f, emissive = atmGlow * 0.4f)
        // ATM Screen
        val scrG = if (snap.atmActive) 0.92f else 0.15f
        val scrB = if (snap.atmActive) 0.55f else 0.20f
        drawBox(6.4f, 1.28f, 1.56f, 0.58f, 0.42f, 0.04f, 0.1f, scrG, scrB, emissive = if (snap.atmActive) 0.85f else 0.1f)
        // ATM keypad shelf
        drawBox(6.4f, 0.92f, 1.48f, 0.62f, 0.06f, 0.28f, 0.22f, 0.24f, 0.26f)

        // Water Cooler beside ATM (Screenshot 5)
        drawBox(4.8f, 0.55f, 2.8f, 0.48f, 1.1f, 0.48f, 0.92f, 0.93f, 0.95f)
        drawCylinder(4.8f, 1.38f, 2.8f, 0.36f, 0.55f, 0.36f, 0.25f, 0.72f, 0.95f, emissive = 0.15f)

        // Potted Plants in corners
        for ((px, pz) in listOf(-7.8f to 2.9f, 7.8f to -8.5f, -7.8f to -8.5f)) {
            drawCylinder(px, 0.30f, pz, 0.52f, 0.60f, 0.52f, 0.55f, 0.30f, 0.18f)
            drawSphere(px, 0.92f, pz, 0.85f, 0.90f, 0.85f, 0.15f, 0.58f, 0.24f)
        }

        // Bouncing 3D Target Arrow over active objective
        val arrowTarget = when (snap.activeTaskTarget) {
            TaskTarget.OWNER -> Vec3(-1.8f, 2.65f, 6.2f)
            TaskTarget.UPGRADE_PAD -> Vec3(2.6f, 1.3f, -1.6f)
            TaskTarget.ATM_MACHINE -> Vec3(6.4f, 2.45f, 2.0f)
            TaskTarget.DOOR_SIGN -> Vec3(-1.8f, 2.25f, 4.15f)
            TaskTarget.TELLER_DESK -> Vec3(-0.2f, 2.25f, -3.8f)
            TaskTarget.CAR_SHOWROOM -> Vec3(-6.4f, 2.15f, -1.5f)
        }
        val bob = sin(t * 4.5f) * 0.16f
        drawBox(arrowTarget.x, arrowTarget.y + bob + 0.22f, arrowTarget.z, 0.16f, 0.38f, 0.16f, 0.18f, 0.92f, 0.32f, emissive = 0.7f)
        drawBox(arrowTarget.x, arrowTarget.y + bob, arrowTarget.z, 0.38f, 0.16f, 0.38f, 0.18f, 0.92f, 0.32f, emissive = 0.7f)
    }

    private fun drawTellerCounter(cx: Float, cz: Float, isMain: Boolean, takaStacks: Int, isHighlighted: Boolean) {
        val hi = if (isHighlighted) 0.25f else 0f
        // Wood counter base + black granite top (Screenshots 6-8)
        drawBox(cx, 0.50f, cz, 2.5f, 1.0f, 0.95f, 0.66f, 0.45f, 0.26f, emissive = hi)
        drawBox(cx, 1.03f, cz, 2.58f, 0.08f, 1.05f, 0.12f, 0.14f, 0.16f, emissive = hi)
        // Side modesty wings
        drawBox(cx - 1.24f, 0.65f, cz - 0.1f, 0.10f, 1.30f, 1.15f, 0.62f, 0.42f, 0.24f)
        drawBox(cx + 1.24f, 0.65f, cz - 0.1f, 0.10f, 1.30f, 1.15f, 0.62f, 0.42f, 0.24f)

        // Desk Mat
        drawBox(cx - 0.45f, 1.075f, cz - 0.02f, 1.15f, 0.015f, 0.62f, 0.12f, 0.18f, 0.32f)
        // PC Monitor
        drawBox(cx - 0.48f, 1.16f, cz + 0.12f, 0.18f, 0.16f, 0.14f, 0.65f, 0.67f, 0.70f)
        drawBox(cx - 0.48f, 1.44f, cz + 0.12f, 0.82f, 0.48f, 0.04f, 0.12f, 0.13f, 0.15f)
        val scrR = if (isMain) 0.06f else 0.10f
        val scrG = if (isMain) 0.48f else 0.35f
        val scrB = if (isMain) 0.85f else 0.65f
        drawBox(cx - 0.48f, 1.44f, cz + 0.095f, 0.76f, 0.42f, 0.02f, scrR, scrG, scrB, emissive = 0.65f)
        // Keyboard & Mouse
        drawBox(cx - 0.56f, 1.095f, cz - 0.12f, 0.56f, 0.025f, 0.20f, 0.28f, 0.30f, 0.34f)
        drawBox(cx - 0.12f, 1.095f, cz - 0.12f, 0.10f, 0.025f, 0.15f, 0.32f, 0.34f, 0.36f)

        // Two Miniature Desk Flags (Bangladesh Flag + Sky Bank Flag!)
        drawCylinder(cx + 0.16f, 1.24f, cz + 0.10f, 0.02f, 0.34f, 0.02f, 0.15f, 0.15f, 0.15f)
        // Bangladesh Green & Red Flag
        drawBox(cx + 0.26f, 1.33f, cz + 0.10f, 0.18f, 0.11f, 0.01f, 0.0f, 0.42f, 0.31f, emissive = 0.15f)
        drawSphere(cx + 0.25f, 1.33f, cz + 0.10f, 0.06f, 0.06f, 0.02f, 0.95f, 0.16f, 0.25f, emissive = 0.2f)
        // Sky Bank Cyan/Gold Flag
        drawBox(cx + 0.06f, 1.33f, cz + 0.10f, 0.18f, 0.11f, 0.01f, 0.0f, 0.68f, 0.85f, emissive = 0.2f)

        // Wooden Cash Tray + Physical Taka Bundles
        drawBox(cx + 0.72f, 1.085f, cz - 0.02f, 0.72f, 0.03f, 0.52f, 0.48f, 0.34f, 0.20f)
        val stacks = takaStacks.coerceIn(0, 12)
        for (i in 0 until stacks) {
            val col = i % 3
            val layer = i / 3
            val sx = cx + 0.50f + col * 0.22f
            val sy = 1.125f + layer * 0.055f
            drawBox(sx, sy, cz - 0.02f, 0.17f, 0.045f, 0.28f, 0.22f, 0.75f, 0.34f, emissive = 0.1f)
            drawBox(sx, sy, cz - 0.02f, 0.175f, 0.048f, 0.06f, 0.95f, 0.92f, 0.82f)
        }

        // Office Chair behind counter
        drawBox(cx, 0.48f, cz - 0.95f, 0.52f, 0.10f, 0.52f, 0.32f, 0.35f, 0.39f)
        drawBox(cx, 0.88f, cz - 1.18f, 0.48f, 0.75f, 0.10f, 0.32f, 0.35f, 0.39f)
        drawCylinder(cx, 0.24f, cz - 0.95f, 0.10f, 0.44f, 0.10f, 0.18f, 0.18f, 0.20f)
    }

    private fun drawCarShowroom(snap: WorldRenderSnapshot, t: Float) {
        val sx = -6.4f
        val sz = -1.5f
        // Showroom circular podium
        val glow = if (snap.carShowroomUnlocked) 0.35f else 0.15f
        drawCylinder(sx, 0.08f, sz, 3.2f, 0.16f, 3.2f, 0.15f, 0.48f, 0.82f, emissive = glow)

        if (snap.carShowroomUnlocked) {
            // Rotating 3D Luxury Sports Sedan for Car Finance!
            val angle = t * 28f
            val rad = Math.toRadians(angle.toDouble())
            val cosA = cos(rad).toFloat()
            val sinA = sin(rad).toFloat()
            // Car chassis
            drawRotatedBox(sx, 0.52f, sz, 1.35f, 0.44f, 2.75f, angle, 0.06f, 0.58f, 0.44f, emissive = 0.12f)
            // Cabin roof + glass
            drawRotatedBox(sx - sinA * 0.15f, 0.88f, sz - cosA * 0.15f, 1.12f, 0.38f, 1.35f, angle, 0.12f, 0.22f, 0.30f, emissive = 0.2f)
            // Wheels
            val wheelOffsets = listOf(-0.68f to -0.82f, 0.68f to -0.82f, -0.68f to 0.82f, 0.68f to 0.82f)
            for ((wx, wz) in wheelOffsets) {
                val rx = sx + wx * cosA + wz * sinA
                val rz = sz - wx * sinA + wz * cosA
                drawCylinder(rx, 0.34f, rz, 0.36f, 0.22f, 0.36f, 0.10f, 0.10f, 0.12f, rotZ = 90f, rotY = angle)
            }
            // Headlights
            for (hx in listOf(-0.45f, 0.45f)) {
                val rx = sx + hx * cosA + 1.38f * sinA
                val rz = sz - hx * sinA + 1.38f * cosA
                drawBox(rx, 0.54f, rz, 0.24f, 0.12f, 0.08f, 1.0f, 0.96f, 0.72f, emissive = 0.9f)
            }
        }
    }

    private fun drawCharacters(snap: WorldRenderSnapshot, t: Float) {
        if (snap.isCutscene && snap.cutscenePhase == 0) {
            // Cutscene 1: Upset customer + sad bank teller inside
            drawHumanoid(-1.2f, 0f, -2.5f, 0f, 0.92f, 0.46f, 0.26f, 0.90f, 0.75f, 0.20f, holdingCash = false, highlight = false, bobOffset = sin(t * 5f) * 0.03f)
            drawHumanoid(0.2f, 0f, -4.4f, 180f, 0.14f, 0.28f, 0.58f, 0.12f, 0.12f, 0.14f, holdingCash = false, highlight = false, bobOffset = 0f)
            return
        }

        // Previous Owner standing outside before bank is bought
        if (!snap.bankOwned) {
            val hi = snap.targetedInteractable == InteractableType.OWNER
            drawHumanoid(
                x = -1.8f, y = 0f, z = 6.2f,
                facingDeg = 180f,
                shirtR = 0.42f, shirtG = 0.36f, shirtB = 0.26f,
                hairR = 0.12f, hairG = 0.10f, hairB = 0.10f,
                holdingCash = false,
                highlight = hi,
                bobOffset = sin(t * 2.5f) * 0.02f
            )
        }

        // Construction Worker when upgrading
        if (snap.isUpgrading) {
            drawHumanoid(
                x = 4.0f, y = 0f, z = -3.8f,
                facingDeg = -90f,
                shirtR = 0.96f, shirtG = 0.42f, shirtB = 0.12f,
                hairR = 0.90f, hairG = 0.18f, hairB = 0.12f,
                holdingCash = false,
                highlight = false,
                bobOffset = sin(t * 7f) * 0.04f
            )
            // Paint bucket
            drawCylinder(4.35f, 0.22f, -3.3f, 0.32f, 0.44f, 0.32f, 0.85f, 0.87f, 0.90f)
        }

        // Extra Teller Staff behind extra counters
        if (snap.counterCount >= 2) {
            drawHumanoid(3.3f, 0f, -4.6f, 180f, 0.10f, 0.38f, 0.32f, 0.12f, 0.10f, 0.10f, false, false, 0f)
        }
        if (snap.counterCount >= 3) {
            drawHumanoid(-3.7f, 0f, -4.6f, 180f, 0.12f, 0.26f, 0.52f, 0.14f, 0.10f, 0.08f, false, false, 0f)
        }

        // Customers Queue in front of main desk
        if (snap.hasActiveCustomer) {
            val sr = ((snap.customerShirtColor shr 16) and 0xFF) / 255f
            val sg = ((snap.customerShirtColor shr 8) and 0xFF) / 255f
            val sb = (snap.customerShirtColor and 0xFF) / 255f
            val hr = ((snap.customerHairColor shr 16) and 0xFF) / 255f
            val hg = ((snap.customerHairColor shr 8) and 0xFF) / 255f
            val hb = (snap.customerHairColor and 0xFF) / 255f

            drawHumanoid(
                x = -0.15f, y = 0f, z = -2.45f,
                facingDeg = 0f,
                shirtR = sr, shirtG = sg, shirtB = sb,
                hairR = hr, hairG = hg, hairB = hb,
                holdingCash = true,
                highlight = snap.isSeatedAtDesk || snap.targetedInteractable == InteractableType.TELLER_DESK,
                bobOffset = sin(t * 2.8f) * 0.018f
            )

            // Additional waiting customers behind in queue
            val queueColors = listOf(
                floatArrayOf(0.22f, 0.58f, 0.72f),
                floatArrayOf(0.82f, 0.38f, 0.52f),
                floatArrayOf(0.32f, 0.62f, 0.38f)
            )
            val extra = (snap.queueCount - 1).coerceIn(0, 3)
            for (i in 0 until extra) {
                val c = queueColors[i % queueColors.size]
                drawHumanoid(
                    x = -0.15f + (i % 2) * 0.25f,
                    y = 0f,
                    z = -1.1f + i * 1.35f,
                    facingDeg = 0f,
                    shirtR = c[0], shirtG = c[1], shirtB = c[2],
                    hairR = 0.12f, hairG = 0.10f, hairB = 0.10f,
                    holdingCash = true,
                    highlight = false,
                    bobOffset = sin(t * 2.5f + i) * 0.02f
                )
            }
        }
    }

    private fun drawHumanoid(
        x: Float, y: Float, z: Float, facingDeg: Float,
        shirtR: Float, shirtG: Float, shirtB: Float,
        hairR: Float, hairG: Float, hairB: Float,
        holdingCash: Boolean,
        highlight: Boolean,
        bobOffset: Float
    ) {
        val em = if (highlight) 0.28f else 0f
        val skinR = 0.85f; val skinG = 0.67f; val skinB = 0.54f
        val rad = Math.toRadians(facingDeg.toDouble())
        val fwdX = sin(rad).toFloat()
        val fwdZ = -cos(rad).toFloat()
        val rightX = cos(rad).toFloat()
        val rightZ = sin(rad).toFloat()

        // Highlight halo ring at feet
        if (highlight) {
            drawCylinder(x, 0.03f, z, 0.85f, 0.02f, 0.85f, 0.15f, 0.95f, 0.35f, emissive = 0.85f)
        }

        // Legs
        drawRotatedBox(x - rightX * 0.12f, y + 0.42f, z - rightZ * 0.12f, 0.17f, 0.84f, 0.19f, facingDeg, 0.14f, 0.16f, 0.22f, emissive = em * 0.4f)
        drawRotatedBox(x + rightX * 0.12f, y + 0.42f, z + rightZ * 0.12f, 0.17f, 0.84f, 0.19f, facingDeg, 0.14f, 0.16f, 0.22f, emissive = em * 0.4f)
        // Torso
        drawRotatedBox(x, y + 1.12f + bobOffset, z, 0.44f, 0.60f, 0.24f, facingDeg, shirtR, shirtG, shirtB, emissive = em)
        // Left Arm
        drawRotatedBox(x - rightX * 0.30f, y + 1.10f + bobOffset, z - rightZ * 0.30f, 0.14f, 0.54f, 0.15f, facingDeg, shirtR * 0.9f, shirtG * 0.9f, shirtB * 0.9f, emissive = em)
        // Right Arm (raised forward if holding cash)
        if (holdingCash) {
            drawRotatedBox(x + rightX * 0.30f + fwdX * 0.16f, y + 1.24f + bobOffset, z + rightZ * 0.30f + fwdZ * 0.16f, 0.14f, 0.16f, 0.48f, facingDeg, shirtR * 0.9f, shirtG * 0.9f, shirtB * 0.9f, emissive = em)
            // Green Taka bundle in hand!
            drawRotatedBox(x + rightX * 0.30f + fwdX * 0.42f, y + 1.25f + bobOffset, z + rightZ * 0.30f + fwdZ * 0.42f, 0.10f, 0.14f, 0.22f, facingDeg, 0.20f, 0.82f, 0.35f, emissive = 0.25f)
        } else {
            drawRotatedBox(x + rightX * 0.30f, y + 1.10f + bobOffset, z + rightZ * 0.30f, 0.14f, 0.54f, 0.15f, facingDeg, shirtR * 0.9f, shirtG * 0.9f, shirtB * 0.9f, emissive = em)
        }
        // Head & Hair
        drawSphere(x, y + 1.58f + bobOffset, z, 0.28f, 0.32f, 0.28f, skinR, skinG, skinB, emissive = em * 0.5f)
        drawSphere(x - fwdX * 0.02f, y + 1.66f + bobOffset, z - fwdZ * 0.02f, 0.30f, 0.22f, 0.30f, hairR, hairG, hairB, emissive = em * 0.3f)
    }

    private fun drawFloorGuides(snap: WorldRenderSnapshot, t: Float) {
        // Glowing Upgrade Pad inside lobby (if owned and not yet upgraded)
        if (snap.bankOwned && !snap.bankUpgraded) {
            drawCylinder(2.6f, 0.03f, -1.6f, 1.55f, 0.04f, 1.55f, 0.05f, 0.82f, 0.95f, emissive = 0.75f, pattern = 4)
        }
        // Car Showroom Pad (always visible on left wing when owned)
        if (snap.bankOwned && !snap.carShowroomUnlocked) {
            drawCylinder(-6.4f, 0.03f, -1.5f, 2.0f, 0.04f, 2.0f, 0.25f, 0.45f, 0.98f, emissive = 0.75f, pattern = 4)
        }

        // Animated blue floor chevron arrows guiding player to active objective!
        if (!snap.isCutscene && !snap.isSeatedAtDesk) {
            val target = when (snap.activeTaskTarget) {
                TaskTarget.OWNER -> -1.8f to 6.2f
                TaskTarget.UPGRADE_PAD -> 2.6f to -1.6f
                TaskTarget.ATM_MACHINE -> 6.4f to 2.0f
                TaskTarget.DOOR_SIGN -> -1.8f to 4.15f
                TaskTarget.TELLER_DESK -> -0.2f to -3.8f
                TaskTarget.CAR_SHOWROOM -> -6.4f to -1.5f
            }
            val dx = target.first - snap.playerX
            val dz = target.second - snap.playerZ
            val dist = sqrt(dx * dx + dz * dz)
            if (dist > 2.2f) {
                val angle = Math.toDegrees(atan2(-dx.toDouble(), -dz.toDouble())).toFloat()
                val steps = 4
                val phase = (t * 2.2f) % 1f
                for (i in 0 until steps) {
                    val frac = ((i + phase) / (steps + 0.5f)).coerceIn(0.15f, 0.88f)
                    val ax = snap.playerX + dx * frac
                    val az = snap.playerZ + dz * frac
                    drawMesh(arrowMesh, ax, 0.025f, az, 0.65f, 1f, 0.65f, angle, 0.15f, 0.72f, 1.0f, emissive = 0.85f, pattern = 0)
                }
            }
        }
    }

    private fun drawTranslucentGlass(snap: WorldRenderSnapshot, t: Float) {
        // Glass sliding doors at z = 4.05, x in [-2.2, 2.2]
        val doorOpenAmt = if (snap.bankOwned && (snap.bankOpen || Math3D.distance2D(snap.playerX, snap.playerZ, 0f, 4.05f) < 3.2f)) 1.55f else 0f
        // Left & Right front glass windows
        drawBox(-4.2f, 1.95f, 4.05f, 3.3f, 3.7f, 0.06f, 0.65f, 0.86f, 0.94f, alpha = 0.30f)
        drawBox(4.2f, 1.95f, 4.05f, 3.3f, 3.7f, 0.06f, 0.65f, 0.86f, 0.94f, alpha = 0.30f)
        // Door frames
        drawBox(-2.45f, 1.95f, 4.05f, 0.14f, 3.8f, 0.14f, 0.10f, 0.10f, 0.12f)
        drawBox(2.45f, 1.95f, 4.05f, 0.14f, 3.8f, 0.14f, 0.10f, 0.10f, 0.12f)
        // Sliding glass panels
        drawBox(-1.15f - doorOpenAmt, 1.90f, 4.05f, 2.2f, 3.6f, 0.05f, 0.68f, 0.88f, 0.96f, alpha = 0.34f)
        drawBox(1.15f + doorOpenAmt, 1.90f, 4.05f, 2.2f, 3.6f, 0.05f, 0.68f, 0.88f, 0.96f, alpha = 0.34f)

        // OPEN / CLOSE door sign on left storefront glass (x = -3.4, y = 1.65, z = 4.12)
        if (snap.bankOpen) {
            drawBox(-3.4f, 1.65f, 4.12f, 1.05f, 0.45f, 0.06f, 0.09f, 0.68f, 0.28f, emissive = 0.45f)
        } else {
            drawBox(-3.4f, 1.65f, 4.12f, 1.05f, 0.45f, 0.06f, 0.82f, 0.16f, 0.16f, emissive = 0.35f)
        }

        // Side Glass Windows
        drawBox(-9.0f, 2.25f, -3.0f, 0.05f, 2.3f, 13.5f, 0.65f, 0.86f, 0.94f, alpha = 0.26f)
        drawBox(9.0f, 2.25f, -3.0f, 0.05f, 2.3f, 13.5f, 0.65f, 0.86f, 0.94f, alpha = 0.26f)
    }

    private fun emitProjectedMarkers(snap: WorldRenderSnapshot, t: Float) {
        val list = ArrayList<ProjectedMarker>(6)
        if (snap.isCutscene && snap.cutscenePhase == 0) {
            list.add(projectPoint("angry_emoji", -1.2f, 2.15f, -2.5f))
        }
        if ((snap.isCutscene && snap.cutscenePhase == 1) || !snap.bankOwned) {
            list.add(projectPoint("owner_debt", -1.8f, 2.10f, 6.2f))
        }
        if (snap.bankOwned && !snap.bankUpgraded) {
            list.add(projectPoint("upgrade_pad", 2.6f, 0.85f, -1.6f))
        }
        if (snap.bankOwned && !snap.carShowroomUnlocked) {
            list.add(projectPoint("car_showroom", -6.4f, 1.05f, -1.5f))
        }
        if (snap.bankOwned) {
            list.add(projectPoint("door_sign", -3.4f, 1.65f, 4.16f))
        }
        if (snap.isSeatedAtDesk) {
            list.add(projectPoint("monitor_screen", -0.68f, 1.44f, -3.55f))
        }
        onMarkersUpdated(list)
    }

    private fun projectPoint(id: String, wx: Float, wy: Float, wz: Float): ProjectedMarker {
        val inVec = floatArrayOf(wx, wy, wz, 1f)
        val outVec = FloatArray(4)
        Matrix.multiplyMV(outVec, 0, vpMatrix, 0, inVec, 0)
        val w = outVec[3]
        if (w <= 0.1f) return ProjectedMarker(id, 0f, 0f, visible = false, distance = 999f)
        val ndcX = outVec[0] / w
        val ndcY = outVec[1] / w
        val normX = (ndcX + 1f) * 0.5f
        val normY = (1f - ndcY) * 0.5f
        val vis = normX in -0.1f..1.1f && normY in -0.1f..1.1f
        return ProjectedMarker(id, normX, normY, vis, w)
    }

    private fun drawBox(
        x: Float, y: Float, z: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float,
        alpha: Float = 1f,
        emissive: Float = 0f,
        pattern: Int = 0
    ) {
        drawMesh(cubeMesh, x, y, z, sx, sy, sz, 0f, r, g, b, alpha, emissive, pattern)
    }

    private fun drawRotatedBox(
        x: Float, y: Float, z: Float,
        sx: Float, sy: Float, sz: Float,
        rotY: Float,
        r: Float, g: Float, b: Float,
        emissive: Float = 0f
    ) {
        drawMesh(cubeMesh, x, y, z, sx, sy, sz, rotY, r, g, b, 1f, emissive, 0)
    }

    private fun drawCylinder(
        x: Float, y: Float, z: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float,
        rotX: Float = 0f,
        rotY: Float = 0f,
        rotZ: Float = 0f,
        emissive: Float = 0f,
        pattern: Int = 0
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        if (rotY != 0f) Matrix.rotateM(modelMatrix, 0, rotY, 0f, 1f, 0f)
        if (rotX != 0f) Matrix.rotateM(modelMatrix, 0, rotX, 1f, 0f, 0f)
        if (rotZ != 0f) Matrix.rotateM(modelMatrix, 0, rotZ, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        renderBoundMesh(cylinderMesh, r, g, b, 1f, emissive, pattern)
    }

    private fun drawSphere(
        x: Float, y: Float, z: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float,
        emissive: Float = 0f
    ) {
        drawMesh(sphereMesh, x, y, z, sx, sy, sz, 0f, r, g, b, 1f, emissive, 0)
    }

    private fun drawMesh(
        mesh: Mesh,
        x: Float, y: Float, z: Float,
        sx: Float, sy: Float, sz: Float,
        rotY: Float,
        r: Float, g: Float, b: Float,
        alpha: Float = 1f,
        emissive: Float = 0f,
        pattern: Int = 0
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        if (rotY != 0f) Matrix.rotateM(modelMatrix, 0, rotY, 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        renderBoundMesh(mesh, r, g, b, alpha, emissive, pattern)
    }

    private fun renderBoundMesh(
        mesh: Mesh,
        r: Float, g: Float, b: Float,
        alpha: Float,
        emissive: Float,
        pattern: Int
    ) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(uMvpLoc, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelLoc, 1, false, modelMatrix, 0)
        GLES20.glUniform4f(uColorLoc, r, g, b, alpha)
        GLES20.glUniform1f(uEmissiveLoc, emissive)
        GLES20.glUniform1i(uPatternLoc, pattern)

        val buf = mesh.vertexBuffer
        buf.position(0)
        GLES20.glEnableVertexAttribArray(aPositionLoc)
        GLES20.glVertexAttribPointer(aPositionLoc, 3, GLES20.GL_FLOAT, false, 24, buf)

        buf.position(3)
        GLES20.glEnableVertexAttribArray(aNormalLoc)
        GLES20.glVertexAttribPointer(aNormalLoc, 3, GLES20.GL_FLOAT, false, 24, buf)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mesh.vertexCount)
    }

    // Renders crisp 3D pixel-grid block letters for "SKY BANK"
    private fun draw3DText(
        text: String,
        centerX: Float, centerY: Float, z: Float,
        letterScale: Float,
        r: Float, g: Float, b: Float,
        emissive: Float
    ) {
        val spacing = letterScale * 4.2f
        val startX = centerX - ((text.length - 1) * spacing) / 2f
        for (idx in text.indices) {
            val ch = text[idx]
            val grid = LETTER_BITMAPS[ch] ?: continue
            val lx = startX + idx * spacing
            for (row in 0 until 5) {
                val bits = grid[row]
                for (col in 0 until 3) {
                    if ((bits and (1 shl (2 - col))) != 0) {
                        val px = lx + (col - 1) * letterScale
                        val py = centerY + (2 - row) * letterScale
                        drawBox(px, py, z, letterScale * 0.96f, letterScale * 0.96f, letterScale * 0.9f, r, g, b, emissive = emissive)
                    }
                }
            }
        }
    }

    private fun createProgram(vertexSrc: String, fragSrc: String): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexSrc)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragSrc)
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vs)
        GLES20.glAttachShader(prog, fs)
        GLES20.glLinkProgram(prog)
        return prog
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e("BankGlRenderer", "Shader compile error: ${GLES20.glGetShaderInfoLog(shader)}")
        }
        return shader
    }

    companion object {
        private val LETTER_BITMAPS = mapOf(
            'S' to intArrayOf(0b111, 0b100, 0b111, 0b001, 0b111),
            'K' to intArrayOf(0b101, 0b101, 0b110, 0b101, 0b101),
            'Y' to intArrayOf(0b101, 0b101, 0b010, 0b010, 0b010),
            'B' to intArrayOf(0b110, 0b101, 0b111, 0b101, 0b110),
            'A' to intArrayOf(0b010, 0b101, 0b111, 0b101, 0b101),
            'N' to intArrayOf(0b101, 0b111, 0b111, 0b101, 0b101)
        )

        private const val VERTEX_SHADER = """
            uniform mat4 uMVPMatrix;
            uniform mat4 uModelMatrix;
            attribute vec4 aPosition;
            attribute vec3 aNormal;
            varying vec3 vNormal;
            varying vec3 vWorldPos;
            void main() {
                vec4 worldPos = uModelMatrix * aPosition;
                vWorldPos = worldPos.xyz;
                vNormal = normalize((uModelMatrix * vec4(aNormal, 0.0)).xyz);
                gl_Position = uMVPMatrix * aPosition;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 uColor;
            uniform vec3 uLightDir;
            uniform float uEmissive;
            uniform int uPattern;
            uniform float uTime;
            varying vec3 vNormal;
            varying vec3 vWorldPos;
            void main() {
                vec3 norm = normalize(vNormal);
                float diff = max(dot(norm, normalize(uLightDir)), 0.0);
                float hemi = 0.5 + 0.5 * norm.y;
                vec3 ambient = mix(vec3(0.32, 0.34, 0.36), vec3(0.55, 0.58, 0.62), hemi);
                vec3 baseCol = uColor.rgb;

                if (uPattern == 1) {
                    // Polished marble checkerboard tiles
                    float tx = floor(vWorldPos.x * 1.1);
                    float tz = floor(vWorldPos.z * 1.1);
                    float chk = mod(tx + tz, 2.0);
                    baseCol = mix(baseCol * 0.90, baseCol * 1.05, chk);
                } else if (uPattern == 2) {
                    // Dhaka brick sidewalk pattern
                    float bx = mod(floor(vWorldPos.x * 2.5) + floor(vWorldPos.z * 5.0), 2.0);
                    baseCol = mix(baseCol * 0.85, baseCol * 1.08, bx);
                } else if (uPattern == 3) {
                    // Subtle emerald plaster texture
                    float n = sin(vWorldPos.x * 3.1) * cos(vWorldPos.y * 2.7);
                    baseCol *= (0.94 + 0.06 * n);
                } else if (uPattern == 4) {
                    // Pulsing cyan floor pad
                    float pulse = 0.65 + 0.35 * sin(uTime * 4.0);
                    baseCol *= pulse;
                }

                vec3 lit = baseCol * (ambient + diff * 0.62) + baseCol * uEmissive;
                gl_FragColor = vec4(clamp(lit, 0.0, 1.0), uColor.a);
            }
        """
    }
}
