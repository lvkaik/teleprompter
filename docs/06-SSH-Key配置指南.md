# SSH Key 配置指南（GitHub 认证）

> 目标：用 SSH key 让 `git push` 不再输密码

---

## ⚡ 一句话总结

你的机器上**已经有一把 SSH key**（`~/.ssh/id_ed25519`，6 月 17 日生成的，注释是 `lvkai-mac-mini`）。**极有可能就是给 GitHub 用的**，但 GitHub 上还没配。

最快的方案：

### A. 复用现成 key（推荐，30 秒）

#### 第 1 步 · 把公钥粘到 GitHub

运行这一行，复制输出的**整段**内容：

```bash
cat ~/.ssh/id_ed25519.pub
```

会得到类似：

```
ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIP2pONZ5vnwDnhXS5LzpWRBsnrrmhO0A+8/9Ip+/3oBV lvkai-mac-mini
```

#### 第 2 步 · 添加到 GitHub

浏览器打开：**https://github.com/settings/keys**

点右上角 **「New SSH key」** 按钮，填：

| 字段 | 填什么 |
|------|--------|
| Title | `Mac mini 提词器` |
| Key type | `Authentication Key` |
| Key | 粘贴刚才复制的整段 |

**勾选**「Allow write access」→ 点 **Add SSH key**。

如果 GitHub 让你输密码确认，输入 GitHub 登录密码。

#### 第 3 步 · 测试

```bash
ssh -T git@github.com
```

**期望输出**：

```
Hi 你的用户名! You've been successfully authenticated, but GitHub does not provide shell access.
```

看到 `Hi xxx` 就 OK 了。

#### 第 4 步 · 推送代码

```bash
cd /Users/lvkai/Documents/ticiqi
./scripts/push-to-github.sh 你的GitHub用户名 teleprompter
```

---

### B. 用一键脚本（含 SSH config 优化）

如果 A 走完仍然连不上（公司网络常踩），用脚本强制走 443：

```bash
cd /Users/lvkai/Documents/ticiqi
./scripts/setup-ssh-github.sh
```

脚本做的事：

1. 检测现成 key，问你是否复用
2. 没有就生成一把新的
3. 写入 `~/.ssh/config`，**强制走 ssh.github.com:443**（绕开 22 端口封锁）
4. 启动 ssh-agent 并加载 key
5. 把 GitHub 公钥加进 known_hosts（避免 "Host key verification failed"）
6. 测试连接

最后还是要把公钥粘到 GitHub（同 A 的步骤 1-2），脚本会打印出来给你。

---

## 🔍 我怎么知道现在的 key 是给什么用的？

跑：

```bash
ssh-keygen -lf ~/.ssh/id_ed25519.pub
```

你机器上的输出：

```
256 SHA256:1fa2QRRRogNeTho53noGyzWwL72uZfDqFkPBawA13iU lvkai-mac-mini (ED25519)
```

注释 `lvkai-mac-mini` 是机器名，没说用途，所以不确定。

**判断方法**：

```bash
# 看 GitHub 上你已加的 key
# 打开 https://github.com/settings/keys
# 对比指纹，找 SHA256:1fa2QRRRogNeTho53noGyzWwL72uZfDqFkPBawA13iU
```

- 如果指纹一致 → 已经在 GitHub 上，直接复用即可
- 如果没找到 → 按 A 走一遍添加

---

## 🛠 故障排查

### 报错：`Permission denied (publickey)`

| 原因 | 解决 |
|------|------|
| 公钥没加到 GitHub | 走 A 第 1-2 步 |
| 加了但 ssh-agent 没加载 | `ssh-add ~/.ssh/id_ed25519` |
| 多 key 混淆 | 在 `~/.ssh/config` 加 `IdentitiesOnly yes` |
| 公司网络封了 22 | 走 443（用脚本 B） |

### 报错：`Host key verification failed`

首次连会问，把 `ssh.github.com` 公钥加进 `known_hosts`：

```bash
ssh-keyscan -t rsa,ecdsa,ed25519 ssh.github.com >> ~/.ssh/known_hosts
```

一键脚本已自动做。

### 报错：`Connection timed out`

走 443：

```bash
# 编辑 ~/.ssh/config，确保是：
Host github.com
    HostName ssh.github.com
    Port 443
```

一键脚本已自动做。

### 我有多个 GitHub 账号（个人 + 公司）

那就别用默认 key，每账号一把，配置 `Host` 别名：

```sshconfig
Host github-personal
    HostName ssh.github.com
    Port 443
    User git
    IdentityFile ~/.ssh/id_ed25519_personal
    IdentitiesOnly yes

Host github-work
    HostName ssh.github.com
    Port 443
    User git
    IdentityFile ~/.ssh/id_ed25519_work
    IdentitiesOnly yes
```

然后 push 的时候用别名：

```bash
git remote add origin git@github-personal:你的用户名/teleprompter.git
```

---

## 🔐 安全小贴士

| 项 | 建议 |
|----|------|
| Key 备份 | `~/.ssh/id_ed25519`（私钥）**绝不外传**，但要自己备份到 1Password / U 盘 |
| 私钥权限 | `chmod 600 ~/.ssh/id_ed25519` |
| 公钥权限 | `chmod 644 ~/.ssh/id_ed25519.pub` |
| ssh-agent | Mac 开机自动启动，不要关闭 |
| 废弃 key | GitHub → Settings → Keys → Delete；本地 `rm ~/.ssh/id_ed25519` |

---

## 📋 完整流程回顾

```
1. cat ~/.ssh/id_ed25519.pub             ← 复制公钥
2. 打开 https://github.com/settings/keys   ← 添加 SSH key
3. ssh -T git@github.com                 ← 看到 Hi xxx 就 OK
4. ./scripts/push-to-github.sh            ← 推送
```

总计 ~2 分钟。

---

## ⚠️ 替代方案：HTTPS + Personal Access Token

如果你不想用 SSH 也没问题：

1. GitHub → Settings → Developer settings → Personal access tokens → **Tokens (classic)**
2. Generate new token
3. 勾 `repo` 权限
4. 复制 token（只显示一次！）
5. 把 `push-to-github.sh` 里 remote 改成 HTTPS：
   ```bash
   git remote set-url origin https://github.com/你的用户名/teleprompter.git
   ```
6. push 时把 token 当密码粘进去（Mac 会存进钥匙串，以后不用再输）

HTTPS 缺点：每次 push 用钥匙串；优点：简单。

**推荐 SSH**，配置一次永久。