package org.ktech.ps4jailbreak

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter.formatIpAddress
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity(){

    //HTTP server
    lateinit var server: NanoServer
    //WifiManager
    lateinit var wifiManager: WifiManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            Toast.makeText(this, "Closing app.", Toast.LENGTH_LONG).show()
            exitProcess(0)
        }

        //Create an array list of items to be displayed in the spinner underneath the start button
        val items: ArrayList<String> = ArrayList()
        //Add items to the list
        //TODO: Allow user to add custom payloads
        items.add("GoldHen 2.0")

        //Create adapter from items list
        val spinnerAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items)
        //Use created adapter on the spinner
        findViewById<Spinner>(R.id.spnrPayload).adapter = spinnerAdapter

        //Initialize the NanoHTTPD server custom class passing the context so we can access resources in the assets folder
        server = NanoServer(this)

        log("Logging enabled...")

        server.onLogMessage = {
            log(it)
        }

        //Get WifiManager Service so we can retrieve our WiFi IP Address
        wifiManager = getApplicationContext().getSystemService(Context.WIFI_SERVICE) as WifiManager

        //Setup event for when the start button is clicked
        findViewById<Button>(R.id.btnStartServer).setOnClickListener {
            //Only start NanoHTTPD server if its not already running
            if (!server.isAlive) {
                log("Starting server...")
                server.start()
            } else {
                log("Server is already running...")
                Toast.makeText(this, "Server is already running...", Toast.LENGTH_LONG).show()
            }

            //Convert IP address to readable string
            val serverIP = formatIpAddress(wifiManager.connectionInfo.ipAddress)
            //Update the TextView above the Start button with below text
            findViewById<TextView>(R.id.txtVWStatus).text = "Visit \"http://$serverIP:8080/\" in PS4 browser"
        }
    }

    private fun log(message: String) {
        runOnUiThread{
            val logTextView = findViewById<TextView>(R.id.txtVwLog)
            logTextView.append(message)
            logTextView.append("\n")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}