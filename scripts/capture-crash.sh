#!/usr/bin/env bash
# 抓取 Teleprompter App 的崩溃栈
# 用法：
#   1. USB 连接手机，打开开发者模式 + USB 调试
#   2. 执行本脚本
#   3. 在手机上触发 App 闪退
#   4. 按 Ctrl+C 结束
#   5. 崩溃日志输出到 ~/Desktop/teleprompter-crash.log
#
# 注意：脚本会自动过滤出我们的 App 相关 tag，
# 包括 CrashReporter 写入的 Java 异常、ColorOS 包装提示等。

set -u
LOG="$HOME/Desktop/teleprompter-crash.log"
PKG="com.yourname.teleprompter"

echo "=========================================="
echo " Teleprompter 崩溃栈抓取"
echo " 输出: $LOG"
echo " 包名: $PKG"
echo "=========================================="
echo ""
echo "[1/4] 检查 adb ..."
# 多源探测 adb：PATH -> ~/adb-tools/adb -> Android Studio 自带
ADB=""
if command -v adb >/dev/null 2>&1; then
  ADB="$(command -v adb)"
elif [ -x "$HOME/adb-tools/adb" ]; then
  ADB="$HOME/adb-tools/adb"
elif [ -x "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/adb" ]; then
  ADB="/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/adb"
fi
if [ -z "$ADB" ]; then
  echo "❌ 找不到 adb。请先安装 Android Platform Tools。"
  echo "   macOS:  brew install android-platform-tools"
  echo "   或者直接解压 platform-tools 到 ~/adb-tools/ 并把 adb 加到 PATH"
  echo "   Win/Linux: https://developer.android.com/tools/releases/platform-tools"
  exit 1
fi
echo "  找到 adb: $ADB"
echo "  版本: $($ADB --version | head -1)"

echo "[2/4] 检查设备连接 ..."
$ADB devices
DEV_COUNT=$($ADB devices | grep -c -E "device$" || true)
if [ "$DEV_COUNT" -lt 1 ]; then
  echo "❌ 没有可用设备。"
  echo "   请确认 USB 调试已开启，并允许此电脑调试。"
  exit 1
fi

echo "[3/4] 清空 logcat buffer ..."
$ADB logcat -c

echo "[4/4] 开始抓取日志。手机端现在可以触发崩溃 ..."
echo "      按 Ctrl+C 结束抓取。"
echo ""

# 抓 AndroidRuntime + CrashReporter + system_server 等关键 tag
$ADB logcat \
  AndroidRuntime:E \
  CrashReporter:V \
  "*:F" \
  > "$LOG" 2>&1 &
LOG_PID=$!

trap "kill $LOG_PID 2>/dev/null; wait $LOG_PID 2>/dev/null; echo ''; echo '✅ 已停止抓取';" INT TERM

# 显示实时进度（每 5 秒闪一次）
i=0
while kill -0 $LOG_PID 2>/dev/null; do
  i=$((i+1))
  printf "\r  ... 已运行 %d 秒（按 Ctrl+C 结束）" $((i*5))
  sleep 5
done

echo ""
echo "=========================================="
echo " 日志大小: $(wc -c < "$LOG") 字节"
echo " 输出位置: $LOG"
echo "=========================================="
echo ""
echo "=== 前 60 行预览 ==="
head -60 "$LOG"