package org.ktech.ps4jailbreak

import android.util.Log
import eu.chainfire.libsuperuser.Shell

class USBMounter {

    public fun mount_image(usb: String = "/sys/class/android_usb/android0", file: String, toast: (msg: String, long: Boolean) -> Unit) {
        try {
            var STDOUT: ArrayList<String> = arrayListOf()
            var STDERR: ArrayList<String> = arrayListOf()

            val exitCode = Shell.Pool.SU.run(arrayOf(
                "echo 0 > $usb/enable",
                // Try to append if the function is not already enabled (by ourselves most likely)
                "grep mass_storage $usb/functions > /dev/null || sed -e 's/$/,mass_storage/' $usb/functions | cat > $usb/functions",
                // If empty, set ourselves as the only function
                "[[ -z $(cat $usb/functions) ]] && echo mass_storage > $usb/functions",
                "echo disk > $usb/f_mass_storage/luns",
                "echo USBMountr > $usb/f_mass_storage/inquiry_string",
                "[[ -f $usb/f_mass_storage/luns ]] && echo > $usb/f_mass_storage/lun0/file",
                "[[ -f $usb/f_mass_storage/luns ]] && echo true > $usb/f_mass_storage/lun0/ro",
                "[[ -f $usb/f_mass_storage/luns ]] && echo $file > $usb/f_mass_storage/lun0/file",
                // Older kernels only support a single lun, cope with it
                "[[ ! -f $usb/f_mass_storage/luns ]] && echo > $usb/f_mass_storage/lun/file",
                "[[ ! -f $usb/f_mass_storage/luns ]] && echo true > $usb/f_mass_storage/lun/ro",
                "[[ ! -f $usb/f_mass_storage/luns ]] && echo $file > $usb/f_mass_storage/lun/file",
                "echo 1 > $usb/enable",
                "echo success"
            ), STDOUT, STDERR, true)

        } catch (e: Shell.ShellDiedException) {
            Log.e("ERROR", "Device not rooted or SU missing.")
            toast("Device not rooted or SU missing.", true)
        }
    }

    public fun unmount_image(usb: String = "/sys/class/android_usb/android0", toast: (msg: String, long: Boolean) -> Unit) {
        try {
            var STDOUT: ArrayList<String> = arrayListOf()
            var STDERR: ArrayList<String> = arrayListOf()

            val exitCode = Shell.Pool.SU.run(arrayOf(
                "echo 0 > $usb/enable",
                "sed -e 's/mass_storage//' $usb/functions | cat > $usb/functions",
                "echo 1 > $usb/enable"
            ), STDOUT, STDERR, true)

        } catch (e: Shell.ShellDiedException) {
            Log.e("ERROR", "Device not rooted or SU missing.")
            toast("Device not rooted or SU missing.", true)
        }
    }

}

/*

inner class UsbScript : AsyncTask<String, Void, Int>() {
        override fun doInBackground(vararg params: String): Int {
            val usb = "/sys/class/android_usb/android0"
            val file = params[0]
            val ro = params[1]
            val enable = params[2]

            if (!(Shell.SU.run(arrayOf(
                    "echo 0 > $usb/enable",
                    // Try to append if the function is not already enabled (by ourselves most likely)
                    "grep mass_storage $usb/functions > /dev/null || sed -e 's/$/,mass_storage/' $usb/functions | cat > $usb/functions",
                    // If empty, set ourselves as the only function
                    "[[ -z $(cat $usb/functions) ]] && echo mass_storage > $usb/functions",
                    // Disable the feature if told to
                    "[[ 0 == $enable ]] && sed -e 's/mass_storage//' $usb/functions | cat > $usb/functions",
                    "echo disk > $usb/f_mass_storage/luns",
                    "echo USBMountr > $usb/f_mass_storage/inquiry_string",
                    "echo 1 > $usb/enable",
                    "[[ -f $usb/f_mass_storage/luns ]] && echo > $usb/f_mass_storage/lun0/file",
                    "[[ -f $usb/f_mass_storage/luns ]] && echo $ro > $usb/f_mass_storage/lun0/ro",
                    "[[ -f $usb/f_mass_storage/luns ]] && echo $file > $usb/f_mass_storage/lun0/file",
                    // Older kernels only support a single lun, cope with it
                    "[[ ! -f $usb/f_mass_storage/luns ]] && echo > $usb/f_mass_storage/lun/file",
                    "[[ ! -f $usb/f_mass_storage/luns ]] && echo $ro > $usb/f_mass_storage/lun/ro",
                    "[[ ! -f $usb/f_mass_storage/luns ]] && echo $file > $usb/f_mass_storage/lun/file",
                    "echo success"
            ))?.isEmpty() ?: true)) {
                if (enable != "0") {
                    return R.string.host_success
                } else {
                    return R.string.host_disable_success
                }
            } else {
                return R.string.host_noroot
            }
        }

 */