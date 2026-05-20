/**
 * Returns the HTML for the public form filler.
 * Self-contained: Tailwind via Play CDN + inline vanilla JS for rendering,
 * live calculations and submission. The form schema is inlined as JSON.
 */
export function publicFormHtml(opts: {
  slug: string;
  title: string;
  description: string;
  fields: unknown[];
  calculations: unknown[];
  submitUrl: string;
}): string {
  const dataPayload = JSON.stringify({
    title: opts.title,
    description: opts.description,
    fields: opts.fields,
    calculations: opts.calculations,
    slug: opts.slug,
    submitUrl: opts.submitUrl,
  });

  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1" />
<meta name="theme-color" content="#7c5cff" />
<title>${escapeHtml(opts.title)} – SuvForm</title>
<script src="https://cdn.tailwindcss.com"></script>
<style>
  :root { color-scheme: dark light; }
  body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", system-ui, sans-serif; }
  .field-card { transition: border-color .15s ease; }
  .field-card:focus-within { border-color: #7c5cff; box-shadow: 0 0 0 3px rgba(124,92,255,.12); }
  input[type="number"]::-webkit-inner-spin-button, input[type="number"]::-webkit-outer-spin-button {
    -webkit-appearance: none; margin: 0;
  }
</style>
</head>
<body class="min-h-screen bg-slate-50 text-slate-900 dark:bg-slate-950 dark:text-slate-100">
<div class="max-w-2xl mx-auto px-4 py-10">
  <header class="mb-6">
    <div class="text-xs uppercase tracking-wider text-violet-500 mb-2 font-semibold">SuvForm</div>
    <h1 id="form-title" class="text-3xl font-bold leading-tight"></h1>
    <p id="form-description" class="mt-2 text-slate-600 dark:text-slate-400"></p>
  </header>

  <form id="form-root" class="space-y-4"></form>

  <div id="calc-section" class="space-y-3 mt-6 hidden">
    <div class="text-sm font-semibold text-slate-500 uppercase tracking-wider">Live calculations</div>
    <div id="calc-list" class="space-y-2"></div>
  </div>

  <button id="submit-btn" type="button"
    class="mt-8 w-full bg-violet-600 hover:bg-violet-700 text-white font-semibold py-3.5 rounded-2xl shadow-lg shadow-violet-600/30 transition disabled:opacity-50 disabled:cursor-not-allowed">
    Submit
  </button>

  <p id="submit-msg" class="mt-3 text-center text-sm hidden"></p>

  <footer class="mt-12 text-center text-xs text-slate-400">
    Made with <span class="text-violet-500">✦</span> using
    <a class="underline hover:text-violet-500" href="https://github.com/suvojeet-sengupta/suvform" target="_blank" rel="noopener">SuvForm</a>
  </footer>
</div>

<script id="form-data" type="application/json">${dataPayload.replace(/</g, "\\u003c")}</script>
<script>
(function() {
  const FORM = JSON.parse(document.getElementById('form-data').textContent);
  document.getElementById('form-title').textContent = FORM.title || 'Untitled form';
  document.getElementById('form-description').textContent = FORM.description || '';
  document.title = (FORM.title || 'Untitled') + ' – SuvForm';

  const root = document.getElementById('form-root');
  const calcList = document.getElementById('calc-list');
  const calcSection = document.getElementById('calc-section');
  const submitBtn = document.getElementById('submit-btn');
  const submitMsg = document.getElementById('submit-msg');

  const answers = {};

  // ---------- Expression evaluator (mirrors Kotlin util/ExpressionEvaluator) ----------
  function tokenize(s) {
    const out = []; let i = 0;
    while (i < s.length) {
      const c = s[i];
      if (/\\s/.test(c)) { i++; continue; }
      if (/[0-9.]/.test(c)) {
        let j = i; while (j < s.length && /[0-9.]/.test(s[j])) j++;
        out.push({ t: 'num', v: parseFloat(s.slice(i, j)) }); i = j; continue;
      }
      if (/[a-zA-Z_]/.test(c)) {
        let j = i; while (j < s.length && /[a-zA-Z0-9_]/.test(s[j])) j++;
        out.push({ t: 'id', v: s.slice(i, j) }); i = j; continue;
      }
      if ('+-*/%()'.indexOf(c) >= 0) { out.push({ t: 'op', v: c }); i++; continue; }
      throw new Error('bad char ' + c);
    }
    out.push({ t: 'end' });
    return out;
  }
  function evalExpr(expr, vars) {
    const tk = tokenize(expr);
    let pos = 0;
    const peek = () => tk[pos];
    const eat = () => tk[pos++];
    function expr_() {
      let l = term();
      while (peek().t === 'op' && (peek().v === '+' || peek().v === '-')) {
        const op = eat().v; const r = term();
        l = op === '+' ? l + r : l - r;
      } return l;
    }
    function term() {
      let l = unary();
      while (peek().t === 'op' && (peek().v === '*' || peek().v === '/' || peek().v === '%')) {
        const op = eat().v; const r = unary();
        if ((op === '/' || op === '%') && r === 0) l = 0;
        else l = op === '*' ? l * r : op === '/' ? l / r : l % r;
      } return l;
    }
    function unary() {
      if (peek().t === 'op' && peek().v === '-') { eat(); return -unary(); }
      if (peek().t === 'op' && peek().v === '+') { eat(); return unary(); }
      return primary();
    }
    function primary() {
      const t = eat();
      if (t.t === 'num') return t.v;
      if (t.t === 'id') return Number(vars[t.v]) || 0;
      if (t.t === 'op' && t.v === '(') {
        const v = expr_(); if (eat().v !== ')') throw new Error('expected )');
        return v;
      }
      throw new Error('unexpected');
    }
    return expr_();
  }

  // ---------- Render ----------
  function makeCard(field) {
    const card = document.createElement('div');
    card.className = 'field-card rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-4';
    const label = document.createElement('label');
    label.className = 'block text-sm font-semibold text-slate-900 dark:text-slate-100 mb-2';
    label.textContent = field.label || 'Untitled';
    if (field.required) {
      const star = document.createElement('span');
      star.textContent = ' *'; star.className = 'text-rose-500'; label.appendChild(star);
    }
    card.appendChild(label);
    card.appendChild(makeInput(field));
    return card;
  }

  function makeInput(field) {
    const cls = 'w-full bg-transparent border-0 border-b-2 border-slate-200 dark:border-slate-700 focus:border-violet-500 focus:ring-0 py-2 text-base placeholder-slate-400';
    const cls2 = 'w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-transparent px-3 py-2 text-base placeholder-slate-400 focus:border-violet-500 focus:ring-0';
    switch (field.type) {
      case 'short_text':
      case 'email':
      case 'phone': {
        const i = document.createElement('input');
        i.type = field.type === 'email' ? 'email' : field.type === 'phone' ? 'tel' : 'text';
        i.className = cls;
        i.placeholder = field.placeholder || '';
        i.addEventListener('input', e => { answers[field.id] = e.target.value; recalc(); });
        return i;
      }
      case 'long_text': {
        const t = document.createElement('textarea');
        t.rows = 3; t.className = cls2; t.placeholder = field.placeholder || '';
        t.addEventListener('input', e => { answers[field.id] = e.target.value; recalc(); });
        return t;
      }
      case 'number': {
        const i = document.createElement('input');
        i.type = 'number'; i.className = cls; i.placeholder = field.placeholder || '';
        i.inputMode = 'decimal';
        i.addEventListener('input', e => { answers[field.id] = e.target.value; recalc(); });
        return i;
      }
      case 'date': {
        const i = document.createElement('input');
        i.type = 'date'; i.className = cls;
        i.addEventListener('input', e => { answers[field.id] = e.target.value; recalc(); });
        return i;
      }
      case 'single_choice': {
        const wrap = document.createElement('div'); wrap.className = 'space-y-1';
        (field.options || []).forEach((opt, idx) => {
          const row = document.createElement('label');
          row.className = 'flex items-center gap-3 py-1.5 cursor-pointer';
          const r = document.createElement('input');
          r.type = 'radio'; r.name = field.id; r.value = opt;
          r.className = 'h-4 w-4 text-violet-600 focus:ring-violet-500';
          r.addEventListener('change', () => { answers[field.id] = opt; recalc(); });
          row.appendChild(r);
          const sp = document.createElement('span'); sp.textContent = opt; row.appendChild(sp);
          wrap.appendChild(row);
        });
        return wrap;
      }
      case 'multi_choice': {
        const wrap = document.createElement('div'); wrap.className = 'space-y-1';
        answers[field.id] = [];
        (field.options || []).forEach(opt => {
          const row = document.createElement('label');
          row.className = 'flex items-center gap-3 py-1.5 cursor-pointer';
          const cb = document.createElement('input');
          cb.type = 'checkbox'; cb.value = opt;
          cb.className = 'h-4 w-4 rounded text-violet-600 focus:ring-violet-500';
          cb.addEventListener('change', e => {
            const arr = answers[field.id] || [];
            if (e.target.checked) arr.push(opt); else { const k = arr.indexOf(opt); if (k >= 0) arr.splice(k, 1); }
            answers[field.id] = arr; recalc();
          });
          row.appendChild(cb);
          const sp = document.createElement('span'); sp.textContent = opt; row.appendChild(sp);
          wrap.appendChild(row);
        });
        return wrap;
      }
      case 'rating': {
        const wrap = document.createElement('div'); wrap.className = 'flex items-center gap-1';
        const stars = [];
        for (let i = 1; i <= 5; i++) {
          const b = document.createElement('button');
          b.type = 'button';
          b.className = 'text-3xl text-slate-300 dark:text-slate-700 hover:text-amber-400 transition';
          b.textContent = '★';
          b.addEventListener('click', () => {
            const cur = answers[field.id] || 0;
            const next = cur === i ? 0 : i;
            answers[field.id] = next;
            stars.forEach((s, idx) => {
              s.classList.toggle('text-amber-400', idx < next);
              s.classList.toggle('text-slate-300', idx >= next);
            });
            recalc();
          });
          wrap.appendChild(b); stars.push(b);
        }
        return wrap;
      }
      default: {
        const span = document.createElement('span'); span.textContent = '(Unsupported field type: ' + field.type + ')'; return span;
      }
    }
  }

  (FORM.fields || []).forEach(f => root.appendChild(makeCard(f)));

  // ---------- Live calculations ----------
  function fmt(v, format) {
    if (format === 'percent') return v.toFixed(1) + '%';
    if (format === 'currency') return '₹' + v.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    if (Number.isInteger(v)) return v.toString();
    return v.toFixed(2);
  }
  const calcCards = {};
  function recalc() {
    const calcs = FORM.calculations || [];
    if (calcs.length === 0) return;
    calcSection.classList.remove('hidden');
    const vars = {};
    (FORM.fields || []).forEach(f => {
      const v = answers[f.id];
      if (f.type === 'number') vars[f.id] = parseFloat(v) || 0;
      else if (f.type === 'rating') vars[f.id] = Number(v) || 0;
      else vars[f.id] = 0;
    });
    calcs.forEach(c => {
      let val = 0;
      try { val = evalExpr(c.expression, vars); } catch (e) { val = 0; }
      let card = calcCards[c.id];
      if (!card) {
        card = document.createElement('div');
        card.className = 'rounded-2xl bg-violet-50 dark:bg-violet-950/40 border border-violet-200 dark:border-violet-900/60 p-4 flex items-center';
        card.innerHTML = '<div class="flex-1"><div class="font-semibold text-violet-900 dark:text-violet-200">' + escapeHtml(c.label || 'Calculation') + '</div>' +
          '<div class="text-xs text-violet-700/70 dark:text-violet-300/50 font-mono">' + escapeHtml(c.expression) + '</div></div>' +
          '<div class="text-2xl font-bold text-violet-900 dark:text-violet-100" data-value></div>';
        calcList.appendChild(card);
        calcCards[c.id] = card;
      }
      card.querySelector('[data-value]').textContent = fmt(val, c.format);
    });
  }
  recalc();

  function escapeHtml(s) {
    return String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  }

  // ---------- Submit ----------
  submitBtn.addEventListener('click', async () => {
    // Validate required
    const missing = [];
    (FORM.fields || []).forEach(f => {
      if (!f.required) return;
      const v = answers[f.id];
      if (v == null || v === '' || (Array.isArray(v) && v.length === 0)) missing.push(f.label);
    });
    if (missing.length) {
      submitMsg.textContent = 'Please fill: ' + missing.join(', ');
      submitMsg.className = 'mt-3 text-center text-sm text-rose-500';
      submitMsg.classList.remove('hidden');
      return;
    }

    submitBtn.disabled = true;
    submitBtn.textContent = 'Submitting…';
    submitMsg.classList.add('hidden');

    const calculated = {};
    const vars = {};
    (FORM.fields || []).forEach(f => {
      const v = answers[f.id];
      vars[f.id] = f.type === 'number' ? (parseFloat(v) || 0) : f.type === 'rating' ? (Number(v) || 0) : 0;
    });
    (FORM.calculations || []).forEach(c => {
      try { calculated[c.id] = evalExpr(c.expression, vars); } catch (e) { calculated[c.id] = 0; }
    });

    try {
      const res = await fetch(FORM.submitUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ answers, calculated }),
      });
      if (!res.ok) {
        const t = await res.text();
        throw new Error('HTTP ' + res.status + ': ' + t.slice(0, 200));
      }
      // Success state
      root.innerHTML = '';
      calcSection.classList.add('hidden');
      submitBtn.style.display = 'none';
      const ok = document.createElement('div');
      ok.className = 'text-center py-16';
      ok.innerHTML = '<div class="text-6xl mb-4">✅</div>' +
        '<h2 class="text-2xl font-bold mb-2">Thanks for your response!</h2>' +
        '<p class="text-slate-500">Your submission has been recorded.</p>';
      root.appendChild(ok);
    } catch (e) {
      submitBtn.disabled = false;
      submitBtn.textContent = 'Submit';
      submitMsg.textContent = e.message || 'Submission failed. Please try again.';
      submitMsg.className = 'mt-3 text-center text-sm text-rose-500';
      submitMsg.classList.remove('hidden');
    }
  });
})();
</script>
</body>
</html>`;
}

function escapeHtml(s: string): string {
  return s.replace(/[&<>"']/g, (c) => {
    const m: Record<string, string> = { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" };
    return m[c];
  });
}
