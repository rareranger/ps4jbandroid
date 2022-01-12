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

    lateinit var server: NanoServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        val items: ArrayList<String> = ArrayList()
        items.add("GoldHen 2.0")
        val x = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items)

        findViewById<Spinner>(R.id.spnrPayload).adapter = x

        server = NanoServer(this)

        val set = findViewById<Button>(R.id.btnStartServer).setOnClickListener {
            server.start()
            val wm: WifiManager = getApplicationContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ip = formatIpAddress(wm.connectionInfo.ipAddress)
            findViewById<TextView>(R.id.txtVWStatus).text = "Visit \"http://$ip:8080/\" in PS4 browser"
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