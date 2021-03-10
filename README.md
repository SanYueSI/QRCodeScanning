# QRCodeScanning
这是一个基于zxing实现的二维码扫描以及生成二维码图片demo
* 干掉了条形码只能二维码了[ps.注释了CameraConfigurationManager的一行代码]
```

 CameraConfigurationUtils.setBarcodeSceneMode(parameters)  
```
 
* 只能竖屏扫描了不过这也符合产品口味，毕竟国内都是竖屏扫描。
* 增加了手势缩放，双击放大/缩小。
* 简单的封装了可以直接抄到项目里不用改啥，集成自最新的zxing库。
* 可以打开闪光灯本地图片二维码识别识别有音效和震动不想要的或者改声音[ps.可以直接替换raw目录下的beep文件]的看代码注释就知道了
* 优化了下识别效果识别的速度，具体可以看下gif和日志图片。
* 1.优化的手段有去除其他的解码格式 只保留了DecodeFormatManager.QR_CODE_FORMATS
* 2.使用了低分辨率 重写了CameraConfigurationUtils的findBestPreviewSizeValue方法将原本的改了下
* 3.把CameraManager中buildLuminanceSource方法改了下

#### [下载体验](https://github.com/SanYueSI/QRCodeScanning/releases/download/v1.0.0/app-debug.apk)
```
   Camera.Size largestPreview = supportedPreviewSizes.get(0);//原来的代码
  Camera.Size largestPreview = supportedPreviewSizes.get(supportedPreviewSizes.size()-1);//改过的代码

```
### 效果图
![图一](https://github.com/SanYueSI/QRCodeScanning/blob/main/4i4jr-q0obx.gif)
![图一](https://github.com/SanYueSI/QRCodeScanning/blob/main/461615362301_.pic_hd.jpg)
![图一](https://github.com/SanYueSI/QRCodeScanning/blob/main/451615271804_.pic.jpg)
![图二](https://github.com/SanYueSI/QRCodeScanning/blob/main/441615271803_.pic.jpg)
