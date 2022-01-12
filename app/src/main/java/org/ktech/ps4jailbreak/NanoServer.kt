package org.ktech.ps4jailbreak

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.net.Socket

class NanoServer(current: Context): NanoHTTPD(8080) {
    var context: Context = current

    override fun serve(session: IHTTPSession?): Response {
        Log.d("NANO", session?.uri.toString())
        val clientIP = session?.headers?.get("http-client-ip").toString()

        when (session?.uri.toString()) {
            "/" -> {
                return NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, "text/html", getResourceAsText("index.html"))
            }
            "/log/done" -> {
                val outSock = Socket(clientIP, 9020)
                val outStream = outSock.getOutputStream()
                outStream.write(getResourceAsBytes("payload/goldhen_2.0b2_900.bin"))
                outStream.flush()
                outStream.close()
            }
            else -> {
                val path = session?.uri.toString().drop(1)
                if ( session?.uri.toString().endsWith(".html") ) {
                    return NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, "text/html", getResourceAsText("index.html"))
                } else if ( session?.uri.toString().endsWith(".js") ) {
                    return NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, "text/javascript ", getResourceAsText(path))
                }
            }
        }
        return super.serve(session)
    }

    private fun getResourceAsText(path: String): String {
        return context.assets.open(path).reader().readText()
    }

    private fun getResourceAsBytes(path: String): ByteArray {
        return context.assets.open(path).readBytes()
    }

}