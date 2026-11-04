# GPU Observatory · Mobile read-only Rung 1

The PhiOS Mobile browser Pulse now includes an **opt-in WebGPU adapter inspector**. It calls `navigator.gpu.requestAdapter()` only when the operator presses the button. It reports browser-exposed vendor/architecture/feature counts, never inferred VRAM or TFLOPS. Unsupported browsers and revoked access show `unavailable`.

The native Android launcher has a **GPU info** action. It opens a local dialog with Android's `ActivityManager.getDeviceConfigurationInfo().getGlEsVersion()` value. OpenGL ES support is NOT a physical GPU performance benchmark, CPU/GPU scheduler reading, or permission to change configuration.

Both surfaces are local, read-only and make no new network calls. There is no periodic sampling, remote reporting, GPU unlocking or root. Reports align with the observational `phios.gpu-observation.v1` PhiOS core envelope in spirit; the native dialog is intentionally not a JSON export in this rung.

Future rungs: opt-in, battery-aware timing experiments on a real phone; graphics frame budget and sustained thermals via documented Android APIs; WebXR frame pacing through a separately validated host adapter. Never extrapolate desktop CUDA figures to mobile.
