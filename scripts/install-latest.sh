#!/usr/bin/env bash
# 一键从 GitHub 下载最新 debug APK 并安装到手机
#
# 用法：
#   ./scripts/install-latest.sh                # 下载最新 + 安装
#   ./scripts/install-latest.sh --no-install   # 只下载不装
#
# 流程：
#   1. 从 GitHub raw 下载 dist/latest.json 拿到最新构建信息
#   2. 下载对应 APK 到 ~/Desktop/teleprompter-latest.apk
#   3. 用本地 adb 推到手机安装

set -e
REPO_OWNER="lvkaik"
REPO_NAME="teleprompter"
BASE="https://raw.githubusercontent.com/$REPO_OWNER/$REPO_NAME/main/dist"
APK_DEST="$HOME/Desktop/teleprompter-latest.apk"
META_DEST="$HOME/Desktop/teleprompter-latest.json"

DO_INSTALL=1
for arg in "$@"; do
  case "$arg" in
    --no-install) DO_INSTALL=0 ;;
  esac
done

echo "=========================================="
echo " Teleprompter 一键下载 + 安装"
echo "=========================================="

echo "[1/4] 获取最新构建信息 ..."
curl -fsSL --max-time 15 "$BASE/latest.json" -o "$META_DEST" || {
  echo "❌ 下载 latest.json 失败，请确认 CI 已跑通且 dist/latest.json 存在。"
  echo "   可以访问 https://github.com/$REPO_OWNER/$REPO_NAME/tree/main/dist 看是否有产物"
  exit 1
}
cat "$META_DEST"
echo ""

APK_URL=$(python3 -c "import json; print(json.load(open('$META_DEST'))['download_url'])" 2>/dev/null) || {
  echo "❌ latest.json 格式不对"
  cat "$META_DEST"
  exit 1
}
COMMIT=$(python3 -c "import json; print(json.load(open('$META_DEST'))['commit'])")
SIZE=$(python3 -c "import json; print(json.load(open('$META_DEST'))['size'])")
echo "  最新构建: $COMMIT  ($SIZE)"
echo "  APK URL: $APK_URL"

echo "[2/4] 下载 APK 到 $APK_DEST ..."
curl -fL --max-time 180 -o "$APK_DEST" "$APK_URL" 2>&1 | tail -3
echo "  下载完成: $(ls -lh "$APK_DEST" | awk '{print $5}')"

if [ "$DO_INSTALL" -eq 0 ]; then
  echo "[3/4] 跳过安装（--no-install）"
  echo "[4/4] 跳过"
  echo "✅ 已下载到: $APK_DEST"
  exit 0
fi

echo "[3/4] 检测 adb ..."
ADB=""
if command -v adb >/dev/null 2>&1; then
  ADB="$(command -v adb)"
elif [ -x "$HOME/adb-tools/adb" ]; then
  ADB="$HOME/adb-tools/adb"
fi
if [ -z "$ADB" ]; then
  echo "❌ 找不到 adb。请先安装 platform-tools 或运行 capture-crash.sh 安装。"
  exit 1
fi
echo "  adb: $ADB"

echo "[4/4] 检查设备 + 安装 ..."
DEV_COUNT=$($ADB devices | grep -c -E "device$" || true)
if [ "$DEV_COUNT" -lt 1 ]; then
  echo "❌ 没有可用设备。USB 调试没开？"
  exit 1
fi
echo "  设备: $($ADB devices | grep -E "device$")"

# 卸载再装（避免签名冲突）
$ADB uninstall com.yourname.teleprompter 2>/dev/null || true
echo "  安装中 ..."
$ADB install -r "$APK_DEST"
echo ""
echo "=========================================="
echo " ✅ 安装完成！"
echo " 在手机上打开 \"悬浮提词器\" App"
echo "=========================================="