/* Universal Shelf R2: this home shelf contains public source bookmarks, not installed apps. */
import {PUBLIC_CATALOG} from "./catalog-data.mjs";
import "./shelf-portable.js";

export const SHELF_KEY="phios.mobile.shelf.r2";

export function initPortableShelf(){
  const api=globalThis.PhiShelf,catalog=PUBLIC_CATALOG;
  const list=document.getElementById("mobile-shelf-list");
  const status=document.getElementById("mobile-shelf-status");
  const exportButton=document.getElementById("mobile-shelf-export");
  const importButton=document.getElementById("mobile-shelf-import");
  const fileInput=document.getElementById("mobile-shelf-import-file");
  if(!api||!list||!status||!exportButton||!importButton||!fileInput)return {isPinned:()=>false,toggle:()=>{}};
  let sources;
  try{sources=api.catalogMap(catalog);}catch{
    status.textContent="Invalid public catalog. Shelf disabled.";exportButton.disabled=true;importButton.disabled=true;
    return {isPinned:()=>false,toggle:()=>{}};
  }
  let storage=null,pins=[];
  try{
    storage=window.localStorage;
    const saved=storage?.getItem(SHELF_KEY);
    if(saved)pins=api.parse(saved,catalog);
  }catch{
    storage=null;
    status.textContent="Stored shelf unavailable. Session-only pins; export to preserve them.";
  }

  function render(){
    list.replaceChildren();
    status.textContent=pins.length+" / "+api.LIMIT+" public-source bookmarks · "+(storage?"local browser storage":"SESSION ONLY, export to keep")+" · NO INSTALLS";
    for(const [i,id] of pins.entries()){
      const entry=sources.get(id);if(!entry)continue;
      const row=document.createElement("article");row.className="portable-shelf-row";row.setAttribute("role","listitem");
      const title=document.createElement("strong");title.textContent=entry.name;
      const badge=document.createElement("small");badge.textContent=api.badge(entry);
      const link=document.createElement("a");link.href=entry.repository;link.target="_blank";link.rel="noopener noreferrer";
      link.textContent="VIEW SOURCE ↗";link.setAttribute("aria-label","View "+entry.name+" source");
      const buttons=document.createElement("div");buttons.className="portable-shelf-row-actions";
      for(const [kind,label,disabled] of [["up","↑",i===0],["down","↓",i===pins.length-1],["unpin","×",false]]){
        const button=document.createElement("button");button.type="button";button.textContent=label;button.disabled=disabled;
        button.setAttribute("aria-label",(kind==="up"?"Move "+entry.name+" up":kind==="down"?"Move "+entry.name+" down":"Unpin "+entry.name));
        button.addEventListener("click",()=>{
          try{
            const next=kind==="up"?api.move(pins,id,-1,catalog):kind==="down"?api.move(pins,id,1,catalog):api.unpin(pins,id,catalog);
            commit(next);
          }catch(error){status.textContent=error.message;}
        });
        buttons.append(button);
      }
      const info=document.createElement("div");info.className="portable-shelf-row-info";info.append(title,badge,link);
      row.append(info,buttons);list.append(row);
    }
    if(!pins.length){
      const hint=document.createElement("p");hint.className="micro-copy";hint.textContent="Your shelf is empty. Browse public sources below and tap PIN SOURCE.";
      list.append(hint);
    }
  }

  function commit(next){
    pins=api.normalize(next,catalog);
    if(storage){
      try{storage.setItem(SHELF_KEY,JSON.stringify(api.packet(pins,catalog)));}
      catch{storage=null;}
    }
    render();
    document.dispatchEvent(new Event("phios-shelf-changed"));
  }

  exportButton.addEventListener("click",()=>{
    try{
      const content=JSON.stringify(api.packet(pins,catalog),null,2);
      const url=URL.createObjectURL(new Blob([content],{type:"application/json"}));
      const link=document.createElement("a");link.href=url;link.download="phios-app-shelf-r2.json";
      document.body.append(link);link.click();link.remove();
      setTimeout(()=>URL.revokeObjectURL(url),1000);
      status.textContent="Exported "+pins.length+" source bookmarks as readable JSON. No credentials or apps included.";
    }catch(error){status.textContent="Export refused: "+error.message;}
  });
  importButton.addEventListener("click",()=>fileInput.click());
  fileInput.addEventListener("change",async()=>{
    const file=fileInput.files?.[0];fileInput.value="";
    if(!file)return;
    if(file.size>api.MAX_BYTES){status.textContent="Import refused: 16 KiB limit";return;}
    try{
      const incoming=api.parse(await file.text(),catalog);
      if(!window.confirm("Replace only "+pins.length+" source bookmarks with "+incoming.length+" pins? Notes, Vault and Android apps stay unchanged."))return;
      commit(incoming);
    }catch(error){status.textContent="Shelf import refused: "+error.message;}
  });
  render();
  return {
    isPinned:id=>pins.includes(id),
    toggle(id){
      try{commit(pins.includes(id)?api.unpin(pins,id,catalog):api.pin(pins,id,catalog));}
      catch(error){status.textContent="Pin refused: "+error.message;}
    }
  };
}
