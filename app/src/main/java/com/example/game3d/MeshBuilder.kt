package com.example.game3d

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class Mesh(
    val vertexBuffer: FloatBuffer,
    val vertexCount: Int
)

object MeshBuilder {
    // Each vertex: x, y, z, nx, ny, nz (6 floats)
    private fun floatsToBuffer(data: FloatArray): Mesh {
        val bb = ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
        val fb = bb.asFloatBuffer()
        fb.put(data)
        fb.position(0)
        return Mesh(fb, data.size / 6)
    }

    fun createUnitCube(): Mesh {
        val s = 0.5f
        val vertices = floatArrayOf(
            // Front (+Z)
            -s, -s,  s,  0f,  0f,  1f,
             s, -s,  s,  0f,  0f,  1f,
             s,  s,  s,  0f,  0f,  1f,
            -s, -s,  s,  0f,  0f,  1f,
             s,  s,  s,  0f,  0f,  1f,
            -s,  s,  s,  0f,  0f,  1f,
            // Back (-Z)
             s, -s, -s,  0f,  0f, -1f,
            -s, -s, -s,  0f,  0f, -1f,
            -s,  s, -s,  0f,  0f, -1f,
             s, -s, -s,  0f,  0f, -1f,
            -s,  s, -s,  0f,  0f, -1f,
             s,  s, -s,  0f,  0f, -1f,
            // Left (-X)
            -s, -s, -s, -1f,  0f,  0f,
            -s, -s,  s, -1f,  0f,  0f,
            -s,  s,  s, -1f,  0f,  0f,
            -s, -s, -s, -1f,  0f,  0f,
            -s,  s,  s, -1f,  0f,  0f,
            -s,  s, -s, -1f,  0f,  0f,
            // Right (+X)
             s, -s,  s,  1f,  0f,  0f,
             s, -s, -s,  1f,  0f,  0f,
             s,  s, -s,  1f,  0f,  0f,
             s, -s,  s,  1f,  0f,  0f,
             s,  s, -s,  1f,  0f,  0f,
             s,  s,  s,  1f,  0f,  0f,
            // Top (+Y)
            -s,  s,  s,  0f,  1f,  0f,
             s,  s,  s,  0f,  1f,  0f,
             s,  s, -s,  0f,  1f,  0f,
            -s,  s,  s,  0f,  1f,  0f,
             s,  s, -s,  0f,  1f,  0f,
            -s,  s, -s,  0f,  1f,  0f,
            // Bottom (-Y)
            -s, -s, -s,  0f, -1f,  0f,
             s, -s, -s,  0f, -1f,  0f,
             s, -s,  s,  0f, -1f,  0f,
            -s, -s, -s,  0f, -1f,  0f,
             s, -s,  s,  0f, -1f,  0f,
            -s, -s,  s,  0f, -1f,  0f
        )
        return floatsToBuffer(vertices)
    }

    fun createCylinder(segments: Int = 18): Mesh {
        val list = ArrayList<Float>(segments * 12 * 6)
        val r = 0.5f
        val h = 0.5f
        for (i in 0 until segments) {
            val a0 = (2.0 * PI * i / segments).toFloat()
            val a1 = (2.0 * PI * (i + 1) / segments).toFloat()
            val x0 = r * cos(a0)
            val z0 = r * sin(a0)
            val x1 = r * cos(a1)
            val z1 = r * sin(a1)
            val nx0 = cos(a0)
            val nz0 = sin(a0)
            val nx1 = cos(a1)
            val nz1 = sin(a1)

            // Side quad (2 triangles)
            addVert(list, x0, -h, z0, nx0, 0f, nz0)
            addVert(list, x1, -h, z1, nx1, 0f, nz1)
            addVert(list, x1,  h, z1, nx1, 0f, nz1)

            addVert(list, x0, -h, z0, nx0, 0f, nz0)
            addVert(list, x1,  h, z1, nx1, 0f, nz1)
            addVert(list, x0,  h, z0, nx0, 0f, nz0)

            // Top cap
            addVert(list, 0f, h, 0f, 0f, 1f, 0f)
            addVert(list, x1, h, z1, 0f, 1f, 0f)
            addVert(list, x0, h, z0, 0f, 1f, 0f)

            // Bottom cap
            addVert(list, 0f, -h, 0f, 0f, -1f, 0f)
            addVert(list, x0, -h, z0, 0f, -1f, 0f)
            addVert(list, x1, -h, z1, 0f, -1f, 0f)
        }
        return floatsToBuffer(list.toFloatArray())
    }

    fun createSphere(stacks: Int = 10, slices: Int = 16): Mesh {
        val list = ArrayList<Float>(stacks * slices * 6 * 6)
        val r = 0.5f
        for (i in 0 until stacks) {
            val phi0 = (PI * (-0.5 + i.toDouble() / stacks)).toFloat()
            val phi1 = (PI * (-0.5 + (i + 1).toDouble() / stacks)).toFloat()
            for (j in 0 until slices) {
                val th0 = (2.0 * PI * j / slices).toFloat()
                val th1 = (2.0 * PI * (j + 1) / slices).toFloat()

                val p00 = spherePoint(r, phi0, th0)
                val p10 = spherePoint(r, phi1, th0)
                val p11 = spherePoint(r, phi1, th1)
                val p01 = spherePoint(r, phi0, th1)

                addVert(list, p00.x, p00.y, p00.z, p00.x * 2f, p00.y * 2f, p00.z * 2f)
                addVert(list, p10.x, p10.y, p10.z, p10.x * 2f, p10.y * 2f, p10.z * 2f)
                addVert(list, p11.x, p11.y, p11.z, p11.x * 2f, p11.y * 2f, p11.z * 2f)

                addVert(list, p00.x, p00.y, p00.z, p00.x * 2f, p00.y * 2f, p00.z * 2f)
                addVert(list, p11.x, p11.y, p11.z, p11.x * 2f, p11.y * 2f, p11.z * 2f)
                addVert(list, p01.x, p01.y, p01.z, p01.x * 2f, p01.y * 2f, p01.z * 2f)
            }
        }
        return floatsToBuffer(list.toFloatArray())
    }

    fun createChevronArrow(): Mesh {
        // Flat horizontal arrow pointing toward -Z
        val list = ArrayList<Float>(12 * 6)
        val y = 0.02f
        // Triangle head
        addVert(list, 0f, y, -0.45f, 0f, 1f, 0f)
        addVert(list, -0.35f, y, 0.05f, 0f, 1f, 0f)
        addVert(list, 0.35f, y, 0.05f, 0f, 1f, 0f)
        // Stem quad
        addVert(list, -0.14f, y, 0.05f, 0f, 1f, 0f)
        addVert(list, -0.14f, y, 0.40f, 0f, 1f, 0f)
        addVert(list, 0.14f, y, 0.40f, 0f, 1f, 0f)

        addVert(list, -0.14f, y, 0.05f, 0f, 1f, 0f)
        addVert(list, 0.14f, y, 0.40f, 0f, 1f, 0f)
        addVert(list, 0.14f, y, 0.05f, 0f, 1f, 0f)
        return floatsToBuffer(list.toFloatArray())
    }

    private fun spherePoint(r: Float, phi: Float, theta: Float): Vec3 {
        val cp = cos(phi)
        return Vec3(r * cp * cos(theta), r * sin(phi), r * cp * sin(theta))
    }

    private fun addVert(list: MutableList<Float>, x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float) {
        list.add(x); list.add(y); list.add(z)
        list.add(nx); list.add(ny); list.add(nz)
    }
}
