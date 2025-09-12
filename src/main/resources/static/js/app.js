
(function(){
  const q=(s,el=document)=>el.querySelector(s), qa=(s,el=document)=>Array.from(el.querySelectorAll(s));
  const on=(el,ev,fn)=>el&&el.addEventListener(ev,fn);
  const toast=(msg)=>{ let tc=q('#toast-container'); if(!tc){ tc=document.createElement('div'); tc.id='toast-container'; tc.className='toast-container'; document.body.appendChild(tc);} const t=document.createElement('div'); t.className='alert alert-primary shadow-sm py-2 px-3 mb-2'; t.textContent=msg; tc.appendChild(t); setTimeout(()=>t.remove(),2200); };

  // Theme
  const themeKey='pm.theme', applyTheme=(t)=>{ if(t==='dark') document.documentElement.classList.add('dark'); else document.documentElement.classList.remove('dark'); localStorage.setItem(themeKey,t); };
  const initTheme=()=>{ applyTheme(localStorage.getItem(themeKey)||'light'); const btn=document.createElement('button'); btn.className='btn btn-outline-secondary theme-toggle'; btn.type='button'; btn.id='theme-toggle'; btn.innerHTML=(localStorage.getItem(themeKey)==='dark')?'☀️ Light':'🌙 Dark'; btn.onclick=()=>{ const now=document.documentElement.classList.contains('dark')?'light':'dark'; applyTheme(now); btn.innerHTML=(now==='dark')?'☀️ Light':'🌙 Dark'; }; document.body.appendChild(btn); };

  // Local Notes (optional, private)
  const NOTES_PREFIX='pm.notes:', notesKey=(id)=>NOTES_PREFIX+id, getNote=(id)=>localStorage.getItem(notesKey(id))||'', setNote=(id,text)=>localStorage.setItem(notesKey(id),text||'');
  const exportNotes=()=>{ const out={}; for(let i=0;i<localStorage.length;i++){ const k=localStorage.key(i); if(k&&k.startsWith(NOTES_PREFIX)){ out[k.slice(NOTES_PREFIX.length)]=localStorage.getItem(k);} } const blob=new Blob([JSON.stringify(out,null,2)],{type:'application/json'}); const url=URL.createObjectURL(blob); const a=document.createElement('a'); a.href=url; a.download='passman-notes.json'; a.click(); URL.revokeObjectURL(url); toast('Local notes exported'); };
  const importNotes=(file)=>{ const fr=new FileReader(); fr.onload=()=>{ try{ const data=JSON.parse(fr.result); Object.entries(data).forEach(([id,text])=>setNote(id,text)); toast('Local notes imported'); }catch(e){ toast('Import failed'); } }; fr.readAsText(file); };

  // Filter/sort
  const initTableTools=()=>{
    const tbl=q('table.table'); if(!tbl) return;
    const filter=q('#filterBox'); on(filter,'input',()=>{ const v=filter.value.toLowerCase(); qa('tbody tr',tbl).forEach(tr=>{ const t=tr.innerText.toLowerCase(); tr.style.display=t.includes(v)?'':''; }); });
    qa('thead th',tbl).forEach((th,idx)=>{ th.addEventListener('click',()=>{ const rows=qa('tbody tr',tbl); const dir=th.dataset.dir=(th.dataset.dir==='asc'?'desc':'asc'); rows.sort((a,b)=>{ const at=a.children[idx]?.innerText.toLowerCase()||''; const bt=b.children[idx]?.innerText.toLowerCase()||''; return dir==='asc'?at.localeCompare(bt):bt.localeCompare(at); }).forEach(r=>tbl.tBodies[0].appendChild(r)); }); });
    on(q('#exportNotesBtn'),'click',exportNotes);
    on(q('#importNotesInput'),'change',e=>{ if(e.target.files[0]) importNotes(e.target.files[0]); });
  };

  // Local notes modal
  const ensureModal=()=>{ if(q('#notesModal')) return; const modal=document.createElement('div'); modal.id='notesModal'; modal.className='modal fade show'; modal.style.cssText='display:none; background:rgba(0,0,0,0.5); position:fixed; inset:0; z-index:1055;'; modal.innerHTML=`
    <div class="d-flex align-items-center justify-content-center h-100">
      <div class="card shadow" style="width: min(640px, 94vw);">
        <div class="card-body">
          <div class="d-flex justify-content-between align-items-center mb-2">
            <h5 class="m-0">Local Notes</h5>
            <button type="button" class="btn btn-sm btn-outline-secondary" id="notesClose">Close</button>
          </div>
          <div class="mb-3"><textarea class="form-control notes-area" id="notesArea" placeholder="Write private notes…"></textarea></div>
          <div class="d-flex justify-content-end gap-2">
            <button class="btn btn-primary" id="notesSave">Save</button>
          </div>
        </div>
      </div>
    </div>`; document.body.appendChild(modal); };
  const openNotes=(id)=>{ ensureModal(); const modal=q('#notesModal'); modal.style.display='block'; modal.dataset.id=id; q('#notesArea').value=getNote(id); q('#notesClose').onclick=()=>{ modal.style.display='none'; }; q('#notesSave').onclick=()=>{ setNote(id,q('#notesArea').value); toast('Local notes saved'); modal.style.display='none'; }; };
  const initRowNotesButtons=()=>{ qa('tbody tr').forEach(tr=>{ if(tr.querySelector('.notes-btn')){ const id=tr.getAttribute('data-id'); const btn=tr.querySelector('.notes-btn'); btn.onclick=()=> openNotes(id); } }); };

  // View page actions
  const initViewPage=()=>{
    const wrap=q('[data-cred-id]'); if(!wrap) return;
    const id=wrap.getAttribute('data-cred-id');
    const pwEl=q('#pwText'), userEl=q('#userText');
    on(q('#copyUser'),'click',()=>{ navigator.clipboard.writeText(userEl?.innerText||''); toast('Username copied'); });
    on(q('#copyPass'),'click',()=>{ const v=pwEl?.innerText||''; navigator.clipboard.writeText(v).then(()=>{ toast('Password copied'); setTimeout(()=> navigator.clipboard.writeText(''), 20000); }); });
    on(q('#togglePass'),'click',()=>{ if(!pwEl) return; const masked=pwEl.style.filter==='blur(6px)'; pwEl.style.filter=masked?'none':'blur(6px)'; q('#togglePass').innerText=masked?'Hide':'Show'; });
  };

  // Add/Edit: strength + generator + toggle
  const strength=(s)=>{ let sc=0; if(!s) return 0; if(s.length>=12) sc++; if(/[A-Z]/.test(s)&&/[a-z]/.test(s)) sc++; if(/[0-9]/.test(s)) sc++; if(/[^A-Za-z0-9]/.test(s)) sc++; return sc; };
  const updateStrength=(val)=>{ const bar=q('#pwStrength'); if(!bar) return; const sc=strength(val); bar.style.width=(sc*25)+'%'; bar.className='progress-bar '+(['bg-danger','bg-danger','bg-warning','bg-info','bg-success'][sc]); bar.setAttribute('aria-valuenow', String(sc*25)); };
  const genPass=()=>{ const chars='abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()-_=+[]{};:,.?'; let out=''; for(let i=0;i<20;i++) out += chars[Math.floor(Math.random()*chars.length)]; return out; };
  const initAddEdit=()=>{ const pw=q('input[name="password"]'); const toggle=q('#togglePwInput'); const gen=q('#genPw'); on(pw,'input',()=> updateStrength(pw.value)); on(toggle,'click',()=>{ pw.type=(pw.type==='password'?'text':'password'); toggle.innerText=pw.type==='password'?'Show':'Hide'; }); on(gen,'click',()=>{ const g=genPass(); pw.value=g; updateStrength(g); toast('Password generated'); }); };

  // Delete confirm
  const initDeleteConfirm=()=>{ qa('a.btn.btn-sm.btn-danger, a.btn.btn-danger').forEach(a=>{ if(a.dataset.confirm) return; a.dataset.confirm='true'; a.addEventListener('click', (e)=>{ if(!confirm('Delete this credential?')) e.preventDefault(); }); }); };

  // Auto-lock (idle logout)
  const initIdleLogout=()=>{ let t; const reset=()=>{ clearTimeout(t); t=setTimeout(()=>{ window.location.href='/logout'; }, 5*60*1000); }; ['click','keydown','mousemove','scroll','touchstart'].forEach(ev=> window.addEventListener(ev, reset)); reset(); };

  document.addEventListener('DOMContentLoaded', ()=>{ initTheme(); initTableTools(); initRowNotesButtons(); initViewPage(); initAddEdit(); initDeleteConfirm(); initIdleLogout(); });
})();

// tag badge quick filter
(function(){ const filter=document.querySelector('#filterBox'); if(!filter) return; document.querySelectorAll('td span.badge').forEach(b=>{ b.addEventListener('click',()=>{ filter.value=b.textContent.trim(); filter.dispatchEvent(new Event('input')); }); }); })();
