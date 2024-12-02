# Kafka Dog

Kafka Dog 是一个轻量级的 Apache Kafka 可视化管理工具，提供直观的界面来管理和监控 Kafka 集群。

## ✨ 特性

- 🚀 原生桌面客户端，简单易用
- 📝 支持多连接配置管理
- 🔍 Topic 和分区信息可视化
- 📊 消息查看和实时预览
- 🎯 支持 Latest/Earliest 消息定位
- 📦 支持 String、Avro、Protobuf 等多种数据格式

## 🖥 支持环境

- Windows

## 📦 安装与使用

### Windows
1. 下载最新版本的 `KafkaDog-windows-x64.zip`
2. 解压到任意目录
3. 双击运行 `KafkaDog.exe`

### macOS
1. 下载最新版本的 `KafkaDog-macos.dmg`
2. 打开 DMG 文件
3. 将 Kafka Dog 拖入 Applications 文件夹
4. 从启动器中运行 Kafka Dog

## 🚀 快速入门

1. 启动 Kafka Dog
2. 点击左下角的"添加连接"按钮
3. 在弹出的对话框中填写：
    - 连接名称（用于标识不同的连接）
    - Kafka 服务器地址
    - 端口号（默认 9092）
4. 点击"测试连接"确认连接是否可用
5. 连接成功后点击"保存"
6. 在左侧面板中：
    - 双击连接名称加载 Topic 列表
    - 展开 Topic 查看分区信息
    - 点击具体分区查看消息内容
7. 在右侧面板中：
    - 选择 Latest/Earliest 切换消息位置
    - 使用分页控件浏览更多消息

## 📸 界面

[主界面](./images/kafkadog.png)

## 🔨 开发计划

- [ ] 支持消息发送功能
- [ ] 添加消费组管理
- [ ] 支持 Topic 创建和配置
- [ ] 集成 Avro Schema Registry
- [ ] 添加消息搜索功能
- [ ] 支持更多消息格式（JSON、XML等）

## 🤝 贡献

如果你想为 Kafka Dog 贡献代码，欢迎提交 Pull Request 和 Issue！


## 📄 开源许可

Kafka Dog 使用 [MIT 许可证](./LICENSE)。

## 🙏 鸣谢

- [Apache Kafka](https://kafka.apache.org/)
- [JavaFX](https://openjfx.io/)
- [OpenJDK](https://openjdk.java.net/)

## 📧 联系方式

- 作者：[Pitayafruits](https://github.com/Pitayafruits)
- Email：[pitaya.cc@qq.com]()
