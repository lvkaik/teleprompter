#!/usr/bin/env bash
#
# 一键配置 SSH key 用于 GitHub
#
# 智能检测：
#   - 如果已有 ed25519/rsa key，问你要不要复用
#   - 如果没有，生成新的
#   - 自动配置 ~/.ssh/config 走 443（解决公司网络/校园网问题）
#   - 测试连接
#
# 用法：
#   ./scripts/setup-ssh-github.sh
#

set -euo pipefail

KEY_DIR="$HOME/.ssh"
CONFIG_FILE="$KEY_DIR/config"
GITHUB_KEY_TYPE="ed25519"   # 优先 ed25519，更现代

echo "==============================================="
echo "  GitHub SSH key 配置助手"
echo "==============================================="
echo ""

# ===== 1. 检测现成 key =====
HAS_KEY=false
KEY_PATH=""
if [ -f "$KEY_DIR/id_ed25519" ]; then
    HAS_KEY=true
    KEY_PATH="$KEY_DIR/id_ed25519"
elif [ -f "$KEY_DIR/id_rsa" ]; then
    HAS_KEY=true
    KEY_PATH="$KEY_DIR/id_rsa"
fi

if [ "$HAS_KEY" = true ]; then
    echo "✅ 检测到现成 SSH key：$KEY_PATH"
    echo ""
    echo "公钥内容："
    echo "----------------------------------------"
    cat "${KEY_PATH}.pub"
    echo "----------------------------------------"
    echo ""
    echo "指纹：$(ssh-keygen -lf "${KEY_PATH}.pub")"
    echo ""
    read -p "复用这把 key？[Y/n] " reuse
    reuse=${reuse:-Y}
    if [[ ! "$reuse" =~ ^[Yy]$ ]]; then
        echo "跳过复用，请手动指定别的 key 路径（或回车重新生成）"
        read -p "请输入 key 路径（如 ~/.ssh/id_ed25519_github）: " custom
        if [ -n "$custom" ] && [ -f "$custom" ]; then
            KEY_PATH="$custom"
        else
            HAS_KEY=false
        fi
    fi
fi

# ===== 2. 没现成 key 就生成新的 =====
if [ "$HAS_KEY" = false ]; then
    echo "🛠  生成新 SSH key（ed25519）"
    echo ""
    read -p "请输入 GitHub 邮箱（用于 key 注释）: " EMAIL
    if [ -z "$EMAIL" ]; then
        EMAIL="lvkai@users.noreply.github.com"
        echo "使用默认: $EMAIL"
    fi

    KEY_PATH="$KEY_DIR/id_ed25519_github"

    mkdir -p "$KEY_DIR"
    chmod 700 "$KEY_DIR"

    ssh-keygen -t ed25519 -C "$EMAIL" -f "$KEY_PATH" -N ""

    echo ""
    echo "✅ 已生成: $KEY_PATH"
    echo ""
fi

# ===== 3. 配置 ~/.ssh/config =====
echo ""
echo "==============================================="
echo "  配置 ~/.ssh/config"
echo "==============================================="

mkdir -p "$KEY_DIR"
touch "$CONFIG_FILE"
chmod 600 "$CONFIG_FILE"

# 检查是否已有 github 块
if grep -q "^Host github.com" "$CONFIG_FILE" 2>/dev/null; then
    echo "⚠️  检测到已有 github.com 配置，将跳过"
else
    cat >> "$CONFIG_FILE" <<EOF

# ===== GitHub（由 setup-ssh-github.sh 生成）=====
Host github.com
    HostName ssh.github.com
    User git
    Port 443
    IdentityFile $KEY_PATH
    IdentitiesOnly yes
    PreferredAuthentications publickey
    AddKeysToAgent yes
EOF
    echo "✅ 已追加 github 配置到 $CONFIG_FILE"
fi

# ===== 4. 启动 ssh-agent + 添加 key =====
echo ""
echo "==============================================="
echo "  启动 ssh-agent 并添加 key"
echo "==============================================="

if [ -z "${SSH_AUTH_SOCK:-}" ]; then
    eval "$(ssh-agent -s)" > /dev/null
    echo "已启动 ssh-agent"
else
    echo "ssh-agent 已在运行"
fi

ssh-add "$KEY_PATH" 2>&1 | head -3
echo ""

# ===== 5. 测试连接 =====
echo "==============================================="
echo "  测试 GitHub 连接"
echo "==============================================="
echo ""
echo "首次连接会提示 'Are you sure you want to continue connecting?'"
echo "输入 yes 然后回车"
echo ""

# 用 ssh-keyscan 把 GitHub 公钥预加到 known_hosts
ssh-keyscan -t rsa,ecdsa,ed25519 ssh.github.com >> "$KEY_DIR/known_hosts" 2>/dev/null || true

# 测试
ssh -T git@github.com 2>&1 | head -5 || true

echo ""
echo "==============================================="
echo "  📋 接下来要做的事"
echo "==============================================="
echo ""
echo "把下面这段公钥复制到 GitHub："
echo ""
echo "----------------------------------------"
cat "${KEY_PATH}.pub"
echo "----------------------------------------"
echo ""
echo "1. 打开 https://github.com/settings/keys"
echo "2. 点 'New SSH key'"
echo "3. Title:    Mac mini 提词器"
echo "4. Key:      粘贴上面整段"
echo "5. 勾 'Allow write access'"
echo "6. 点 'Add SSH key'"
echo ""
echo "完成后重新跑一次："
echo "  ssh -T git@github.com"
echo ""
echo "如果看到 'Hi 你的用户名! You've been successfully authenticated' 就 OK 了"
echo ""
echo "或者直接跑推送脚本："
echo "  cd /Users/lvkai/Documents/ticiqi"
echo "  ./scripts/push-to-github.sh 你的用户名 teleprompter"
echo ""