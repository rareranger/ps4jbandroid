package org.ktech.ps4jailbreak

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter.formatIpAddress
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(){

    //HTTP server
    lateinit var server: NanoServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
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

        //Setup event for when the start button is clicked
        findViewById<Button>(R.id.btnStartServer).setOnClickListener {
            //Start the NanoHTTPD server
            server.start()
            //Get WifiManager Service so we can retrieve the IP Address
            val wm: WifiManager = getApplicationContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
            //Convert IP address to readable string
            val serverIP = formatIpAddress(wm.connectionInfo.ipAddress)
            //Update the TextView above the Start button with below text
            findViewById<TextView>(R.id.txtVWStatus).text = "Visit \"http://$serverIP:8080/\" in PS4 browser"
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