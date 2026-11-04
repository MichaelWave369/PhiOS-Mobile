/**
 * Opt-in, browser-scoped GPU capability snapshot.
 * WebGPU is NOT CUDA, native Android, VRAM telemetry, or a tensor benchmark.
 */
export async function inspectBrowserGpu({ gpu, secureContext = false } = {}) {
  const base = {
    schema: "phios.gpu-observation.v1",
    source: "webgpu-browser",
    authority: "observe-only",
    devices: [],
  };
  if (!secureContext || !gpu || typeof gpu.requestAdapter !== "function") {
    return { ...base, status: "unavailable", reason: "WebGPU not exposed in a secure browser context" };
  }
  try {
    const adapter = await gpu.requestAdapter();
    if (!adapter) return { ...base, status: "unavailable", reason: "No adapter granted by browser" };
    return {
      ...base, status: "observed",
      devices: [{
        name: "Browser WebGPU adapter",
        reported_vendor: typeof adapter.info?.vendor === "string" ? adapter.info.vendor.slice(0, 80) : null,
        reported_architecture: typeof adapter.info?.architecture === "string" ? adapter.info.architecture.slice(0, 80) : null,
        reported_feature_count: typeof adapter.features?.size === "number" ? adapter.features.size : null,
        vram_total_mib: null, gpu_utilization_percent: null,
      }],
      limitations: ["No physical VRAM, CUDA, clocks, thermal, FPS or tensor throughput available",
        "Adapter data may be filtered by the browser"],
    };
  } catch {
    return { ...base, status: "unavailable", reason: "Browser adapter request failed" };
  }
}
