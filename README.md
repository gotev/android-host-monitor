# Android Host Monitor
Easily monitor remote hosts and ports reachability on Android.

[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Android%20Host%20Monitor-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/2626)

[Setup instructions](#setup)

## Purpose
In today's massively interconnected world, it's a common thing for an App to communicate with remote servers to provide contents and functionalities to the end user, so it's a good thing to check if the app can "talk" with the needed servers before doing any other network operation. Often the app has to do something based on the reachability status of a particular server or handle transitions between networks. This kind of checking can easily become a mess when we have to deal with continuous network switching (Mobile to WiFi, WiFi to Mobile, Airplane mode, no connection) and battery life! Android Host Monitor tries to solve this issue and let you focus purely on your app's business logic :)

## How it works
Android Host Monitor is made of two parts:
* A `BroadcastReceiver` monitoring the device's connectivity changes
* A `ScheduledExecutorService` which periodically checks the reachability status of the configured hosts

When your device is connected (WiFi or Mobile), the scheduler is run at the interval that you have configured. When the device is not connected (it may be transitioning from a network to another one, airplane mode may be activated or the user is in the middle of a desert :D, ...) the scheduler is stopped to preserve battery life and a reachability status change gets broadcasted for all the configured hosts. When the connection becomes available again, the scheduler gets automatically restarted and a reachability test gets done immediately. The `HostMonitor` stops and doesn't do anything automatically only when you explicitly shut it down (read below to discover how).

## Setup <a id="setup"></a>
Ensure that you have jcenter in your gradle build file:
```
repositories {
    jcenter()
}
```
then in your dependencies section add:

```
dependencies {
    compile 'com.alexbbb:hostmonitor:1.0'
}
```

and do a project sync. To start using the library, you have to initialize it. I suggest you to do that in your `Application` subclass:

```java
public class Initializer extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // setup the broadcast action string which will be used to notify
        // reachability changes. It can be any string of your choice. 
        // Here's just an example:
        HostMonitor.setBroadcastAction("com.yourcompany.yourapp.reachability");
        
        // add all the hosts and ports you want to monitor
        HostMonitor.add("web.yourcompany.com", 80);
        HostMonitor.add("web.yourcompany.com", 443);
        HostMonitor.add("other.site.com", 5060);
        
        // invoke this method to enable debug messages (disabled by default)
        //HostMonitor.enableDebug();
        
        // start the host monitor
        HostMonitor.start(getApplicationContext(),
                          HostMonitor.DEFAULT_CHEK_INTERVAL,
                          HostMonitor.DEFAULT_TIMEOUT);
    }
}
```

...and you're done, the setup is complete! You can add and remove the hosts to be monitored dynamically even after the host monitor is started.

## How to receive reachability status updates
For your convenience, a reference `BroadcastReceiver` implementation has been made, so if you want to monitor host reachability globally in your app, all you have to do is create a new class (called `HostReachabilityReceiver` in this example):

```java
public class HostReachabilityReceiver extends HostMonitorBroadcastReceiver {

    private static final String LOG_TAG = "HostReachability";

    @Override
    public void onHostStatusChanged(HostStatus status) {
        Log.i(LOG_TAG, status.toString());
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

You can receive status updates also in your activity:

```java
public class YourActivity extends Activity {

    private static final String TAG = "YourActivity";

    ...

    private final HostMonitorBroadcastReceiver receiver =
      new HostMonitorBroadcastReceiver() {
        @Override
        public void onHostStatusChanged(HostStatus status) {
           Log.i(LOG_TAG, status.toString());
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

## How to stop the monitoring
Call this method from anywhere you want to stop the `HostMonitor`:
```java
// true to send a broadcast host status update notifying that
// all the monitored hosts are unreachable.
HostMonitor.shutdown(true);
```

## License

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
