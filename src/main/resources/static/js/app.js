(function(){
  const q = (s, el=document)=>el.querySelector(s);
  const qa = (s, el=document)=>Array.from(el.querySelectorAll(s));
  const on = (el, ev, fn)=>el && el.addEventListener(ev, fn);
  const toast = (msg)=>{
    let tc = q('#toast-container');
    if(!tc){
      tc = document.createElement('div');
      tc.id = 'toast-container';
      tc.className = 'toast-container';
      document.body.appendChild(tc);
    }
    const t = document.createElement('div');
    t.className = 'alert alert-primary shadow-sm py-2 px-3 mb-2';
    t.textContent = msg;
    tc.appendChild(t);
    setTimeout(()=>t.remove(), 5000);
  };
  window.showToast = toast;

  function initTheme(){
    const saved = localStorage.getItem('theme');
    if(saved === 'dark') document.documentElement.classList.add('dark');
    const btn = q('#themeToggle');
    on(btn, 'click', ()=>{
      document.documentElement.classList.toggle('dark');
      localStorage.setItem('theme', document.documentElement.classList.contains('dark') ? 'dark' : 'light');
    });
  }

  function initCopyButtons(){
    qa('[data-copy]').forEach(btn=>{
      on(btn,'click', async ()=>{
        const text = btn.getAttribute('data-copy') || btn.previousElementSibling?.textContent;
        if(!text) return;
        await navigator.clipboard.writeText(text);
        toast('Copied to clipboard');
      });
    });
  }

  function sortableTable(){
    const table = q('[data-table]');
    if(!table) return;
    const tbody = table.tBodies[0];
    const rows = ()=>Array.from(tbody.querySelectorAll('tr'));
    let sortKey = 0, sortAsc = true;

    // filtering
    const filterInput = q('#tableFilter');
    on(filterInput, 'input', ()=>apply());

    // pagination
    const pager = q('#pager'), rowsInfo = q('#rowsInfo');
    const pageSizeSel = q('#pageSize');
    let pageSize = pageSizeSel ? parseInt(pageSizeSel.value,10) : 25;
    on(pageSizeSel, 'change', ()=>{ pageSize = parseInt(pageSizeSel.value,10); apply(); });
    let page = 1;

    function apply(){
      const term = (filterInput?.value || '').toLowerCase();
      let r = rows().map(tr=>{
        const text = tr.innerText.toLowerCase();
        tr.dataset.match = text.includes(term) ? '1' : '0';
        return tr;
      }).filter(tr=>tr.dataset.match === '1');

      // sort
      r.sort((a,b)=>{
        const aText = a.children[sortKey].innerText.trim().toLowerCase();
        const bText = b.children[sortKey].innerText.trim().toLowerCase();
        return sortAsc ? aText.localeCompare(bText) : bText.localeCompare(aText);
      });

      // paginate
      const total = r.length;
      const pages = Math.max(1, Math.ceil(total / pageSize));
      if(page > pages) page = pages;
      tbody.innerHTML='';
      r.slice((page-1)*pageSize, page*pageSize).forEach(tr=>tbody.appendChild(tr));
      // pager ui
      if(pager){
        pager.innerHTML='';
        function btn(n, label=n){
          const li = document.createElement('li'); li.className='page-item'+(n===page?' active':'');
          const a = document.createElement('a'); a.className='page-link'; a.href='#'; a.textContent=label;
          a.addEventListener('click', e=>{ e.preventDefault(); page=n; apply(); });
          li.appendChild(a); pager.appendChild(li);
        }
        btn(Math.max(1,page-1), '«');
        for(let i=1;i<=pages && i<=7;i++) btn(i);
        btn(Math.min(pages,page+1), '»');
      }
      if(rowsInfo) rowsInfo.textContent = `${total} item(s), page ${page} of ${pages}`;
    }

    qa('th[data-sort="text"]', table).forEach((th,i)=>{
      th.style.cursor = 'pointer';
      th.addEventListener('click', ()=>{
        sortAsc = sortKey === i ? !sortAsc : true;
        sortKey = i;
        apply();
      });
    });

    apply();
  }

  function initDeleteConfirm(){
    qa('[data-delete]').forEach(a=>{
      a.addEventListener('click', (e)=>{
        if(!confirm('Delete this credential?')) e.preventDefault();
      });
    });
  }

  document.addEventListener('DOMContentLoaded', ()=>{
    initTheme();
    sortableTable();
    initCopyButtons();
    initDeleteConfirm();
  });
})();
