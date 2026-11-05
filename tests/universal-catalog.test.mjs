import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import {PUBLIC_CATALOG} from '../src/catalog-data.mjs';
import {validatePublicCatalog} from '../src/catalog.mjs';

const root=new URL('../',import.meta.url);
const html=await readFile(new URL('index.html',root),'utf8');
const build=await readFile(new URL('scripts/build.mjs',root),'utf8');
const sw=await readFile(new URL('sw.js',root),'utf8');
const json=JSON.parse(await readFile(new URL('public-repo-catalog.v1.json',root),'utf8'));

test('Mobile and canonical source inventory are equal, complete-at-snapshot and public-only',()=>{
  assert.deepEqual(PUBLIC_CATALOG,json);
  assert.equal(json.apps.length,225);
  assert.equal(json.provenance.source_blob_sha,'e295f6265e60032de98a2acd8e53295505fc47e8');
  assert.equal(json.provenance.scope,'public');
  assert.equal(json.provenance.complete,true);
  assert.equal(validatePublicCatalog()?.apps.length,225);
  assert.equal(new Set(json.apps.map(a=>a.id)).size,225);
});

test('Public app list cannot claim native install or leak other owners',()=>{
  const changed=structuredClone(json);
  changed.apps[0].availability='installed_verified';
  assert.equal(validatePublicCatalog(changed),null);
  changed.apps[0].availability='source_only';
  changed.policy.automatic_install=true;
  assert.equal(validatePublicCatalog(changed),null);
  changed.policy.automatic_install=false;
  changed.apps[0].repository='https://github.com/AnotherOwner/12fold';
  assert.equal(validatePublicCatalog(changed),null);
  changed.apps[0].repository=json.apps[0].repository;
  changed.apps[1].id=changed.apps[0].id;
  assert.equal(validatePublicCatalog(changed),null);
  changed.apps[1].id=json.apps[1].id;
  changed.provenance.scope='private';
  assert.equal(validatePublicCatalog(changed),null);
});

test('Mobile home offers accessible source-only catalog without APK controls',()=>{
  for(const id of ['catalog-toggle','catalog-panel','catalog-query','catalog-results','catalog-more','catalog-meta'])
    assert(html.includes('id="'+id+'"'),id);
  assert(html.includes('aria-expanded="false"'));
  assert(html.includes('No APKs, executables, or native permissions are installed'));
  assert(!html.includes('id="catalog-install"'));
  assert(build.includes('public-repo-catalog.v1.json'));
  assert(sw.includes('src/catalog-data.mjs'));
  assert(sw.includes('src/catalog.mjs'));
});
