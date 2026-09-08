# 本地开发与 AI 服务连接

开发电脑没有 Redis、MinerU 或 Qwen 时，通过 SSH 将 `my-5090server` 上的服务转发到本机回环地址。业务代码始终访问 `127.0.0.1`，不会直接暴露服务器服务端口。

## 1. 建立 SSH 转发

在单独的 PowerShell 窗口运行：

```powershell
ssh -N -T `
  -o ExitOnForwardFailure=yes `
  -o ServerAliveInterval=30 `
  -o ServerAliveCountMax=3 `
  -L 127.0.0.1:6379:127.0.0.1:6379 `
  -L 127.0.0.1:8001:127.0.0.1:8001 `
  -L 127.0.0.1:18081:127.0.0.1:8081 `
  my-5090server
```

端口对应关系：

| 服务 | 开发电脑访问地址 | 服务器实际地址 |
| --- | --- | --- |
| Redis | `127.0.0.1:6379` | `127.0.0.1:6379` |
| MinerU | `http://127.0.0.1:8001` | `http://127.0.0.1:8001` |
| Qwen | `http://127.0.0.1:18081/v1` | `http://127.0.0.1:8081/v1` |

可用下面的命令检查本机转发端口：

```powershell
Test-NetConnection 127.0.0.1 -Port 6379
Test-NetConnection 127.0.0.1 -Port 8001
Test-NetConnection 127.0.0.1 -Port 18081
```

## 2. 配置覆盖

开发环境的默认值已经写在 `admin/src/main/resources/application.yml` 中，一般无需设置环境变量。如端口不同，可在启动后端前覆盖：

```powershell
$env:REDIS_HOST = "127.0.0.1"
$env:REDIS_PORT = "6379"
$env:MINERU_API_URL = "http://127.0.0.1:8001"
$env:QWEN_BASE_URL = "http://127.0.0.1:18081/v1"
```

真实密码和密钥只应放在本机环境变量或服务器的 `/etc/default/agent-material` 中，不要提交到 Git。

## 3. 服务器部署

服务器启动时使用 `deploy/ubuntu/application-prod.yml`。生产配置默认直连服务器本机服务：

- Redis：`127.0.0.1:6379`
- MinerU：`http://127.0.0.1:8001`
- Qwen：`http://127.0.0.1:8081/v1`

因此，从 GitHub 拉取代码到服务器后不需要 SSH 转发。若服务运行在容器或其它服务器上，通过 `REDIS_HOST`、`MINERU_API_URL` 和 `QWEN_BASE_URL` 环境变量覆盖即可。
