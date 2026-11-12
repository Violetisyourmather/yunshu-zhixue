# HTTPS 证书目录

本目录用于存放 `deploy/docker-compose.https.yml` 挂载的证书文件。证书与私钥**不要提交到版本库**（已在 `.gitignore` 中忽略）。

## 文件要求

| 文件 | 说明 |
|---|---|
| `fullchain.pem` | 服务器证书 + 中间证书链（PEM） |
| `privkey.pem` | 证书对应私钥（PEM，无密码） |

## 生产证书

从云厂商 SSL 证书服务或 Let's Encrypt 获取证书后，将文件重命名为上述名称放入本目录，然后启动：

```bash
docker compose -f deploy/docker-compose.prod.yml \
  -f deploy/docker-compose.https.yml up -d --build
```

## 本地验证（自签名）

仅用于本机验证 HTTPS 配置，浏览器会提示证书不受信任：

```bash
openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
  -keyout privkey.pem -out fullchain.pem \
  -subj "/CN=localhost"
```

或将证书放在其他目录并指定：

```bash
TLS_CERT_DIR=/path/to/certs docker compose \
  -f deploy/docker-compose.prod.yml \
  -f deploy/docker-compose.https.yml up -d --build
```

## 使用负载均衡 / SLB

如果 TLS 在云负载均衡或外层 Nginx 终止，不要叠加 `docker-compose.https.yml`，继续使用 `deploy/nginx.conf`（监听 80），由外层把 HTTPS 请求转发到 `WEB_PORT`。
