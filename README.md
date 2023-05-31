# Measure_PPG_UI_modified

## Bluno beetle連接PPG Sensor接線圖如下

![](https://github.com/joe94113/Measure_PPG_UI_modified/blob/main/public/1685540387538.jpg)

## 安卓 code

前往firebase，新開一個專案並將`google-services.json`放置`Measure_PPG_UI_modified_latest\app\`底下
啟用即時資料庫並將規則更改成以下
```
{
  "rules": {
    ".read": true,
    ".write": true,
  }
}
```
