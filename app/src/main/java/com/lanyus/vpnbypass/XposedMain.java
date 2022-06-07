package com.lanyus.vpnbypass;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedMain implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (lpparam.packageName.equals("com.lanyus.vpnbypass")) {
            return;
        }
        XposedBridge.log("com.lanyus.vpnbypass Loaded app: " + lpparam.packageName);

        hook(lpparam.classLoader);

        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                ClassLoader classLoader = ((Context) param.args[0]).getClassLoader();
                hook(classLoader);
            }
        });
    }

    public void hook(ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod("java.lang.System", classLoader, "getProperty", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String arg = (String) param.args[0];
                if (arg != null && (arg.equalsIgnoreCase("http.proxyHost") || arg.equalsIgnoreCase("http.proxyPort"))) {
                    param.setResult(null);
                    XposedBridge.log("com.lanyus.vpnbypass modify java.lang.System.getProperty(http.proxyHost) return null");
                }
            }
        });

        XposedHelpers.findAndHookMethod("java.net.NetworkInterface", classLoader, "getName", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String result = (String) param.getResult();
                if (result.startsWith("tun") || result.startsWith("ppp")) {
                    param.setResult("rmnet_data0");
                    XposedBridge.log("com.lanyus.vpnbypass java.net.NetworkInterface.getName return " + result + " modify to rmnet_data0");
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.net.NetworkInfo", classLoader, "isConnectedOrConnecting", new XC_MethodHook() {

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (((NetworkInfo) param.thisObject).getType() == ConnectivityManager.TYPE_VPN) {
                    param.setResult(false);
                    XposedBridge.log("com.lanyus.vpnbypass modify android.net.NetworkInfo(ConnectivityManager.TYPE_VPN).isConnectedOrConnecting return false");
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.net.NetworkCapabilities", classLoader, "hasTransport", int.class, new XC_MethodHook() {

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                int transportType = (int) param.args[0];
                if (transportType == NetworkCapabilities.TRANSPORT_VPN) {
                    param.setResult(false);
                    XposedBridge.log("com.lanyus.vpnbypass modify android.net.NetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) return false");
                }
            }
        });
    }
}
