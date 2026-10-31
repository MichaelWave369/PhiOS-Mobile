import { rm, mkdir, copyFile, cp } from "node:fs/promises";

const source = new URL("../", import.meta.url);
const output = new URL("../dist/", import.meta.url);
await rm(output, { recursive: true, force: true });
await mkdir(output, { recursive: true });

for (const name of ["index.html", "styles.css", "sw.js", "manifest.webmanifest"]) {
  await copyFile(new URL(name, source), new URL(name, output));
}
for (const directory of ["assets", "src"]) {
  await cp(new URL(directory + "/", source), new URL(directory + "/", output), { recursive: true });
}
console.log("Static PhiOS Mobile shell built to dist/");
