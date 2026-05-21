/**
 * Public form filler page — self-contained HTML with inline JS.
 * Clean, professional layout (Inter font, indigo accent, slate neutrals).
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
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover" />
<meta name="theme-color" content="#ffffff" media="(prefers-color-scheme: light)" />
<meta name="theme-color" content="#020617" media="(prefers-color-scheme: dark)" />
<meta name="color-scheme" content="light dark" />
<meta name="referrer" content="strict-origin-when-cross-origin" />
<title>${escapeHtml(opts.title)} – SuvForm</title>
<meta property="og:title" content="${escapeHtml(opts.title)}" />
<meta property="og:description" content="${escapeHtml(opts.description || "Fill out this form")}" />
<meta property="og:type" content="website" />
<link rel="preconnect" href="https://fonts.googleapis.com" />
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet" />
<script src="https://cdn.tailwindcss.com"></script>
<script>
  tailwind.config = {
    theme: {
      extend: {
        fontFamily: { sans: ['Inter', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'system-ui', 'sans-serif'] },
        colors: {
          brand: { 50: '#eef2ff', 100: '#e0e7ff', 500: '#6366f1', 600: '#4f46e5', 700: '#4338ca', 900: '#312e81' }
        }
      }
    }
  };
</script>
<style>
  :root { color-scheme: light dark; }
  html { font-feature-settings: "cv02","cv03","cv04","cv11"; -webkit-font-smoothing: antialiased; }
  body { font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif; }
  input[type="number"]::-webkit-inner-spin-button,
  input[type="number"]::-webkit-outer-spin-button { -webkit-appearance: none; margin: 0; }
  input[type="date"] { color-scheme: light dark; }
  /* Subtle focus ring, accessible */
  .field-input:focus { outline: none; border-color: #6366f1; box-shadow: 0 0 0 3px rgba(99,102,241,.15); }
  .submit-btn:active { transform: translateY(1px); }
  /* Hide default radio/checkbox accent in dark mode */
  input[type="radio"], input[type="checkbox"] { accent-color: #4f46e5; }
</style>
</head>
<body class="min-h-screen bg-white text-slate-900 dark:bg-slate-950 dark:text-slate-100 antialiased">

<!-- Top bar -->
<header class="border-b border-slate-200 dark:border-slate-800/60">
  <div class="max-w-2xl mx-auto px-5 sm:px-6 py-3 flex items-center justify-between">
    <div class="flex items-center gap-2.5">
      <div class="h-7 w-7 rounded-lg bg-slate-900 dark:bg-white flex items-center justify-center">
        <span class="text-white dark:text-slate-900 text-sm font-bold leading-none">S</span>
      </div>
      <span class="text-sm font-semibold text-slate-700 dark:text-slate-300">SuvForm</span>
    </div>
    <span class="text-xs text-slate-400 hidden sm:inline">Secure form</span>
  </div>
</header>

<main class="max-w-2xl mx-auto px-5 sm:px-6 py-8 sm:py-12">
  <div id="form-content">
    <!-- Form header -->
    <div class="mb-8">
      <h1 id="form-title" class="text-2xl sm:text-3xl font-semibold tracking-tight leading-tight"></h1>
      <p id="form-description" class="mt-3 text-slate-600 dark:text-slate-400 leading-relaxed"></p>
    </div>

    <!-- Required note -->
    <p id="required-note" class="text-xs text-slate-500 mb-5 hidden">
      <span class="text-rose-500">*</span> indicates a required field
    </p>

    <!-- Fields -->
    <form id="form-root" class="space-y-5"></form>

    <!-- Calculations summary -->
    <section id="calc-section" class="mt-8 hidden">
      <div class="text-xs font-semibold uppercase tracking-wider text-slate-500 mb-3">Summary</div>
      <div id="calc-list" class="rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900/40 divide-y divide-slate-200 dark:divide-slate-800"></div>
    </section>

    <!-- Submit -->
    <div class="mt-10">
      <button id="submit-btn" type="button"
        class="submit-btn w-full bg-brand-600 hover:bg-brand-700 active:bg-brand-700 text-white font-semibold py-3.5 rounded-xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
        Submit
      </button>
      <p id="submit-msg" class="mt-3 text-center text-sm hidden"></p>
      <p class="mt-4 text-xs text-slate-400 text-center">Your response is private and only visible to the form's owner.</p>
    </div>
  </div>

  <!-- Already responded -->
  <div id="already-responded" class="hidden text-center py-16 sm:py-24 animate-in fade-in zoom-in duration-500">
    <div class="inline-flex h-16 w-16 items-center justify-center rounded-full bg-blue-50 dark:bg-blue-950/40 mb-6">
      <svg class="h-8 w-8 text-blue-600 dark:text-blue-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
        <polyline points="22 4 12 14.01 9 11.01"></polyline>
      </svg>
    </div>
    <h2 class="text-2xl font-semibold mb-2">You've already responded</h2>
    <p class="text-slate-600 dark:text-slate-400 max-w-sm mx-auto leading-relaxed mb-8">
      You have already submitted this form. You can submit another response if you need to provide new information.
    </p>
    <button id="refill-btn" class="inline-flex items-center justify-center px-8 py-3.5 border border-transparent text-base font-semibold rounded-xl text-white bg-brand-600 hover:bg-brand-700 transition-all shadow-lg shadow-brand-500/20 active:scale-95">
      Submit another response
    </button>
  </div>

  <!-- Success -->
  <div id="success-content" class="hidden text-center py-16 sm:py-24 animate-in fade-in zoom-in duration-500">
    <div class="inline-flex h-16 w-16 items-center justify-center rounded-full bg-emerald-50 dark:bg-emerald-950/40 mb-6">
      <svg class="h-8 w-8 text-emerald-600 dark:text-emerald-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="20 6 9 17 4 12"></polyline>
      </svg>
    </div>
    <h2 class="text-2xl font-semibold mb-2">Response submitted</h2>
    <p class="text-slate-600 dark:text-slate-400 max-w-sm mx-auto leading-relaxed">Thank you. Your response has been recorded and sent to the form owner.</p>
  </div>

  <!-- Footer -->
  <footer class="mt-16 pt-6 border-t border-slate-200 dark:border-slate-800/60 text-center">
    <p class="text-xs text-slate-400">Powered by <a href="https://github.com/suvojeet-sengupta/suvform" target="_blank" rel="noopener" class="font-medium text-slate-500 hover:text-slate-700 dark:hover:text-slate-300 transition-colors">SuvForm</a></p>
  </footer>
</main>

<script id="form-data" type="application/json">${dataPayload.replace(/</g, "\\u003c")}</script>
<script>
(function() {
  const FORM = JSON.parse(document.getElementById('form-data').textContent);
  const STORAGE_KEY = 'sub:' + FORM.slug;

  const formContent = document.getElementById('form-content');
  const alreadyResponded = document.getElementById('already-responded');
  const successContent = document.getElementById('success-content');
  const refillBtn = document.getElementById('refill-btn');

  function init() {
    if (localStorage.getItem(STORAGE_KEY)) {
      formContent.classList.add('hidden');
      alreadyResponded.classList.remove('hidden');
    } else {
      formContent.classList.remove('hidden');
      alreadyResponded.classList.add('hidden');
    }
  }

  refillBtn.addEventListener('click', () => {
    localStorage.removeItem(STORAGE_KEY);
    init();
  });

  init();

  document.getElementById('form-title').textContent = FORM.title || 'Untitled form';
  const descEl = document.getElementById('form-description');
  if (FORM.description && FORM.description.trim()) descEl.textContent = FORM.description;
  else descEl.style.display = 'none';
  document.title = (FORM.title || 'Untitled') + ' – SuvForm';

  const hasRequired = (FORM.fields || []).some(f => f.required);
  if (hasRequired) document.getElementById('required-note').classList.remove('hidden');

  const root = document.getElementById('form-root');
  const calcList = document.getElementById('calc-list');
  const calcSection = document.getElementById('calc-section');
  const submitBtn = document.getElementById('submit-btn');
  const submitMsg = document.getElementById('submit-msg');

  const answers = {};

  // ---------- Expression evaluator ----------
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
  const FIELD_INPUT_CLS = 'field-input w-full bg-white dark:bg-slate-900 border border-slate-300 dark:border-slate-700 rounded-lg px-3.5 py-2.5 text-[15px] placeholder-slate-400 dark:placeholder-slate-500 transition-colors';

  function makeFieldBlock(field) {
    const block = document.createElement('div');
    const label = document.createElement('label');
    label.className = 'block text-sm font-medium text-slate-900 dark:text-slate-100 mb-2';
    label.textContent = field.label || 'Untitled';
    if (field.required) {
      const star = document.createElement('span');
      star.textContent = ' *'; star.className = 'text-rose-500'; label.appendChild(star);
    }
    block.appendChild(label);
    block.appendChild(makeInput(field));
    return block;
  }

  function makeInput(field) {
    switch (field.type) {
      case 'short_text':
      case 'email':
      case 'phone': {
        const i = document.createElement('input');
        i.type = field.type === 'email' ? 'email' : field.type === 'phone' ? 'tel' : 'text';
        i.className = FIELD_INPUT_CLS;
        i.placeholder = field.placeholder || '';
        if (field.type === 'email') i.autocomplete = 'email';
        if (field.type === 'phone') i.autocomplete = 'tel';
        i.addEventListener('input', e => { answers[field.id] = e.target.value; recalc(); });
        return i;
      }
      case 'long_text': {
        const t = document.createElement('textarea');
        t.rows = 4;
        t.className = FIELD_INPUT_CLS + ' resize-y';
        t.placeholder = field.placeholder || '';
        t.addEventListener('input', e => { answers[field.id] = e.target.value; recalc(); });
        return t;
      }
      case 'number': {
        const i = document.createElement('input');
        i.type = 'number';
        i.className = FIELD_INPUT_CLS;
        i.placeholder = field.placeholder || '';
        i.inputMode = 'decimal';
        i.addEventListener('input', e => { answers[field.id] = e.target.value; recalc(); });
        return i;
      }
      case 'date': {
        const i = document.createElement('input');
        i.type = 'date';
        i.className = FIELD_INPUT_CLS;
        i.addEventListener('input', e => { answers[field.id] = e.target.value; recalc(); });
        return i;
      }
      case 'single_choice': {
        const wrap = document.createElement('div'); wrap.className = 'space-y-2';
        (field.options || []).forEach(opt => {
          const row = document.createElement('label');
          row.className = 'flex items-center gap-3 px-3.5 py-2.5 border border-slate-300 dark:border-slate-700 rounded-lg cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-900/50 transition-colors has-[:checked]:border-brand-600 has-[:checked]:bg-brand-50 dark:has-[:checked]:bg-brand-900/20';
          const r = document.createElement('input');
          r.type = 'radio'; r.name = field.id; r.value = opt;
          r.className = 'h-4 w-4';
          r.addEventListener('change', () => { answers[field.id] = opt; recalc(); });
          row.appendChild(r);
          const sp = document.createElement('span');
          sp.textContent = opt;
          sp.className = 'text-[15px]';
          row.appendChild(sp);
          wrap.appendChild(row);
        });
        return wrap;
      }
      case 'multi_choice': {
        const wrap = document.createElement('div'); wrap.className = 'space-y-2';
        answers[field.id] = [];
        (field.options || []).forEach(opt => {
          const row = document.createElement('label');
          row.className = 'flex items-center gap-3 px-3.5 py-2.5 border border-slate-300 dark:border-slate-700 rounded-lg cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-900/50 transition-colors has-[:checked]:border-brand-600 has-[:checked]:bg-brand-50 dark:has-[:checked]:bg-brand-900/20';
          const cb = document.createElement('input');
          cb.type = 'checkbox'; cb.value = opt;
          cb.className = 'h-4 w-4 rounded';
          cb.addEventListener('change', e => {
            const arr = answers[field.id] || [];
            if (e.target.checked) arr.push(opt);
            else { const k = arr.indexOf(opt); if (k >= 0) arr.splice(k, 1); }
            answers[field.id] = arr; recalc();
          });
          row.appendChild(cb);
          const sp = document.createElement('span');
          sp.textContent = opt;
          sp.className = 'text-[15px]';
          row.appendChild(sp);
          wrap.appendChild(row);
        });
        return wrap;
      }
      case 'rating': {
        const wrap = document.createElement('div');
        wrap.className = 'flex items-center gap-1.5';
        const stars = [];
        for (let i = 1; i <= 5; i++) {
          const b = document.createElement('button');
          b.type = 'button';
          b.className = 'h-10 w-10 rounded-lg flex items-center justify-center text-2xl text-slate-300 dark:text-slate-700 hover:text-amber-400 hover:bg-slate-50 dark:hover:bg-slate-900/50 transition-colors';
          b.setAttribute('aria-label', i + ' stars');
          b.textContent = '★';
          b.addEventListener('click', () => {
            const cur = answers[field.id] || 0;
            const next = cur === i ? 0 : i;
            answers[field.id] = next;
            stars.forEach((s, idx) => {
              s.classList.toggle('text-amber-400', idx < next);
              s.classList.toggle('text-slate-300', idx >= next);
              s.classList.toggle('dark:text-slate-700', idx >= next);
            });
            recalc();
          });
          wrap.appendChild(b); stars.push(b);
        }
        return wrap;
      }
      default: {
        const span = document.createElement('span');
        span.className = 'text-slate-400 italic';
        span.textContent = 'Unsupported field type: ' + field.type;
        return span;
      }
    }
  }

  (FORM.fields || []).forEach(f => root.appendChild(makeFieldBlock(f)));

  // ---------- Live calculations ----------
  function fmt(v, format) {
    if (format === 'percent') return v.toFixed(1) + '%';
    if (format === 'currency') return '₹' + v.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    if (Number.isInteger(v)) return v.toString();
    return v.toFixed(2);
  }
  const calcRows = {};
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
      let row = calcRows[c.id];
      if (!row) {
        row = document.createElement('div');
        row.className = 'flex items-center justify-between px-4 py-3';
        row.innerHTML = '<div class="font-medium text-slate-700 dark:text-slate-300 text-sm">' + escapeHtml(c.label || 'Calculation') + '</div>' +
          '<div class="text-base font-semibold text-slate-900 dark:text-slate-100 tabular-nums" data-value></div>';
        calcList.appendChild(row);
        calcRows[c.id] = row;
      }
      row.querySelector('[data-value]').textContent = fmt(val, c.format);
    });
  }
  recalc();

  function escapeHtml(s) {
    return String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  }

  // ---------- Submit ----------
  submitBtn.addEventListener('click', async () => {
    const missing = [];
    (FORM.fields || []).forEach(f => {
      if (!f.required) return;
      const v = answers[f.id];
      if (v == null || v === '' || (Array.isArray(v) && v.length === 0)) missing.push(f.label);
    });
    if (missing.length) {
      submitMsg.textContent = 'Please fill in: ' + missing.join(', ');
      submitMsg.className = 'mt-3 text-center text-sm text-rose-500';
      submitMsg.classList.remove('hidden');
      return;
    }

    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="inline-flex items-center gap-2"><svg class="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" opacity=".25"/><path d="M22 12a10 10 0 00-10-10" stroke="currentColor" stroke-width="3" stroke-linecap="round"/></svg>Submitting…</span>';
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
      // Success
      localStorage.setItem(STORAGE_KEY, '1');
      showSuccess();
    } catch (e) {
      submitBtn.disabled = false;
      submitBtn.textContent = 'Submit';
      submitMsg.textContent = e.message || 'Submission failed. Please try again.';
      submitMsg.className = 'mt-3 text-center text-sm text-rose-500';
      submitMsg.classList.remove('hidden');
    }
  });

  function showSuccess() {
    formContent.classList.add('hidden');
    successContent.classList.remove('hidden');
  }
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
