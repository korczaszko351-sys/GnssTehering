package com.example.gnsstethering

import java.io.BufferedOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class GnssTcpBridge(private val listenPort: Int, private val remoteHost: String, private val remotePort: Int) {
    private val running = AtomicBoolean(false)
    private val queue = ConcurrentLinkedQueue<ByteArray>()

    fun start() {
        if (!running.compareAndSet(false, true)) return
        thread(name = "GnssHotspotServer") {
            ServerSocket(listenPort).use { server ->
                while (running.get()) { handleClient(server.accept()) }
            }
        }
        thread(name = "LteUploader") { uploadLoop() }
    }

    fun stop() { running.set(false) }
    fun bufferedPackets(): Int = queue.size

    private fun handleClient(client: Socket) = thread {
        client.use { socket ->
            val input = socket.getInputStream()
            val buffer = ByteArray(16384)
            while (running.get()) {
                val n = input.read(buffer)
                if (n <= 0) break
                queue.add(buffer.copyOf(n))
            }
        }
    }

    private fun uploadLoop() {
        while (running.get()) {
            val batch = ArrayList<ByteArray>()
            while (batch.size < 128) { queue.poll()?.let { batch.add(it) } ?: break }
            if (batch.isEmpty()) { Thread.sleep(100); continue }
            try {
                Socket(remoteHost, remotePort).use { uplink ->
                    val out = BufferedOutputStream(uplink.getOutputStream())
                    batch.forEach { out.write(it) }
                    out.flush()
                }
            } catch (e: Exception) {
                batch.forEach { queue.add(it) }
                Thread.sleep(2000)
            }
        }
    }
}
