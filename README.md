Android Host Monitor
====================

[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Android%20Host%20Monitor-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/2626) [ ![Download](https://api.bintray.com/packages/gotev/maven/android-host-monitor/images/download.svg) ](https://bintray.com/gotev/maven/android-host-monitor/_latestVersion) [![Donate](https://www.paypalobjects.com/en_US/i/btn/btn_donate_SM.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=alexgotev%40gmail%2ecom&lc=US&item_name=Android%20Upload%20Service&item_number=AndroidHostMonitor&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donate_SM%2egif%3aNonHosted)

Easily monitor device network state and remote hosts reachability on Android.

## Purpose
In today's massively interconnected world, it's a common thing for an App to communicate with remote servers to provide contents and functionalities to the end user, so it's a good thing to check if the app can "talk" with the needed servers before doing any other network operation. Often the app has to do something based on the reachability status of a particular server or handle transitions between networks. This kind of checking can easily become a mess when we have to deal with continuous network switching (Mobile to WiFi, WiFi to Mobile, Airplane mode, no connection) and battery life! Android Host Monitor handles that for you and let you focus purely on your app's business logic.

## Setup <a name="setup"></a>
#### Maven

```
<dependency>
  <groupId>net.gotev</groupId>
  <artifactId>hostmonitor</artifactId>
  <version>2.0</version>
  <type>aar</type>
</dependency>
```

#### Gradle

```
dependencies {
    compile 'net.gotev:hostmonitor:2.0@aar'
}
```
and do a project sync.

## Get started
Configuring a remote host reachability check is as simple as:
```java
new HostMonitorConfig(context)
        .setBroadcastAction(BuildConfig.APPLICATION_ID)
        .add("my.server.com", 80)
        .save();
```
You can do that from wherever you have a `Context`. What you've done here is the following:
* you've set the broadcast action used by the library to notify reachability changes. In this case you used the Gradle application ID, but you can use whatever string you want, as long as it's unique in your app.
* you've added the monitoring of `my.server.com` on port `80`. The library will immediately perform a reachability check and notify you of the status. Whenever the device connectivity status changes (e.g. from WiFi to 3G, from 3G to Airplane, from no connection to 3G, ...) the library will automatically perform a reachability check in the background and will notify you only if the status has been changed from the last time you received a notification.

When you call `save()` the settings are persisted and immediately applied.

Settings survives to application restarts and android restarts, so until you want to change the host monitor configuration, you can simply start the reachability check when your app starts by invoking:
```java
new HostMonitorConfig(context).save();
```

The library can also automatically perform scheduled periodic reachability checks, so for example if you want to monitor your server every 15 minutes, all you have to do is:
```java
new HostMonitorConfig(context).setCheckIntervalInMinutes(15).save();
```
Bear in mind that more frequent reachability checks drains your battery faster!

You can also set other things such as socket connection timeout and maximum connection attempts before notifying failure. Check [JavaDocs](http://gotev.github.io/android-host-monitor/javadoc/).

#### Unmonitor a host and port
```java
new HostMonitorConfig(context).remove("my.server.com", 80).save();
```

#### Remove all the monitored hosts
```java
new HostMonitorConfig(context).removeAll().save();
```
When there are no hosts left to be monitored, the library automatically shuts down the connectivity monitoring and clears all the scheduled checks.

#### Reset configuration to factory defaults
If you want to reset the persisted configuration, just invoke:
```java
HostMonitorConfig.reset(context);
```
This will reset the configuration to factory defaults and will stop any active and scheduled network check.

#### Receive reachability status changes <a name="receive-status"></a>
To listen for the status, subclass `HostMonitorBroadcastReceiver`.
If you want to monitor host reachability globally in your app, all you have to do is create a new class (called `HostReachabilityReceiver` in this example):

```java
public class HostReachabilityReceiver extends HostMonitorBroadcastReceiver {
    @Override
    public void onHostStatusChanged(HostStatus status) {
        Log.i("HostReachability", status.toString());
    }
}
```

and register it as a Broadcast receiver in your manifest:

```xml
<receiver
    android:name=".HostReachabilityReceiver"
    android:enabled="true"
    android:exported="false" >
    <intent-filter>
        <action android:name="com.yourcompany.yourapp.reachability" />
    </intent-filter>
</receiver>
```

`com.yourcompany.yourapp.reachability` must be the same string you set as broadcast action in the configuration, otherwise you will not receive reachability events.

You can receive status updates also in your `Activity` or `Service`. Here there is an example inside an `Activity`:

```java
public class YourActivity extends Activity {

    private final HostMonitorBroadcastReceiver receiver =
      new HostMonitorBroadcastReceiver() {
        @Override
        public void onHostStatusChanged(HostStatus status) {
           Log.i("HostMonitor", status.toString());
        }
      };

    @Override
    protected void onResume() {
        super.onResume();
        receiver.register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        receiver.unregister(this);
    }
}
```
A partial wake lock is automatically held for the entire execution of the `onHostStatusChanged` method and is released as soon as the method returns.


## Logging <a name="logging"></a>
By default the library logging is disabled. You can enable debug log by invoking:
```java
Logger.setLogLevel(LogLevel.DEBUG);
```

The library logger uses `android.util.Log`, but you can override that by providing your own logger implementation like this:
```java
Logger.setLoggerDelegate(new Logger.LoggerDelegate() {
    @Override
    public void error(String tag, String message) {
        //your own implementation here
    }

    @Override
    public void error(String tag, String message, Throwable exception) {
        //your own implementation here
    }

    @Override
    public void debug(String tag, String message) {
        //your own implementation here
    }

    @Override
    public void info(String tag, String message) {
        //your own implementation here
    }
});
```

## Issues
When you post a new issue regarding a possible bug in the library, make sure to add as many details as possible to be able to reproduce and solve the error you encountered in less time. Thank you :)

## Contribute <a name="contribute"></a>
* Do you have a new feature in mind?
* Do you know how to improve existing docs or code?
* Have you found a bug?

Contributions are welcome and encouraged! Just fork the project and then send a pull request. Be ready to discuss your code and design decisions :)

## Do you like the project? <a name="donate"></a>
Put a star, spread the word and if you want to offer me a free beer, [![Donate](https://www.paypalobjects.com/en_US/i/btn/btn_donate_SM.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=alexgotev%40gmail%2ecom&lc=US&item_name=Android%20Upload%20Service&item_number=AndroidHostMonitor&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donate_SM%2egif%3aNonHosted)

## License <a name="license"></a>

    Copyright (C) 2015-2016 Aleksandar Gotev

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
