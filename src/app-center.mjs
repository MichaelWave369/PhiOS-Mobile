/* R4 App Center: one reviewed browser PWA, not an Android APK or install grant. */
import { APP_CENTER_REVIEWS } from "./app-center-data.mjs";
import { PUBLIC_CATALOG } from "./catalog-data.mjs";

export function reviewedPhiOSWeb(reviews=APP_CENTER_REVIEWS,catalog=PUBLIC_CATALOG){
  if(!reviews||reviews.schema!=="phios.app-center-reviews.v1"||
    reviews.catalog_source_sha!==catalog?.provenance?.source_blob_sha||
    reviews.policy?.source_listing_is_not_installation!==true||
    reviews.policy?.autonomous_installs!==false||
    reviews.policy?.native_install_grants!==false||
    reviews.policy?.user_gesture_required!==true||
    reviews.policy?.browser_controls_pwa_install!==true||
    !Array.isArray(reviews.reviewed_web_apps)||reviews.reviewed_web_apps.length!==1)return null;
  const app=reviews.reviewed_web_apps[0];
  const source=catalog.apps?.find(x=>x.id==="github:1442243366");
  if(!source||source.name!=="phi-field"||
    source.repository!=="https://github.com/MichaelWave369/phi-field"||
    app?.app_id!==source.id||app.name!=="PhiOS Web"||app.repository!==source.repository||
    app.runtime!=="browser_pwa"||app.review_status!=="ci_reviewed_browser_app"||
    app.launch_url!=="https://phi-field.com/os/"||app.scope_path!=="/os/"||
    app.evidence?.repository!=="MichaelWave369/phi-field"||
    app.evidence?.test_path!=="tests/browser_web_os.py"||app.evidence?.source_pr!==8||
    app.evidence?.evidence_scope!=="static_build_and_chromium_ci"||
    app.evidence?.live_endpoint_independently_verified!==false||
    app.actions?.open!=="explicit_navigation"||
    app.actions?.install!=="browser_mediated_if_available"||
    app.actions?.native_install!==false||app.actions?.automatic_execution!==false)return null;
  return app;
}

export function initReviewedAppCenter(){
  const card=document.getElementById("reviewed-app-center");
  const app=reviewedPhiOSWeb();
  if(!card)return;
  const status=document.getElementById("reviewed-app-status");
  const open=document.getElementById("reviewed-app-open");
  if(!app){
    if(status)status.textContent="Reviewed-app metadata unavailable or invalid. No app launch allowed.";
    if(open)open.hidden=true;
    return;
  }
  if(status)status.textContent="1 CI-reviewed browser PWA · production endpoint not independently verified · "+(PUBLIC_CATALOG.apps.length-1)+" other public sources remain source-only";
  const context=document.getElementById("mobile-pwa-context");
  if(context){
    const standalone=window.matchMedia?.("(display-mode: standalone)")?.matches===true||
      window.navigator.standalone===true;
    context.textContent=standalone
      ?"Current Mobile PWA is running in standalone display mode; install version and provenance are not attested."
      :"Current Mobile PWA is a browser session. Installed status on this device is unknown.";
  }
  const check=document.getElementById("mobile-pwa-update");
  const updateStatus=document.getElementById("mobile-pwa-update-status");
  if(check&&updateStatus)check.addEventListener("click",async()=>{
    if(!("serviceWorker" in navigator)){
      updateStatus.textContent="No service-worker update interface on this browser.";
      return;
    }
    updateStatus.textContent="Checking this origin's PWA service worker…";
    try{
      const registration=await navigator.serviceWorker.getRegistration("./");
      if(!registration){
        updateStatus.textContent="No PWA service-worker registration found. No update applied.";
        return;
      }
      await registration.update();
      updateStatus.textContent="Service-worker update check finished; a newer installation is not guaranteed. Manage or uninstall in browser/device settings.";
    }catch{
      updateStatus.textContent="PWA update check unavailable, possibly offline. No update applied or claimed.";
    }
  });
  if(open){
    open.href=app.launch_url;
    open.target="_blank";
    open.rel="noopener noreferrer";
    open.hidden=false;
    open.setAttribute("aria-label","Open PhiOS Web in an external browser tab");
  }
}
