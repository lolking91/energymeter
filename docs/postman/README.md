# Fox ESS Open API — Postman Collection

Manual test setup for the Fox ESS real-time query endpoint, without writing any Java code.

## Setup

1. Open Postman → **Import** → select [`FoxESS-OpenAPI.postman_collection.json`](FoxESS-OpenAPI.postman_collection.json)
2. Click the collection → **Variables** tab → fill in:
   - `apiKey` — your Fox ESS Open API key (Fox ESS Cloud → User → API Management)
   - `deviceSn` — your inverter's serial number
3. Open the **Get real-time device data** request → **Send**

The pre-request script computes `timestamp` and `signature` automatically on every send — no manual hashing needed.

## Notes

- A successful response has `"errno": 0` and a `result` array containing `deviceSN` and `datas` (variable/value/unit triples).
- Fox ESS only refreshes real-time data roughly every 5 minutes — resending immediately will return the same values.
- The `apiKey` variable is marked as `secret` in the collection so Postman masks it in the UI; it is still stored in plain text in the collection JSON, so don't commit a copy with a real key filled in (this file ships with empty values).
