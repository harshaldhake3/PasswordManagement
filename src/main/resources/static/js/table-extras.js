// CSV export & simple column visibility toggles
(function(){
  function toCsv(table){
    const rows = Array.from(table.querySelectorAll('tr'));
    return rows.map(tr => Array.from(tr.cells).map(td => {
      const t = td.innerText.replace(/"/g,'""');
      return `"${t}"`;
    }).join(',')).join('\n');
  }
  window.bindCsvExport = function(btnSelector, tableSelector){
    const btn = document.querySelector(btnSelector);
    const table = document.querySelector(tableSelector);
    if(!btn || !table) return;
    btn.addEventListener('click', ()=>{
      const csv = toCsv(table);
      const blob = new Blob([csv], {type:'text/csv'});
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = 'table.csv';
      a.click();
    });
  }
  window.bindColumnToggles = function(containerSelector, tableSelector){
    const container = document.querySelector(containerSelector);
    const table = document.querySelector(tableSelector);
    if(!container || !table) return;
    Array.from(table.tHead.rows[0].cells).forEach((th, i)=>{
      const id = 'col_'+i;
      const label = th.innerText || `Col ${i+1}`;
      const wrap = document.createElement('label');
      wrap.className = 'me-3';
      wrap.innerHTML = `<input type="checkbox" id="${id}" checked> ${label}`;
      container.appendChild(wrap);
      wrap.querySelector('input').addEventListener('change', e=>{
        const show = e.target.checked;
        Array.from(table.rows).forEach(r => {
          if(r.cells[i]) r.cells[i].style.display = show ? '' : 'none';
        });
      });
    });
  }
})();
