import test from "node:test";
import assert from "node:assert/strict";
import { inspectBrowserGpu } from "../src/browser-gpu.mjs";

test("unsupported contexts are unavailable and never fabricate a GPU", async () => {
  const result = await inspectBrowserGpu();
  assert.equal(result.status, "unavailable");
  assert.deepEqual(result.devices, []);
  assert.equal(result.authority, "observe-only");
});
test("user-initiated browser observation reports only permitted hints", async () => {
  let calls = 0;
  const gpu = { requestAdapter: async () => {
    calls++;
    return { info: {vendor: "Test vendor", architecture: "Test architecture"}, features: new Set(["shader-f16"]) };
  } };
  assert.equal(calls, 0);
  const result = await inspectBrowserGpu({gpu, secureContext:true});
  assert.equal(calls, 1);
  assert.equal(result.status, "observed");
  assert.equal(result.devices[0].reported_feature_count, 1);
  assert.equal(result.devices[0].vram_total_mib, null);
});
test("denials and thrown adapters never turn into invented throughput", async () => {
  const denied = await inspectBrowserGpu({gpu:{requestAdapter:async()=>null},secureContext:true});
  assert.equal(denied.status, "unavailable");
  const failed = await inspectBrowserGpu({gpu:{requestAdapter:async()=>{throw new Error("denied");}},secureContext:true});
  assert.equal(failed.status, "unavailable");
  assert.deepEqual(failed.devices, []);
});
