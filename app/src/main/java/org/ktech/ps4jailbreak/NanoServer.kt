package org.ktech.ps4jailbreak

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.net.Socket

//Initialize NanoHTTPD server on port 8080
//TODO: Allow user to configure custom port to run the server on
class NanoServer(current: Context): NanoHTTPD(8080) {
    //Store context so we can access assets from main activity
    var context: Context = current

    //Handle connection from (hopefully) PS4
    //TODO: Check if device is a PS4 and if it is running Firmware 9.0
    override fun serve(session: IHTTPSession?): Response {
        //Retrieve client IP address from the request headers. Seems to be the only way possible with
        // NanoHTTPD
        val clientIP: String = session?.headers?.get("http-client-ip").toString()

        //React to request uri path
        when (session?.uri.toString()) {
            //Return index.html to client/PS4
            "/" -> {
                return NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, "text/html", getResourceAsText("index.html"))
            }
            /* When /log/done is received from the exploit script, send the payload to the PS4
                log/done is sent from 2 places:
                    index.html:62 which I added for when the exploit is already running and the page is refreshed
                    and kexploit.js:640
             */
            "/log/done" -> {
                //Setup a connection to the PS4 to send the payload on port 9020
                val outSock = Socket(clientIP, 9020)
                val outStream = outSock.getOutputStream()
                //Send the payload from the assets folder to the PS4 then close the connection
                //TODO: Allow user to send other payloads
                outStream.write(getResourceAsBytes("payload/goldhen_2.0b2_900.bin"))
                outStream.flush()
                outStream.close()
                outSock.close()
            }
            else -> {
                //This is a hack to serve all the static files in the assets folder
                val path = session?.uri.toString().drop(1)
                //if the request path ends with .html then always return the index.html file
                if ( session?.uri.toString().endsWith(".html") ) {
                    return NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, "text/html", getResourceAsText("index.html"))
                //Else if the request path ends with .js then return the javascript files with the correct mime type
                } else if ( session?.uri.toString().endsWith(".js") ) {
                    return NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, "text/javascript ", getResourceAsText(path))
                }
            }
        }
        return super.serve(session)
    }

    //get the string contents of a resource from the assets folder using its path
    private fun getResourceAsText(path: String): String {
        return context.assets.open(path).reader().readText()
    }

    //get the bytes contents of a resource from the assets folder using its path
    private fun getResourceAsBytes(path: String): ByteArray {
        return context.assets.open(path).readBytes()
    }

}