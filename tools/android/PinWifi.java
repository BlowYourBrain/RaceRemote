import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import java.util.List;

/** ADB shell diagnostic, verified on Samsung API31. Uses the saved profile with its password masked. */
public class PinWifi {
    public static void main(String[] args) throws Exception {
        try { run(args); }
        catch (Exception failure) {
            System.err.println(failure.getClass().getSimpleName() + ": " + failure.getMessage());
            System.exit(1);
        }
    }

    private static void run(String[] args) throws Exception {
        if (android.os.Process.myUid() != 2000) throw new IllegalStateException("ADB shell UID required");
        if (args.length < 1 || args.length > 2) throw new IllegalArgumentException("network ID, optional BSSID/clear");
        int id = Integer.parseInt(args[0]);
        Class<?> activityThread = Class.forName("android.app.ActivityThread");
        android.os.Looper.prepareMainLooper();
        java.lang.reflect.Constructor<?> constructor = activityThread.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object thread = constructor.newInstance();
        Context system = (Context) activityThread.getMethod("getSystemContext").invoke(thread);
        Context shell = new android.content.ContextWrapper(system.createPackageContext("com.android.shell", 0)) {
            @Override public String getOpPackageName() { return "com.android.shell"; }
            @Override public Context getApplicationContext() { return this; }
        };
        WifiManager wifi = shell.getSystemService(WifiManager.class);
        // Attribute the cached system-context manager to the actual shell caller, preserving its normal permissions.
        java.lang.reflect.Field contextField = WifiManager.class.getDeclaredField("mContext");
        contextField.setAccessible(true);
        contextField.set(wifi, shell);
        List<WifiConfiguration> configs = wifi.getConfiguredNetworks();
        if (configs == null) throw new IllegalStateException("Saved network access unavailable");
        WifiConfiguration config = null;
        for (WifiConfiguration candidate : configs) if (candidate.networkId == id) { config = candidate; break; }
        if (config == null) throw new IllegalStateException("Saved network missing");
        if (config.preSharedKey != null && !"*".equals(config.preSharedKey)) throw new IllegalStateException("Expected masked credential; no mutation performed");
        System.out.println("networkId=" + id + " pinned=" + isPinned(config.BSSID) + " frequency=" + wifi.getConnectionInfo().getFrequency());
        if (args.length == 1) { System.exit(0); return; }
        boolean clear = "clear".equals(args[1]);
        String target = clear ? "any" : args[1];
        if (!clear && !target.matches("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}")) throw new IllegalArgumentException("Invalid BSSID");
        config.BSSID = target;
        int updated = wifi.updateNetwork(config);
        if (updated != id) throw new IllegalStateException("Profile update rejected: " + updated);
        WifiConfiguration saved = null;
        for (WifiConfiguration candidate : wifi.getConfiguredNetworks()) if (candidate.networkId == id) { saved = candidate; break; }
        if (saved == null || (clear ? isPinned(saved.BSSID) : !target.equalsIgnoreCase(saved.BSSID)))
            throw new IllegalStateException("Saved BSSID did not match requested change");
        if (!wifi.disconnect() || !wifi.enableNetwork(id, true) || !wifi.reconnect()) throw new IllegalStateException("Reconnect rejected; restore pin using clear");
        System.out.println("verified same saved profile; pinned=" + !clear);
        System.exit(0);
    }

    private static boolean isPinned(String bssid) { return bssid != null && !"any".equalsIgnoreCase(bssid); }
}
