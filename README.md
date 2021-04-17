# SauceNAO Plugin

## 简介
一个基于 Mirai 的搜图插件, 后端是 SauceNAO (不过理论上通过修改后端的 .py 文件可以适配任何一个搜图 API)

## 原理
Mirai 接收消息 -> 插件解析并下载图片 -> 通过 Python 调用 SauceNAO 的 API -> 获得结果 -> 插件上传原图并补充信息

## TODO
- 拓展下载原图的适用范围(当前仅适用于 Pixiv)
- 支持管理员动态设置配额
- 支持黑名单