import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { createPortablePassportStore, parsePortablePassport,
  summarizePortablePassport } from "../src/operator-passport.mjs";

const sample = (scores=[]) => JSON.stringify({format:"phioperator-passport",version:1,
  results:scores.map((score,i)=>({rank:i+1,mode:"standard",bestScore:score}))});
test("portable SDK accepts identical PhiOperator JSON and sequential cosmetics",()=>{
 assert.equal(summarizePortablePassport(parsePortablePassport(sample([9,7]))).clearedRank,2);
 assert.equal(summarizePortablePassport(parsePortablePassport(sample([9,6,9]))).clearedRank,1);
 assert.equal(summarizePortablePassport(parsePortablePassport(sample())).imported,true);
 assert.throws(()=>parsePortablePassport(sample([10])),TypeError);
 assert.throws(()=>parsePortablePassport("{broken"),SyntaxError);
});
test("import is explicit, local, exportable and reversible without touching shell settings",()=>{
 const data=new Map([["phios.mobile.r1","keep existing notes"]]);
 const storage={getItem:k=>data.get(k),setItem:(k,v)=>data.set(k,v),removeItem:k=>data.delete(k)};
 const bridge=createPortablePassportStore({storage});
 assert.equal(bridge.hasBenefit("phios-mobile-halo"),false);
 const result=bridge.importJSON(sample([9,9]));
 assert.equal(result.persisted,true);
 assert.equal(bridge.hasBenefit("phios-mobile-halo"),true);
 assert.equal(createPortablePassportStore({storage}).snapshot().clearedRank,2);
 assert.equal(JSON.parse(bridge.exportJSON()).results.length,2);
 bridge.clear();
 assert.equal(bridge.snapshot().imported,false);
 assert.equal(data.get("phios.mobile.r1"),"keep existing notes");
});
test("service-worker precache includes standalone SDK and a new cache generation",async()=>{
 const sw=await readFile(new URL("../sw.js",import.meta.url),"utf8");
 assert.match(sw,/src\/operator-passport\.mjs/);
 assert.match(sw,/phios-mobile-operator-r4-v1/);
});
