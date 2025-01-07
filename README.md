# ArtMapDrawer for DripDrop
这是 Mod  [drawler](https://github.com/miniking1000/drawler) 的 Fork 版本，适配 DripDrop 服务器尺寸为 32×32 的 ArtMap，同时做了一些其他修改。

## 修改
* 图片来源改为本地
* 添加简体中文语言
* 适配 ModMenu 配置入口
* 集成指令至`/artMapDrawer`

## 通用步骤
1. 将需要绘制的图像保存至本地
2. 创建一张新画布，并取得该画布 id
3. 进入游戏，使用指令`/artMapDrawer <画布 id> <图像路径>`获取染料清单与预览
4. 将画布挂上画布并与画架互动，按下`开始/暂停绘制`开始自动绘画

## 按键与指令
* 应用的指令集成在`/artMapDrawer`中
* 配置界面：`R`
* 开始/暂停绘制：`小键盘 1`
* 显示预览图：`小键盘 2`