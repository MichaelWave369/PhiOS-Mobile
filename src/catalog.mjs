/* Shared PhiOS public discovery UI: source metadata only, never installer. */
import { PUBLIC_CATALOG } from "./catalog-data.mjs";

const OWNER="MichaelWave369";
export function validatePublicCatalog(value=PUBLIC_CATALOG){
  if(!value||value.schema!=="phios.public-repo-catalog.v1"||
    value.provenance?.owner!==OWNER||value.provenance?.scope!=="public"||
    value.provenance?.complete!==true||
    value.policy?.review_required!==true||value.policy?.automatic_install!==false||
    value.policy?.automatic_launch!==false||value.policy?.cross_platform_qualification!==false||
    !Array.isArray(value.apps)||value.apps.length>1000)return null;
  const ids=new Set(),names=new Set();
  for(const item of value.apps){
    if(!item||typeof item.id!=="string"||!/^github:[0-9]{1,20}$/.test(item.id)||
      typeof item.name!=="string"||!/^[A-Za-z0-9_.-]{1,100}$/.test(item.name)||
      item.repository!=="https://github.com/"+OWNER+"/"+item.name||
      item.availability!=="source_only"||
      typeof item.description!=="string"||item.description.length>600||
      ids.has(item.id)||names.has(item.name.toLowerCase()))return null;
    ids.add(item.id);names.add(item.name.toLowerCase());
  }
  return value;
}

export function initPublicCatalog(){
  const toggle=document.getElementById("catalog-toggle"),panel=document.getElementById("catalog-panel");
  const results=document.getElementById("catalog-results"),query=document.getElementById("catalog-query");
  const more=document.getElementById("catalog-more"),status=document.getElementById("catalog-meta");
  if(!toggle||!panel||!results||!query||!more||!status)return;
  const catalog=validatePublicCatalog();
  if(!catalog){
    toggle.disabled=true;toggle.textContent="Catalog unavailable; source validation failed";
    return;
  }
  const count=catalog.apps.length;
  toggle.textContent="EXPLORE "+count+" PUBLIC SOURCES";
  let matching=catalog.apps,shown=0;
  const append=()=>{
    const batch=matching.slice(shown,shown+24);
    for(const item of batch){
      const row=document.createElement("article");row.className="public-catalog-row";row.setAttribute("role","listitem");
      const info=document.createElement("div");
      const name=document.createElement("strong");name.textContent=item.name;
      const description=document.createElement("p");description.textContent=item.description||"No description provided.";
      const note=document.createElement("small");
      note.textContent=(item.language||"Unknown language")+" · SOURCE ONLY"+(item.archived?" · ARCHIVED":"");
      const link=document.createElement("a");link.href=item.repository;link.target="_blank";link.rel="noopener noreferrer";
      link.textContent="VIEW SOURCE ↗";link.setAttribute("aria-label","View "+item.name+" public GitHub source in a new tab");
      info.append(name,description,note);row.append(info,link);results.append(row);
    }
    shown+=batch.length;
    more.hidden=shown>=matching.length;
    status.textContent=matching.length+" / "+count+" public repositories · snapshot "+catalog.provenance.snapshot_fetched_at+" · "+shown+" shown";
  };
  const filter=()=>{
    const term=query.value.trim().toLowerCase().slice(0,100);
    matching=catalog.apps.filter(app=>app.name.toLowerCase().includes(term)||
      app.description.toLowerCase().includes(term));
    shown=0;results.replaceChildren();append();
  };
  toggle.addEventListener("click",()=>{
    panel.hidden=!panel.hidden;
    toggle.setAttribute("aria-expanded",String(!panel.hidden));
    if(!panel.hidden){filter();query.focus();}
  });
  query.addEventListener("input",filter);
  more.addEventListener("click",append);
}
