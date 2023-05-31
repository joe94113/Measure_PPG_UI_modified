# Measure_PPG_UI_modified

## Bluno beetle連接PPG Sensor接線圖如下

![](https://github.com/joe94113/Measure_PPG_UI_modified/blob/main/public/1685540387538.jpg)

## 部屬安卓前置作業

前往[Firebase](https://console.firebase.google.com/)，新開一個專案並將`google-services.json`放置在`Measure_PPG_UI_modified_latest\app\`底下，並啟用即時資料庫並更改規則成以下
```
{
  "rules": {
    ".read": true,
    ".write": true,
  }
}
```
