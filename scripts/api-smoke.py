#!/usr/bin/env python3
"""API smoke test for the ClearHub trade core demo.

Usage:
    python scripts/api-smoke.py --base-url http://127.0.0.1:8088

The script exercises the full order lifecycle: market data, a crossing buy,
a resting buy that is cancelled, price-limit rejection, insufficient-funds
rejection, position/funds queries and demo reset.
"""

import argparse
import json
import sys
import time
import urllib.error
import urllib.request


class ApiError(Exception):
    def __init__(self, code, message):
        super().__init__(f"[{code}] {message}")
        self.code = code


class Client:
    def __init__(self, base_url):
        self.base_url = base_url.rstrip("/")

    def request(self, method, path, body=None, firm_id=None):
        url = self.base_url + path
        data = json.dumps(body).encode("utf-8") if body is not None else None
        request = urllib.request.Request(url, data=data, method=method)
        request.add_header("Content-Type", "application/json")
        if firm_id is not None:
            request.add_header("X-Firm-Id", str(firm_id))
        try:
            with urllib.request.urlopen(request, timeout=20) as response:
                payload = json.loads(response.read().decode("utf-8"))
        except urllib.error.HTTPError as error:
            raw = error.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"HTTP {error.code}: {raw}") from error
        if isinstance(payload, dict) and "code" in payload:
            if payload["code"] != 0:
                raise ApiError(payload["code"], payload.get("message", ""))
            return payload.get("data")
        return payload

    def get(self, path, firm_id=None):
        return self.request("GET", path, firm_id=firm_id)

    def post(self, path, body=None, firm_id=None):
        return self.request("POST", path, body=body, firm_id=firm_id)


class Smoke:
    def __init__(self, base_url):
        self.client = Client(base_url)
        self.results = []

    def run(self, name, fn):
        try:
            detail = fn()
            self.results.append((True, name, detail))
            print(f"PASS  {name}  {detail}")
            return True
        except Exception as error:  # noqa: BLE001 - smoke test reports everything
            self.results.append((False, name, str(error)))
            print(f"FAIL  {name}  {error}")
            return False

    def summary(self):
        passed = sum(1 for ok, _, _ in self.results if ok)
        total = len(self.results)
        print(f"\n{passed}/{total} checks passed")
        return 0 if passed == total else 1


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://127.0.0.1:8088")
    parser.add_argument("--firm-id", type=int, default=2)
    args = parser.parse_args()

    smoke = Smoke(args.base_url)
    client = smoke.client
    firm_id = args.firm_id
    state = {}

    def health():
        payload = client.get("/actuator/health")
        assert payload.get("status") == "UP", payload
        return "UP"

    smoke.run("actuator health", health)

    def firms():
        data = client.get("/api/firms")
        assert len(data) >= 2, data
        state["firms"] = data
        return f"{len(data)} accounts"

    smoke.run("list accounts", firms)

    def commodities():
        data = client.get("/api/commodities")
        assert data, "no commodities"
        state["commodity"] = data[0]
        assert float(data[0]["lastPrice"]) > 0, data[0]
        return f"{data[0]['code']} last={data[0]['lastPrice']}"

    smoke.run("list commodities", commodities)

    def orderbook():
        commodity_id = state["commodity"]["id"]
        data = client.get(f"/api/market/{commodity_id}/orderbook?depth=5")
        state["book"] = data
        assert data["asks"], "no ask levels; market maker may be disabled"
        assert data["bids"], "no bid levels; market maker may be disabled"
        return f"bids={len(data['bids'])} asks={len(data['asks'])}"

    if not smoke.run("order book has two sides", orderbook):
        return smoke.summary()

    def crossing_buy():
        commodity = state["commodity"]
        ask = float(state["book"]["asks"][0]["price"])
        price = round(ask + 1, 2)
        order = client.post(
            "/api/orders",
            {"commodityId": commodity["id"], "side": "BUY", "price": price, "quantity": 1},
            firm_id=firm_id,
        )
        state["order_id"] = order["id"]
        return f"order #{order['id']} {price}"

    smoke.run("place crossing buy", crossing_buy)

    def wait_trade():
        commodity_id = state["commodity"]["id"]
        deadline = time.time() + 8
        last = []
        while time.time() < deadline:
            last = client.get(f"/api/trades?firmId={firm_id}&limit=50")
            if any(int(item["commodityId"]) == int(commodity_id) for item in last):
                return f"{len(last)} trades"
            time.sleep(0.4)
        raise AssertionError(f"no trade produced for firm {firm_id}: {last}")

    smoke.run("trade is produced", wait_trade)

    def positions():
        data = client.get(f"/api/positions?firmId={firm_id}")
        commodity_id = state["commodity"]["id"]
        row = next((item for item in data if int(item["commodityId"]) == int(commodity_id)), None)
        assert row is not None, data
        assert float(row["quantity"]) > 0, row
        state["position"] = row
        return f"quantity={row['quantity']}"

    smoke.run("position updated", positions)

    def resting_buy():
        commodity = state["commodity"]
        last = float(commodity["lastPrice"])
        price = round(last * 0.95, 2)
        order = client.post(
            "/api/orders",
            {"commodityId": commodity["id"], "side": "BUY", "price": price, "quantity": 1},
            firm_id=firm_id,
        )
        state["resting_order_id"] = order["id"]
        funds = client.get(f"/api/funds?firmId={firm_id}")
        assert float(funds["frozenFunds"]) > 0, funds
        return f"order #{order['id']} frozen={funds['frozenFunds']}"

    smoke.run("place resting buy and freeze funds", resting_buy)

    def cancel():
        order = client.post(f"/api/orders/{state['resting_order_id']}/cancel", firm_id=firm_id)
        assert order["status"] == 3, order
        funds = client.get(f"/api/funds?firmId={firm_id}")
        assert float(funds["frozenFunds"]) == 0, funds
        return "cancelled, frozen=0"

    smoke.run("cancel releases frozen funds", cancel)

    def price_limit():
        commodity = state["commodity"]
        last = float(commodity["lastPrice"])
        try:
            client.post(
                "/api/orders",
                {"commodityId": commodity["id"], "side": "BUY", "price": round(last * 2, 2), "quantity": 1},
                firm_id=firm_id,
            )
        except ApiError as error:
            assert error.code == 40007, error
            return f"rejected with {error.code}"
        raise AssertionError("price limit was not enforced")

    smoke.run("price limit rejected", price_limit)

    def insufficient_funds():
        commodity = state["commodity"]
        last = float(commodity["lastPrice"])
        try:
            client.post(
                "/api/orders",
                {"commodityId": commodity["id"], "side": "BUY", "price": round(last, 2), "quantity": 100000},
                firm_id=firm_id,
            )
        except ApiError as error:
            assert error.code == 40003, error
            return f"rejected with {error.code}"
        raise AssertionError("insufficient funds was not enforced")

    smoke.run("insufficient funds rejected", insufficient_funds)

    def reset():
        client.post("/api/demo/reset")
        status = client.get("/api/demo/status")
        assert status["status"] == "UP", status
        return f"orders={status['orders']} trades={status['trades']}"

    smoke.run("reset demo data", reset)

    return smoke.summary()


if __name__ == "__main__":
    sys.exit(main())
