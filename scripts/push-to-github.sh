#!/usr/bin/env bash
#
# 一键推送到 GitHub，触发 CI 自动打包
#
# 用法：
#   ./scripts/push-to-github.sh <github-username> [repo-name]
#
# 例：
#   ./scripts/push-to-github.sh lvkai teleprompter
#
set -euo pipefail

USERNAME="${1:-}"
REPO="${2:-teleprompter}"

if [ -z "$USERNAME" ]; then
    echo "用法: $0 <github-username> [repo-name]"
    echo ""
    echo "例:"
    echo "  $0 lvkai teleprompter"
    exit 1
fi

echo "==============================================="
echo "  推送代码到 GitHub 触发 CI"
echo "==============================================="
echo ""
echo "目标: https://github.com/$USERNAME/$REPO"
echo ""

# ===== 1. 检查是否已经是 git 仓库 =====
if [ ! -d ".git" ]; then
    echo "[1/6] git init"
    git init -b main
else
    echo "[1/6] 已存在 .git，跳过 init"
fi

# ===== 2. 配置 user（只在本仓库，不改全局）=====
echo "[2/6] 配置 git user（仅本仓库）"
git config user.name "$USERNAME"
git config user.email "${USERNAME}@users.noreply.github.com"

# ===== 3. 添加所有文件 =====
echo "[3/6] git add ."
git add .

# ===== 4. 提交 =====
echo "[4/6] git commit"
if git diff --cached --quiet; then
    echo "没有改动，跳过 commit"
else
    git commit -m "feat: 初始版本

- 悬浮窗提词（拖动 + 缩放）
- 文稿编辑（Room 持久化）
- AI 提词（MiniMax ASR + 编辑距离匹配）
- realme UI 7.0 / Android 16 适配
- GitHub Actions CI 自动打包"
fi

# ===== 5. 检查 remote =====
echo "[5/6] 检查 remote"
if ! git remote get-url origin > /dev/null 2>&1; then
    echo "添加 remote: git@github.com:$USERNAME/$REPO.git"
    git remote add origin "git@github.com:$USERNAME/$REPO.git"
fi
git remote -v

# ===== 6. 推送 =====
echo ""
echo "[6/6] git push -u origin main"
echo ""
echo "⚠️  重要：如果推送失败 'Repository not found'，请先到 GitHub 创建空仓库"
echo "    访问: https://github.com/new  (名字填: $REPO, 不要勾 README)"
echo ""
echo "按回车继续推送，或 Ctrl+C 取消..."
read -r

git push -u origin main

echo ""
echo "==============================================="
echo "  ✅ 推送完成！"
echo "==============================================="
echo ""
echo "现在去 GitHub 触发 CI："
echo "  https://github.com/$USERNAME/$REPO/actions"
echo ""
echo "或者等 30 秒让 push 触发器自动开始（默认会跑）"
echo ""
echo "等约 8 分钟后下载 APK："
echo "  Actions → 选 build run → 底部 Artifacts → teleprompter-debug-apk.zip"
echo "==============================================="