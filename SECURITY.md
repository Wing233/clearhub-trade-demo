# 安全说明

> 这是一个**演示系统**，不是生产交易系统。默认配置为了演示方便，牺牲了鉴权与运维安全。

## 1. 默认安全姿态

| 项 | 现状 | 风险 |
|---|---|---|
| 登录鉴权 | 无 | 任何人可访问页面和 API |
| 账户身份 | `X-Firm-Id` 请求头 | 可伪造，可操作任意演示账户 |
| 重置接口 | `POST /api/demo/reset` 默认开启 | 可清空全部交易数据 |
| Swagger | 默认开启 `/swagger-ui.html` | 暴露全部接口 |
| Actuator | 暴露 `health,info,metrics` | 泄露运行信息 |
| 数据库 | 仅 Compose 内网，未映射宿主端口 | 相对安全 |
| 传输 | 默认 HTTP | 明文 |
| 数据 | 全部为合成数据 | 无真实客户信息 |

## 2. 对外演示的最小加固

如果要把演示放到公网，至少做以下任意组合：

1. **前置认证**：Nginx Basic Auth、Caddy `basicauth`、VPN、零信任网关。
2. **关闭重置**：`.env` 设置 `DEMO_RESET_ENABLED=false`。
3. **关闭 Swagger**：
   ```yaml
   springdoc:
     api-docs:
       enabled: false
     swagger-ui:
       enabled: false
   ```
4. **限制 Actuator**：只保留 `health`，并考虑关闭 `show-details`。
5. **HTTPS**：Caddy 自动证书，或 Nginx + certbot。
6. **限流**：在 Nginx/Caddy 层限制 `/api/orders` 的速率。
7. **只开 8088/443**：不要暴露 3306、8080、8089。
8. **定期重置**：演示前用页面按钮或 `down -v` 恢复干净数据。

## 3. 凭据管理

- 所有密码通过 `.env` 注入，`.env` 已在 `.gitignore` 中。
- 仓库中**没有**真实数据库密码、邮箱授权码、银行证书、SSH 私钥、SFTP 密码。
- 如果你从原项目复制过任何配置，请确认没有把真实凭据带进来；建议用 `gitleaks`/`trufflehog` 扫描后再推送。
- MySQL root 与应用账号使用不同密码；生产环境应使用密钥管理服务。

## 4. 已知安全边界

- 没有 CSRF 防护（无 Cookie 会话，风险较低，但公网仍应加）。
- 没有请求签名/防重放；下单接口可被脚本刷。
- 没有审计日志；只有应用日志。
- 没有输入长度/频率限制；`quantity`/`price` 有业务上限，但没有 HTTP 层限流。
- WebSocket 允许所有 Origin（`setAllowedOriginPatterns("*")`），公网部署应改为具体域名。
- 重置接口无二次确认 token，只有前端确认框。

## 5. 数据与合规

- 演示数据由 `DemoDataService` 生成，不含任何真实客户、银行、清算所信息。
- 商品名、账户名、品牌均为中性示例。
- 不包含原公司的银行协议、清算文件、证书、内网地址。
- 对外展示前，请再次确认你方对原代码的演绎/展示授权。

## 6. 漏洞反馈

这是个人演示项目，没有正式的安全响应流程。若发现明显问题，请直接在使用方内部记录并修复，不要将包含真实凭据的复现材料公开。
