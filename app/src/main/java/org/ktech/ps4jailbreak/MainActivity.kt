package org.ktech.ps4jailbreak

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.format.Formatter.formatIpAddress
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.lang.Exception
import java.net.Socket
import java.util.*
import kotlin.collections.ArrayList
import kotlin.system.exitProcess

const val PICK_BIN_FILE = 2
const val IMG_FILENAME = "exfathax.img"

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    //HTTP server
    lateinit var server: NanoServer
    //WifiManager
    lateinit var wifiManager: WifiManager
    //Share Preferences
    lateinit var sharedPreferences:SharedPreferences
    //List of payloads
    lateinit var payloads: MutableSet<String>
    //List of payload names for spinner
    lateinit var items: ArrayList<String>
    //Content URI of selected payload
    var customPayload: Uri? = null
    //String for adding a new payload
    val ADD_CUSTOM_PAYLOAD_STRING: String = "Add custom payload..."
    //Permission stuff for persistent URI to custom payload files
    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    //Listener for changing preferences
    lateinit var listener: SharedPreferences.OnSharedPreferenceChangeListener

    lateinit var usbMounter: USBMounter

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        //Close app when floating exit button is clicked
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            updatePreferences()
            showToast("Closing app.", true)
            exitProcess(0)
        }

        //Get app preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        //Store listener so that change listener is always triggered on changes
        listener = SharedPreferences.OnSharedPreferenceChangeListener() { sharedPreferences: SharedPreferences, s: String ->
            if (s == "lastPS4") {
                server.lastPS4 = sharedPreferences.getString(s, null)
                findViewById<TextView>(R.id.txtVwPS4IP).text = "PS4 Last IP Address: ${server.lastPS4}"
            }
        }
        //Register listener
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        //Get payloads already store in app preferences
        payloads = HashSet<String>(sharedPreferences.getStringSet("payloads", HashSet<String>()))
        //Update the spinner with values
        updateSpinnerItems()

        //Enable scrolling in log text box
        findViewById<TextView>(R.id.txtVwLog).movementMethod = ScrollingMovementMethod()

        //Setup event for when an item in the spinner is clicked
        findViewById<Spinner>(R.id.spnrManualPayload).onItemSelectedListener = this

        //Initialize the NanoHTTPD server custom class passing the context so we can access resources in the assets folder
        server = NanoServer(this)

        //Get the last IP address of the PS4 stored in the app preferences
        // or null if there is none
        server.lastPS4 = sharedPreferences.getString("lastPS4", null)

        //If an address is stored then update the IP address info text box
        updateUIPS4IPAddress()

        //Notify that logging is enabled
        log("Logging enabled...")

        //Setup event for when log messages is received from server (NanoServer) class
        server.onLogMessage = {
            log(it)
        }

        //Setup event for when a new PS4 ip address is received from server (NanoServer) class
        server.onLastPS4Changed = {
            updatePreferences()
            findViewById<TextView>(R.id.txtVwPS4IP).text = "PS4 Last IP Address: ${server.lastPS4}"
        }

        //Get WifiManager Service so we can retrieve our WiFi IP Address
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

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

        //Setup event for when the send payload button is clicked
        findViewById<Button>(R.id.btnSendPayload).setOnClickListener {
            if (customPayload != null) {
                sendPayload(customPayload!!)
            } else {
                showToast("Add and choose a payload to send!")
            }
        }

        //Initialize USBMounter class
        usbMounter = USBMounter()
        //Log on event
        usbMounter.onLogMessage = {
            log(it)
        }

        //Setup event for when mount button is clicked
        findViewById<Button>(R.id.btnMountUSB).setOnClickListener {
            val imgFileName: String = getFileFromAssets(this, IMG_FILENAME).absolutePath
            GlobalScope.launch {
                //Attempt to mount image
                usbMounter.mountImage(imageFile = imgFileName, toast = this@MainActivity::showToast)
            }
        }

        findViewById<Button>(R.id.btnUnmountUSB).setOnClickListener {
            GlobalScope.launch {
                //Attempt to unmount image
                usbMounter.unmount_image(toast = this@MainActivity::showToast)
            }
        }

    }

    fun getFileFromAssets(context: Context, fileName: String): File = File(context.cacheDir, fileName)
        .also {
            if (!it.exists()) {
                it.outputStream().use { cache ->
                    context.assets.open(fileName).use { inputStream ->
                        inputStream.copyTo(cache)
                        inputStream.close()
                    }
                }
            }
        }

    //Update spinner items
    private fun updateSpinnerItems() {
        items = ArrayList()

        //If no payloads available in memory yet the add this item
        if (payloads.size == 0) {
            items.add("No payloads added")
        }

        //Get filename of each payload and add to items list
        for (pl in payloads) {
            val uri = Uri.parse(pl)
            val fname = uri.path.toString().substringAfterLast("/")
            items.add(fname)
        }

        //Add option to add payload from storage
        items.add(ADD_CUSTOM_PAYLOAD_STRING)
        //Create adapter from items list
        val spinnerAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items)
        //Use created adapter on the spinner
        findViewById<Spinner>(R.id.spnrManualPayload).adapter = spinnerAdapter
    }

    //Opens file picker to choose a payload .bin file
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

    //Custom function for showing toast message
    public fun showToast(msg: String, long: Boolean = false) {
        runOnUiThread {
            if (long) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    //Custom function for logging to text box
    private fun log(message: String) {
        //Fix for logging from other threads
        runOnUiThread{
            val logTextView = findViewById<TextView>(R.id.txtVwLog)
            logTextView.append(message)
            logTextView.append("\n")

            updateUIPS4IPAddress()
        }
    }

    //Custom function to update ui ip address
    private fun updateUIPS4IPAddress() {
        if (server.lastPS4 != null) {
            findViewById<TextView>(R.id.txtVwPS4IP).text = "PS4 Last IP Address: ${server.lastPS4}"
        }
    }

    //Custom function to send a payload to the PS4
    private fun sendPayload(payloadUri: Uri) {
        if (server.lastPS4 == null) {
            log("NO PS4 Client in memory")
            log("!!!")
            showToast("Go to Settings and set PS4 IP address.", true)
            return
        }

        GlobalScope.launch {
            //Get payload filename
            val fname = payloadUri.path.toString().substringAfterLast("/")
            log("Sending $fname payload to PS4 with IP ${server.lastPS4} on port 9090...")
            var outSock: Socket? = null
            //Attempt to connect to PS4 and deal with errors
            try {
                outSock = Socket(server.lastPS4, 9090)
            } catch (e: java.net.ConnectException) {
                if (e.message?.contains("ECONNREFUSED") == true) {
                    val msg = "Failed to connect to port 9090 on PS4. Make sure you have binloader enabled in GoldHen settings."
                    log(msg)
                    showToast(msg,true)
                } else if (e.message?.contains("ENETUNREACH") == true) {
                    val msg = "Failed to connect to PS4. Make sure WiFi is enabled and you are on the same WiFi network as the PS4."
                    log(msg)
                    showToast(msg, true)
                } else {
                    Log.e("NET", e.message.toString())
                }
                return@launch
            } catch (e: java.net.NoRouteToHostException) {
                log("Last stored IP address is invalid or the PS4 is not connected to WiFi")
                log("Go to Settings and set PS4 IP address.")
                showToast("Error. Go to Settings to set PS4 IP address.", true)
                return@launch
            }
            //Setup stream to PS4 and send payload the close connection
            var stream: InputStream? = null
            try {
                stream = contentResolver.openInputStream(payloadUri)
            } catch (e: java.io.FileNotFoundException) {
                val msg = "ERROR. Cannot find the payload $fname from internal storage. " +
                        "Try to add it again."
                showToast(msg,true)
                log(msg)
                log("!!!!!Disable and re-enable binloader to send payload again!!!!!")
                payloads.remove(payloadUri.toString())
                runOnUiThread {
                    updateSpinnerItems()
                }
                outSock.close()
                return@launch
            } catch (e: Exception) {
                Log.e("ERROR", e.message.toString())
                log(e.message.toString())
                log("!!!!!Disable and re-enable binloader to send payload again!!!!!")
                outSock.close()
                return@launch
            }
            val outStream = outSock.getOutputStream()
            outStream.write(stream?.readBytes())
            outStream.flush()
            outStream.close()
            outSock.close()
        }
    }

    //Custom function for updating preferences when modified from outside settings page
    private  fun updatePreferences() {
        val editor = sharedPreferences.edit()

        if (server.lastPS4 != null) {
            editor.putString("lastPS4", server.lastPS4)
        }
        //Store payloads in app preferences
        editor.putStringSet("payloads", payloads)
        editor.commit()
    }

    //Respond to file being picked from file picker
    override fun onActivityResult(
            requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == PICK_BIN_FILE
                && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri ->
                //Check if the file ends with .bin or do nothing
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

    //On spinner item selected
    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        //Get selected item
        val item = parent.getItemAtPosition(pos).toString()

        //If user chose add custom payload then open file picker
        if (item == ADD_CUSTOM_PAYLOAD_STRING) {
            openFile(Uri.parse("/"))
            findViewById<Spinner>(R.id.spnrManualPayload).setSelection(0)
        } else {
            //else get the URI of the chosen payload
            customPayload = Uri.parse(payloads.elementAtOrNull(pos).toString())
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        // Another interface callback
    }

    //Inflate actionbar menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    //Handle options clicked in actionbar menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            //Open settings page when settings is clicked
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                return true
            }
            else -> { super.onOptionsItemSelected(item) }
        }
    }

    //Update preferences on event
    override fun onPause() {
        super.onPause()
        updatePreferences()
    }
    //Update preferences on event
    override fun onStop() {
        super.onStop()
        updatePreferences()
    }
    //Update preferences on event
    override fun onDestroy() {
        super.onDestroy()
        updatePreferences()
    }
}