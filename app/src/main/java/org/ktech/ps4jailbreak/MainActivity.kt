package org.ktech.ps4jailbreak

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.format.Formatter.formatIpAddress
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.Socket
import java.util.*
import kotlin.collections.ArrayList
import kotlin.system.exitProcess

const val PICK_BIN_FILE = 2

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    //HTTP server
    lateinit var server: NanoServer
    //WifiManager
    lateinit var wifiManager: WifiManager

    lateinit var sharedPreferences:SharedPreferences

    lateinit var payloads: MutableSet<String>

    lateinit var items: ArrayList<String>

    var customPayload: Uri? = null

    val ADD_CUSTOM_PAYLOAD_STRING: String = "Add custom payload..."

    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            updatePreferences()
            showToast("Closing app.", true)
            exitProcess(0)
        }

        sharedPreferences = getSharedPreferences("main", Context.MODE_PRIVATE)

        payloads = HashSet<String>(sharedPreferences.getStringSet("payloads", HashSet<String>()))

        updateSpinnerItems()

        findViewById<Spinner>(R.id.spnrPayload).onItemSelectedListener = this
        findViewById<Spinner>(R.id.spnrManualPayload).onItemSelectedListener = this

        //Initialize the NanoHTTPD server custom class passing the context so we can access resources in the assets folder
        server = NanoServer(this)
        server.lastPS4 = sharedPreferences.getString("lastPS4", null)

        if (server.lastPS4 != null) {
            findViewById<TextView>(R.id.txtVwPS4IP).text = "PS4 Last IP Address: ${server.lastPS4}"
        }

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
                showToast("Server is already running...", true)
            }

            //Convert IP address to readable string
            val serverIP = formatIpAddress(wifiManager.connectionInfo.ipAddress)
            //Update the TextView above the Start button with below text
            findViewById<TextView>(R.id.txtVWStatus).text = "Visit \"http://$serverIP:8080/\" in PS4 browser"
        }

        findViewById<Button>(R.id.btnSendPayload).setOnClickListener {
            if (customPayload != null) {
                sendPayload(customPayload!!)
            } else {
                showToast("Add and choose a payload to send!")
            }
        }
    }

    private fun updateSpinnerItems() {
        items = ArrayList()
        val items2: ArrayList<String> = ArrayList()

        items.add("GoldHen 2.0")
        for (pl in payloads) {
            val uri = Uri.parse(pl)
            val fname = uri.path.toString().substringAfterLast("/")
            items.add(fname)
            items2.add(fname)
        }

        items.add(ADD_CUSTOM_PAYLOAD_STRING)
        items2.add(ADD_CUSTOM_PAYLOAD_STRING)
        //Create adapter from items list
        val spinnerAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items)
        val spinnerAdapter2 = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items2)
        //Use created adapter on the spinner
        findViewById<Spinner>(R.id.spnrPayload).adapter = spinnerAdapter
        findViewById<Spinner>(R.id.spnrManualPayload).adapter = spinnerAdapter2
    }

    fun openFile(pickerInitialUri: Uri) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"

            // Optionally, specify a URI for the file that should appear in the
            // system file picker when it loads.
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        }

        startActivityForResult(intent, PICK_BIN_FILE)
    }

    override fun onActivityResult(
            requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == PICK_BIN_FILE
                && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            resultData?.data?.also { uri ->
                // Perform operations on the document using its URI.
                if (uri.path?.endsWith(".bin") == true) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                        contentResolver.takePersistableUriPermission(uri, takeFlags)
                    }
                    payloads.add(uri.toString())
                    updateSpinnerItems()
                }
            }
        }
    }

    private fun showToast(msg: String, long: Boolean = false) {
        if (long) {
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
        val item = parent.getItemAtPosition(pos).toString()

        //Log.d("LOG", parent.toString())

        if (item == ADD_CUSTOM_PAYLOAD_STRING) {
            openFile(Uri.parse("/"))
        } else if (parent == findViewById<Spinner>(R.id.spnrPayload)) {
            if (pos == 0) {
                server.load_payload("", null)
            } else {
                val uri = Uri.parse(payloads.elementAtOrNull(pos - 1).toString())
                val fname = uri.path.toString().substringAfterLast("/")
                val stream = contentResolver.openInputStream(uri)
                log(fname)
                server.load_payload(fname, stream!!)
            }
        } else {
            customPayload = Uri.parse(payloads.elementAtOrNull(pos).toString())
        }
    }

    private fun sendPayload(payloadUri: Uri) {

        if (server.lastPS4 == null) {
            showToast("NO PS4 Client in memory")
            log("!!!")
            log("NO PS4 Client in memory")
            log("!!!")
            log("Please load the exploit using this app at least once so the PS4 IP Address can be saved.")
            log("!!!")
            return
        }

        GlobalScope.launch {
            val fname = payloadUri.path.toString().substringAfterLast("/")
            log("Sending $fname payload to PS4 with IP ${server.lastPS4} on port 9090...")
            var outSock: Socket? = null
            try {
                outSock = Socket(server.lastPS4, 9090)
            } catch (e: java.net.ConnectException) {
                if (e.message?.contains("ECONNREFUSED") == true) {
                    log("Failed to connect to port 9090 on PS4. Make sure you have binloader enabled in GoldHen settings.")
                } else if (e.message?.contains("ENETUNREACH") == true) {
                    log("Failed to connect to PS4. Make sure WiFi is enabled and you are on the same WiFi network as the PS4.")
                } else {
                    Log.e("NET", e.message.toString())
                }
                return@launch
            } catch (e: java.net.NoRouteToHostException) {
                log("Last stored IP address is invalid or the PS4 is not connected to WiFi")
                log("To refresh IP address load the exploit again from the app and connect to it on the PS4")
                return@launch
            }
            val outStream = outSock.getOutputStream()
            val stream = contentResolver.openInputStream(payloadUri)
            outStream.write(stream?.readBytes())
            outStream.flush()
            outStream.close()
            outSock.close()
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        // Another interface callback
    }

    private fun log(message: String) {
        runOnUiThread{
            val logTextView = findViewById<TextView>(R.id.txtVwLog)
            logTextView.append(message)
            logTextView.append("\n")

            if (server.lastPS4 != null) {
                findViewById<TextView>(R.id.txtVwPS4IP).text = "PS4 Last IP Address: ${server.lastPS4}"
            }
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

    private  fun updatePreferences() {
        val editor = sharedPreferences.edit()

        if (server.lastPS4 != null) {
            editor.putString("lastPS4", server.lastPS4)
        }

        editor.putStringSet("payloads", payloads)
        editor.commit()
    }

    override fun onPause() {
        super.onPause()
        updatePreferences()
    }

    override fun onStop() {
        super.onStop()
        updatePreferences()
    }

    override fun onDestroy() {
        super.onDestroy()
        updatePreferences()
    }
}