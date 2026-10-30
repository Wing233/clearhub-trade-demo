# Render 单服务部署

Render 免费实例只有 512MB 内存，无法运行原来的 MySQL + 三个 Java 服务 + Nginx。  
本目录的 `Dockerfile.render` 把交易核心、清算适配器、模拟银行合并进一个 Spring Boot 进程，使用 H2 内存库，并由同一个进程托管前端静态资源。

## 架构

```text
Render Web Service (Docker)
└─ trade-core.jar (Spring Boot)
   ├─ 撮合 / 订单 / 资金 / 持仓 / 行情
   ├─ embedded clearing adapter
   ├─ embedded mock bank
   ├─ H2 in-memory database
   └─ /app/static -> Vue dist
```

- `integration.mode=embedded`：`AdapterClient` 不再走 HTTP，直接调用进程内的适配器和模拟银行。
- H2 使用 `MODE=MySQL`，由 Hibernate 自动建表；容器重启后恢复初始演示数据。
- 前端与 API 同源，WebSocket 走同一个 Render 端口，无需 CORS/Nginx。

## 部署

1. 把仓库推到 GitHub。
2. 打开 Render Dashboard → **New** → **Blueprint**，选择该仓库。
3. Render 会读取根目录 `render.yaml`，创建 `clearhub-trade-demo` 免费 Web Service。
4. 等待 Docker 构建和健康检查 `/actuator/health` 通过。

也可以直接使用：

```text
https://render.com/deploy?repo=https://github.com/Wing233/clearhub-trade-demo
```

## 注意事项

- 免费实例 15 分钟无请求会休眠，首次访问冷启动约 30–60 秒。
- 仓库自带 `.github/workflows/keepalive.yml`，每 5 分钟请求一次健康检查，避免演示期间休眠；不需要时删除该 workflow 即可。
- H2 是内存库，重启/重新部署会清空交易、清算和银行数据，只保留种子账户。
- 适配器/模拟银行是进程内演示实现，不再有独立的 8090/8091 服务。
- 免费实例 CPU/内存有限，已把 JVM 限制为 `-Xmx192m`；不要同时打开大量标签页。
- 无登录鉴权，公网可见；只放演示数据。

## 本地验证

```bash
# 构建并运行与 Render 相同的单服务镜像
docker build -f Dockerfile.render -t clearhub-render .
docker run --rm -p 18080:10000 clearhub-render

# 冒烟
python scripts/api-smoke.py --base-url http://127.0.0.1:18080
python scripts/ui-smoke.py --base-url http://127.0.0.1:18080
```

也可以直接用 H2 profile 跑 jar：

```bash
java -jar target/trade-core.jar \
  --spring.profiles.active=h2 \
  --server.port=18080 \
  --spring.web.resources.static-locations=file:web/dist/
```
