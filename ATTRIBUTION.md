# 第三方资源与署名

本仓库是 [trime](https://github.com/osfans/trime)（GPL-3.0）的衍生项目，在其之上内置了若干第三方方案与数据。特此署名，并说明各自来源与许可状况。

## 1. 预测词库 predict.db（联想功能）

| 项 | 说明 |
| --- | --- |
| 文件 | `app/data/rime/clover-jiugong/predict.db`（经 `app/src/main/assets/shared/predict.db` 软链打包进 APK） |
| 用途 | 为 `clover_jiugong` 方案提供下一词预测，由本项目已编译的 `librime-predict` 插件读取 |
| 来源 | 第三方仓库 [klchen0112/rime-combo-ice-pinyin](https://github.com/klchen0112/rime-combo-ice-pinyin) 中的 `wanxiang-lts-zh-predict.db` |
| 许可 | **该来源仓库未声明任何许可证**；数据文件名显示疑似源自「万象拼音」（[amzxyz/rime-wanxiang](https://github.com/amzxyz/rime-wanxiang)，CC-BY-4.0），但未见上游对该数据出处与授权的明确声明 |

**风险提示**：由于来源仓库未声明许可证，本仓库收录该数据当前仅用于个人学习与自用。若用于公开发布、商业分发或其他再分发场景，请先自行向原作者确认授权，或改用许可清晰的数据自行生成——生成方式为 `librime-predict` 官方工具链：

```text
中文语料 --(tools/make_predict_data, Rust)--> key/value/weight TSV
        --(tools/build_predict, C++)--> predict.db
```

## 2. 四叶草九宫格方案 clover_jiugong

`app/data/rime/clover-jiugong/` 下的方案与词库取自 **SivanLaai/rime-pure** 的「Clover 四叶草九宫拼音」方案（MIT License），本项目对其做了适配性裁剪，详见 `clover_jiugong.schema.yaml` 头部注释。

## 3. 其他预置资源

主题 `trime.yaml`、`default.yaml`、`punctuation.yaml`、`symbols.yaml`、`key_bindings.yaml`、`stroke` 词库等，来自 trime 上游及其子模块（GPL-3.0）。
