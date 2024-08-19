# xProof Face SDK

The xProof Face SDK can be used in conjunction with the IdentityX server to offer face-processing capabilities on Android mobile devices.

The SDK is made up of the following core features:

- Face Quality Analysis
- Passive Liveness Detection
- Eye Blink Detection
- Head Movement Detection (nod and shake)
- Light Reflection Liveness Detection

## License
The xProof Face SDK requires a license that is bound to an application identifier. This license may in turn embed licenses that are required for specific authenticators. Contact Daon Support or Sales to request a license.

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
- **IFP Capture** Demonstrates how to use the face Capture API to capture an image for enrollment and verification. The Capture API supports injection attack prevention when used in conjunction with the IdentityX server. 

## SDK repository
In your project-level build.gradle file, make sure to include the Daon Maven repository in your buildscript or allprojects sections.

[Daon Maven repository](https://github.com/daoninc/sdk-packages/blob/main/README.md)

## API

### Initialize

**Kotlin**
```kotlin
val daonFace = DaonFace(context, options)
```
**Java**
```Java
DaonFace daonFace = new DaonFace(context, options);
```

Where the options and corresponding dependencies are:

-   OPTION_LIVENESS_BLINK -- Blink detection

    Dependencies:<BR>
    `com.daon.sdk:face`

-   OPTION_LIVENESS -- Passive liveness V1

    Dependencies:<BR>
    `com.daon.sdk:face`<BR>
    `com.daon.sdk:face-liveness`<BR>

- OPTION_LIVENESS_V2 -- Passive liveness V2

  Dependencies:<BR>
  `com.daon.sdk:face`<BR>
  `com.daon.sdk:face-liveness-dfl`<BR>
  
  Transitive dependencies:<BR>
  *com.daon.sdk:face-quality*<BR>
  *com.daon.sdk:face-matcher*<BR>
  *com.daon.sdk:face-detector*

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
    `com.daon.sdk:face-matcher`<BR>
    `com.daon.sdk:crypto`<BR>

    Transitive dependencies:<BR>
    *com.daon.sdk:face-detector*
    
-   OPTION_MASK -- Medical/surgical mask detection.
  
    Dependencies:<BR>
    `com.daon.sdk:face`<BR>
    `com.daon.sdk:face-mask`

### Analyze a bitmap

**Kotlin**
```kotlin
var result = daonFace.analyze(bitmap)
```

**Java**
```Java
Result result = daonFace.analyze(bitmap);
```

### Analyze a video stream
Analyzes an image buffer from a live camera feed. This method is asynchronous and returns an Analysis object. Your application will be notified asynchronously via the Analysis callbacks.

**Kotlin**
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

**Java**
```java
daonFace.analyze(image)
    .addAnalysisListener((result, img) -> {
        // Frame result
    })
    .addAlertListener((result, alert) -> {
        // Alerts, e.g. face too far, errors, timeout, etc.
    })
    .addEventDetectedListener((result, event, image) -> {
       // Liveness event, e.g. blink     
    });
```

### Face Capture API
The `CameraController` will handle the camera and can be used with and without a preview.

The Capture API supports injection attack prevention when used in conjunction with the IdentityX server. The Capture API supports server liveness, client liveness, medical mask detection and quality assessment.


```
implementation 'com.daon.sdk:face-capture:1.7.<build>'
```

**Kotlin**
```kotlin
val builder = CameraController.Builder(context, lifecycleOwner)
if (previewView != null)
    builder.setPreviewView(previewView)

builder.setMedicalMaskDetection(false)
builder.setCaptureQuality(Quality.High) // High | Low

builder.setErrorListener { exception ->
    errorMessage.postValue(exception.message)
}.setPhotoListener { bitmap ->
    // This event is triggered when a photograph is being processed.
    // It allows you to use the photograph that has been taken, which can be
    // useful for showing a preview of the photograph taken
}.setCaptureCompleteListener { data ->
    // Event that triggers after a photograph has been taken and processed.
    // The data can be submitted to the server for further processing.
}.setFaceDetectionListener { result ->
    // Face to close, not centered, etc.
    faceDetectionHint.postValue(getQualityMessage(result))
}

cameraController = builder.build()
```

**Java**
```java
CameraController.Builder builder = CameraController.Builder(context, lifecycleOwner);
if (previewView != null)
    builder.setPreviewView(previewView);

builder.setMedicalMaskDetection(false);
builder.setCaptureQuality(Quality.High); // High | Low


builder.setErrorListener( (exception) -> {
    errorMessage.postValue(exception.message)
}).setPhotoListener( (bitmap) -> {
    // This event is triggered when a photograph is being processed.
    // It allows you to use the photograph that has been taken, which can be
    // useful for showing a preview of the photograph taken
}).setCaptureCompleteListener( (data) -> {
    // Event that triggers after a photograph has been taken and processed.
    // The data can be submitted to the server for further processing.
}).setFaceDetectionListener( (result) -> {
    // Face to close, not centered, etc.
    faceDetectionHint.postValue(getQualityMessage(result))
});

cameraController = builder.build();
```

See the [xProof Face SDK Documentation](https://developer.identityx-cloud.com/client/face/android/) for more information.    

