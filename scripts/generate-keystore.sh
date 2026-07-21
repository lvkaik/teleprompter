#!/usr/bin/env bash
#
# 生成 release keystore 并打包成 base64，配置到 GitHub Secrets
#
# 用法：
#   chmod +x scripts/generate-keystore.sh
#   ./scripts/generate-keystore.sh
#
# 输出：
#   - release.jks（备份保存，绝对不能丢！）
#   - keystore-base64.txt（复制到 GitHub Secrets）
#

set -euo pipefail

KEYSTORE_FILE="release.jks"
ALIAS="teleprompter"
VALIDITY_DAYS=10000
KEY_SIZE=2048
DNAME="CN=Teleprompter, OU=App, O=YourName, L=City, S=State, C=CN"

echo "==============================================="
echo "  生成 release 签名 keystore"
echo "==============================================="
echo ""

if [ -f "$KEYSTORE_FILE" ]; then
    read -p "$KEYSTORE_FILE 已存在，是否覆盖？[y/N] " ans
    if [ "$ans" != "y" ]; then
        echo "已取消"
        exit 0
    fi
fi

read -s -p "请输入 keystore 密码（建议 16+ 位字母数字混合）: " STORE_PWD
echo ""
read -s -p "请再输入一次: " STORE_PWD2
echo ""
if [ "$STORE_PWD" != "$STORE_PWD2" ]; then
    echo "两次输入不一致，已取消"
    exit 1
fi
if [ ${#STORE_PWD} -lt 8 ]; then
    echo "密码太短（${#STORE_PWD} 位），至少 8 位"
    exit 1
fi

read -s -p "请输入 key 密码（可与 keystore 密码相同）: " KEY_PWD
echo ""
read -s -p "请再输入一次: " KEY_PWD2
echo ""
if [ "$KEY_PWD" != "$KEY_PWD2" ]; then
    echo "两次输入不一致，已取消"
    exit 1
fi

keytool -genkey -v \
    -keystore "$KEYSTORE_FILE" \
    -alias "$ALIAS" \
    -keyalg RSA -keysize "$KEY_SIZE" \
    -validity "$VALIDITY_DAYS" \
    -storepass "$STORE_PWD" \
    -keypass "$KEY_PWD" \
    -dname "$DNAME"

echo ""
echo "✅ keystore 已生成：$KEYSTORE_FILE"
echo ""
echo "⚠️  警告：此文件无法重新生成，丢了就再也签不出同样签名的 APK"
echo "   请立即备份到 1Password / 加密 U 盘 / 安全的云盘"
echo ""

# 输出 SHA256 指纹
echo "===== 指纹信息（用于验证）====="
keytool -list -v -keystore "$KEYSTORE_FILE" -storepass "$STORE_PWD" | grep -E 'SHA1|SHA256|Alias' | head -10
echo ""

# 转 base64
echo "===== base64 编码（将用于 GitHub Secret）====="
B64=$(base64 -i "$KEYSTORE_FILE" | tr -d '\n')
echo ""
echo "已将 base64 写入 keystore-base64.txt"
echo "$B64" > keystore-base64.txt
echo "文件大小：$(wc -c < keystore-base64.txt) 字符"
echo ""

# 写一份 keystore.properties 模板
cat > keystore.properties <<EOF
# 本地 release 签名配置（不要提交到 git！）
storeFile=$KEYSTORE_FILE
storePassword=$STORE_PWD
keyAlias=$ALIAS
keyPassword=$KEY_PWD
EOF

echo "===== 下一步 ====="
echo "1. 把 $KEYSTORE_FILE 备份到安全地方"
echo "2. 把 keystore-base64.txt 里的整段字符粘贴到 GitHub:"
echo "   Repo → Settings → Secrets and variables → Actions → New repository secret"
echo "   Name: RELEASE_KEYSTORE_BASE64"
echo "   Value: 粘贴"
echo ""
echo "3. 再创建以下 secrets："
echo "   RELEASE_KEYSTORE_PASSWORD = $STORE_PWD"
echo "   RELEASE_KEY_ALIAS         = $ALIAS"
echo "   RELEASE_KEY_PASSWORD      = $KEY_PWD"
echo ""
echo "4. 把 keystore.properties 加入 .gitignore（脚本已经做了，见下条提示）"
echo ""

# 自动加入 .gitignore（如果还没在）
GITIGNORE=".gitignore"
if [ -f "$GITIGNORE" ]; then
    grep -qxF "release.jks" "$GITIGNORE" || echo "release.jks" >> "$GITIGNORE"
    grep -qxF "keystore.properties" "$GITIGNORE" || echo "keystore.properties" >> "$GITIGNORE"
    grep -qxF "keystore-base64.txt" "$GITIGNORE" || echo "keystore-base64.txt" >> "$GITIGNORE"
else
    cat > "$GITIGNORE" <<EOF
release.jks
keystore.properties
keystore-base64.txt
EOF
fi

echo "已更新 $GITIGNORE"
echo ""
echo "🎉 完成"