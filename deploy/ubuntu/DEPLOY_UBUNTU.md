# Ubuntu 部署说明

这个项目是 Spring Boot 2.5 + Maven 多模块后端、Vue 2 + Vue CLI 前端，依赖 MySQL、Redis、MinerU 和 Qwen。Web 项目本身不直接加载 GPU 模型，而是通过 HTTP 调用服务器上已有的 MinerU 与 Qwen 服务。

## 1. 服务器软件

建议后端使用 JDK 8 或 11；如果 Ubuntu 软件源没有 JDK 11，可以先用 JDK 17 做启动验证。前端只在构建时需要 Node，建议用 Node 16/18 构建，构建后由 Nginx 托管静态文件。

```bash
sudo apt update
sudo apt install -y git curl unzip nginx mysql-server redis-server maven
sudo apt install -y openjdk-11-jdk || sudo apt install -y openjdk-17-jdk
```

Node 建议用 nvm 安装一个较旧的 LTS 版本：

```bash
nvm install 16
nvm use 16
node -v
npm -v
```

如果服务器在国内网络环境，可以配置镜像：

```bash
npm config set registry https://registry.npmmirror.com
```

## 2. 创建运行用户和目录

```bash
sudo useradd -r -m -d /opt/agentForMaterial -s /usr/sbin/nologin agentmat || true
sudo mkdir -p /opt/agentForMaterial/{backend,config,uploadPath}
sudo mkdir -p /var/www/agentForMaterial
sudo chown -R agentmat:agentmat /opt/agentForMaterial
```

## 3. 初始化数据库

```bash
sudo mysql
```

在 MySQL 里执行：

```sql
CREATE DATABASE aiforscience DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'aiforscience'@'localhost' IDENTIFIED BY '替换成强密码';
CREATE USER 'aiforscience'@'127.0.0.1' IDENTIFIED BY '替换成强密码';
GRANT ALL PRIVILEGES ON aiforscience.* TO 'aiforscience'@'localhost';
GRANT ALL PRIVILEGES ON aiforscience.* TO 'aiforscience'@'127.0.0.1';
FLUSH PRIVILEGES;
```

回到项目目录后导入脚本：

```bash
mysql -u aiforscience -p --default-character-set=utf8mb4 aiforscience < sql/ry_20250522.sql
mysql -u aiforscience -p --default-character-set=utf8mb4 aiforscience < sql/quartz.sql
mysql -u aiforscience -p --default-character-set=utf8mb4 aiforscience < deploy/ubuntu/material_extension.sql
```

不要在正式库直接执行 `sql/2026.1.15新增标签.sql`，它会删除并重建 `sys_user`，容易清空用户数据。`sql/sys_oper_log.sql` 是历史操作日志，干净部署通常不需要导入。

## 4. 生产配置

```bash
sudo cp deploy/ubuntu/application-prod.yml /opt/agentForMaterial/config/application-prod.yml
sudo cp deploy/ubuntu/agent-material.env.example /etc/default/agent-material
sudo nano /etc/default/agent-material
```

至少要修改这些值：

```bash
DB_PASSWORD=你的数据库强密码
TOKEN_SECRET=一个足够长的随机字符串
DRUID_PASSWORD=一个强密码
BAIDU_API_KEY=百度OCR Key
BAIDU_API_SECRET=百度OCR Secret
ALIYUN_OSS_KEY_ID=阿里云OSS Key
ALIYUN_OSS_KEY_SECRET=阿里云OSS Secret
ALIYUN_OSS_BUCKET=OSS Bucket
```

AI 服务在服务器上的默认地址为：

```bash
MINERU_API_URL=http://127.0.0.1:8001
QWEN_BASE_URL=http://127.0.0.1:8081/v1
QWEN_MODEL=qwen3.8-27b
```

服务器直接访问本机 Redis、MinerU 和 Qwen，不需要建立 SSH 转发。如果端口或服务地址不同，只需修改 `/etc/default/agent-material`，不需要修改代码。

当前仓库的 `admin/src/main/resources/application.yml` 里已有云服务密钥样式配置，正式部署建议改到 `/etc/default/agent-material`，并考虑轮换已经暴露过的密钥。

## 5. 构建后端

```bash
mvn clean package -DskipTests
sudo cp admin/target/ruoyi-admin.jar /opt/agentForMaterial/backend/ruoyi-admin.jar
sudo chown -R agentmat:agentmat /opt/agentForMaterial
```

## 6. 构建前端

```bash
cd ui
npm install
npm run build:prod
cd ..
sudo rsync -av --delete ui/dist/ /var/www/agentForMaterial/
```

生产环境前端接口前缀是 `/prod-api`，Nginx 配置会把它反代到后端 `127.0.0.1:8080`。

## 7. 启动后端 systemd 服务

```bash
sudo cp deploy/ubuntu/agent-material.service /etc/systemd/system/agent-material.service
sudo systemctl daemon-reload
sudo systemctl enable --now agent-material
sudo systemctl status agent-material
journalctl -u agent-material -f
```

后端健康验证：

```bash
curl http://127.0.0.1:8080/captchaImage
```

能返回 JSON 就说明 Spring Boot、MySQL、Redis 基本连通。文档解析和信息提取还应分别确认 MinerU、Qwen 服务已启动并监听上述地址。

## 8. 启用 Nginx

```bash
sudo cp deploy/ubuntu/nginx-agent-material.conf /etc/nginx/sites-available/agent-material
sudo ln -sf /etc/nginx/sites-available/agent-material /etc/nginx/sites-enabled/agent-material
sudo nginx -t
sudo systemctl reload nginx
```

浏览器访问服务器 IP 或域名。默认账号是：

```text
admin / admin123
```

首次登录后立刻修改默认密码。

## 9. 防火墙建议

只对外开放 SSH、HTTP/HTTPS，不要把 MySQL 和 Redis 暴露到公网或校园网。

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

如果有域名，再用 Certbot 配 HTTPS。

## 10. 常见问题

`Unknown column status in field list`：没有导入 `deploy/ubuntu/material_extension.sql`，或者旧库的 `sys_document` 表结构缺字段。

前端登录接口 404：检查 Nginx 里 `/prod-api/` 的 `proxy_pass`，以及后端是否在 `8080` 端口正常运行。

验证码或登录失败：检查 Redis 是否启动，`/etc/default/agent-material` 的 Redis 配置是否和实际一致。

MinerU 或 Qwen 调用失败：先用 `ss -lntp | grep -E ':(8001|8081)'` 检查服务监听端口，再核对 `/etc/default/agent-material` 中的 `MINERU_API_URL` 和 `QWEN_BASE_URL`。

上传失败：检查 `/opt/agentForMaterial/uploadPath` 目录权限，以及 `spring.servlet.multipart` 和 Nginx `client_max_body_size` 限制。

OCR 或 OSS 失败：检查百度 OCR、阿里云 OSS 环境变量是否填入，并确认服务器能访问外网。
