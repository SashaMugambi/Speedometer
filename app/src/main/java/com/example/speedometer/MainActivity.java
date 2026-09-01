package com.example.speedometer;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Collections;
import java.util.List;

public class NetworkMonitor {
    private final Context context;

    public NetworkMonitor(Context context) {
        this.context = context;
    }

    public String getNetworkType() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) {
            return "Unknown";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return "WiFi";
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    return getCellularNetworkType();
                }
            }
        } else {
            // Fallback for older Android versions
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork != null) {
                return activeNetwork.getTypeName();
            }
        }

        return "Unknown";
    }

    private String getCellularNetworkType() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());

        if (capabilities == null) {
            return "Unknown";
        }

        // Use getNetworkSpecifier() for API 30+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.net.NetworkSpecifier specifier = capabilities.getNetworkSpecifier();
            if (specifier != null) {
                return "Cellular";
            }
        }

        // Fallback for older versions
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) {
            return activeNetwork.getSubtypeName();
        }

        return "Unknown";
    }

    public int getSignalStrength() {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        if (wifiManager != null) {
            int rssi = wifiManager.getConnectionInfo().getRssi();
            return WifiManager.calculateSignalLevel(rssi, 5);
        }

        return 0;
    }

    public String getIPv4Address() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf('.') > 0;
                        if (isIPv4) {
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.e("NetworkMonitor", "Error getting IPv4 address", ex);
        }
        return "No IPv4 address";
    }

    public String getIPv6Address() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        boolean isIPv6 = sAddr.contains(":");
                        if (isIPv6) {
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.e("NetworkMonitor", "Error getting IPv6 address", ex);
        }
        return "No IPv6 address";
    }

    public double measureDownloadSpeed() {
        long startTime, endTime;
        final int BUFFER_SIZE = 1024 * 8; // 8KB buffer

        try {
            URL url = new URL("https://speed.hetzner.de/100MB.bin"); // Large test file
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            startTime = System.currentTimeMillis();

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long totalBytesRead = 0;

            while ((bytesRead = connection.getInputStream().read(buffer)) != -1) {
                totalBytesRead += bytesRead;

                // Stop test after reading 100KB to avoid excessive data usage
                if (totalBytesRead >= 100 * 1024) {
                    break;
                }
            }

            endTime = System.currentTimeMillis();

            double durationInSeconds = (endTime - startTime) / 1000.0;
            double speedMbps = ((totalBytesRead * 8) / durationInSeconds) / 1_000_000;

            connection.disconnect();
            return speedMbps;

        } catch (IOException e) {
            Log.e("NetworkMonitor", "Error measuring download speed", e);
            return 0;
        }
    }

    public long measureLatency() {
        try {
            URL url = new URL("https://google.com");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.connect();

            long startTime = System.currentTimeMillis();
            connection.connect();
            long endTime = System.currentTimeMillis();

            long latency = endTime - startTime;
            connection.disconnect();

            return latency;
        } catch (IOException e) {
            Log.e("NetworkMonitor", "Error measuring latency", e);
            return Long.MAX_VALUE;
        }
    }
}