/**
 * Public form filler page — self-contained HTML with inline JS.
 * Editorial "paper" identity matching the SuvForm app (light only):
 * warm paper background, Fraunces serif headings, Geist body, red-orange accent.
 *
 * Enhancements included:
 * - Progress bar for long forms (5+ fields)
 * - Local draft auto-save (restored on reload, cleared on successful submit)
 * - Print-friendly view (clean @media print styles + button)
 * - Keyboard submit (⌘/Ctrl + Enter)
 * - Improved mobile focus + scroll behavior + touch feedback
 */
export function publicFormHtml(opts: {
  slug: string;
  title: string;
  description: string;
  fields: unknown[];
  calculations: unknown[];
  theme: any;
  versionId: string;
  responseLimit?: number | null;
  currentResponseCount?: number;
  isClosed?: boolean;
  submitUrl: string;
}): string {
  const theme = opts.theme || {};
  const dataPayload = JSON.stringify({
    title: opts.title,
    description: opts.description,
    fields: opts.fields,
    calculations: opts.calculations,
    versionId: opts.versionId,
    slug: opts.slug,
    submitUrl: opts.submitUrl,
    responseLimit: opts.responseLimit ?? null,
    currentResponseCount: opts.currentResponseCount ?? 0,
    isClosed: !!opts.isClosed,
  });

  const primaryColor = theme.primaryColor || '#E94221';
  const accentColor = theme.accentColor || '#E94221';
  const backgroundColor = theme.backgroundColor || '#F4F1EA';
  const cardBackgroundColor = theme.cardBackgroundColor || '#FFFFFF';
  const textColor = theme.textColor || '#0F0F10';
  const mutedTextColor = theme.mutedTextColor || '#6E6B62';
  
  const fontSans = theme.fontFamily === 'mono' ? 'JetBrains Mono' : theme.fontFamily === 'serif' ? 'Fraunces' : 'Geist';
  const fontSerif = theme.fontFamily === 'serif' ? 'Fraunces' : 'Fraunces';
  
  const borderRadius = theme.borderRadius === 'none' ? '0px' : 
                     theme.borderRadius === 'small' ? '4px' :
                     theme.borderRadius === 'large' ? '24px' :
                     theme.borderRadius === 'full' ? '9999px' : '12px'; // medium/default

  // Unsplash search for dynamic cover images based on keyword
  const finalCoverImage = theme.coverImageKeyword 
    ? `https://source.unsplash.com/featured/1200x400/?${encodeURIComponent(theme.coverImageKeyword)}`
    : null;

  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover" />
<meta name="theme-color" content="${backgroundColor}" />
<meta name="color-scheme" content="light" />
<meta name="referrer" content="strict-origin-when-cross-origin" />
<title>${escapeHtml(opts.title)} – SuvForm</title>
<meta property="og:title" content="${escapeHtml(opts.title)}" />
<meta property="og:description" content="${escapeHtml(opts.description || "Fill out this form")}" />
<meta property="og:type" content="website" />
${finalCoverImage ? `<meta property="og:image" content="${finalCoverImage}" />` : ''}
<link rel="preconnect" href="https://fonts.googleapis.com" />
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
<link href="https://fonts.googleapis.com/css2?family=Fraunces:ital,opsz,wght@0,9..144,300..600;1,9..144,300..600&family=Geist:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet" />
<script src="https://cdn.tailwindcss.com"></script>
<script>
  tailwind.config = {
    theme: {
      extend: {
        fontFamily: {
          serif: ['Fraunces', 'Georgia', 'serif'],
          sans: ['Geist', '-apple-system', 'BlinkMacSystemFont', 'system-ui', 'sans-serif'],
          mono: ['JetBrains Mono', 'ui-monospace', 'monospace'],
        },
        colors: {
          paper: '${backgroundColor}', paper2: '${backgroundColor}', ink: '${textColor}',
          muted: '${mutedTextColor}', muted2: '${mutedTextColor}CC', line: '#DDD6C7', line2: '#E8E2D2',
          accent: '${primaryColor}', accentsoft: '${primaryColor}22', accentdeep: '${primaryColor}',
          ok: '#1F7A4D', oksoft: '#DAEEDE',
        },
        borderRadius: {
          'form': '${borderRadius}',
        }
      },
    },
  };
</script>
<style>
  :root { color-scheme: light; }
  html { -webkit-font-smoothing: antialiased; text-rendering: optimizeLegibility; }
  body { 
    font-family: '${fontSans}', -apple-system, BlinkMacSystemFont, sans-serif; 
    background-color: ${backgroundColor};
    color: ${textColor};
  }
  .font-serif { font-family: '${fontSerif}', Georgia, serif; }
  .font-mono { font-family: 'JetBrains Mono', monospace; }
  
  input[type="number"]::-webkit-inner-spin-button,
  input[type="number"]::-webkit-outer-spin-button { -webkit-appearance: none; margin: 0; }
  input[type="date"] { color-scheme: light; }
  .field-input { border-radius: ${borderRadius}; }
  .field-input:focus { outline: none; border-color: ${primaryColor}; box-shadow: 0 0 0 3px ${primaryColor}22; }
  .submit-btn:active { transform: translateY(1px); }
  .submit-btn { border-radius: ${borderRadius}; background-color: ${primaryColor}; }
  input[type="radio"], input[type="checkbox"] { accent-color: ${primaryColor}; }
  
  #form-card {
    background-color: ${cardBackgroundColor};
    border-radius: ${borderRadius};
    ${cardBackgroundColor !== backgroundColor ? 'box-shadow: 0 4px 20px rgba(0,0,0,0.05);' : ''}
  }

  /* Print-friendly styles */
  @media print {
    @page { margin: 1.2cm; }
    body { background: white !important; color: #111 !important; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
    header, footer, #print-btn, #submit-btn, #submit-msg, #draft-status, #calc-section, .submit-btn, .cover-image { display: none !important; }
    main { max-width: 100% !important; padding: 0 !important; }
    #form-card { box-shadow: none !important; padding: 0 !important; background: white !important; }
    .field-input, textarea { border: 1px solid #ccc !important; background: white !important; }
    .field-input:focus { box-shadow: none !important; border-color: #999 !important; }
    label { color: #222 !important; }
    .text-muted, .text-muted2 { color: #555 !important; }
    #progress-container { display: none !important; }
    input[type="text"], input[type="email"], input[type="tel"], input[type="number"], input[type="date"], textarea {
      min-height: 2.1em;
      padding-bottom: 1.6em;
    }
    .space-y-5 > div { margin-bottom: 1.25rem !important; }
  }
</style>
</head>
<body class="min-h-screen bg-paper text-ink antialiased">

<!-- Top bar -->
<header class="border-b border-line/30">
  <div class="max-w-2xl mx-auto px-5 sm:px-6 py-3.5 flex items-center justify-between">
    <div class="flex items-center gap-2.5">
      <div class="h-8 w-8 rounded-[9px] bg-accent flex items-center justify-center">
        <span class="text-white text-base font-serif italic font-medium leading-none">S</span>
      </div>
      <span class="font-serif text-lg text-ink">SuvForm<span class="text-accent">.</span></span>
    </div>
    <div class="flex items-center gap-2">
      <button id="print-btn" type="button" aria-label="Print or save as PDF"
        class="flex items-center gap-1.5 px-2 py-1 rounded-lg text-muted hover:text-ink hover:bg-black/5 active:bg-black/10 transition text-[11px] font-mono uppercase tracking-wider">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"><path stroke-linecap="round" stroke-linejoin="round" d="M17 17h2a2 2 0 002-2v-4a2 2 0 00-2-2H3a2 2 0 00-2 2v4a2 2 0 002 2h2m2 4h6a2 2 0 002-2v-4a2 2 0 00-2-2H9a2 2 0 00-2 2v4a2 2 0 002 2zm8-12V5a2 2 0 00-2-2H9a2 2 0 00-2 2v4" /></svg>
        <span class="hidden sm:inline">Print</span>
      </button>
      <span class="font-mono text-[11px] uppercase tracking-wider text-muted">Secure form</span>
    </div>
  </div>
</header>

<main class="max-w-2xl mx-auto px-5 sm:px-6 py-8 sm:py-12">
  <div id="form-card" class="overflow-hidden">
    ${finalCoverImage ? `
      <div class="cover-image w-full h-40 sm:h-56 overflow-hidden">
        <img src="${finalCoverImage}" alt="Cover" class="w-full h-full object-cover" />
      </div>
    ` : ''}

    <div id="form-content" class="${finalCoverImage ? 'p-6 sm:p-10' : ''}">
      <!-- Form header -->
      <div class="mb-8">
        <h1 id="form-title" class="font-serif text-3xl sm:text-4xl tracking-tight leading-[1.05]"></h1>
        <p id="form-description" class="mt-3 text-muted leading-relaxed"></p>
      </div>

      <!-- Required note -->
      <p id="required-note" class="font-mono text-[11px] text-muted mb-5 hidden">
        <span class="text-accent">*</span> indicates a required field
      </p>

      <!-- Progress -->
      <div id="progress-container" class="hidden mb-6">
        <div class="flex items-center justify-between text-[11px] font-mono uppercase tracking-[0.08em] text-muted mb-1.5">
          <span>Progress</span>
          <span id="progress-text">0 of 0</span>
        </div>
        <div class="h-1.5 w-full bg-black/5 rounded-full overflow-hidden">
          <div id="progress-bar" class="h-full bg-accent transition-all duration-200 rounded-full" style="width: 0%"></div>
        </div>
      </div>

      <!-- Fields -->
      <form id="form-root" class="space-y-5"></form>

      <!-- Calculations summary -->
      <section id="calc-section" class="mt-8 hidden">
        <div class="font-mono text-[11px] font-medium uppercase tracking-[0.12em] text-muted mb-3">Summary</div>
        <div id="calc-list" class="rounded-form border border-line2 bg-white divide-y divide-line2"></div>
      </section>

      <!-- Submit -->
      <div class="mt-10">
        <button id="submit-btn" type="button"
          class="submit-btn w-full hover:opacity-90 text-white font-semibold py-3.5 transition disabled:opacity-50 disabled:cursor-not-allowed">
          Submit
        </button>
        <div class="mt-2 flex items-center justify-between text-[11px]">
          <span id="draft-status" class="font-mono text-muted2 opacity-70 hidden">Draft saved locally</span>
          <span class="font-mono text-muted2">Your response is private and only visible to the form's owner.</span>
        </div>
        <p id="submit-msg" class="mt-3 text-center text-sm hidden"></p>
      </div>
    </div>
  </div>

  <!-- Already responded -->
  <div id="already-responded" class="hidden text-center py-16 sm:py-24">
    <div class="inline-flex h-16 w-16 items-center justify-center rounded-full bg-accentsoft mb-6">
      <svg class="h-8 w-8 text-accentdeep" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
        <polyline points="22 4 12 14.01 9 11.01"></polyline>
      </svg>
    </div>
    <h2 class="font-serif text-2xl mb-2">You've already responded</h2>
    <p class="text-muted max-w-sm mx-auto leading-relaxed mb-8">
      You have already submitted this form. You can submit another response if you need to provide new information.
    </p>
    <button id="refill-btn" class="inline-flex items-center justify-center px-8 py-3.5 text-base font-semibold rounded-2xl text-white bg-ink hover:opacity-90 transition active:scale-95">
      Submit another response
    </button>
  </div>

  <!-- Success -->
  <div id="success-content" class="hidden text-center py-16 sm:py-24">
    <div class="inline-flex h-16 w-16 items-center justify-center rounded-full bg-oksoft mb-6">
      <svg class="h-8 w-8 text-ok" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="20 6 9 17 4 12"></polyline>
      </svg>
    </div>
    <h2 class="font-serif text-2xl mb-2">Response submitted</h2>
    <p class="text-muted max-w-sm mx-auto leading-relaxed">Thank you. Your response has been recorded and sent to the form owner.</p>
  </div>

  <!-- Form Closed (response limit reached) -->
  <div id="form-closed" class="hidden text-center py-16 sm:py-24">
    <div class="inline-flex h-16 w-16 items-center justify-center rounded-full bg-accentsoft mb-6">
      <svg class="h-8 w-8 text-accentdeep" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2Zm0 18a8 8 0 1 1 0-16 8 8 0 0 1 0 16Z"/>
        <path d="M15 9l-6 6M9 9l6 6"/>
      </svg>
    </div>
    <h2 class="font-serif text-2xl mb-2">This form is closed</h2>
    <p class="text-muted max-w-sm mx-auto leading-relaxed">
      The owner has set a limit on the number of responses. This form is no longer accepting new submissions.
    </p>
    <p id="closed-count" class="mt-4 text-sm text-muted2 font-mono"></p>
  </div>

  <!-- Footer -->
  <footer class="mt-16 pt-6 border-t border-line text-center">
    <p class="font-mono text-[11px] text-muted2">Powered by <a href="https://github.com/suvojeet-sengupta/suvform" target="_blank" rel="noopener" class="font-medium text-muted hover:text-ink transition-colors">SuvForm</a></p>
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
  const formClosed = document.getElementById('form-closed');
  const refillBtn = document.getElementById('refill-btn');

  function init() {
    if (FORM.isClosed) {
      formContent.classList.add('hidden');
      alreadyResponded.classList.add('hidden');
      if (formClosed) {
        formClosed.classList.remove('hidden');
        const countEl = document.getElementById('closed-count');
        if (countEl && FORM.responseLimit) {
          countEl.textContent = \`\${FORM.currentResponseCount} / \${FORM.responseLimit} responses received\`;
        }
      }
      return;
    }

    if (localStorage.getItem(STORAGE_KEY)) {
      formContent.classList.add('hidden');
      alreadyResponded.classList.remove('hidden');
    } else {
      formContent.classList.remove('hidden');
      alreadyResponded.classList.add('hidden');
      loadDraft(); // restore any previous work
    }
  }

  refillBtn.addEventListener('click', () => {
    localStorage.removeItem(STORAGE_KEY);
    init();
    // Re-apply any restored draft into the (still existing) inputs
    if (!formContent.classList.contains('hidden')) {
      hydrateFromAnswers();
    }
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
  const draftStatus = document.getElementById('draft-status');
  const progressContainer = document.getElementById('progress-container');
  const progressBar = document.getElementById('progress-bar');
  const progressText = document.getElementById('progress-text');
  const printBtn = document.getElementById('print-btn');

  const DRAFT_KEY = 'draft:' + FORM.slug;
  let saveDraftTimer = null;

  const answers = {};

  // Central place to update an answer (used by all field inputs)
  function updateAnswer(id, value) {
    answers[id] = value;
    scheduleSaveDraft();
    recalc();
    updateProgress();
  }

  function scheduleSaveDraft() {
    if (saveDraftTimer) clearTimeout(saveDraftTimer);
    saveDraftTimer = setTimeout(() => {
      try {
        localStorage.setItem(DRAFT_KEY, JSON.stringify(answers));
        if (draftStatus) {
          draftStatus.classList.remove('hidden');
          draftStatus.style.opacity = '0.85';
        }
      } catch (e) { /* quota or private mode */ }
    }, 420); // gentle debounce
  }

  function loadDraft() {
    try {
      const raw = localStorage.getItem(DRAFT_KEY);
      if (!raw) return;
      const saved = JSON.parse(raw);
      if (saved && typeof saved === 'object') {
        Object.assign(answers, saved);
        if (draftStatus) draftStatus.classList.remove('hidden');
      }
    } catch (e) { /* ignore */ }
  }

  function clearDraft() {
    try { localStorage.removeItem(DRAFT_KEY); } catch (e) {}
    if (draftStatus) draftStatus.classList.add('hidden');
  }

  function countAnswered() {
    const fields = FORM.fields || [];
    let answered = 0;
    for (const f of fields) {
      const v = answers[f.id];
      const empty = v == null || v === '' || (Array.isArray(v) && v.length === 0);
      if (!empty) answered++;
    }
    return { answered, total: fields.length };
  }

  function updateProgress() {
    const { answered, total } = countAnswered();
    if (!progressContainer || !progressBar || !progressText) return;
    if (total < 5) { // only show for reasonably long forms
      progressContainer.classList.add('hidden');
      return;
    }
    progressContainer.classList.remove('hidden');
    const pct = total > 0 ? Math.round((answered / total) * 100) : 0;
    progressBar.style.width = pct + '%';
    progressText.textContent = \`\${answered} of \${total}\`;
  }

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
  const FIELD_INPUT_CLS = 'field-input w-full bg-white border border-line2 rounded-xl px-3.5 py-3 text-[15px] text-ink placeholder-muted2 transition-colors';

  function makeFieldBlock(field) {
    const block = document.createElement('div');
    const label = document.createElement('label');
    label.className = 'block text-sm font-medium text-ink mb-2';
    label.textContent = field.label || 'Untitled';
    if (field.required) {
      const star = document.createElement('span');
      star.textContent = ' *'; star.className = 'text-accent'; label.appendChild(star);
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
        i.dataset.fieldId = field.id;
        if (field.type === 'email') i.autocomplete = 'email';
        if (field.type === 'phone') i.autocomplete = 'tel';
        i.addEventListener('input', e => updateAnswer(field.id, e.target.value));
        return i;
      }
      case 'long_text': {
        const t = document.createElement('textarea');
        t.rows = 4;
        t.className = FIELD_INPUT_CLS + ' resize-y';
        t.placeholder = field.placeholder || '';
        t.dataset.fieldId = field.id;
        t.addEventListener('input', e => updateAnswer(field.id, e.target.value));
        return t;
      }
      case 'number': {
        const i = document.createElement('input');
        i.type = 'number';
        i.className = FIELD_INPUT_CLS;
        i.placeholder = field.placeholder || '';
        i.inputMode = 'decimal';
        i.dataset.fieldId = field.id;
        i.addEventListener('input', e => updateAnswer(field.id, e.target.value));
        return i;
      }
      case 'date': {
        const i = document.createElement('input');
        i.type = 'date';
        i.className = FIELD_INPUT_CLS;
        i.dataset.fieldId = field.id;
        i.addEventListener('input', e => updateAnswer(field.id, e.target.value));
        return i;
      }
      case 'single_choice': {
        const wrap = document.createElement('div'); wrap.className = 'space-y-2';
        (field.options || []).forEach(opt => {
          const row = document.createElement('label');
          row.className = 'flex items-center gap-3 px-3.5 py-3 border border-line2 rounded-xl cursor-pointer hover:bg-paper2 transition-colors has-[:checked]:border-accent has-[:checked]:bg-accentsoft';
          const r = document.createElement('input');
          r.type = 'radio'; r.name = field.id; r.value = opt;
          r.className = 'h-4 w-4';
          r.dataset.fieldId = field.id;
          r.addEventListener('change', () => updateAnswer(field.id, opt));
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
          row.className = 'flex items-center gap-3 px-3.5 py-3 border border-line2 rounded-xl cursor-pointer hover:bg-paper2 transition-colors has-[:checked]:border-accent has-[:checked]:bg-accentsoft';
          const cb = document.createElement('input');
          cb.type = 'checkbox'; cb.value = opt;
          cb.className = 'h-4 w-4 rounded';
          cb.dataset.fieldId = field.id;
          cb.addEventListener('change', e => {
            const arr = answers[field.id] || [];
            if (e.target.checked) arr.push(opt);
            else { const k = arr.indexOf(opt); if (k >= 0) arr.splice(k, 1); }
            updateAnswer(field.id, arr);
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
          b.className = 'h-10 w-10 rounded-lg flex items-center justify-center text-2xl text-line hover:text-accent transition-colors';
          b.setAttribute('aria-label', i + ' stars');
          b.textContent = '★';
          b.addEventListener('click', () => {
            const cur = answers[field.id] || 0;
            const next = cur === i ? 0 : i;
            updateAnswer(field.id, next);
            stars.forEach((s, idx) => {
              s.classList.toggle('text-accent', idx < next);
              s.classList.toggle('text-line', idx >= next);
            });
          });
          wrap.appendChild(b); stars.push(b);
        }
        return wrap;
      }
      default: {
        const span = document.createElement('span');
        span.className = 'text-muted2 italic';
        span.textContent = 'Unsupported field type: ' + field.type;
        return span;
      }
    }
  }

  (FORM.fields || []).forEach(f => root.appendChild(makeFieldBlock(f)));

  // Re-render values from answers into DOM (used after draft load)
  function hydrateFromAnswers() {
    (FORM.fields || []).forEach(f => {
      const val = answers[f.id];
      if (val == null || val === '') return;

      const selector = \`[data-field-id="\${f.id}"]\`;
      const els = root.querySelectorAll(selector);
      if (!els.length) return;

      els.forEach(el => {
        if (el.type === 'radio') {
          if (el.value === val) el.checked = true;
        } else if (el.type === 'checkbox') {
          if (Array.isArray(val) && val.includes(el.value)) el.checked = true;
        } else {
          el.value = Array.isArray(val) ? '' : val; // safety
        }
      });
    });
    updateProgress();
  }

  hydrateFromAnswers();

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
        row.innerHTML = '<div class="font-medium text-ink text-sm">' + escapeHtml(c.label || 'Calculation') + '</div>' +
          '<div class="font-serif text-lg text-ink tabular-nums" data-value></div>';
        calcList.appendChild(row);
        calcRows[c.id] = row;
      }
      row.querySelector('[data-value]').textContent = fmt(val, c.format);
    });
  }
  recalc();
  updateProgress();

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
      submitMsg.className = 'mt-3 text-center text-sm text-accent';
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
        body: JSON.stringify({ answers, calculated, versionId: FORM.versionId }),
      });
      if (!res.ok) {
        const t = await res.text();
        throw new Error('HTTP ' + res.status + ': ' + t.slice(0, 200));
      }
      localStorage.setItem(STORAGE_KEY, '1');
      clearDraft();
      showSuccess();
    } catch (e) {
      submitBtn.disabled = false;
      submitBtn.textContent = 'Submit';

      if (e && e.message && e.message.includes('response_limit_reached')) {
        submitMsg.textContent = 'This form has reached its response limit.';
        // Hide form and show closed state
        formContent.classList.add('hidden');
        if (formClosed) formClosed.classList.remove('hidden');
      } else {
        submitMsg.textContent = (e && e.message) || 'Submission failed. Please try again.';
      }
      submitMsg.className = 'mt-3 text-center text-sm text-accent';
      submitMsg.classList.remove('hidden');
    }
  });

  function showSuccess() {
    formContent.classList.add('hidden');
    successContent.classList.remove('hidden');
  }

  // ---------- Keyboard + Print + Mobile polish ----------
  if (printBtn) {
    printBtn.addEventListener('click', () => window.print());
    // Also allow keyboard access on mobile header
    printBtn.setAttribute('tabindex', '0');
  }

  // Global keyboard submit (Cmd/Ctrl + Enter)
  document.addEventListener('keydown', (e) => {
    if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
      // Only trigger if the form is visible and not already submitting
      if (!formContent.classList.contains('hidden') && !submitBtn.disabled) {
        e.preventDefault();
        submitBtn.click();
      }
    }
    // Escape clears submit error message
    if (e.key === 'Escape' && !submitMsg.classList.contains('hidden')) {
      submitMsg.classList.add('hidden');
    }
  });

  // Improve mobile experience: scroll the submit area into view when last field is focused
  // (helps when virtual keyboard appears on long forms)
  const lastField = root.lastElementChild;
  if (lastField) {
    const focusables = lastField.querySelectorAll('input, textarea, button');
    focusables.forEach(el => {
      el.addEventListener('focus', () => {
        // Small delay so keyboard has time to appear
        setTimeout(() => {
          el.scrollIntoView({ block: 'center', behavior: 'smooth' });
        }, 120);
      }, { passive: true });
    });
  }

  // Make rating stars more touch friendly on mobile (larger hit area via CSS is already good)
  // Add subtle active state feedback for all choice rows
  root.querySelectorAll('label').forEach(lab => {
    lab.addEventListener('touchstart', () => lab.classList.add('!border-accent'), { passive: true });
    lab.addEventListener('touchend', () => lab.classList.remove('!border-accent'), { passive: true });
  });

  // Initial progress in case of restored draft
  updateProgress();
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
