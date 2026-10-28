from playwright.sync_api import sync_playwright
import argparse
import os
import json

OUT = r"D:\ToUpload\EEE\work\program\trade-demo\docs\screenshots"
os.makedirs(OUT, exist_ok=True)
errors = []

parser = argparse.ArgumentParser()
parser.add_argument("--base-url", default="http://127.0.0.1:8088")
args = parser.parse_args()
base_url = args.base_url.rstrip("/")

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={"width": 1600, "height": 1000}, device_scale_factor=1)
    page.on("console", lambda msg: errors.append(f"console {msg.type}: {msg.text}") if msg.type == "error" else None)
    page.on("pageerror", lambda exc: errors.append(f"pageerror: {exc}"))

    page.goto(base_url, wait_until="networkidle")
    page.wait_for_timeout(4000)
    page.wait_for_selector(".level.ask", timeout=15000)

    print("title:", page.title())
    print("brand:", page.locator(".brand").inner_text())
    print("markets:", page.locator(".market-item").count())
    print("asks:", page.locator(".level.ask").count(), "bids:", page.locator(".level.bid").count())
    print("metrics:", page.locator(".metric").all_inner_texts())
    print("submit:", page.locator(".submit-btn").inner_text())
    page.screenshot(path=os.path.join(OUT, "01-overview.png"), full_page=True)

    inputs = page.locator(".form-body .el-input-number input")
    inputs.nth(0).fill("101.00")
    inputs.nth(0).press("Enter")
    inputs.nth(1).fill("2")
    inputs.nth(1).press("Enter")
    page.wait_for_timeout(300)
    page.locator(".submit-btn").click()
    page.wait_for_timeout(2500)
    page.screenshot(path=os.path.join(OUT, "02-after-buy.png"), full_page=True)

    page.get_by_role("tab", name="我的成交").click()
    page.wait_for_timeout(1000)
    trade_rows = page.locator(".el-tab-pane:visible .el-table__body tr").count()
    print("trade rows:", trade_rows)
    page.screenshot(path=os.path.join(OUT, "03-trades.png"), full_page=True)

    page.get_by_role("tab", name="持仓").click()
    page.wait_for_timeout(800)
    print("position rows:", page.locator(".el-tab-pane:visible .el-table__body tr").count())
    page.screenshot(path=os.path.join(OUT, "04-positions.png"), full_page=True)

    page.get_by_role("tab", name="资金流水").click()
    page.wait_for_timeout(800)
    print("flow rows:", page.locator(".el-tab-pane:visible .el-table__body tr").count())
    page.screenshot(path=os.path.join(OUT, "05-flows.png"), full_page=True)

    # ---- clearing view ----
    page.get_by_role("button", name="清算与银行").click()
    page.wait_for_selector(".clearing-cards", timeout=10000)
    page.wait_for_timeout(1500)
    print("clearing summary:", page.locator(".clear-card").all_inner_texts())
    print("run enabled:", page.get_by_role("button", name="执行日终清算").is_enabled())
    page.screenshot(path=os.path.join(OUT, "06-clearing.png"), full_page=True)

    page.get_by_role("button", name="执行日终清算").click()
    page.wait_for_timeout(500)
    page.get_by_role("button", name="执行", exact=True).click()
    page.wait_for_timeout(2500)
    batch_rows = page.locator(".clearing-grid .panel").first.locator(".el-table__body tr").count()
    detail_rows = page.locator(".clearing-grid .panel").nth(1).locator(".el-table__body tr").count()
    print("batch rows:", batch_rows, "detail/recon rows:", detail_rows)
    page.screenshot(path=os.path.join(OUT, "07-clearing-detail.png"), full_page=True)

    # ---- bank deposit ----
    page.get_by_role("button", name="入金").first.click()
    page.wait_for_selector(".el-dialog", timeout=5000)
    page.locator(".el-dialog .el-input-number input").fill("500")
    page.locator(".el-dialog .el-input-number input").press("Enter")
    page.get_by_role("button", name="确认", exact=True).click()
    page.wait_for_timeout(2000)
    print("bank transactions rows:", page.locator(".panel").last.locator(".el-table__body tr").count())
    page.screenshot(path=os.path.join(OUT, "08-bank.png"), full_page=True)

    print("errors:", json.dumps(errors, ensure_ascii=False, indent=2))
    browser.close()
