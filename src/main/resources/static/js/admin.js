document.addEventListener('DOMContentLoaded', () => {
  const ta = document.querySelector('textarea[name="q"]');
  if (ta) {
    ta.addEventListener('keydown', (e) => {
      if (e.key === 'Tab') { e.preventDefault(); const s = ta.selectionStart; ta.value = ta.value.slice(0,s) + '  ' + ta.value.slice(s); ta.selectionStart = ta.selectionEnd = s + 2; }
    });
  }
});
