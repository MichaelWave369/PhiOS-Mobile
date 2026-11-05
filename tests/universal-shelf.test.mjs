import test from "node:test";
import assert from "node:assert/strict";
import {readFile} from "node:fs/promises";
import {PUBLIC_CATALOG} from "../src/catalog-data.mjs";
import "../src/shelf-portable.js";
import {SHELF_KEY} from "../src/shelf-ui.mjs";

const api=globalThis.PhiShelf;
const root=new URL("../",import.meta.url);
const html=await readFile(new URL("index.html",root),"utf8");
const sw=await readFile(new URL("sw.js",root),"utf8");
const shell=await readFile(new URL("src/shelf-ui.mjs",root),"utf8");
const catalog=PUBLIC_CATALOG;
const ids=catalog.apps.map(app=>app.id);
const date=new Date("2026-11-05T22:00:00.000Z");

test("universal bookmark format is deterministic and portable",()=>{
  const pins=[ids[0],ids[1]];
  const packet=api.packet(pins,catalog,date);
  assert.deepEqual(Object.keys(packet),["format","version","catalog_source_sha","exported_at","pinned"]);
  assert.equal(packet.format,"phios-app-shelf");
  assert.equal(packet.version,1);
  assert.equal(packet.catalog_source_sha,"e295f6265e60032de98a2acd8e53295505fc47e8");
  assert.deepEqual(api.parse(JSON.stringify(packet),catalog),pins);
  assert.equal(api.LIMIT,24);
  assert.equal(SHELF_KEY,"phios.mobile.shelf.r2");
});

test("reordering and pin removal preserve stable GitHub identity",()=>{
  let pins=api.pin([],ids[0],catalog);
  pins=api.pin(pins,ids[1],catalog);
  pins=api.move(pins,ids[1],-1,catalog);
  assert.deepEqual(pins,[ids[1],ids[0]]);
  assert.deepEqual(api.unpin(pins,ids[1],catalog),[ids[0]]);
  assert.deepEqual(api.pin([ids[0]],ids[0],catalog),[ids[0]]);
  assert.throws(()=>api.pin([], "github:999999999999999",catalog),/Invalid/);
});

test("backup rejects injected capabilities, unknown IDs, mismatched snapshots, duplicates and oversize",()=>{
  const original=api.packet([ids[0]],catalog,date);
  assert.throws(()=>api.parse(JSON.stringify({...original,install_authority:true}),catalog),/contract/);
  assert.throws(()=>api.parse(JSON.stringify({...original,pinned:[ids[0],ids[0]]}),catalog),/duplicate/);
  assert.throws(()=>api.parse(JSON.stringify({...original,pinned:["github:999999999999999"]}),catalog),/Invalid/);
  assert.throws(()=>api.parse(JSON.stringify({...original,catalog_source_sha:"0".repeat(40)}),catalog),/contract/);
  assert.throws(()=>api.parse("x".repeat(16385),catalog),/16 KiB/);
  assert.throws(()=>api.normalize(ids.slice(0,25),catalog),/24/);
  const altered=structuredClone(catalog);altered.policy.automatic_install=true;
  assert.throws(()=>api.packet([],altered,date),/Untrusted/);
});

test("UI keeps existing Android and Notes unrelated to portable public pins",()=>{
  for(const id of ["mobile-shelf-list","mobile-shelf-status","mobile-shelf-export","mobile-shelf-import","mobile-shelf-import-file"])
    assert(html.includes('id="'+id+'"'),id);
  assert(shell.includes("window.confirm("));
  assert(shell.includes("new Event(\"phios-shelf-changed\")"));
  assert(shell.includes("JSON.stringify(api.packet("));
  assert(shell.includes("api.parse(await file.text(),catalog)"));
  assert(sw.includes("src/shelf-portable.js"));
  assert(sw.includes("src/shelf-ui.mjs"));
  assert(!shell.includes("getInstalledPackages("));
  const entry=catalog.apps.find(x=>x.declared_homepage);
  assert.match(api.badge(entry),/UNVERIFIED/);
});
