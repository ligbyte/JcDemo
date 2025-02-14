# 精臣打印机 SDK

Android Studio Iguana | 2023.2.1  
Build #AI-232.10227.8.2321.11479570, built on February 22, 2024  
Runtime version: 17.0.9+0--11185874 amd64  
VM: OpenJDK 64-Bit Server VM by JetBrains s.r.o.  
Windows 11.0  
JDK: 17  




Dialog
./gradlew FlycoDialog_Lib:assembleRelease



Android将完整项目或Module打包成aar
https://juejin.cn/post/6854573219785998344

1.在App的build.gradle
    a.将 id 'com.android.application'   改为    id 'com.android.library'
    b.删除applicationId

2.去清单文件修改AndroidManifest配置:  删除相关参数

      <application
        android:name="com.niimbot.printer.app.MyApplication"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.JcDemo">

            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>

3.执行打包aar命令:
```shell
./gradlew app:assembleRelease
```