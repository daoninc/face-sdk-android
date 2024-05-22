# Daon Face SDK

The Daon Face SDK can be used in conjunction with the IdentityX server to offer face-processing capabilities on Android mobile devices.

The IdentityX Face SDK is made up of the following core features:

- Face Quality Analysis
- Passive Liveness Detection
- Eye Blink Detection
- Head Movement Detection (head nod and shake)
- Light Reflection Liveness Detection

## License
The IdentityX FIDO Client SDK requires a license that is bound to an application identifier. This license may in turn embed licenses that are required for specific authenticators. Contact Daon Support or Sales to request a license.

## Samples

The demo sample includes the following:

- **Face/Passive/Blink** Provides a reference implementation of Best Practices using Passive Liveness and blink detection.
- **Face/Passive V2** Demonstrates the Daon Passive Liveness V2 algorithm.
- **Face/Quality/Live** Head Movement Detection (HMD), Blink and quality.
- **Face/Quality/Photo** Bitmap quality analysis.
- **Face/CLR** How to use Light Reflection liveness detection.
- **Face/Matching** An example of client based face enrollment and face matching using Passive Liveness and blink detection. 
- **Face/Bitmap** Shows how to find faces in a photo/bitmap.
- **Face Capture** Demonstrates how to capture a good image for enrollment or authentication using liveness and quality.
- **Face/Custom Analyzer** Shows how to add a custom analyzer.

## API

```kotlin
val daonFace = DaonFace(context, options)
```

Where the options and corresponding dependencies are:

-   OPTION_LIVENESS_BLINK -- Blink detection

    Dependencies:<BR>
    `com.daon.sdk:face`

-   OPTION_LIVENESS -- Passive liveness
    
    V1 Dependencies:<BR>
    `com.daon.sdk:face`<BR>
    `com.daon.sdk:face-liveness`<BR>
    `org.slf4j:slf4j-android:1.7.12`

    V2 Dependencies:<BR>
    `com.daon.sdk:face`<BR>
    `com.daon.sdk:face-liveness-dfl`<BR>
    `com.daon.sdk:face-quality`<BR>
    `com.daon.sdk:face-matcher`<BR>
    `com.daon.sdk:face-detector` 

-   OPTION_LIVENESS_HMD -- Face nod and shake detection.

    Dependencies:<BR>
    `com.daon.sdk:face`<BR>
    `com.daon.sdk:face-hmd`

-   OPTION_QUALITY -- Face quality analysis.

    Dependencies:<BR>
    `com.daon.sdk:face`<BR>
    `com.daon.sdk:face-quality`

-   OPTION_DEVICE_POSITION -- Device position using device sensors if
    available.

    Dependencies:<BR>
    `com.daon.sdk:face`<BR>

-   OPTION_LIVENESS_CLR -- Colored Light Reflection liveness
    detection.

    Dependencies:<BR>
    `com.daon.sdk:face`<BR>
    `com.daon.sdk:face-clr`

-   OPTION_RECOGNITION -- Face matching.

    Dependencies:<BR>
    `com.daon.sdk:face`<BR>
    `com.daon.sdk:crypto`<BR>
    `com.daon.sdk:face-matcher`<BR>
    `com.daon.sdk:face-detector`<BR>    

-   OPTION_MASK -- Medical/surgical mask detection.
    Dependencies:<BR>
    `com.daon.sdk:face`<BR>
    `com.daon.sdk:face-detector.mask`

Analyze a bitmap.

```kotlin
var result = daonFace.analyze(bitmap);
```

Analyzes an image buffer from a live video feed. This method is asynchronous and returns an Analysis object. Your application will be notified asynchronously via the Analysis callbacks.


```kotlin
daonFace.analyze(image)
    .addAnalysisListener { result, img ->
        // Frame result
    }
    .addAlertListener { result, alert ->
        // Alerts, e.g. face too far, errors, timeout, etc.
    }    
    .addEventDetectedListener { result, event, image ->
        // Liveness event, e.g. blink
    }
```
    

