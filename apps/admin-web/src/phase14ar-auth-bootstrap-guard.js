/* Phase 14AR startup-safe auth bootstrap guard.
   The initial /api/auth/me probe is best-effort only. A true fail-open race returns
   a synthetic 401 after 8 seconds even if the underlying browser fetch never settles,
   so React can always leave the splash screen and show Sign in. */
const AUTH_BOOTSTRAP_TIMEOUT_MS=8000;
const priorFetch=globalThis.fetch.bind(globalThis);

function requestUrl(input){
  return typeof input==="string"?input:String(input?.url||"");
}
function requestMethod(input,init){
  return String(init?.method||(typeof input!=="string"?input?.method:"GET")||"GET").toUpperCase();
}
function isAuthBootstrap(input,init){
  if(requestMethod(input,init)!=="GET")return false;
  try{return new URL(requestUrl(input),window.location.origin).pathname==="/api/auth/me"}catch{return false}
}
function timeoutResponse(){
  return new Response(JSON.stringify({ok:false,error:"AUTH_BOOTSTRAP_TIMEOUT",message:"Session check timed out."}),{
    status:401,
    headers:{"content-type":"application/json; charset=utf-8","cache-control":"no-store","x-zhagmag-auth-bootstrap":"timeout"}
  });
}

globalThis.fetch=async(input,init={})=>{
  if(!isAuthBootstrap(input,init))return priorFetch(input,init);
  const controller=new AbortController();
  const upstream=init?.signal;
  let detach=null;
  if(upstream){
    if(upstream.aborted)controller.abort();
    else{
      const forwardAbort=()=>controller.abort();
      upstream.addEventListener("abort",forwardAbort,{once:true});
      detach=()=>upstream.removeEventListener("abort",forwardAbort);
    }
  }
  let timer=0;
  let timedOut=false;
  const network=priorFetch(input,{...init,signal:controller.signal}).catch(error=>{
    if(timedOut&&!upstream?.aborted)return timeoutResponse();
    throw error;
  });
  const timeout=new Promise(resolve=>{
    timer=window.setTimeout(()=>{
      timedOut=true;
      controller.abort();
      resolve(timeoutResponse());
    },AUTH_BOOTSTRAP_TIMEOUT_MS);
  });
  try{
    return await Promise.race([network,timeout]);
  }finally{
    if(timer)window.clearTimeout(timer);
    detach?.();
  }
};
