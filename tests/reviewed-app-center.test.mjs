import test from "node:test";
import assert from "node:assert/strict";
import {readFile} from "node:fs/promises";
import {APP_CENTER_REVIEWS} from "../src/app-center-data.mjs";
import {PUBLIC_CATALOG} from "../src/catalog-data.mjs";
import {reviewedPhiOSWeb} from "../src/app-center.mjs";

const root=new URL("../",import.meta.url);
const json=JSON.parse(await readFile(new URL("app-center-reviews.v1.json",root),"utf8"));
const html=await readFile(new URL("index.html",root),"utf8");
const sw=await readFile(new URL("sw.js",root),"utf8");
const build=await readFile(new URL("scripts/build.mjs",root),"utf8");
const main=await readFile(new URL("src/main.js",root),"utf8");

test("R4 mobile has exact CI-reviewed PWA registry and no native install claim",()=>{
  assert.deepEqual(APP_CENTER_REVIEWS,json);
  assert.equal(json.catalog_source_sha,PUBLIC_CATALOG.provenance.source_blob_sha);
  const record=reviewedPhiOSWeb();
  assert.equal(record.app_id,"github:1442243366");
  assert.equal(record.launch_url,"https://phi-field.com/os/");
  assert.equal(record.review_status,"ci_reviewed_browser_app");
  assert.equal(record.evidence.live_endpoint_independently_verified,false);
  assert.equal(record.actions.native_install,false);
  assert.equal(json.reviewed_web_apps.length,1);
  assert.equal(json.policy.autonomous_installs,false);
});

test("Review refuses untrusted sites, foreign source identities and inflated verification",()=>{
  const change=fn=>{const packet=structuredClone(json);fn(packet);return reviewedPhiOSWeb(packet,PUBLIC_CATALOG);};
  assert.equal(change(x=>x.policy.autonomous_installs=true),null);
  assert.equal(change(x=>x.reviewed_web_apps[0].launch_url="https://evil.invalid/os/"),null);
  assert.equal(change(x=>x.reviewed_web_apps[0].app_id="github:000000"),null);
  assert.equal(change(x=>x.reviewed_web_apps[0].evidence.live_endpoint_independently_verified=true),null);
  assert.equal(change(x=>x.reviewed_web_apps[0].actions.native_install=true),null);
  assert.equal(change(x=>x.reviewed_web_apps.push(x.reviewed_web_apps[0])),null);
});

test("Mobile PWA opens only an explicit external browser page; no APK install",()=>{
  for(const id of ["reviewed-app-center","reviewed-app-open","reviewed-app-status"])
    assert(html.includes('id="'+id+'"'));
  assert(html.includes("not an Android APK"));
  assert(html.includes('target="_blank" rel="noopener noreferrer"'));
  assert(build.includes("app-center-reviews.v1.json"));
  assert(sw.includes("src/app-center.mjs"));
  assert(sw.includes("src/app-center-data.mjs"));
  assert(main.includes("initReviewedAppCenter();"));
  assert(!main.includes("PackageInstaller"));
});
