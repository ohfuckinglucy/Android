package com.example.visprog

import android.content.Context
import android.util.Log
import org.zeromq.ZMQ
import java.io.File

fun sendAllData(context: Context, address: String, onComplete: (Boolean) -> Unit) {
    Thread {
        var ok = false
        try {
            val file = File(context.filesDir, "pending.jsonl")
            if (!file.exists() || file.length() == 0L) {
                onComplete(true)
                return@Thread
            }

            val lines = file.readLines()
            if (lines.isEmpty()) {
                onComplete(true)
                return@Thread
            }

            val ctx = ZMQ.context(1)
            val socket = ctx.socket(ZMQ.REQ)
            socket.connect("tcp://$address")
            socket.setSendTimeOut(3000)
            socket.setReceiveTimeOut(3000)

            for (line in lines) {
                socket.send(line, 0)
                val reply = socket.recv(0) ?: throw Exception("No reply")
            }

            socket.close()
            ctx.close()
            ok = true
        } catch (e: Exception) {
            Log.e("DataSender", "Send failed", e)
            ok = false
        } finally {
            onComplete(ok)
        }
    }.start()
}

fun clearPendingData(context: Context) {
    val f = File(context.filesDir, "pending.jsonl")
    if (f.exists()) f.delete()
}
