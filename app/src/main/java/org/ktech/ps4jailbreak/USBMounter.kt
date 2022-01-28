package org.ktech.ps4jailbreak

import android.util.Log
import eu.chainfire.libsuperuser.Shell

class USBMounter {

    enum class BACKEND_TYPE {
        LUNS, LUN, CONFIGFS, NA
    }

    var backendType: BACKEND_TYPE? = null

    //Create event to send log messages
    var onLogMessage: ((String) -> Unit) = {}

    private fun gettBackendType(): BACKEND_TYPE {
        var exitCode: Int = 0

        exitCode = Shell.Pool.SU.run("stat /sys/class/android_usb/android0/f_mass_storage/luns")
        if (exitCode == 0) {
            onLogMessage("Backend Type Found :: LUNS")
            return BACKEND_TYPE.LUNS
        }

        exitCode = Shell.Pool.SU.run("stat /sys/class/android_usb/android0/f_mass_storage/lun")
        if (exitCode == 0) {
            onLogMessage("Backend Type Found :: LUN")
            return BACKEND_TYPE.LUN
        }

        exitCode = Shell.Pool.SU.run("stat /config/usb_gadget/g1")
        if (exitCode == 0) {
            onLogMessage("Backend Type Found :: CONFIGFS")
            return BACKEND_TYPE.CONFIGFS
        }

        return BACKEND_TYPE.NA
    }

    public fun mountImage(usb: String = "/sys/class/android_usb/android0", imageFile: String, toast: (msg: String, long: Boolean) -> Unit) {

        if (backendType == null) {
            try {
                onLogMessage("Figuring out backend to use...")
                backendType = gettBackendType()
            } catch (e:Shell.ShellDiedException ) {
                Log.e("ERROR", "Device not rooted or SU missing.")
                toast("Device not rooted or SU missing.", true)
                backendType = BACKEND_TYPE.NA
                return
            }
        }

        if (backendType == BACKEND_TYPE.NA) {
            val msg = "Couldn't figure out USB Gadget Backend type. Your device is not supported."
            Log.e("ERROR", msg)
            onLogMessage(msg)
            toast(msg, true)
            return
        }

        when (backendType) {
            BACKEND_TYPE.LUN -> {
                val usb = "/sys/class/android_usb/android0"
                try {
                    val exitCode = Shell.Pool.SU.run(arrayOf(
                            //Disable USB gadget
                            "echo -n 0 > $usb/enable",
                            // Try to append if the function is not already enabled (by ourselves most likely)
                            "grep mass_storage $usb/functions > /dev/null || sed -e 's/$/,mass_storage/' $usb/functions | cat > $usb/functions",
                            // If empty, set ourselves as the only function
                            "[[ -z $(cat $usb/functions) ]] && echo -n mass_storage > $usb/functions",
                            //LUN name for sysfs
                            "echo -n USBMountr > $usb/f_mass_storage/inquiry_string",
                            //Clear LUN file
                            "echo -n > $usb/f_mass_storage/lun/file",
                            //Set image to read-only
                            "echo -n true > $usb/f_mass_storage/lun/ro",
                            //Set LUN file from imageFile
                            "echo -n $imageFile > $usb/f_mass_storage/lun/file",
                            //Re-enabled USB Gadget
                            "echo -n 1 > $usb/enable",
                            "echo success"
                    ))
                    onLogMessage("Operation exited with code: $exitCode...")
                } catch (e: Shell.ShellDiedException) {
                    val msg = "Device not rooted or SU missing."
                    Log.e("ERROR", msg)
                    toast(msg, true)
                    onLogMessage(msg)
                }
            }

            BACKEND_TYPE.LUNS -> {
                val usb = "/sys/class/android_usb/android0"
                try {
                    val exitCode = Shell.Pool.SU.run(arrayOf(
                            //Disable USB gadget
                            "echo -n 0 > $usb/enable",
                            // Try to append if the function is not already enabled (by ourselves most likely)
                            "grep mass_storage $usb/functions > /dev/null || sed -e 's/$/,mass_storage/' $usb/functions | cat > $usb/functions",
                            // If empty, set ourselves as the only function
                            "[[ -z $(cat $usb/functions) ]] && echo -n mass_storage > $usb/functions",
                            "echo -n disk > $usb/f_mass_storage/luns",
                            //LUN name for sysfs
                            "echo -n USBMountr > $usb/f_mass_storage/inquiry_string",
                            //Clear LUN file
                            "echo -n > $usb/f_mass_storage/lun0/file",
                            //Set image to read-only
                            "echo -n true > $usb/f_mass_storage/lun0/ro",
                            //Set LUN file from imageFile
                            "echo -n $imageFile > $usb/f_mass_storage/lun0/file",
                            //Re-enabled USB Gadget
                            "echo -n 1 > $usb/enable",
                            "echo -n success"
                    ))
                    onLogMessage("Operation exited with code: $exitCode...")
                } catch (e: Shell.ShellDiedException) {
                    val msg = "Device not rooted or SU missing."
                    Log.e("ERROR", msg)
                    toast(msg, true)
                    onLogMessage(msg)
                }
            }

            BACKEND_TYPE.CONFIGFS -> {
                /*
                    GET UDC
                    val udc = cat /config/usb_gadget/g1/UDC
                    
                    at first disable usb by setttings UDC to null
                    
                    echo -n "" > /config/usb_gadget/g1/UDC
                    
                    then do below
                    
                    and finish with setting UDC again
                    
                    echo -n "$udc" > /config/usb_gadget/g1/UDC
                */
                val usb = "/config/usb_gadget/g1"
                try {
                    val exitCode = Shell.Pool.SU.run(arrayOf(
                            //Set mode to msc
                            "echo -n 'msc' > $usb/configs/b.1/strings/0x409/configuration",
                            //Set vendor and id to Mass Storage Device
                            "echo -n 05C6 > idVendor",
                            "echo -n 1000 > idProduct",
                            //Clear configs folder
                            "for f in $usb/configs/b.1/f*; do rm \$f; done",
                            //Symlink proper config
                            "ln -s $usb/functions/mass_storage.0 $usb/configs/b.1/f1",
                            //Set LUN file from image file
                            "echo -n $imageFile > $usb/configs/b.1/f1/lun.0/file"
                    ))
                    onLogMessage("Operation exited with code: $exitCode...")
                } catch (e: Shell.ShellDiedException) {
                    val msg = "Device not rooted or SU missing."
                    Log.e("ERROR", msg)
                    toast(msg, true)
                    onLogMessage(msg)
                }
            }
        }
    }

    public fun unmount_image(usb: String = "/sys/class/android_usb/android0", toast: (msg: String, long: Boolean) -> Unit) {
        if (backendType == null) {
            try {
                onLogMessage("Figuring out backend to use...")
                backendType = gettBackendType()
            } catch (e:Shell.ShellDiedException ) {
                Log.e("ERROR", "Device not rooted or SU missing.")
                toast("Device not rooted or SU missing.", true)
                backendType = BACKEND_TYPE.NA
                return
            }
        }

        if (backendType == BACKEND_TYPE.NA) {
            val msg = "Couldn't figure out USB Gadget Backend type. Your device is not supported."
            Log.e("ERROR", msg)
            onLogMessage(msg)
            toast(msg, true)
            return
        }

        if (backendType == BACKEND_TYPE.LUNS || backendType == BACKEND_TYPE.LUN ) {
            try {
                val exitCode = Shell.Pool.SU.run(arrayOf(
                        "echo -n 0 > $usb/enable",
                        "sed -e 's/mass_storage//' $usb/functions | cat > $usb/functions",
                        "echo -n 1 > $usb/enable"
                ))
                onLogMessage("Operation exited with code: $exitCode...")
            } catch (e: Shell.ShellDiedException) {
                val msg = "Device not rooted or SU missing."
                Log.e("ERROR", msg)
                toast(msg, true)
                onLogMessage(msg)
            }
        } else if (backendType == BACKEND_TYPE.CONFIGFS) {
            val msg = "Reboot Device to reset USB for normal usage."
            toast(msg, true)
            onLogMessage(msg)
        }
    }

}
