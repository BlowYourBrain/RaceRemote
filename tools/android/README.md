# Samsung Wi-Fi band diagnostic

`PinWifi.java` is a standalone ADB-shell helper, verified on SM-G973F Android12/API31.
It queries a saved profile, optionally pins its BSSID or clears that pin. It does not
read or supply a password. Android12's WifiConfigManager preserves the existing PSK
when the configuration contains the `*` password mask; the helper refuses unmasked
credentials. This rule was checked against the [Android12 source](https://android.googlesource.com/platform/packages/modules/Wifi/+/refs/tags/android-12.0.0_r1/service/java/com/android/server/wifi/WifiConfigManager.java).

Only use while control is stopped. Pinning one BSSID restricts roaming for that saved
network. Record the original pin before changing it; `clear` restores an originally
unpinned profile using Android's `any` BSSID sentinel; the saved value is read back
before reporting success. The commands below are for the task-local SDK/JDK used in this repo:

```powershell
$jdk='D:/RaceRemote-build-checks/tooling/jdk-17.0.20.1+1/bin'
$sdk='C:/Users/fetel/AppData/Local/Android/Sdk'
$out='build/wifi-pin'
New-Item -ItemType Directory -Force "$out/dex" | Out-Null
& "$jdk/javac.exe" -source 8 -target 8 -cp "$sdk/platforms/android-33/android.jar" -d $out tools/android/PinWifi.java
$classes = @(Get-ChildItem "$out/PinWifi*.class" | ForEach-Object FullName)
& "$jdk/java.exe" -cp "$sdk/build-tools/30.0.3/lib/d8.jar" com.android.tools.r8.D8 --lib "$sdk/platforms/android-33/android.jar" --output "$out/dex" @classes
& "$sdk/platform-tools/adb.exe" -s RF8M811X8FK push "$out/dex/classes.dex" /data/local/tmp/rr-pin-wifi.dex
# Read-only query of saved profile0:
& "$sdk/platform-tools/adb.exe" -s RF8M811X8FK shell 'CLASSPATH=/data/local/tmp/rr-pin-wifi.dex app_process /system/bin PinWifi 0'
# Set: append the observed BSSID to the shell command above.
# Clear an existing pin and reconnect using the preserved saved profile:
& "$sdk/platform-tools/adb.exe" -s RF8M811X8FK shell 'CLASSPATH=/data/local/tmp/rr-pin-wifi.dex app_process /system/bin PinWifi 0 clear'
```

Inspect `cmd wifi status` afterwards to verify the actual connection frequency.
This uses hidden Android context APIs and shell privileges; newer Android releases
and other vendors are unverified. No root, runtime permission grant, or router change
is involved. It is a diagnostic tool, not part of the Android application.
