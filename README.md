# Web to Notion

把内容保存到 Notion 的 Android 应用 + Chrome 插件。**无后端**——客户端直连 Notion API。

## 功能

- **便签记录**：快速记录文本便签，自动同步到 Notion
- **URL 全文保存**：浏览器分享链接，抓取网页正文+图片保存到 Notion（Phase 2）
- **分享接收**：Android 分享菜单直接发送内容
- **AI 格式化**：可选的 Groq AI 整理格式（Phase 3）
- **离线支持**：断网时本地缓存，联网后自动同步

## 架构

```
┌─────────────┐     ┌──────────────┐
│  Android    │     │   Chrome     │
│    APK      │     │  Extension   │
│  (Kotlin)   │     │    (TS)      │
└──────┬──────┘     └──────┬───────┘
       │                    │
       └────────┬───────────┘
                │
                ▼
        ┌───────────────┐
        │  Notion API   │
        │ api.notion.com │
        └───────────────┘
```

- **无 Cloudflare Worker**——Android OkHttp 不受 CORS 限制，Chrome 扩展有 host_permissions
- Notion 是数据源，客户端只是视图
- 零部署、零成本、零运维

## 技术栈

### Android APK
- Kotlin 1.9.22 + Jetpack Compose + Material3
- 华为 HarmonyOS Design 设计规范（色彩/字体/圆角/间距 Token）
- Room 2.6.1（本地缓存）+ WorkManager 2.10（离线同步）
- Retrofit 2.11 + OkHttp 4.12（Notion API 客户端）
- kotlinx.serialization 1.7.3
- jsoup 1.18.2（网页解析，Phase 2）
- DataStore Preferences（配置存储）

### Chrome 插件（Phase 4）
- Manifest V3 + TypeScript + Vite
- @mozilla/readability（网页解析）
- turndown（HTML→Markdown）

## 项目结构

```
web-to-notion/
├── android/          # Android APK
│   ├── app/src/main/kotlin/io/trae/webtonotion/
│   │   ├── data/     # Room + Retrofit + DataStore
│   │   ├── work/     # WorkManager (SaveNoteWorker)
│   │   ├── ui/       # Compose 主题 + 页面 + 组件
│   │   └── util/     # 工具类
│   └── .github/workflows/build-apk.yml
├── extension/        # Chrome 插件 (Phase 4)
└── docs/             # 架构文档
```

## 开发指南

### 前置条件
- JDK 17
- Android SDK（compileSdk 34）
- Android Studio（推荐）或命令行 Gradle

### 本地构建
```bash
cd android
./gradlew assembleRelease
# APK 输出: app/build/outputs/apk/release/app-release.apk
```

### CI 构建
推送代码到 GitHub，Actions 自动构建 APK 并上传 artifact。

## Notion 配置

1. 访问 https://www.notion.so/my-integrations 创建 Internal Integration
2. 复制 API Token
3. 在 Notion 创建一个 Database，属性：
   - Title (title)
   - Type (select: note / webpage)
   - URL (url)
   - Tags (multi_select)
   - Created (date)
4. 把 Integration 连接到该 Database（页面右上角 → Connect to）
5. 在 APK 设置页输入 Token 和 Database ID

## 设计参考

- 华为 HarmonyOS Design 规范：`D:\Dev\Github\OpenCode\.opencode\skills\HuaweiDesign.md`
- 品牌色 `#0A59F7`、HarmonyOS Sans 字体、卡片圆角 12dp、8dp 间距网格
