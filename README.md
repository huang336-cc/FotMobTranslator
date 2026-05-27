# FotMob 翻译器 (FotMobTranslator)

一个 Android 应用，通过调用 FotMob API 获取足球数据，并使用 Google 翻译实时将英文内容翻译为中文。

## 功能

- **实时比分** — 查看今日/昨日/明日比赛，球队名和联赛名自动翻译
- **联赛与积分榜** — 浏览所有联赛，查看积分榜，球队名自动翻译
- **足球新闻** — 获取最新足球新闻，标题和摘要自动翻译
- **球员数据** — 查看球员信息、评分、进球、助攻等数据

## 技术栈

- Kotlin + Android SDK 34
- Material Design 3 (深色主题)
- Retrofit2 + OkHttp (网络请求)
- Coil (图片加载)
- Navigation Component (页面导航)
- Coroutines + LiveData (异步处理)
- Jsoup (新闻 XML 解析)

## 翻译引擎

使用 Google 翻译免费接口，无需 API Key：
- 批量翻译（每批最多 20 条）
- 速率限制（每秒 5 次）
- 自动重试（最多 3 次，指数退避）
- 双层缓存（内存 LRU + SharedPreferences 持久化）

## 如何编译

### 前提条件

- Android Studio Hedgehog (2023.1.1) 或更新版本
- JDK 17
- Android SDK 34

### 步骤

1. 克隆项目到本地
2. 用 Android Studio 打开项目根目录
3. 等待 Gradle 同步完成
4. 连接 Android 设备或启动模拟器
5. 点击 Run (▶) 运行

### 命令行编译

```bash
# Linux / macOS
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

APK 输出路径: `app/build/outputs/apk/debug/app-debug.apk`

## 项目结构

```
app/src/main/java/com/fotmob/translator/
├── FotMobApplication.kt          # Application 入口，初始化网络和翻译
├── MainActivity.kt               # 主界面，底部导航
├── data/
│   ├── FotMobApi.kt              # Retrofit API 接口定义
│   ├── model/                    # 数据模型 (Match, League, Player, Standing, NewsItem)
│   └── repository/               # 数据仓库，解析 JSON/XML
├── translate/
│   ├── GoogleTranslator.kt       # Google 翻译引擎
│   └── TranslationCache.kt       # 翻译缓存
├── ui/
│   ├── matches/                  # 比分页面
│   ├── leagues/                  # 联赛 + 积分榜页面
│   ├── news/                     # 新闻页面
│   └── players/                  # 球员页面
└── util/
    └── DateUtils.kt              # 日期工具
```

## 注意事项

- FotMob API 为非官方接口，可能随时变更
- Google 翻译接口为非官方免费接口，高频使用可能被限流
- 首次加载需要翻译，后续从缓存读取会很快
- 需要网络连接才能获取数据和翻译

## 常见联赛 ID 参考

| 联赛 | ID |
|------|-----|
| 英超 (Premier League) | 47 |
| 欧冠 (Champions League) | 42 |
| 西甲 (La Liga) | 87 |
| 德甲 (Bundesliga) | 35 |
| 意甲 (Serie A) | 54 |
| 法甲 (Ligue 1) | 34 |
