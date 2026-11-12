/* News Crux — UPSC CSE current-affairs app (WebView UI). */
(function () {
  'use strict';

  /* ---------------- bridge ---------------- */
  const N = window.NBNative || {
    boot: () => '{}', search: () => '[]', edition: () => 'null', live: () => '{"topics":[]}', feed: () => '[]', factbank: () => '[]', health: () => '[]',
    checklog: () => '[]', asset: () => '', bookmarks: () => '[]', saveBookmarks() {}, saveFactbank() {}, sync: () => 'busy', generateAi: () => 'busy',
    setString() {}, setBool() {}, setInt() {}, setLong() {}, openUrl(u) { window.open(u, '_blank'); }, share() {}, copy() {}, setBars() {},
    testNotification() {}, requestNotifPermission() {}, openBatterySettings() {}, openNotifSettings() {}, resetData() {},
    reported: () => '[]', saveReported() {}, backup() {}, restore() {}, printHtml() {}, openAppSettings() {}, updateInfo: () => '{}', checkUpdate() {}, installUpdate() {}, pwExplain: () => '', pyqQuestion: () => '', pyqTries: () => '{}', savePyqTries() {},
    shareImage(d) { const w = window.open(); if (w) w.document.write('<img src="' + d + '">'); }, saveImage(d, n) { const a = document.createElement('a'); a.href = d; a.download = n + '.png'; a.click(); return 'ok'; }
  };
  const $ = (s, r) => (r || document).querySelector(s);
  const $$ = (s, r) => Array.from((r || document).querySelectorAll(s));
  const J = (s, d) => { try { const v = JSON.parse(s); return v == null ? d : v; } catch (e) { return d; } };
  const ESC = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&apos;' };
  const esc = s => String(s == null ? '' : s).replace(/[&<>"']/g, c => ESC[c]);

  /* ---------------- icons ---------------- */
  const P = {
    bolt: '<path d="M13 2 4 14h7l-1 8 9-12h-7z"/>',
    refresh: '<path d="M21 12a9 9 0 1 1-3-6.7L21 8"/><path d="M21 3v5h-5"/>',
    gear: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z"/>',
    paper: '<path d="M4 4h13a3 3 0 0 1 3 3v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2z"/><path d="M8 8h8M8 12h8M8 16h5"/>',
    pulse: '<path d="M3 12h4l3-8 4 16 3-8h4"/>',
    book: '<path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20V3H6.5A2.5 2.5 0 0 0 4 5.5z"/><path d="M4 19.5A2.5 2.5 0 0 0 6.5 22H20v-5"/>',
    layers: '<path d="m12 2 10 5-10 5L2 7z"/><path d="m2 17 10 5 10-5"/><path d="m2 12 10 5 10-5"/>',
    star: '<path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1 6.2L12 17.3 6.5 20.2l1-6.2L3 9.6l6.2-.9z"/>',
    share: '<circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><path d="m8.6 13.5 6.8 4M15.4 6.5l-6.8 4"/>',
    copy: '<rect x="9" y="9" width="12" height="12" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>',
    link: '<path d="M10 13a5 5 0 0 0 7.5.5l3-3a5 5 0 0 0-7-7l-1.7 1.7"/><path d="M14 11a5 5 0 0 0-7.5-.5l-3 3a5 5 0 0 0 7 7l1.7-1.7"/>',
    pin: '<path d="M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 0 1 16 0z"/><circle cx="12" cy="10" r="3"/>',
    search: '<circle cx="11" cy="11" r="7"/><path d="m21 21-4.3-4.3"/>',
    check: '<path d="M20 6 9 17l-5-5"/>',
    alert: '<path d="M12 9v4M12 17h.01"/><path d="M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z"/>',
    x: '<path d="M18 6 6 18M6 6l12 12"/>',
    chev: '<path d="m9 18 6-6-6-6"/>',
    down: '<path d="m6 9 6 6 6-6"/>',
    back: '<path d="m15 18-6-6 6-6"/>',
    spark: '<path d="M12 3v4M12 17v4M3 12h4M17 12h4M5.6 5.6l2.8 2.8M15.6 15.6l2.8 2.8M5.6 18.4l2.8-2.8M15.6 8.4l2.8-2.8"/>',
    bell: '<path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.9 1.9 0 0 0 3.4 0"/>',
    archive: '<rect x="2" y="3" width="20" height="5" rx="1"/><path d="M4 8v11a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8M10 12h4"/>',
    plus: '<path d="M12 5v14M5 12h14"/>',
    save: '<path d="M12 3v12M7 10l5 5 5-5"/><path d="M5 21h14"/>',
    trash: '<path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6"/>',
    target: '<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="5"/><circle cx="12" cy="12" r="1"/>',
    shield: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><path d="m9 12 2 2 4-4"/>',
    clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
    key: '<circle cx="7.5" cy="15.5" r="4.5"/><path d="m10.7 12.3 9.3-9.3M17 6l3 3M14 9l2 2"/>',
    flag: '<path d="M4 22V4M4 4h12l-2 4 2 4H4"/>',
    eye: '<path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/>',
    file: '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6M8 13h8M8 17h5"/>',
    sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/>',
    thread: '<path d="M6 3v12"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="6" r="3"/><path d="M18 9a9 9 0 0 1-9 9"/>',
    flame: '<path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.4-.5-2-1-3-1.1-2.1-.2-4 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.2.4-2.3 1-3.3.3 1.6 1.2 2.8 2.5 2.8z"/>'
  };
  const ic = (n, c) => `<svg class="i ${c || ''}" viewBox="0 0 24 24" aria-hidden="true">${P[n] || ''}</svg>`;

  /* ---------------- KB lookups ---------------- */
  const KB = window.KB || { themes: [], entities: [], maps: [] };
  const ENT = {}, MAP = {}, THEME = {}, CON = {};
  KB.entities.forEach(e => (ENT[e.id] = e));
  (KB.concepts || []).forEach(c => (CON[c.id] = c));
  KB.maps.forEach(m => (MAP[m.id] = m));
  KB.themes.forEach(t => (THEME[t.id] = t));
  const SRC_NAME = {};
  (window.SOURCES || []).forEach(s => (SRC_NAME[s.code] = s.name));

  /* ---------------- state ---------------- */
  const S = {
    tab: 'edition', boot: {}, date: null, mode: 'smart', ed: null, live: null, liveFilter: 'All', liveMode: 'stories', feedSrc: '',
    bookmarks: [], pyq: { subj: 'All', q: '', year: 'All', limit: 40 }, rev: 'archive', factQ: '', flash: null, syncing: false,
    reported: [], hidden: new Set(), revSpan: 7, cuOpen: new Set(), cuBusy: {}, cuErr: {}
  };
  /* Report error: reported lines are hidden everywhere and kept in a shareable list */
  const rkey = s => String(s || '').toLowerCase().replace(/\s+/g, ' ').trim();
  function loadReported() { S.reported = J(N.reported(), []); S.hidden = new Set(S.reported.map(r => r.k)); }
  const hid = s => S.hidden.has(rkey(s));

  /* ---------------- utils ---------------- */
  const pad = n => (n < 10 ? '0' : '') + n;
  const IST_OFF = 330 * 60000;
  function istParts(ms) { const d = new Date(ms + IST_OFF); return { y: d.getUTCFullYear(), m: d.getUTCMonth(), d: d.getUTCDate(), h: d.getUTCHours(), mi: d.getUTCMinutes(), wd: d.getUTCDay() }; }
  const MON = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  const WD = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  function hhmm(ms) { const p = istParts(ms); const h = p.h % 12 || 12; return `${h}:${pad(p.mi)} ${p.h < 12 ? 'AM' : 'PM'}`; }
  function ago(ms) {
    if (!ms) return 'never';
    const s = (Date.now() - ms) / 1000;
    if (s < 60) return 'just now';
    if (s < 3600) return Math.floor(s / 60) + 'm ago';
    if (s < 86400) return Math.floor(s / 3600) + 'h ago';
    return Math.floor(s / 86400) + 'd ago';
  }
  function keyParts(k) { const [y, m, d] = k.split('-').map(Number); const dt = new Date(Date.UTC(y, m - 1, d)); return { d, mon: MON[m - 1], wd: WD[dt.getUTCDay()], y }; }
  function short(msg) { // a pop-up is two short lines at most: drop the technical tail of long messages
    let m = String(msg == null ? '' : msg).trim();
    if (m.length > 64) { const k = m.search(/(: |; | — | \()/); if (k >= 12) m = m.slice(0, k); }
    return m.length > 80 ? m.slice(0, 78).replace(/\s+\S*$/, '') + '…' : m;
  }
  function toast(msg) { const t = $('#toast'); t.textContent = short(msg); t.classList.add('on'); clearTimeout(toast._t); toast._t = setTimeout(() => t.classList.remove('on'), 2600); }
  const FIG = /(₹\s?[\d][\d,.]*(?:\s?(?:lakh crore|crore|lakh|bn|mn|tn|k))?|\$\s?[\d][\d,.]*(?:\s?(?:bn|mn|tn|billion|million|trillion))?|\b\d[\d,.]*\s?(?:%|per ?cent|crore|lakh|bn|mn|tn|km|sq km|MW|GW|GWh|ha|tonnes?|MT|years?|yrs|days?|months?|members|countries|states|seats|judges)(?![a-z])|\b(?:1[89]|20)\d\d(?:[-–]\d{2,4})?\b|\b\d+[-–]\d+\b|\bArts?\s?\d+[A-Z]?(?:\(\d+\))?|\b\d+(?:st|nd|rd|th)\b|\b\d[\d,.]*\b)/g;
  // a figure two outlets (or an official release) agree on gets a small tick; fx = {figure key: "TH,PIB"} from the app
  const figKey = f => String(f).toLowerCase().replace(/rs\.?\s?/g, '₹').replace(/per ?cent/g, '%').replace(/billion/g, 'bn').replace(/million/g, 'mn').replace(/trillion/g, 'tn').replace(/hectares?/g, 'ha').replace(/tonnes/g, 'tonne').replace(/[\s,]/g, '').replace(/\.$/, '');
  const figs = (s, fx) => esc(s).replace(FIG, (m, g) => { const k = figKey(m), by = fx && (fx[k] || fx['₹' + k] || fx[k.replace(/^[₹$]/, '')]); return by ? `<span class="fig fok">${g}<span class="fv" aria-label="checked in ${esc(by.replace(/,/g, ', '))}">✓</span></span>` : `<span class="fig">${g}</span>`; });
  function gsClass(gs) { gs = gs || ''; if (gs.startsWith('GS-I/')) return 'gs1'; if (gs.startsWith('GS-II/')) return 'gs2'; if (gs.startsWith('GS-III/')) return 'gs3'; if (gs.startsWith('GS-IV')) return 'gs4'; return 'gsm'; }
  function gsPaper(gs) { const m = /^GS-(I{1,3}|IV)\//.exec(gs || ''); return m ? 'GS-' + m[1] : 'Misc'; }

  /* ---------------- PYQ line (evidence computed natively from your PYQ database) ---------------- */
  const PYQ = window.PYQ || [];
  const PAPER = { CSE: 'Civil Services Prelims', CDS: 'CDS', CAPF: 'CAPF', NDA: 'NDA', CGS: 'Geo-Scientist', EPFO: 'EPFO', ESE: 'Engineering Services' };
  function pyqBlock(t) {
    const line = String(t.pyq || '');
    const ev = t.pyqEv || [];
    if (ev.length) {
      const m = /^PYQ:\s*(?:(.+?)\s+—\s+)?/.exec(line);
      const who = m && m[1] ? m[1] : '';
      // grouped by paper (CSE Prelims first), one row per distinct "what was asked", years merged
      const EX = /\s\((CDS|CAPF|NDA|CGS|EPFO|ESE)\)$/;
      const groups = {}, order = ['CSE', 'CDS', 'CAPF', 'NDA', 'CGS', 'EPFO', 'ESE'];
      ev.forEach(e => {
        const m2 = EX.exec(String(e[1])), ex = m2 ? m2[1] : 'CSE', a = m2 ? String(e[1]).replace(EX, '') : String(e[1]);
        const g = groups[ex] || (groups[ex] = { rows: [], idx: {} }), k = a.toLowerCase();
        if (g.idx[k] == null) { g.idx[k] = g.rows.length; g.rows.push({ a, y: [e[0]], q: [e[2] || ''] }); }
        else if (g.rows[g.idx[k]].q.indexOf(e[2] || '') < 0) { g.rows[g.idx[k]].y.push(e[0]); g.rows[g.idx[k]].q.push(e[2] || ''); }
      });
      const tr = tries();
      // every year opens its question (question, options; the answer after you try)
      const yr = (r, j) => { if (!r.q[j]) return `<span class="pqy">’${pad(r.y[j] % 100)}</span>`; const x = tr[r.q[j]], m = x ? (x[0] ? ' r' : ' w') : ''; return `<button class="pqy pqo${m}" data-pyq="${esc(r.q[j])}" aria-label="Try the ${r.y[j]} question${x ? (x[0] ? ', answered right before' : ', missed before') : ''}">’${pad(r.y[j] % 100)}${x ? (x[0] ? ' ✓' : ' ✗') : ''}</button>`; };
      const row = r => `<div class="pqr${r.q.some(Boolean) ? ' tap' : ''}" data-pyq="${r.q.some(Boolean) ? esc(r.q.find(Boolean)) : '-'}" role="button"><span class="pqys">${r.y.map((y, j) => yr(r, j)).join('')}</span><span class="pqa">${r.a === 'asked' ? 'asked (try the question)' : esc(r.a)}</span></div>`;
      const present = order.filter(x => groups[x]), multi = present.length > 1 || !groups.CSE;
      let shown = 0, total = 0;
      const body = present.map(x => {
        const rs = groups[x].rows; total += rs.length;
        const hidden = shown >= 6;
        const html = rs.map(r => { shown++; return shown > 6 ? row(r).replace('class="pqr', 'class="pqr pqmore') : row(r); }).join('');
        return (multi ? `<div class="pqg${hidden ? ' pqmore' : ''}">${esc(PAPER[x])} <span>${rs.reduce((n, r) => n + r.q.filter(Boolean).length, 0)}</span></div>` : '') + html;
      }).join('');
      const more = total > 6 ? `<button class="pqall" data-act="pqall" data-lab="Show all ${total} questions">Show all ${total} questions</button>` : '';
      return `<div class="pyq"><div class="lab">${ic('target', 'xs')}<span>${multi ? 'Asked in UPSC exams' : 'Asked in Prelims'}${who ? `: <b>${esc(who)}</b>` : ''}</span></div>${body}${more}</div>`;
    }
    const th = THEME[t.theme];
    const ang = th && th.angles ? th.angles : [];
    const body = ang.length ? `<p class="pqn">No direct PYQ on this exact story. UPSC asks <b>${esc(th.label)}</b> as:</p><div class="pqs">${ang.map(a => `<span class="pq">${esc(a)}</span>`).join('')}</div>`
      : `<p>${esc(line.replace(/^PYQ THEME:\s*/, ''))}</p>`;
    return `<div class="pyq"><div class="lab">${ic('target', 'xs')}How Prelims asks this</div>${body}</div>`;
  }

  /* ---------------- shell ---------------- */
  function applyTheme() {
    const st = S.boot.settings || {};
    let th = st.theme || 'auto';
    if (th === 'auto') th = typeof S.boot.systemDark === 'boolean' ? (S.boot.systemDark ? 'dark' : 'light') : (window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark');
    document.documentElement.dataset.theme = th;
    document.documentElement.style.setProperty('--fs', ((st.textScale || 100) / 100).toFixed(2));
    N.setBars(th === 'light' ? '#F2F5F3' : '#1D2527', th !== 'light');
  }
  function loadBoot() {
    S.boot = J(N.boot(), {});
    S.boot.settings = S.boot.settings || {};
    S.boot.editions = S.boot.editions || [];
    S.syncing = !!S.boot.syncing;
    setSyncBtn();
  }
  /* Sync progress is drawn on the C of the logo: the curve fills as the sync moves on. null = idle. */
  function setProg(p) {
    const lg = $('#logo'); if (!lg) return;
    const lp = lg.querySelector('.lp'), was = lg.classList.contains('busy');
    if (p != null && !was) { lp.style.transition = 'none'; lp.style.strokeDashoffset = '100'; lp.getBoundingClientRect(); lp.style.transition = ''; }
    lg.classList.toggle('busy', p != null);
    S.pct = p == null ? 0 : p;
    lp.style.strokeDashoffset = p == null ? '0' : String(100 - Math.max(0, Math.min(100, p)));
  }
  function setSyncBtn() {
    if (S.syncing && !S.pct) setProg(6);
    const b = $('#btnSync');
    b.innerHTML = ic('refresh');
    b.classList.toggle('spin', S.syncing);
    $('#prog').classList.toggle('on', S.syncing);
    const ls = S.boot.lastSync;
    $('#subline').textContent = S.syncing ? ({ coaching: 'Reading coaching pages…', articles: 'Reading full articles…' }[S.syncStage] || 'Syncing sources…') : (ls ? 'Synced ' + ago(ls) + ', ' + (S.boot.feedsOk || 0) + ' of ' + (S.boot.feedsTotal || 0) + ' sources' : 'Current affairs for UPSC Prelims');
  }
  const TABS = [['edition', 'paper', 'Edition'], ['live', 'pulse', 'Live'], ['revise', 'archive', 'Library']];
  function renderTabs() {
    $('#tabs').innerHTML = TABS.map(([k, i, l]) => `<button data-tab="${k}" class="${S.tab === k ? 'on' : ''}">${ic(i)}<span>${l}</span>${k === 'live' && hasFreshLive() ? '<i class="dot"></i>' : ''}</button>`).join('');
  }
  function hasFreshLive() { return S.liveFresh > 0 && S.tab !== 'live'; }
  function go(tab, opts) {
    if (S.tab === 'live' && tab !== 'live') markLiveSeen();
    S.tab = tab;
    renderTabs();
    render(opts);
  }
  function markLiveSeen() { const now = Date.now(); N.setLong('lastSeen', String(now)); S.boot.settings.lastSeen = now; S.liveFresh = 0; }
  function render(opts) {
    const v = $('#view');
    const keepScroll = opts && opts.keepScroll;
    const st = $('#main').scrollTop;
    try {
      if (S.tab === 'edition') v.innerHTML = viewEdition();
      else if (S.tab === 'live') v.innerHTML = viewLive();
      else v.innerHTML = viewRevise();
    } catch (e) {
      v.innerHTML = `<div class="empty"><div class="orb">${ic('alert')}</div><h3>Something went wrong</h3><p>${esc(e && e.message)}</p></div>`;
    }
    $('#main').scrollTop = keepScroll ? st : 0;
    readBar();
    const f = v.firstElementChild; if (f && !keepScroll) { f.style.animation = 'none'; f.offsetHeight; f.style.animation = ''; }
  }

  /* ---------------- topic card ---------------- */
  function isBookmarked(id) { return S.bookmarks.some(b => b.id === id); }
  function card(t, i, ctx) {
    const flags = [];
    if (t.amend) flags.push(`<span class="flag amend">${ic('alert', 'xs')}Amendment</span>`);
    if (t.cont) { const k = keyParts(t.cont); flags.push(`<span class="flag cont">Update on the ${k.d} ${k.mon} story</span>`); }
    if (ctx && ctx.fresh && t.fs > ctx.fresh) flags.push('<span class="flag new">NEW</span>');
    const from = t.gap || t.via;
    if (from) flags.push(`<span class="flag gap">From ${esc(SRC_NAME[from] || from)}${t.gapDate ? ', ' + keyParts(t.gapDate).d + ' ' + keyParts(t.gapDate).mon : ''}</span>`);
    if (t.upd && (!ctx || !ctx.fresh || t.upd > (t.fs || 0) + 45 * 60000)) flags.push(`<span class="flag upd">Updated ${hhmm(t.upd)}</span>`);
    const srcs = (t.srcs || []).map(s => `<b>${esc(s)}</b>`).join('<span>/</span>');
    const coach = (t.coach || []).map(s => `<span class="coach">(${esc(s)})</span>`).join(' ');
    const n = (t.corrob || (t.srcs || []).length) + (t.coach || []).length; // a coaching page counts as a confirmation
    const single = n >= 2 ? `<span class="ver">${ic('check', 'xs')}${n} sources</span>` : '<span class="sgl">single-source</span>';
    const pay = t.paywalled ? `<span class="sgl">${esc(t.paywalled)}: full text paywalled</span>` : '';
    const bul = bulletsOf(t).filter(b => !hid(b)).map(b => `<li>${figs(b, t.fx)}</li>`).join('');
    const ents = (t.cons || []).map(id => CON[id]).concat((t.ents || []).map(id => ENT[id])).filter(Boolean); // the exact subject first, then the parent body
    const thr = (t.thread || []);
    const thread = thr.length ? `<div class="thread"><button class="smore" data-act="thread">${ic('thread', 'xs')}Story so far, ${thr.length} earlier ${thr.length > 1 ? 'editions' : 'edition'}</button><div class="thb${t.cont ? ' on' : ''}">${thr.map(x => { const k = keyParts(x.d); return `<div class="th1" data-open="${esc(x.d)}"><span class="td">${k.d} ${k.mon}</span><div><b>${esc(x.h)}</b>${x.b && !hid(x.b) ? `<small>${figs(x.b)}</small>` : ''}</div></div>`; }).join('')}</div></div>` : '';
    // Same-day rule: a static recap / map / concept appears once per day — later cards only point to it
    const seen = ctx && !t.cont && ctx.seen; // a continuing card's background is folded away: later cards don't point to it
    const fresh = seen ? ents.filter(e => !seen.e[e.id]) : ents, already = seen ? ents.filter(e => seen.e[e.id]) : [];
    if (seen) fresh.forEach(e => { seen.e[e.id] = i + 1; });
    let prof = fresh.map((e, k) => staticBlock(e, k === 0, t.id + ':' + e.id)).join('');
    if (already.length) prof += `<div class="seen">${ic('book', 'xs')}<span>Background already covered:</span>${already.map(e => `<button class="seenb" data-goto="${seen.e[e.id]}">${esc(e.name)}, story ${seen.e[e.id]}</button>`).join('')}</div>`;
    const also = (t.also || []).filter(x => !hid(x.head)).map(x => `<div class="ali"><div class="alh">${figs(x.head)}${(x.srcs || []).length ? ` <small>${esc((x.srcs || []).join('/'))}</small>` : ''}</div>${(x.bullets || []).filter(b => !hid(b)).map(b => `<div class="alb">${figs(b, t.fx)}</div>`).join('')}</div>`).join('');
    const alsoHtml = also ? `<div class="also"><div class="all">${ic('layers', 'xs')}${t.pent && ENT[t.pent] ? 'More on ' + esc(ENT[t.pent].name) + ' today' : 'More on this today'}</div>${also}</div>` : '';
    const mp = (t.maps || []).map(id => MAP[id]).filter(m => m && !hid(m.line) && !(seen && seen.m[m.id])).map(m => { if (seen) seen.m[m.id] = i + 1; return m; }).map(m => `<div class="mapl">${ic('pin', 'sm')}<div><b>Map: ${esc(m.name)}</b> — ${esc(m.line)} <span class="mhook">| PYQ: ${esc(m.hook)}</span>${(m.traps || []).filter(x => !hid(x)).map(x => `<div class="st"><span class="tb">Trap</span><span>${figs(x)}</span></div>`).join('')}</div></div>`).join('');
    const pyqHtml = hid('pyq|' + t.id) ? '' : pyqBlock(t);
    const subj = subjBlock(t, seen, i);
    const conf = (t.conflicts || []).concat((t.ai && t.ai.cf) || []).map(c => `<div class="callout warnc">${ic('alert', 'sm')}<div><b>Sources disagree:</b> ${esc(c)}. Check before you memorise it.</div></div>`).join('');
    const bm = isBookmarked(t.id);
    return `<article class="card" data-id="${esc(t.id)}"${i >= 0 ? ` data-n="${i + 1}"` : ''} style="animation-delay:${Math.min(i, 8) * 35}ms">
      <div class="row1">${i >= 0 ? `<span class="num">${i + 1}</span>` : ''}<span class="tag ${gsClass(t.gs)}">${esc(t.gs)}</span><span class="tag tier ${esc(t.tier)}">${t.tier === 'HIGH' ? 'High yield' : t.tier === 'MED' || t.tier === 'MEDIUM' ? 'Medium yield' : 'Low yield'}</span>${t.rising ? '<span class="flag rise">Rising in recent papers</span>' : ''}${flags.join('')}</div>
      <h3>${esc(t.head)}</h3>
      <div class="src">${srcs}${coach ? ' ' + coach : ''}${single}${pay}</div>
      ${bul ? `<ul class="bul">${bul}</ul>` : ''}
      ${alsoHtml}${conf}${t.cont && (subj || prof || mp) ? `<div class="bgw"><button class="smore" data-act="bgmore">${ic('book', 'xs')}Background, covered on ${keyParts(t.cont).d} ${keyParts(t.cont).mon}</button><div class="bgb">${subj}${prof}${mp}</div></div>` : subj + prof + mp}${thread}
      ${pyqHtml}
      <div class="acts">
        <button data-act="links">${ic('link', 'sm')}Sources${(t.links || []).length > 1 ? ' (' + t.links.length + ')' : ''}</button>
        <span class="sp"></span>
        <button data-act="report" aria-label="Report an error">${ic('flag', 'sm')}</button>
        <button data-act="copy" aria-label="Copy">${ic('copy', 'sm')}</button>
        <button data-act="share" aria-label="Share as image or text">${ic('share', 'sm')}</button>
        <button data-act="bm" class="${bm ? 'on' : ''}" aria-label="Save">${ic('star', 'sm')}</button>
      </div></article>`;
  }
  /* Static recap: profile line + shorthand key facts + traps (primary body open, others collapsed) */
  function staticBlock(e, open, key) {
    const segs = e.line.split(' | ').filter(x => !hid(x)).map(esc).join('<i>|</i>');
    const facts = (e.facts || []).filter(x => !hid(x)), traps = (e.traps || []).filter(x => !hid(x));
    const more = facts.length + traps.length;
    // facts collapse for secondary bodies; TRAPS are always on show — they are what UPSC tests
    const body = facts.length ? `<div class="sfx ${open ? 'on' : ''}">${facts.map(f => `<div class="sf">${figs(f)}</div>`).join('')}</div>` : '';
    const trapHtml = traps.length ? `<div class="traps">${traps.map(x => `<div class="st"><span class="tb">Trap</span><span>${figs(x)}</span></div>`).join('')}</div>` : '';
    const tog = facts.length && !open ? `<button class="smore" data-act="smore">+ ${facts.length} key fact${facts.length > 1 ? 's' : ''}</button>` : '';
    return `<div class="prof"><span class="k">${esc(e.kind === 'International' ? 'Intl org' : e.kind)}</span><div class="pb"><div><b>${esc(e.name)}</b> — <span class="segs">${segs}</span></div>${body}${tog}${trapHtml}</div></div>`;
  }
  /* Bullets: the AI-checked version when the AI pass has run, else the ones read from the articles / coaching pages */
  function bulletsOf(t) { return t.ai && !t.ai.stale && (t.ai.b || []).length ? t.ai.b : (t.bullets || []); }
  /* "About": the exact subject in the news (temple, technique, flood type…), from the AI pass or the coaching notes */
  function aboutOf(t) { const a = t.ai && !t.ai.stale && t.ai.ab; return a && (a.pts || []).length ? a : (t.bg && (t.bg.pts || []).length ? t.bg : null); }
  function subjBlock(t, seen, i) {
    const b = aboutOf(t);
    const traps = ((t.ai && !t.ai.stale && t.ai.tr) || []).filter(x => !hid(x));
    const key = b ? 'b:' + String(b.h).toLowerCase() : '';
    if (b && seen && seen.e[key]) return traps.length ? `<div class="traps">${traps.map(x => `<div class="st"><span class="tb">Trap</span><span>${figs(x)}</span></div>`).join('')}</div>` : '';
    if (b && seen) seen.e[key] = i + 1;
    const pts = b ? (b.pts || []).filter(x => !hid(x)) : [];
    if (!pts.length && !traps.length) return '';
    const by = t.ai && t.ai.ab === b ? (t.ai.chk === 'web' ? 'Checked on the web' : t.ai.chk === 'sources' ? 'Checked against the sources' : '') : (b && b.via ? 'From ' + (SRC_NAME[b.via] || b.via) : '');
    return `<div class="prof subj"><span class="k">About</span><div class="pb">${pts.length ? `<div><b>${esc(b.h)}</b></div><div class="sfx on">${pts.map(f => `<div class="sf">${figs(f)}</div>`).join('')}</div>` : ''}${traps.length ? `<div class="traps">${traps.map(x => `<div class="st"><span class="tb">Trap</span><span>${figs(x)}</span></div>`).join('')}</div>` : ''}${by ? `<div class="by">${esc(by)}</div>` : ''}</div></div>`;
  }
  function topicText(t) {
    const lines = [`*${t.head}* — ${t.gs} — ${(t.srcs || []).concat((t.coach || []).map(c => '(' + c + ')')).join('/')}${t.single && !(t.coach || []).length ? ' (single-source)' : ''}`];
    bulletsOf(t).forEach(b => lines.push('• ' + b));
    const ab = aboutOf(t);
    if (ab) { lines.push('About ' + ab.h + ':'); (ab.pts || []).forEach(p => lines.push('   – ' + p)); }
    ((t.ai && t.ai.tr) || []).forEach(x => lines.push('   ⚠ Trap: ' + x));
    (t.also || []).forEach(x => { lines.push('• Also: ' + x.head); (x.bullets || []).forEach(b => lines.push('   – ' + b)); });
    (t.ents || []).map(id => ENT[id]).concat((t.cons || []).map(id => CON[id])).forEach(e => { if (!e) return; lines.push('• ' + e.name + ' — ' + e.line); (e.facts || []).forEach(f => lines.push('   – ' + f)); (e.traps || []).forEach(x => lines.push('   ⚠ Trap: ' + x)); });
    (t.maps || []).forEach(id => MAP[id] && lines.push('Map: ' + MAP[id].name + ' — ' + MAP[id].line + ' | PYQ: ' + MAP[id].hook));
    (t.maps || []).forEach(id => MAP[id] && (MAP[id].traps || []).forEach(x => lines.push('   ⚠ Trap: ' + x)));
    if ((t.pyqEv || []).length) { lines.push('PYQ — what was asked:'); t.pyqEv.forEach(e => lines.push('   ’' + pad(e[0] % 100) + ' ' + e[1])); }
    else lines.push(t.pyq || '');
    return lines.join('\n');
  }

  /* ---------------- Share a card as an image (logo + name, light or dark, full card or key points) ---------------- */
  const LOGO = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="30" height="30"><rect width="108" height="108" rx="24" fill="#22302F"/><path d="M73.5 37.5A25 25 0 1 0 73.5 70.5" fill="none" stroke="#F2F5F3" stroke-width="11" stroke-linecap="round"/><circle cx="56" cy="54" r="8" fill="#F6BE8A"/></svg>';
  const SHC = {
    light: { bg: '#FFFFFF', page: '#F2F5F3', text: '#1E2A2D', t2: '#3F4E52', muted: '#677A7E', line: '#DCE4E1', hi: '#FFF0A0', mint: '#F0F8F2', mintL: '#CFE7D6', teal: '#2F6E4C', rose: '#FDEEF2', red: '#A8344F', redInk: '#FFFFFF', peach: '#FFF6EC', peachL: '#F6D3B3', sa: '#A55A1A', gs: { gs1: ['#FFF2C4', '#8A6510'], gs2: ['#DFEDFB', '#2C5D8F'], gs3: ['#E0F2E5', '#2F6E4C'], gs4: ['#ECE6FA', '#5D4A96'], gsm: ['#E9EEEC', '#566468'] } },
    dark: { bg: '#263133', page: '#1D2527', text: '#E9F0ED', t2: '#C3CFCB', muted: '#93A39F', line: 'rgba(205,228,222,.16)', hi: 'rgba(240,214,110,.32)', mint: 'rgba(158,217,176,.08)', mintL: 'rgba(158,217,176,.22)', teal: '#9ED9B0', rose: 'rgba(242,165,184,.10)', red: '#F2A5B8', redInk: '#3A1724', peach: 'rgba(246,190,138,.08)', peachL: 'rgba(246,190,138,.26)', sa: '#F6BE8A', gs: { gs1: ['rgba(233,204,126,.15)', '#E9CC7E'], gs2: ['rgba(156,196,238,.15)', '#9CC4EE'], gs3: ['rgba(158,217,176,.14)', '#9ED9B0'], gs4: ['rgba(200,182,238,.15)', '#C8B6EE'], gsm: ['rgba(183,195,192,.12)', '#B7C3C0'] } }
  };
  function shareCss(th) {
    const c = SHC[th] || SHC.light, g = c.gs;
    return `.ncw{background:${c.page};padding:18px;width:540px;box-sizing:border-box}
      .nc{background:${c.bg};color:${c.text};border-radius:14px;padding:22px 22px 12px;font:400 15.5px/1.5 "NB Digits","Atkinson Hyperlegible",system-ui,sans-serif;border:1px solid ${c.line};box-sizing:border-box}
      .nc *{box-sizing:border-box}.nc-top{display:flex;align-items:center;gap:9px;margin-bottom:14px}.nc-top svg{display:block}
      .nc-brand{display:flex;flex-direction:column;gap:3px}.nc-name{font:700 19px/1 "Zilla Slab",Georgia,serif;color:${c.text}}.nc-tag2{font-size:11.5px;line-height:1;color:${c.muted}}.nc-date{margin-left:auto;color:${c.muted};font-size:13px}
      .nc-tags{display:flex;gap:6px;flex-wrap:wrap;margin-bottom:8px}.nc-tag{font-size:12.5px;font-weight:700;padding:2px 9px;border-radius:5px}
      ${Object.keys(g).map(k => `.nc-tag.${k}{background:${g[k][0]};color:${g[k][1]}}`).join('')}
      .nc-tag.y{background:${c.peach};color:${c.sa};font-weight:400;border:1px solid ${c.peachL}}
      .nc h1{font:700 23px/1.25 "Zilla Slab",Georgia,serif;margin:2px 0 6px;color:${c.text}}
      .nc-src{color:${c.muted};font-size:12.5px;margin-bottom:10px}
      .nc ul{margin:0 0 10px;padding:0;list-style:none}.nc li{position:relative;padding:5px 0 5px 18px;border-top:1px dashed ${c.line}}.nc li:first-child{border-top:0}
      .nc li:before{content:"";position:absolute;left:2px;top:15px;width:8px;height:2px;background:${c.sa};border-radius:1px}
      .nc .fig{background:${c.hi};padding:0 3px;border-radius:3px;font-weight:700}
      .nc-box{border-radius:9px;padding:9px 12px;margin:8px 0}.nc-lab{font-size:12.5px;font-weight:700;margin-bottom:3px}
      .nc-about{background:${c.mint};border:1px solid ${c.mintL}}.nc-about .nc-lab{color:${c.teal}}.nc-p{padding:2px 0 2px 12px;position:relative;color:${c.t2};font-size:14.5px}
      .nc-p:before{content:"";position:absolute;left:1px;top:11px;width:4px;height:4px;border-radius:2px;background:${c.teal}}
      .nc-st{color:${c.t2};font-size:14.5px;margin-top:4px}
      .nc-trap{display:flex;gap:8px;align-items:flex-start;background:${c.rose};border-left:3px solid ${c.red};border-radius:6px;padding:7px 10px;margin:6px 0;font-weight:700;font-size:14.5px}
      .nc-trap span.tb{flex:none;background:${c.red};color:${c.redInk};font-size:11.5px;padding:1px 7px;border-radius:4px;margin-top:2px}
      .nc-pyq{background:${c.peach};border:1px solid ${c.peachL}}.nc-pyq .nc-lab{color:${c.sa}}.nc-r{font-size:14.5px;padding:2px 0}.nc-r b{color:${c.sa};margin-right:6px}
      `;
  }
  function shareHtml(t, o) {
    const full = o.full;
    const dk = t.date || (S.tab === 'live' ? S.boot.today : S.date) || S.boot.today;
    const k = dk ? keyParts(dk) : null;
    const tier = t.tier === 'HIGH' ? 'High yield' : t.tier === 'MED' || t.tier === 'MEDIUM' ? 'Medium yield' : 'Low yield';
    const n = (t.corrob || (t.srcs || []).length) + (t.coach || []).length;
    let h = `<div class="ncw"><div class="nc"><div class="nc-top">${LOGO}<span class="nc-brand"><span class="nc-name">News Crux</span><span class="nc-tag2">Current affairs for UPSC Prelims</span></span>${k ? `<span class="nc-date">${k.d} ${monthLong(k.mon)} ${k.y}</span>` : ''}</div>`;
    h += `<div class="nc-tags"><span class="nc-tag ${gsClass(t.gs)}">${esc(t.gs)}</span><span class="nc-tag y">${tier}</span></div><h1>${esc(t.head)}</h1>`;
    h += `<div class="nc-src">${esc((t.srcs || []).join(' / '))}${n >= 2 ? `, ${n} sources` : ''}</div>`;
    const bl = bulletsOf(t).filter(b => !hid(b)).slice(0, full ? 5 : 4);
    if (bl.length) h += `<ul>${bl.map(b => `<li>${figs(b)}</li>`).join('')}</ul>`;
    const traps = ((t.ai && !t.ai.stale && t.ai.tr) || []).filter(x => !hid(x));
    if (full) {
      const ab = aboutOf(t);
      if (ab) h += `<div class="nc-box nc-about"><div class="nc-lab">About ${esc(ab.h)}</div>${(ab.pts || []).filter(x => !hid(x)).slice(0, 4).map(p => `<div class="nc-p">${figs(p)}</div>`).join('')}</div>`;
      const ents = (t.cons || []).map(id => CON[id]).concat((t.ents || []).map(id => ENT[id])).filter(Boolean).slice(0, 2);
      if (ents.length) h += `<div class="nc-box nc-about"><div class="nc-lab">Static recap</div>${ents.map(e => `<div class="nc-st"><b>${esc(e.name)}</b>: ${figs(e.line.split(' | ').filter(x => !hid(x)).join('; '))}</div>`).join('')}</div>`;
      ents.forEach(e => (e.traps || []).filter(x => !hid(x)).forEach(x => traps.push(x)));
    }
    traps.slice(0, full ? 3 : 2).forEach(x => { h += `<div class="nc-trap"><span class="tb">Trap</span><span>${figs(x)}</span></div>`; });
    const ev = t.pyqEv || [];
    if (ev.length) {
      const rows = [], idx = {};
      ev.forEach(e => { const kk = String(e[1]).toLowerCase(); if (idx[kk] == null) { idx[kk] = rows.length; rows.push({ a: e[1], y: [e[0]] }); } else rows[idx[kk]].y.push(e[0]); });
      h += `<div class="nc-box nc-pyq"><div class="nc-lab">Asked in Prelims</div>${rows.slice(0, 3).map(r => `<div class="nc-r"><b>${r.y.map(y => '’' + pad(y % 100)).join(' ')}</b>${esc(r.a === 'asked' ? 'asked (see question)' : r.a)}</div>`).join('')}</div>`;
    }
    h += `</div></div>`;
    return h;
  }
  function fontCss() {
    if (S.fontCss != null) return S.fontCss;
    let css = '';
    try { const a = N.asset('app.css'); if (a && a.length > 5000) css = (a.match(/@font-face\{[^}]*\}/g) || []).join(''); } catch (e) {}
    if (!css) { try { for (const sh of document.styleSheets) for (const r of sh.cssRules) if (r.type === 5) css += r.cssText; } catch (e) {} }
    S.fontCss = css;
    return css;
  }
  async function cardImage(t, o) {
    const css = shareCss(o.theme), body = shareHtml(t, o);
    const host = document.createElement('div');
    host.style.cssText = 'position:fixed;left:-20000px;top:0;width:540px;pointer-events:none';
    host.innerHTML = `<style>${css}</style>${body}`;
    document.body.appendChild(host);
    try { if (document.fonts && document.fonts.ready) await document.fonts.ready; } catch (e) {}
    const H = Math.ceil(host.querySelector('.ncw').getBoundingClientRect().height);
    const xhtml = new XMLSerializer().serializeToString(host.querySelector('.ncw'));
    host.remove();
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="540" height="${H}"><foreignObject x="0" y="0" width="540" height="${H}"><div xmlns="http://www.w3.org/1999/xhtml"><style>${fontCss()}${css}</style>${xhtml}</div></foreignObject></svg>`;
    const img = new Image();
    img.src = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg);
    await img.decode();
    const sc = 2, cv = document.createElement('canvas');
    cv.width = 540 * sc; cv.height = H * sc;
    const ctx = cv.getContext('2d');
    ctx.scale(sc, sc);
    ctx.drawImage(img, 0, 0);
    return cv.toDataURL('image/png');
  }
  function shareSheet(t) {
    S.shareT = t;
    const cur = document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light';
    S.shareOpt = S.shareOpt || { theme: cur, full: true };
    openSheet(`<h3>Share this story</h3>
      <div class="shprev" id="shprev"><div class="note">Preparing the image…</div></div>
      <div class="shopts"><div class="opts">${[['light', 'Light'], ['dark', 'Dark']].map(([k, l]) => `<button data-shtheme="${k}" class="${S.shareOpt.theme === k ? 'on' : ''}">${l}</button>`).join('')}</div>
      <div class="opts">${[[1, 'Full card'], [0, 'Key points']].map(([k, l]) => `<button data-shfull="${k}" class="${S.shareOpt.full === !!k ? 'on' : ''}">${l}</button>`).join('')}</div></div>
      <div class="shbtns"><button class="btn" data-act="shimg">${ic('share', 'sm')}Share image</button><button class="btn ghost" data-act="shsave">${ic('save', 'sm')}Save to phone</button></div>
      <div class="shbtns"><button class="chip" data-act="shtext">${ic('paper', 'xs')}Share as text</button><button class="chip" data-act="shcopy">${ic('copy', 'xs')}Copy text</button></div>`);
    renderShare();
  }
  async function renderShare() {
    const t = S.shareT, o = Object.assign({}, S.shareOpt), token = (S.shareTok = (S.shareTok || 0) + 1);
    S.shareUrl = null;
    try {
      const url = await cardImage(t, o);
      if (token !== S.shareTok) return;
      S.shareUrl = url;
      const box = $('#shprev'); if (box) box.innerHTML = `<img src="${url}" alt="Preview of the shared card">`;
    } catch (e) {
      const box = $('#shprev'); if (box) box.innerHTML = '<div class="note">Couldn\'t draw the image on this phone. Share as text instead.</div>';
    }
  }
  function shareName() { const t = S.shareT || {}; return 'NewsCrux-' + String(t.head || 'story').replace(/[^A-Za-z0-9]+/g, '-').slice(0, 40).replace(/-+$/, ''); }
  function shareNow(save) {
    if (!S.shareUrl) { toast('Still preparing the image…'); return; }
    if (save) {
      const r = N.saveImage ? N.saveImage(S.shareUrl, shareName()) : 'error';
      toast(r === 'ok' ? 'Saved to Pictures, News Crux' : r === 'perm' ? 'Allow storage access, then tap Save again' : 'Couldn\'t save the image');
    } else { N.shareImage(S.shareUrl, shareName(), (S.shareT ? S.shareT.head : '') + '\n— via News Crux'); }
  }

  /* ---------------- Edition ---------------- */
  function editions() { return S.boot.editions || []; }
  function defaultDate() {
    const eds = editions();
    if (eds.length) return eds[0].date;
    return null;
  }
  const nStories = n => n + (n === 1 ? ' story' : ' stories');
  /* One-line date bar: ‹ older day | date (opens the calendar) | newer day ›. It stays at the top while
     you read and shows which story you are on, with a progress line under it. */
  function dayBar(n) {
    const eds = editions(), i = eds.findIndex(e => e.date === S.date);
    const older = i >= 0 ? (eds[i + 1] || {}).date : (eds[0] || {}).date, newer = i > 0 ? eds[i - 1].date : null;
    const k = keyParts(S.date), yr = k.y !== istParts(Date.now()).y ? ' ' + k.y : '';
    const nav = (d, lab, icn) => `<button class="dnav" ${d ? `data-date="${d}"` : 'disabled'} aria-label="${lab}">${ic(icn, 'sm')}</button>`;
    return `<div class="dbar" id="dbar"><div class="dbr">${nav(older, 'Previous day', 'back')}
      <button class="dsel" data-act="cal" aria-label="Pick a date"><b><span>${dayLong(k.wd)}, ${k.d} ${monthLong(k.mon)}${yr}</span>${ic('down', 'xs')}</b><small id="rbn" data-n="${n}">${nStories(n)}</small></button>
      ${nav(newer, 'Next day', 'chev')}</div><div class="rprog"><i id="rbp"></i></div></div>`;
  }
  /* Reading progress: the line fills as you scroll through the stories (back down when you scroll up). */
  let rbRaf = 0;
  function readBar() {
    if (rbRaf) return;
    rbRaf = requestAnimationFrame(() => {
      rbRaf = 0;
      const p = $('#rbp'), lab = $('#rbn'), bar = $('#dbar');
      if (!p || S.tab !== 'edition') return;
      const m = $('#main'), mt = m.getBoundingClientRect().top, cards = $$('#view > .card');
      bar.classList.toggle('stuck', bar.getBoundingClientRect().top <= mt + 1 && m.scrollTop > 4);
      if (!cards.length) { p.style.width = '0'; return; }
      const last = cards[cards.length - 1], end = last.getBoundingClientRect().bottom - mt + m.scrollTop - m.clientHeight;
      p.style.width = (end <= 0 ? 100 : Math.max(0, Math.min(100, m.scrollTop / end * 100))).toFixed(1) + '%';
      const y = bar.getBoundingClientRect().bottom + 12;
      let at = 0;
      for (const c of cards) { if (c.getBoundingClientRect().top <= y) at = Number(c.dataset.n); else break; }
      const total = Number(lab.dataset.n), cu = $('#catchup'), ck = $('#checked');
      let txt = at && m.scrollTop > 4 ? `Story ${at} of ${total}` : nStories(total);
      if (ck && ck.getBoundingClientRect().top <= y) txt = 'What was checked';
      else if (cu && cu.getBoundingClientRect().top <= y) txt = 'Catch-up';
      if (lab.textContent !== txt) lab.textContent = txt;
    });
  }
  /* Calendar sheet: jump straight to any saved day. A dot marks a day with an edition on this phone. */
  function calSheet() {
    const eds = editions(), have = new Set(eds.map(e => e.date));
    const ym = S.calYm || (S.date || '').slice(0, 7), [y, m] = ym.split('-').map(Number);
    const months = Array.from(new Set(eds.map(e => e.date.slice(0, 7)))).sort();
    const prev = months.filter(x => x < ym).pop(), next = months.find(x => x > ym);
    const lead = (new Date(Date.UTC(y, m - 1, 1)).getUTCDay() + 6) % 7, nd = new Date(Date.UTC(y, m, 0)).getUTCDate();
    const tp = istParts(Date.now()), today = `${tp.y}-${pad(tp.m + 1)}-${pad(tp.d)}`;
    let cells = '<span></span>'.repeat(lead);
    for (let d = 1; d <= nd; d++) {
      const key = `${y}-${pad(m)}-${pad(d)}`, cls = (key === S.date ? ' on' : '') + (key === today ? ' td' : '');
      cells += have.has(key) ? `<button class="cd has${cls}" data-date="${key}" aria-label="${d} ${monthLong(MON[m - 1])}">${d}<i></i></button>` : `<span class="cd${cls}">${d}</span>`;
    }
    const mb = (to, lab, icn) => `<button class="cmn" ${to ? `data-calm="${to}"` : 'disabled'} aria-label="${lab}">${ic(icn, 'sm')}</button>`;
    return `<div class="cal"><div class="calh">${mb(prev, 'Previous month', 'back')}<b>${monthLong(MON[m - 1])} ${y}</b>${mb(next, 'Next month', 'chev')}</div>
      <div class="calg calw">${['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map(w => `<span>${w.slice(0, 2)}</span>`).join('')}</div>
      <div class="calg">${cells}</div>
      <div class="calf">${eds.length && eds[0].date !== S.date ? `<button class="btn" data-date="${eds[0].date}">Latest edition</button>` : ''}<span>A dot marks a day with an edition on this phone.</span></div></div>`;
  }
  /* Optional (Settings → GS-wise view): counts per GS paper; tap one to show only that paper's stories. */
  function gsRow(topics) {
    const by = {}; topics.forEach(t => { const g = gsPaper(t.gs); by[g] = (by[g] || 0) + 1; });
    const ps = PAPERS.map(p => p[0]).filter(g => by[g]);
    if (S.gsF && !by[S.gsF]) S.gsF = null;
    if (ps.length < 2 && !S.gsF) return '';
    const cls = g => g === 'Misc' ? 'gsm' : gsClass(g + '/');
    return `<div class="gsrow">${ps.map(g => `<button class="gst ${cls(g)}${S.gsF === g ? ' on' : ''}" data-gsf="${g}"><b>${by[g]}</b><small>${g}</small></button>`).join('')}</div>`
      + (S.gsF ? `<div class="gsnote">Showing ${S.gsF}, ${by[S.gsF]} of ${topics.length}. Tap it again to show all.</div>` : '');
  }
  function viewEdition() {
    if (!S.date) S.date = defaultDate();
    if (!S.date || S.date === 'live') { S.date = defaultDate(); if (!S.date) return emptyFirstRun(); }
    const ed = S.ed && S.ed.date === S.date ? S.ed : (S.ed = J(N.edition(S.date), null));
    if (!ed) return dayBar(0) + emptyFirstRun();
    const topics = ed.topics || [];
    const st = ed.status === 'final' ? '<span class="pill ok">' + ic('check', 'xs') + 'Final</span>' : '<span class="pill warn">' + ic('clock', 'xs') + 'Compiling</span>';
    let h = dayBar(topics.length);
    h += `<div class="dmeta">${st}${checkPill(ed)}</div>`;
    const gsOn = !!(S.boot.settings || {}).gsView;
    if (gsOn) h += gsRow(topics); else S.gsF = null;
    if (!topics.length) h += `<div class="empty"><div class="orb">${ic('paper')}</div><h3>Quiet window</h3><p>No story in this window cleared the PYQ-alignment bar yet. Fewer topics is fine — no forcing.</p></div>`;
    const ctx = { seen: { e: {}, m: {}, th: {} } };
    h += topics.map((t, i) => S.gsF && gsPaper(t.gs) !== S.gsF ? '' : card(t, i, ctx)).join('');
    h += catchupSection(ed);
    h += checkedCard(ed);
    h += `<div class="foot">${ed.window ? esc(ed.window) + '. ' : ''}${(ed.sources || []).length} sources. Saved to archive on ${esc(ed.date)}, built at ${hhmm(ed.builtAt || Date.now())} IST</div>`;
    return h;
  }
  const ML = { Jan: 'January', Feb: 'February', Mar: 'March', Apr: 'April', May: 'May', Jun: 'June', Jul: 'July', Aug: 'August', Sep: 'September', Oct: 'October', Nov: 'November', Dec: 'December' };
  const DL = { Sun: 'Sunday', Mon: 'Monday', Tue: 'Tuesday', Wed: 'Wednesday', Thu: 'Thursday', Fri: 'Friday', Sat: 'Saturday' };
  const monthLong = m => ML[m] || m, dayLong = d => DL[d] || d;
  /* ---------- What was checked, and what was missed ---------- */
  function checkState(ed) {
    const tp = ed.topics || [], st = S.boot.settings || {}, a = ed.ai || {};
    const missed = ed.missed || [], conf = tp.filter(t => (t.conflicts || []).length || (t.ai && (t.ai.cf || []).length)).length;
    const aiOk = (a.cards || 0) > 0 && !ed.aiError;
    return { tp, st, a, missed, conf, aiOk };
  }
  function checkPill(ed) {
    const c = checkState(ed);
    const cu = (ed.catchup || []).length;
    const main = c.missed.length ? `<button class="pill warn" data-act="tocheck">${ic('alert', 'xs')}${c.missed.length} missed</button>` : `<button class="pill ok" data-act="tocheck">${ic('check', 'xs')}All checked</button>`;
    return main + (cu ? `<button class="pill" data-act="tocatch">+${cu} catch-up</button>` : '');
  }
  /* ---------- Catch-up: Prelims topics coaching covered in the last 10 days that no edition carried yet ---------- */
  function catchupSection(ed) {
    const cu = (ed.catchup || []).filter(t => !hid(t.head));
    if (!cu.length) return '';
    const days = (ed.coverage && ed.coverage.days) || 10;
    let h = `<div class="sect" id="catchup"><h2>Catch-up</h2><span class="c">${cu.length}</span><span class="rule"></span></div><div class="note cun">Prelims topics the coaching pages covered in the last ${days} days that no edition carried yet. Tap one to read it.</div>`;
    PAPERS.forEach(([g, l]) => {
      const ts = cu.filter(t => gsPaper(t.gs) === g);
      if (!ts.length) return;
      h += `<div class="cuh">${esc(l)}</div><div class="list cul">${ts.map(t => {
        const open = S.cuOpen.has(t.id);
        const who = (t.cu || []).map(x => { const k = keyParts(x.d); return `${esc(SRC_NAME[x.s] || x.s)}, ${k.d} ${k.mon}`; }).join('; ');
        const tier = t.tier === 'HIGH' ? 'High yield' : t.tier === 'MED' || t.tier === 'MEDIUM' ? 'Medium yield' : '';
        let row = `<div class="li cu ${open ? 'on' : ''}" data-cu="${esc(t.id)}"><div class="t"><b>${esc(t.head)}</b><small>${who}${tier ? `. ${tier}` : ''}</small></div>${ic(open ? 'down' : 'chev', 'sm')}</div>`;
        if (open) {
          const st = S.boot.settings || {};
          const busy = S.cuBusy[t.id], err = S.cuErr[t.id];
          const note = busy ? `<div class="note cuw">${ic('refresh', 'xs')} Writing and checking this card…</div>` : err ? `<div class="note cuw">${esc(err)}</div>` : (!t.ai && !st.hasKey && t.needAi !== false ? '<div class="note cuw">Shown from the coaching notes. With an AI key in Settings, thin cards like this one are filled in.</div>' : '');
          row += `<div class="cuc">${note}${card(t, -1, { seen: null })}</div>`;
        }
        return row;
      }).join('')}</div>`;
    });
    return h;
  }
  function checkedCard(ed) {
    const c = checkState(ed), cv = ed.coverage;
    const multi = c.tp.filter(t => (t.corrob || (t.srcs || []).length) + (t.coach || []).length >= 2).length;
    const rows = [];
    rows.push(['ok', `${c.tp.length} ${c.tp.length === 1 ? 'story' : 'stories'}; ${multi} confirmed by two or more sources (newspapers, PIB, coaching)`]);
    const figN = c.tp.reduce((n, t) => n + Object.keys(t.fx || {}).length, 0);
    if (figN) rows.push(['ok', `✓ beside a figure: two outlets or an official release give the same number`]);
    if (cv && cv.pages != null) {
      rows.push([c.missed.length ? 'warn' : 'ok', `Coaching, last ${cv.days || 10} days: ${cv.pages} pages, ${cv.prelims} Prelims topics. ${cv.today} today, ${cv.earlier} in earlier editions, ${cv.catchup} in catch-up, ${c.missed.length} missed`]);
      if (cv.mains) rows.push(['off', `${cv.mains} Mains-only piece${cv.mains > 1 ? 's' : ''} left out`]);
    } else if (cv) rows.push([c.missed.length ? 'warn' : 'ok', `Coaching pages: ${cv.covered} of ${cv.of} topics covered${cv.earlier ? ` (${cv.earlier} in earlier editions)` : ''}`]);
    else rows.push(['off', 'No coaching pages for these days yet']);
    if (S.aiBusy === ed.date) rows.push(['busy', ({ verify: 'Fact-checking the cards…' }[S.aiStage] || (/^fallback:/.test(S.aiStage || '') ? S.aiStage.slice(9) + ' is taking over…' : 'Writing the cards…'))]);
    const selfN = c.tp.filter(t => t.needAi === false).length;
    if (c.tp.some(t => t.needAi !== undefined)) rows.push(['ok', `${selfN} of ${c.tp.length} cards complete from the sources alone (articles, coaching notes, verified facts)`]);
    if (S.aiBusy === ed.date) {} else if (c.aiOk) rows.push(['ok', `AI bonus: ${c.a.cards} card${c.a.cards > 1 ? 's' : ''} polished by ${c.a.provider || 'AI'} and ${c.a.verified ? 'fact-checked on the web' : 'checked against the sources'}${c.a.fixes ? `; ${c.a.fixes} correction${c.a.fixes > 1 ? 's' : ''} made` : ''}`]);
    else if (ed.aiError) rows.push(['off', 'AI bonus skipped this time: ' + String(ed.aiError).split('\n').filter(Boolean).slice(0, 1).join(' ') + ' The edition is complete without it.']);
    else if (c.st.hasKey && selfN < c.tp.length) rows.push(['off', 'AI bonus fills in the thin cards in the background']);
    rows.push(['ok', 'Static facts from the verified knowledge base, each matched to its story']);
    if ((ed.held || []).length) rows.push(['ok', `${ed.held.length} earlier ${ed.held.length > 1 ? 'stories' : 'story'} left out: nothing new since ${ed.held.length > 1 ? 'they were' : 'it was'} covered`]);
    if (c.conf) rows.push(['warn', `${c.conf} figure conflict${c.conf > 1 ? 's' : ''} between outlets flagged on the cards`]);
    const icn = { ok: 'check', warn: 'alert', off: 'clock', busy: 'refresh' };
    let h = `<div class="sect" id="checked"><h2>Checked</h2><span class="rule"></span></div><div class="qual chk ${c.missed.length || c.conf ? 'warn' : 'ok'}"><div class="qt">${rows.map(r => `<div class="qr ${r[0]}">${ic(icn[r[0]], 'xs')}<span>${esc(r[1])}</span></div>`).join('')}`;
    if (ed.aiError) h += `<div class="row" style="margin-top:8px;display:flex;gap:6px"><button class="chip" data-act="copyerr" data-err="${esc(ed.aiError)}">${ic('copy', 'xs')}Copy error</button><button class="chip" data-act="settings">${ic('gear', 'xs')}Settings</button></div>`;
    if (c.st.hasKey && S.aiBusy !== ed.date) h += `<button class="chip" data-act="ai-gen" style="margin-top:10px">${ic('spark', 'xs')}Rewrite cards with AI now</button>`;
    h += '</div></div>';
    if (c.missed.length) h += `<button class="chip" data-act="missed" style="margin-top:10px">${ic('alert', 'xs')}${c.missed.length} missed: show</button><div class="list missedl" style="display:${S.missedOpen ? 'block' : 'none'};margin-top:8px">${c.missed.map(m => { const k = keyParts(m.d || ed.date); return `<div class="li"><span class="ic warn">${ic('alert', 'xs')}</span><div class="t">${esc(m.t)}<small>${esc(SRC_NAME[m.s] || m.s)}, ${k.d} ${k.mon}. Not added: ${esc(m.why || '')}</small></div></div>`; }).join('')}</div>`;
    return h;
  }
  function emptyFirstRun() {
    if (S.syncing || !S.boot.lastSync) {
      return `<div class="empty"><div class="orb">${ic('refresh')}</div><h3>Building your first edition</h3><p>Fetching PIB, The Hindu, Indian Express, HT, TOI, ET, BS, Moneycontrol, Reuters, ANI, PTI and the coaching cross-check sources…</p></div><div class="skel"></div><div class="skel"></div>`;
    }
    return `<div class="empty"><div class="orb">${ic('paper')}</div><h3>No edition for this date</h3><p>Editions are compiled from the previous day's news window (00:00–23:59 IST). Check Live for today.</p><button class="btn" data-act="sync">${ic('refresh', 'sm')}Sync now</button></div>`;
  }

  function domain(u) { try { return new URL(u).hostname.replace(/^www\./, ''); } catch (e) { return u; } }
  /* ---------------- Live ---------------- */
  function loadLive() { S.live = J(N.live(), { topics: [] }); const ls = (S.boot.settings || {}).lastSeen || 0; S.liveFresh = (S.live.topics || []).filter(t => t.fs > ls).length; }
  function liveBoard(inEdition) {
    if (!S.live) loadLive();
    const all = S.live.topics || [];
    const f = S.liveFilter;
    const list = f === 'All' ? all : all.filter(t => gsPaper(t.gs) === f);
    const counts = { All: all.length };
    all.forEach(t => { const p = gsPaper(t.gs); counts[p] = (counts[p] || 0) + 1; });
    let h = inEdition ? '' : `<div class="h-title">Breaking today</div><div class="h-sub">${esc(S.live.heading || '')}. Ranked by how often Prelims asks the topic.</div>`;
    if (inEdition) h += `<section class="mast"><div class="kicker">Live board<span class="rule"></span>${hhmm(Date.now())} IST</div><h1>Today, so far</h1><div class="win">Stories from today's window, ranked by PYQ alignment. They roll into tomorrow's edition after 23:59 IST.</div><div class="meta"><span class="pill">${all.length} stories</span><span class="pill">${S.live.itemsInWindow || 0} items scanned</span></div></section>`;
    if (!inEdition) h += `<div class="seg"><button data-lmode="stories" class="${S.liveMode === 'stories' ? 'on' : ''}">${ic('flame', 'sm')}Top stories</button><button data-lmode="feed" class="${S.liveMode === 'feed' ? 'on' : ''}">${ic('paper', 'sm')}All sources</button></div>`;
    if (!inEdition && S.liveMode === 'feed') return h + feedView();
    h += `<div class="chips">${['All', 'GS-I', 'GS-II', 'GS-III', 'Misc'].filter(k => k === 'All' || counts[k]).map(k => `<button class="chip ${f === k ? 'on' : ''}" data-lf="${k}">${k}<span class="n">${counts[k] || 0}</span></button>`).join('')}</div>`;
    if (!list.length) h += `<div class="empty"><div class="orb">${ic('pulse')}</div><h3>${S.syncing ? 'Fetching…' : 'Nothing PYQ-relevant yet today'}</h3><p>Stories appear here as sources publish. Background sync runs every ${(S.boot.settings || {}).intervalHours || 3} hours; pull down to refresh.</p></div>`;
    const fresh = (S.boot.settings || {}).lastSeen || 0;
    const lctx = { fresh, seen: { e: {}, m: {}, th: {} } };
    h += list.map((t, i) => card(t, i, lctx)).join('');
    return h;
  }
  function viewLive() { loadLive(); renderTabs(); return liveBoard(false); }
  function feedView() {
    const codes = (window.SOURCES || []).map(s => s.code);
    let h = `<div class="chips"><button class="chip ${!S.feedSrc ? 'on' : ''}" data-fs="">All</button>${codes.map(c => `<button class="chip ${S.feedSrc === c ? 'on' : ''}" data-fs="${c}">${c}</button>`).join('')}</div>`;
    const items = J(N.feed(S.feedSrc, 120), []);
    if (!items.length) return h + `<div class="empty"><div class="orb">${ic('paper')}</div><h3>No items stored</h3><p>Sync to pull the latest from this source.</p></div>`;
    h += `<div class="list">${items.map(it => `<div class="li"><span class="ic ${it.sc >= 6 ? 'warn' : ''}" title="PYQ score">${it.sc >= 6 ? ic('target', 'xs') : ic('paper', 'xs')}</span><div class="t"><span class="lnk" data-url="${esc(it.u)}">${esc(it.t)}</span><small>${esc(it.s)} · ${hhmm(it.p)} · ${ago(it.p)}${it.gs && it.gs !== 'Misc' ? ' · ' + esc(it.gs) : ''} · score ${it.sc}</small></div></div>`).join('')}</div>`;
    return h;
  }

  /* ---------------- Revise ---------------- */
  function facts() { return J(N.factbank(), []); }
  function viewRevise() {
    let h = `<div class="h-title">Library</div><div class="h-sub">Every archived edition, your fact bank and saved topics</div>`;
    h += `<div class="search">${ic('search', 'sm')}<input id="gq" placeholder="Search all editions — e.g. Ramsar, Article 200, repo" value="${esc(S.gq || '')}" autocomplete="off"></div>`;
    if (S.gq && S.gq.trim().length >= 2) {
      const res = J(N.search(S.gq), []);
      S.searchRes = res;
      h += `<div class="sect" style="margin-top:4px"><h2>Results</h2><span class="c">${res.length}</span><span class="rule"></span></div>`;
      if (!res.length) return h + '<div class="note">No topic in your archive matches.</div>';
      return h + res.map((t, i) => { const k = keyParts(t.date); return `<div class="note" style="padding:6px 2px 4px">${k.d} ${k.mon} ${k.y}</div>` + card(t, i); }).join('');
    }
    h += `<div class="chips">${[['archive', 'Archive'], ['sheet', 'Revision'], ['facts', 'Facts'], ['saved', 'Saved'], ['reported', 'Reported' + (S.reported.length ? ' (' + S.reported.length + ')' : '')]].map(([k, l]) => `<button class="chip ${S.rev === k ? 'on' : ''}" data-rev="${k}">${l}</button>`).join('')}</div>`;
    if (S.rev === 'facts') h += factsView();
    else if (S.rev === 'saved') h += savedView();
    else if (S.rev === 'sheet') h += sheetView();
    else if (S.rev === 'reported') h += reportedView();
    else h += archiveView();
    return h;
  }
  function factsView() {
    const all = facts().map((f, i) => Object.assign({ i }, f));
    const q = S.factQ.toLowerCase();
    const list = (q ? all.filter(f => (f.t + ' ' + f.d).toLowerCase().includes(q)) : all).reverse();
    let h = `<div style="display:flex;gap:8px;margin-bottom:12px"><input class="field" id="fq" placeholder="Filter ${all.length} facts" value="${esc(S.factQ)}" style="flex:1" autocomplete="off"><button class="btn ghost" data-act="fexport" style="height:44px" aria-label="Export">${ic('share', 'sm')}</button></div>`;
    if (!list.length) return h + `<div class="empty"><div class="orb">${ic('book')}</div><h3>${q ? 'No match' : 'Fact bank is empty'}</h3><p>${q ? 'Try another term.' : 'Each final edition appends its new exam figures here, tagged [DD-MM-YYYY] and de-duplicated.'}</p></div>`;
    return h + `<div class="list">${list.slice(0, 500).map(f => `<div class="fact"><span class="d">${esc((f.d || '').slice(0, 5))}<br><span style="opacity:.7">${f.src === 'ai' ? 'Checked' : 'Auto'}</span></span><div style="flex:1">${figs(f.t)}</div><button class="x" data-fdel="${f.i}" aria-label="Delete">${ic('x', 'sm')}</button></div>`).join('')}</div>`;
  }
  /* ---------- Revision sheet: last 7 / 30 days, grouped by GS paper, no questions ---------- */
  function sheetData(days) {
    const ck = days + '|' + editions().length + '|' + (S.boot.lastSync || 0) + '|' + S.hidden.size;
    if (S.sheetCache && S.sheetCache.k === ck) { S.sheetCache.d.ents = new Set(); return S.sheetCache.d; }
    const d = sheetData0(days);
    S.sheetCache = { k: ck, d };
    return d;
  }
  function sheetData0(days) {
    const eds = editions().slice(0, days);
    const groups = {}, seen = [], facts = [], linked = {};
    const tok = s => new Set(String(s).toLowerCase().replace(/[^a-z0-9 ]+/g, ' ').split(' ').filter(w => w.length > 3));
    const jac = (a, b) => { let i = 0; a.forEach(x => b.has(x) && i++); return i / Math.max(1, a.size + b.size - i); };
    eds.forEach(e => {
      const ed = J(N.edition(e.date), null);
      if (!ed) return;
      (ed.topics || []).concat(ed.catchup || []).forEach(t => {
        const tk = tok(t.head);
        const dup = linked[ed.date + '|' + t.head] || seen.find(x => jac(x.tk, tk) >= 0.5);
        if (dup) { dup.t.days = (dup.t.days || 1) + 1; return; }
        const row = { t: Object.assign({ date: ed.date }, t), tk };
        seen.push(row);
        (t.thread || []).forEach(x => { linked[x.d + '|' + x.h] = row; });
        const g = gsPaper(t.gs);
        (groups[g] = groups[g] || []).push(row.t);
      });
    });
    const from = eds.length ? eds[eds.length - 1].date : '', to = eds.length ? eds[0].date : '';
    J(N.factbank(), []).forEach(f => { const d = (f.d || '').split('-').reverse().join('-'); if (!from || (d >= from && d <= to)) if (!hid(f.t)) facts.push(f); });
    return { groups, facts, from, to, n: seen.length, ents: new Set() };
  }
  function sheetTopic(t, plain, D) {
    const k = keyParts(t.date);
    const bul = bulletsOf(t).filter(b => !hid(b)).slice(0, 2);
    const ents = (t.ents || []).map(id => ENT[id]).concat((t.cons || []).map(id => CON[id])).filter(Boolean).slice(0, 2);
    const mp = (t.maps || []).map(id => MAP[id]).filter(m => m && !hid(m.line));
    const statics = ents.map(e => { const again = D && D.ents.has(e.id); if (D) D.ents.add(e.id); return { n: e.name, l: again ? 'see above' : e.line.split(' | ').filter(x => !hid(x)).join(' | '), tr: again ? [] : (e.traps || []).filter(x => !hid(x)).slice(0, 2) }; });
    const pq = (t.pyqEv || []).length ? (t.pyq || '').replace(/^PYQ:\s*/, '') : '';
    const also = (t.also || []).filter(x => !hid(x.head)).map(x => x.head);
    if (plain) {
      const L = [`• ${t.head} [${t.gs}; ${k.d} ${k.mon}${t.days > 1 ? '; ' + t.days + ' days' : ''}]`];
      bul.forEach(b => L.push('   – ' + b));
      also.forEach(x => L.push('   + Also: ' + x));
      statics.forEach(x => { L.push('   S: ' + x.n + ' — ' + x.l); x.tr.forEach(tr => L.push('   ⚠ ' + tr)); });
      mp.forEach(m => L.push('   Map: ' + m.name + ' — ' + m.line));
      if (pq) L.push('   PYQ: ' + pq);
      return L.join('\n');
    }
    return `<div class="rv"><div class="rvh"><b>${esc(t.head)}</b><span>${k.d} ${k.mon}${t.days > 1 ? ' · ' + t.days + ' days' : ''}</span></div>
      ${bul.map(b => `<div class="rvb">${figs(b)}</div>`).join('')}
      ${also.map(x => `<div class="rvb">Also: ${figs(x)}</div>`).join('')}
      ${statics.map(x => `<div class="rvs"><b>${esc(x.n)}</b> — ${figs(x.l)}${x.tr.map(tr => `<div class="rvt">⚠ ${figs(tr)}</div>`).join('')}</div>`).join('')}
      ${mp.map(m => `<div class="rvm">${ic('pin', 'xs')} ${esc(m.name)} — ${esc(m.line)}</div>`).join('')}
      ${pq ? `<div class="rvp">${ic('target', 'xs')} ${esc(pq)}</div>` : ''}</div>`;
  }
  const PAPERS = [['GS-I', 'GS-I: History, Culture, Geography, Society'], ['GS-II', 'GS-II: Polity, Governance, IR'], ['GS-III', 'GS-III: Economy, Environment, S&T, Security'], ['GS-IV', 'GS-IV: Ethics'], ['Misc', 'Miscellaneous']];
  function sheetView() {
    const D = sheetData(S.revSpan);
    let h = `<div class="seg"><button data-span="7" class="${S.revSpan === 7 ? 'on' : ''}">${ic('clock', 'sm')}Last 7 days</button><button data-span="30" class="${S.revSpan === 30 ? 'on' : ''}">${ic('archive', 'sm')}Last 30 days</button></div>`;
    if (!D.n) return h + `<div class="empty"><div class="orb">${ic('file')}</div><h3>Nothing to revise yet</h3><p>The sheet fills up as daily editions are archived.</p></div>`;
    const a = keyParts(D.from), b = keyParts(D.to);
    h += `<div class="rvtop"><div><b>${D.n} topics, ${D.facts.length} facts</b><small>${a.d} ${a.mon} to ${b.d} ${b.mon} ${b.y}. Repeats merged.</small></div><button class="btn" data-act="pdf" style="height:38px">${ic('file', 'sm')}PDF</button><button class="btn ghost" data-act="sheetshare" style="height:38px" aria-label="Share as text">${ic('share', 'sm')}</button></div>`;
    PAPERS.forEach(([g, l]) => { const ts = D.groups[g]; if (!ts || !ts.length) return; h += `<div class="sect rvsect"><h2>${esc(l)}</h2><span class="c">${ts.length}</span><span class="rule"></span></div><div class="list rvl">${ts.map(t => sheetTopic(t, false, D)).join('')}</div>`; });
    if (D.facts.length) h += `<div class="sect"><h2>Fact bank</h2><span class="c">${D.facts.length}</span><span class="rule"></span></div><div class="list">${D.facts.slice(-200).reverse().map(f => `<div class="fact"><span class="d">${esc((f.d || '').slice(0, 5))}</span><div style="flex:1">${figs(f.t)}</div></div>`).join('')}</div>`;
    return h;
  }
  function sheetText() {
    const D = sheetData(S.revSpan);
    const a = keyParts(D.from), b = keyParts(D.to);
    const L = [`NEWS CRUX — REVISION SHEET (${a.d} ${a.mon} – ${b.d} ${b.mon} ${b.y})`, ''];
    PAPERS.forEach(([g, l]) => { const ts = D.groups[g]; if (!ts || !ts.length) return; L.push('== ' + l + ' =='); ts.forEach(t => L.push(sheetTopic(t, true, D))); L.push(''); });
    if (D.facts.length) { L.push('== Fact bank =='); D.facts.forEach(f => L.push(`[${f.d || ''}] ${f.t}`)); }
    return L.join('\n');
  }
  function sheetHtml() {
    const D = sheetData(S.revSpan);
    const a = keyParts(D.from), b = keyParts(D.to);
    const e = s => esc(s);
    let h = `<!doctype html><html><head><meta charset="utf-8"><title>News Crux revision</title><style>
      @page{size:A4;margin:14mm 12mm}body{font:10.5pt/1.42 Georgia,serif;color:#16120c;margin:0}
      h1{font:700 18pt Georgia,serif;margin:0 0 2px}.sub{color:#6b5d4a;font:9pt sans-serif;margin-bottom:10px}
      h2{font:700 12.5pt Georgia,serif;border-bottom:2px solid #C9B6F2;padding-bottom:2px;margin:14px 0 6px;page-break-after:avoid}
      .t{margin:0 0 7px;page-break-inside:avoid}.t b{font:700 10.5pt sans-serif}.d{color:#8a7a62;font:8.5pt sans-serif}
      .b{margin:1px 0 0 12px}.s{margin:2px 0 0 12px;font:9.2pt sans-serif;color:#2b2419}.tr{margin:1px 0 0 22px;font:9pt sans-serif;color:#A65555}
      .m{margin:1px 0 0 12px;font:9pt sans-serif;color:#2E7A62}.p{margin:1px 0 0 12px;font:9pt sans-serif;color:#A0563F}
      .f{font:9.3pt sans-serif;margin:0 0 2px}</style></head><body>
      <h1>News Crux: revision sheet</h1><div class="sub">${a.d} ${a.mon} – ${b.d} ${b.mon} ${b.y} · ${D.n} topics · ${D.facts.length} facts · UPSC CSE Prelims</div>`;
    PAPERS.forEach(([g, l]) => {
      const ts = D.groups[g]; if (!ts || !ts.length) return;
      h += `<h2>${e(l)}</h2>`;
      ts.forEach(t => {
        const k = keyParts(t.date);
        h += `<div class="t"><b>${e(t.head)}</b> <span class="d">${k.d} ${k.mon}${t.days > 1 ? ' · ' + t.days + ' days' : ''}</span>`;
        bulletsOf(t).filter(x => !hid(x)).slice(0, 2).forEach(x => { h += `<div class="b">– ${e(x)}</div>`; });
        (t.also || []).filter(x => !hid(x.head)).forEach(x => { h += `<div class="b">+ Also: ${e(x.head)}</div>`; });
        (t.ents || []).map(id => ENT[id]).concat((t.cons || []).map(id => CON[id])).filter(Boolean).slice(0, 2).forEach(en => {
          if (D.ents.has(en.id)) { h += `<div class="s"><b>${e(en.name)}</b> — see above</div>`; return; }
          D.ents.add(en.id);
          h += `<div class="s"><b>${e(en.name)}</b> — ${e(en.line.split(' | ').filter(x => !hid(x)).join(' | '))}</div>`; (en.traps || []).filter(x => !hid(x)).slice(0, 2).forEach(x => { h += `<div class="tr">⚠ ${e(x)}</div>`; }); });
        (t.maps || []).map(id => MAP[id]).filter(m => m && !hid(m.line)).forEach(m => { h += `<div class="m">Map: ${e(m.name)} — ${e(m.line)}</div>`; });
        if ((t.pyqEv || []).length) h += `<div class="p">PYQ: ${e((t.pyq || '').replace(/^PYQ:\s*/, ''))}</div>`;
        h += '</div>';
      });
    });
    if (D.facts.length) { h += '<h2>Fact bank</h2>'; D.facts.forEach(f => { h += `<div class="f">[${e(f.d || '')}] ${e(f.t)}</div>`; }); }
    return h + '</body></html>';
  }
  function reportedView() {
    const r = S.reported.slice().reverse();
    if (!r.length) return `<div class="empty"><div class="orb">${ic('flag')}</div><h3>No reported lines</h3><p>Spot a doubtful fact? Tap the flag on any topic — the line is hidden at once and listed here so you can share it.</p></div>`;
    return `<div class="rvtop"><div><b>${r.length} reported line${r.length > 1 ? 's' : ''}</b><small>Hidden from every edition and the revision sheet</small></div><button class="btn ghost" data-act="repshare" style="height:38px">${ic('share', 'sm')}Share</button></div>
      <div class="list">${r.map((x, i) => `<div class="li"><span class="ic warn">${ic('flag', 'xs')}</span><div class="t">${esc(x.line)}<small>${esc(x.h || '')}${x.d ? ' · ' + esc(x.d) : ''}${x.note ? ' · “' + esc(x.note) + '”' : ''}</small></div><button class="mini wide" data-unrep="${esc(x.k)}">${ic('eye', 'xs')}Unhide</button></div>`).join('')}</div>`;
  }
  function reportSheet(t) {
    const rows = [];
    bulletsOf(t).forEach(b => rows.push({ k: b, line: b }));
    const ab = aboutOf(t);
    if (ab) (ab.pts || []).forEach(x => rows.push({ k: x, line: ab.h + ': ' + x }));
    ((t.ai && t.ai.tr) || []).forEach(x => rows.push({ k: x, line: 'Trap — ' + x }));
    (t.also || []).forEach(x => { rows.push({ k: x.head, line: 'Also: ' + x.head }); (x.bullets || []).forEach(b => rows.push({ k: b, line: b })); });
    (t.ents || []).map(id => ENT[id]).concat((t.cons || []).map(id => CON[id])).filter(Boolean).forEach(e => {
      e.line.split(' | ').forEach(x => rows.push({ k: x, line: e.name + ': ' + x }));
      (e.facts || []).forEach(x => rows.push({ k: x, line: e.name + ': ' + x }));
      (e.traps || []).forEach(x => rows.push({ k: x, line: 'Trap — ' + x }));
    });
    (t.maps || []).map(id => MAP[id]).filter(Boolean).forEach(m => { rows.push({ k: m.line, line: 'Map ' + m.name + ': ' + m.line }); (m.traps || []).forEach(x => rows.push({ k: x, line: 'Trap — ' + x })); });
    rows.push({ k: 'pyq|' + t.id, line: 'PYQ link doesn\'t fit this story — ' + (t.pyq || '') });
    const vis = rows.filter(r => !hid(r.k));
    S.repRows = vis; S.repTopic = t;
    openSheet(`<h3>Report an error</h3><div class="note" style="padding-top:0">Tick the wrong or doubtful line(s). They are hidden immediately and saved in Library → Reported.</div>
      <div class="list replist">${vis.map((r, i) => `<label class="li rep"><input type="checkbox" data-rep="${i}"><div class="t">${figs(r.line)}</div></label>`).join('')}</div>
      <input class="field" id="repnote" placeholder="Optional note — what's wrong?" style="margin:12px 0" autocomplete="off">
      <button class="btn block" data-act="repsave">${ic('flag', 'sm')}Hide & report</button>`);
  }
  function savedView() {
    if (!S.bookmarks.length) return `<div class="empty"><div class="orb">${ic('star')}</div><h3>No saved topics</h3><p>Tap the star on any topic to keep it here for revision.</p></div>`;
    return S.bookmarks.slice().reverse().map((t, i) => card(t, i)).join('');
  }
  function archiveView() {
    const eds = editions();
    if (!eds.length) return `<div class="empty"><div class="orb">${ic('archive')}</div><h3>Archive is empty</h3><p>Each day's edition is saved here under its news date.</p></div>`;
    return `<div class="list">${eds.map(e => `<div class="li" data-open="${e.date}"><span class="ic">${ic('paper', 'xs')}</span><div class="t"><b style="font-family:var(--serif);font-size:15.5px">${esc(e.heading || e.date)}</b><small>${e.topics} stories, ${esc(e.status)}</small>${e.lead ? `<div style="color:var(--muted);font-size:12.8px;margin-top:3px">${esc(e.lead)}</div>` : ''}</div>${ic('chev', 'sm')}</div>`).join('')}</div>`;
  }

  /* ---------------- Settings page ---------------- */
  const PROVS = ['gemini', 'groq', 'openrouter', 'openai', 'xai', 'claude'];
  const MODELS = {
    gemini: [['gemini-3.5-flash-lite', 'Gemini 3.5 Flash-Lite (recommended · light)'], ['gemini-3.1-flash-lite', 'Gemini 3.1 Flash-Lite'], ['gemini-3.8-flash', 'Gemini 3.8 Flash (stronger)']],
    groq: [['openai/gpt-oss-120b', 'GPT-OSS 120B (recommended · web search)'], ['openai/gpt-oss-20b', 'GPT-OSS 20B (lighter · web search)'], ['llama-3.3-70b-versatile', 'Llama 3.3 70B (no web search)']],
    openrouter: [['openrouter/free', 'Free models router (recommended · no credits needed)'], ['google/gemini-2.5-flash-lite', 'Gemini 2.5 Flash-Lite (needs credits · web search)']],
    openai: [['gpt-6-luna', 'GPT-6 Luna (recommended · light)'], ['gpt-6.1-sol', 'GPT-6.1 Sol (stronger)']],
    xai: [['grok-4.7', 'Grok 4.7 (web search)']],
    claude: [['claude-haiku-4-5-20251001', 'Claude Haiku 4.5 (light)'], ['claude-sonnet-5-5', 'Claude Sonnet 5.5'], ['claude-opus-5-5', 'Claude Opus 5.5']]
  };
  const KEYHELP = { gemini: 'aistudio.google.com → Get API key (starts with AIza). Free tier works; live web search needs a paid tier.', groq: 'console.groq.com → API Keys (starts with gsk_). Note: Groq ≠ Grok (xAI).', openrouter: 'openrouter.ai → Keys (starts with sk-or-). The free router needs no credits (about 50 requests/day — enough as a backup); live web search needs credits.', openai: 'platform.openai.com → API keys. Needs prepaid API credit (a ChatGPT subscription does not include it).', xai: 'console.x.ai → API keys (starts with xai-). Needs API credit.', claude: 'console.anthropic.com → API keys.' };
  const MODELNOTE = { gemini: 'Grounded with Google Search.', groq: 'Free-tier token limits are small — News Crux sends the cards to Groq in small batches.', openrouter: 'Free router = cross-checked against fetched sources (no live web). Any OpenRouter model ID works via “Other model ID”.', openai: 'Uses the Responses API with web search.', xai: 'Uses xAI web search.', claude: 'Uses Claude web search.' };
  function openPage(html) { const p = $('#page'); p.innerHTML = html; p.classList.add('on'); S.pageOpen = true; }
  function closePage() { $('#page').classList.remove('on'); S.pageOpen = false; S.subPage = false; }
  /* A failing feed: how long it has been silent; removed after 7 days without a response */
  function feedDown(x) {
    const days = x.since ? Math.floor((Date.now() - x.since) / 864e5) : 0;
    return days >= 1 ? `No response for ${days} day${days > 1 ? 's' : ''}, retrying every sync (removed after 7)` : 'retrying every sync (' + x.fails + ' misses)';
  }
  function sw(id, on) { return `<label class="sw"><input type="checkbox" data-sw="${id}" ${on ? 'checked' : ''}><i></i></label>`; }
  function settingsPage() {
    loadBoot();
    const st = S.boot.settings;
    const health = J(N.health(), []);
    const by = {};
    health.forEach(h => { (by[h.code] = by[h.code] || []).push(h); });
    const disabled = new Set((st.disabledSources || '').split(',').filter(Boolean));
    const srcRows = (window.SOURCES || []).map(s => {
      const hs = by[s.code] || []; const ok = hs.filter(x => x.ok).length;
      const live = hs.filter(x => !x.retired).length, gone = hs.length - live;
      const items = hs.reduce((a, x) => a + (x.count || 0), 0);
      const state = disabled.has(s.code) ? 'off' : !hs.length ? 'pending' : ok ? 'ok' : 'bad';
      return `<div class="set" style="align-items:flex-start"><div class="t"><b>${esc(s.name)} <span style="font-family:var(--mono);font-size:11px;color:var(--muted)">${esc(s.code)}${s.tier === 'coaching' ? ', coaching (priority)' : ''}</span></b>
        <small>${state === 'off' ? 'Disabled' : state === 'pending' ? 'Not fetched yet' : `${ok}/${live} feeds OK · ${items} items${gone ? ` · ${gone} removed (no response for 7 days)` : ''}`}</small>
        <div class="feeds" style="display:none">${hs.map(x => `<div class="feed"><span class="st ${x.ok ? 'ok' : 'bad'}"></span><span style="flex:1">${esc(x.label)}</span><small>${x.ok ? x.count + ' · ' + x.ms + 'ms' : x.retired ? 'Removed: no response for 7 days. Checked again weekly' : x.fails ? feedDown(x) : 'retrying next sync'}</small></div>`).join('')}</div>
        ${hs.length ? '<button class="chip" data-act="feeds" style="margin-top:6px;height:26px">Feed details</button>' : ''}</div>${sw('src:' + s.code, !disabled.has(s.code))}</div>`;
    }).join('');
    st.providers = st.providers || {};
    st.provider = st.provider || 'gemini';
    const pv = st.providers[st.provider] || { name: st.provider, model: '' };
    const order = (st.order || []).filter(p => (st.providers[p] || {}).hasKey);
    const models = MODELS[st.provider] || [];
    const custom = pv.model && !models.some(m => m[0] === pv.model);
    return `<header class="top"><button class="iconbtn" data-act="close">${ic('back')}</button><div class="ttl">Settings</div></header><div class="body">
      <div class="gh">Sync</div><div class="group">
        <div class="set"><div class="t"><b>Background sync</b><small>Fetches every source and rebuilds editions. Also runs whenever you open the app.</small></div><div class="opts">${[1, 3, 6].map(n => `<button data-int="${n}" class="${st.intervalHours === n ? 'on' : ''}">${n}h</button>`).join('')}</div></div>
        <div class="set" style="display:block"><div class="t"><b>Morning edition time</b><small>Yesterday's edition is finalised and notified at this time (IST) — even if the phone is asleep, it uses what was already fetched.</small></div><div class="opts" style="margin-top:10px;display:inline-flex">${[[300, '5:00'], [360, '6:00'], [420, '7:00'], [480, '8:00'], [540, '9:00']].map(([m, l]) => `<button data-dlv="${m}" class="${(st.deliverAt == null ? 360 : st.deliverAt) === m ? 'on' : ''}">${l}</button>`).join('')}</div></div>
        <div class="set"><div class="t"><b>Last sync</b><small>${S.boot.lastSync ? hhmm(S.boot.lastSync) + ' IST, ' + ago(S.boot.lastSync) + '. ' + S.boot.feedsOk + ' of ' + S.boot.feedsTotal + ' sources working' : 'Never'}</small></div><button class="btn" data-act="sync" style="height:38px">${ic('refresh', 'sm')}Sync</button></div>
        <div class="set"><div class="t"><b>Unrestricted battery</b><small>${S.boot.batteryUnrestricted ? 'Allowed — 3-hourly sync runs reliably.' : esc(oemTip(S.boot.maker))}</small></div>${S.boot.batteryUnrestricted ? `<span class="pill ok">${ic('check', 'xs')}On</span>` : '<button class="btn ghost" data-act="battery" style="height:38px">Allow</button>'}</div>
      </div>
      <div class="gh">Notifications</div><div class="group">
        ${S.boot.notifPermission === false ? `<div class="set"><div class="t"><b>Permission needed</b><small>Android is blocking News Crux notifications.</small></div><button class="btn" data-act="notifperm" style="height:38px">Allow</button></div>` : ''}
        <div class="set"><div class="t"><b>Edition & amendments</b><small>When the day's digest is final, and for post-delivery amendments.</small></div>${sw('notif', st.notif)}</div>
        <div class="set"><div class="t"><b>High-yield alerts</b><small>New stories scoring high against your PYQ tiers.</small></div>${sw('alerts', st.alerts)}</div>
        <div class="set"><div class="t"><b>Quiet hours</b><small>No alerts 10:30 PM – 6:30 AM IST.</small></div>${sw('quiet', st.quiet)}</div>
        <div class="set"><div class="t"><b>Send a test notification</b></div><button class="btn ghost" data-act="testnotif" style="height:38px">${ic('bell', 'sm')}Test</button></div>
      </div>
      <div class="gh">AI writer (optional)</div><div class="group">
        <div class="set" style="display:block"><div class="t"><b>Order of use</b><small>The first service rewrites and fact-checks the cards. If it fails (bad key, quota, outage), the next one takes over automatically.</small></div>
          <div class="order">${order.length ? order.map((p, i) => `<div class="ord"><span class="on">${i + 1}</span><b>${esc((st.providers[p] || {}).name || p)}</b><small>${i === 0 ? 'primary' : 'backup'}</small><span class="sp"></span>${i > 0 ? `<button class="mini" data-up="${p}" aria-label="Move up">${ic('back', 'xs')}</button>` : ''}</div>`).join('') : '<div class="note" style="padding:4px 0">No key saved yet.</div>'}${order.length === 1 ? '<div class="note" style="padding:6px 0 0">Add a second key (e.g. Groq) to get an automatic backup.</div>' : ''}</div></div>
        <div class="set" style="display:block"><div class="t"><b>Add or edit a key</b><small>Keys are stored only on this phone and sent only to that service. Light, low-cost models are pre-selected.</small></div>
          <div class="opts wrap" style="margin-top:10px">${PROVS.map(p => `<button data-prov="${p}" class="${st.provider === p ? 'on' : ''}">${esc((st.providers[p] || {}).name || p)}${(st.providers[p] || {}).hasKey ? ' ●' : ''}</button>`).join('')}</div></div>
        <div class="set" style="display:block"><div class="t"><b>${esc(pv.name)} API key</b><small>${esc(KEYHELP[st.provider] || '')}</small></div>
          <div style="display:flex;gap:8px;margin-top:10px"><input class="field" id="apikey" type="password" placeholder="${pv.hasKey ? esc(pv.keyHint) + ' (saved)' : 'Paste key'}" autocomplete="off"><button class="btn" data-act="savekey" style="height:44px">Save</button></div>
          <div style="display:flex;gap:8px;margin-top:10px;flex-wrap:wrap">${pv.hasKey ? `<button class="chip" data-act="testkey">${ic('shield', 'xs')}Test key</button><button class="chip" data-act="clearkey">Remove key</button>` : ''}</div>
          <div id="aitest" class="aitest">${S.aitest || ''}</div></div>
        <div class="set" style="display:block"><div class="t"><b>Model</b><small>${esc(MODELNOTE[st.provider] || '')}</small></div><select class="field" id="model" style="margin-top:8px">${models.map(m => `<option value="${m[0]}" ${pv.model === m[0] ? 'selected' : ''}>${esc(m[1])}</option>`).join('')}${custom ? `<option value="${esc(pv.model)}" selected>${esc(pv.model)}</option>` : ''}<option value="__custom">Other model ID…</option></select></div>
        <div class="set"><div class="t"><b>Fact-check pass</b><small>A second pass checks every figure, date and static fact against authentic sources before a card is shown.</small></div>${sw('verifyPass', st.verifyPass)}</div>
        <div class="set"><div class="t"><b>Web search for the fact-check</b><small>Checks on the live web when your key allows it; otherwise against the articles already read.</small></div>${sw('webSearch', st.webSearch)}</div>
        <div class="set"><div class="t"><b>Write cards automatically</b><small>Runs in the background after each sync, so the edition is ready by ${dlvLabel(st.deliverAt)} IST.</small></div>${sw('autoAI', st.autoAI)}</div>
      </div>
      <div class="gh">Appearance</div><div class="group">
        <div class="set"><div class="t"><b>Theme</b></div><div class="opts">${[['auto', 'Auto'], ['dark', 'Twilight'], ['light', 'Daylight']].map(([k, l]) => `<button data-theme-set="${k}" class="${st.theme === k ? 'on' : ''}">${l}</button>`).join('')}</div></div>
        <div class="set"><div class="t"><b>GS-wise view</b><small>Shows story counts for GS-I to GS-IV on the edition. Tap one to see only that paper.</small></div>${sw('gsView', st.gsView)}</div>
        <div class="set"><div class="t"><b>Text size</b></div><div class="opts">${[[90, 'S'], [100, 'M'], [112, 'L'], [125, 'XL']].map(([k, l]) => `<button data-ts="${k}" class="${st.textScale === k ? 'on' : ''}">${l}</button>`).join('')}</div></div>
      </div>
      <div class="gh">Sources (newspapers, PIB and coaching pages)</div><div class="group">${srcRows}</div>
      <div class="gh">Your data</div><div class="group">
        <div class="set"><div class="t"><b>Back up</b><small>One file with settings, API keys, fact bank, saved topics, reported lines and the last 90 editions. Keep it private.</small></div><button class="btn ghost" data-act="backup" style="height:38px">Save</button></div>
        <div class="set"><div class="t"><b>Restore</b><small>Pick a News Crux (or News Break) backup file — merges into this phone.</small></div><button class="btn ghost" data-act="restore" style="height:38px">Open</button></div>
        <div class="set" data-act="setup"><div class="t"><b>Setup checklist</b><small>Notifications, battery and AI keys — re-run any time.</small></div>${ic('chev', 'sm')}</div>
      </div>
      <div class="gh">App</div><div class="group">${updRow()}</div>
      <div class="gh">Rules</div><div class="group">
        <div class="set" data-act="rules"><div class="t"><b>Your master prompt</b><small>The locked format and rules every edition follows.</small></div>${ic('chev', 'sm')}</div>
        <div class="set" data-act="checklog"><div class="t"><b>Pre-send check log</b><small>Rule trips by date (pattern flagged after 3 in a row).</small></div>${ic('chev', 'sm')}</div>
      </div>
      <div class="foot">News Crux ${esc(S.boot.version || '')} · 3,726 CSE (1995–2026) + 3,363 CDS, CAPF, NDA, Geo-Scientist, EPFO and ESE PYQs · ${KB.entities.length} verified profiles · ${(window.SOURCES || []).length} sources</div>
    </div>`;
  }
  function updRow() {
    const u = S.boot.update || {};
    const when = u.checkedAt ? 'Checked ' + ago(u.checkedAt) + '.' : 'Not checked yet.';
    if (u.newer) return `<div class="set"><div class="t"><b>Version ${esc(u.latest)} is out</b><small>You have ${esc(u.current || S.boot.version || '')}. Your editions and settings stay.</small></div><button class="btn" data-act="upd-install" style="height:38px">${S.updBusy ? 'Downloading…' : 'Install'}</button></div>`;
    return `<div class="set"><div class="t"><b>Updates</b><small>Version ${esc(u.current || S.boot.version || '')}. ${u.error ? 'Last check failed. ' : 'Up to date. '}${when}</small></div><button class="btn ghost" data-act="upd-check" style="height:38px">${u.busy ? 'Checking…' : 'Check'}</button></div>`;
  }
  function dlvLabel(m) { m = m == null ? 360 : m; return Math.floor(m / 60) + ':' + pad(m % 60); }
  /* ---------------- First-run setup ---------------- */
  const OEM = {
    nothing: 'Nothing phone: Settings → Apps → News Crux → Battery → Unrestricted.',
    xiaomi: 'Xiaomi/Redmi/POCO: App info → Autostart ON, and Battery saver → No restrictions.',
    vivo: 'vivo/iQOO: Settings → Battery → Background power consumption → News Crux → Allow.',
    oppo: 'OPPO/realme: App info → Battery usage → Allow background activity + Allow auto launch.',
    oneplus: 'OnePlus: App info → Battery → Unrestricted (and Allow auto launch).',
    samsung: 'Samsung: App info → Battery → Unrestricted, and remove News Crux from Sleeping apps.',
    google: 'Pixel: App info → App battery usage → Unrestricted.'
  };
  function oemTip(m) {
    m = m || '';
    if (/xiaomi|redmi|poco/.test(m)) return OEM.xiaomi;
    if (/vivo|iqoo/.test(m)) return OEM.vivo;
    if (/oppo|realme/.test(m)) return OEM.oppo;
    for (const k in OEM) if (m.indexOf(k) >= 0) return OEM[k];
    return 'App info → Battery → Unrestricted (allow background activity / auto-start if your phone offers it).';
  }
  function setupPage() {
    loadBoot();
    const b = S.boot, st = b.settings || {};
    const notifOk = b.notifPermission !== false && b.notifEnabled !== false;
    const batOk = !!b.batteryUnrestricted;
    const order = (st.order || []).filter(p => ((st.providers || {})[p] || {}).hasKey);
    const step = (n, ok, title, body) => `<div class="stp ${ok ? 'ok' : ''}"><span class="sn">${ok ? ic('check', 'xs') : n}</span><div class="t"><b>${title}</b>${body}</div></div>`;
    return `<header class="top"><button class="iconbtn" data-act="setupdone" aria-label="Close">${ic('x')}</button><div class="ttl">Set up News Crux</div></header><div class="body">
      <div class="h-sub" style="margin:12px 2px 16px">Three quick steps so the morning edition and alerts arrive on time. You can change everything later in Settings.</div>
      ${step(1, notifOk, 'Allow notifications', `<small>${notifOk ? 'On — you\'ll get the morning edition, amendments and high-yield alerts.' : 'Needed for the morning edition and high-yield alerts.'}</small>${notifOk ? '' : `<div class="row"><button class="btn" data-act="notifperm" style="height:38px">Allow</button><button class="btn ghost" data-act="notifsettings" style="height:38px">Open settings</button></div>`}`)}
      ${step(2, batOk, 'Let it run in the background', `<small>${batOk ? 'Unrestricted — 3-hourly sync runs reliably.' : 'Phones pause background apps to save battery; allow News Crux so the 3-hourly sync and morning edition aren\'t delayed.'}</small><div class="tip">${esc(oemTip(b.maker))}</div>${batOk ? '' : `<div class="row"><button class="btn" data-act="battery" style="height:38px">Allow</button><button class="btn ghost" data-act="appsettings" style="height:38px">App info</button></div>`}`)}
      ${step(3, order.length > 0, 'Add your AI key (optional)', `<small>${order.length ? 'Using ' + order.map(p => (st.providers[p] || {}).name).join(' → ') + (order.length === 1 ? '. Add a second key for an automatic backup.' : '.') : 'Without a key you still get the full edition, built from the articles and coaching pages. With one, every card is rewritten in crisp points and fact-checked. Gemini (free) first, Groq or OpenRouter (free) as backup works well.'}</small>
        <div class="row" style="margin-top:8px"><input class="field" id="setupkey" type="password" placeholder="Paste any key — provider is detected" autocomplete="off"><button class="btn" data-act="setupkey" style="height:44px">Save</button></div><div id="aitest" class="aitest">${S.aitest || ''}</div>`)}
      ${step(4, true, 'Morning edition time', `<small>Yesterday's edition is finalised and notified at:</small><div class="opts" style="margin-top:8px;display:inline-flex">${[[300, '5:00'], [360, '6:00'], [420, '7:00'], [480, '8:00'], [540, '9:00']].map(([m, l]) => `<button data-dlv="${m}" class="${(st.deliverAt == null ? 360 : st.deliverAt) === m ? 'on' : ''}">${l}</button>`).join('')}</div>`)}
      <button class="btn block" data-act="setupdone" style="margin-top:18px">Done</button>
    </div>`;
  }
  function rulesPage() {
    const t = N.asset('data/master_prompt.txt') || 'Master prompt not available.';
    return `<header class="top"><button class="iconbtn" data-act="back-settings">${ic('back')}</button><div class="ttl">Master prompt</div></header><div class="body prompt"><pre>${esc(t)}</pre></div>`;
  }
  function checklogPage() {
    const log = J(N.checklog(), []).slice().reverse();
    return `<header class="top"><button class="iconbtn" data-act="back-settings">${ic('back')}</button><div class="ttl">Pre-send check log</div></header><div class="body">${log.length ? `<div class="list">${log.map(l => `<div class="li"><span class="ic warn">${ic('alert', 'xs')}</span><div class="t">Rule ${l.rule} · ${esc(l.fix)}<small>${esc(l.date)}</small></div></div>`).join('')}</div>` : '<div class="empty"><div class="orb">' + ic('shield') + '</div><h3>No rule trips logged</h3><p>Every delivered edition has passed the checklist so far.</p></div>'}</div>`;
  }

  /* ---------------- sheet ---------------- */
  /* Full PYQ (question, options, answer) — loaded on first use */
  function loadPyqText(cb) {
    if (window.PYQ_TEXT) return cb();
    const sc = document.createElement('script'); sc.src = 'data/pyq_text.js';
    sc.onload = () => cb(); sc.onerror = () => toast('Could not open the question');
    document.head.appendChild(sc);
  }
  /* ---------------- PYQ practice: try first, then see the answer ---------------- */
  let TRIES = null;
  const tries = () => TRIES || (TRIES = J(N.pyqTries ? N.pyqTries() : '{}', {}));
  function saveTry(key, ok, ch) { const t = tries(); t[key] = [ok ? 1 : 0, Date.now(), ch]; try { if (N.savePyqTries) N.savePyqTries(JSON.stringify(t)); } catch (e) {} }
  function pyqSet(t) { // every question linked to this card, newest first
    const out = [], seen = {};
    ((t && t.pyqEv) || []).forEach(e => { if (e[2] && !seen[e[2]]) { seen[e[2]] = 1; out.push({ k: e[2], y: Number(e[0]), a: String(e[1] || '') }); } });
    return out.sort((x, y) => y.y - x.y);
  }
  function getQ(key, cb) {
    const q = N.pyqQuestion ? J(N.pyqQuestion(key), null) : null; // the app hands it over directly
    if (q) return cb(q);
    loadPyqText(() => { const x = (window.PYQ_TEXT || {})[key]; if (!x) toast('Question text not available'); else cb(x); });
  }
  function openPyq(key, tid) {
    const t = tid ? findTopic(tid) : null;
    const set = pyqSet(t);
    let i = set.findIndex(x => x.k === key);
    if (i < 0) { set.unshift({ k: key, y: Number(key.split('|')[0]), a: '' }); i = 0; }
    S.pq = { set, i, t };
    loadPq();
  }
  function loadPq() {
    const p = S.pq, cur = p.set[p.i];
    p.q = null; p.pick = null; p.out = {};
    getQ(cur.k, q => { p.q = q; showPyq(); });
  }
  function showPyq(keepScroll) {
    try {
      const p = S.pq, cur = p.set[p.i], q = p.q, key = cur.k, done = p.pick != null, ans = q[2];
      const yr = key.split('|')[0], qn = key.split('|')[1];
      const stem = stemHtml(q[0]);
      const last = tries()[key];
      const ctx = [];
      if (cur.a && cur.a !== 'asked') ctx.push(`Asked about: <b>${esc(cur.a)}</b>`);
      if (p.t && p.t.head) ctx.push(`In the news: ${esc(p.t.head.length > 90 ? p.t.head.slice(0, 88) + '…' : p.t.head)}`);
      const opts = (q[1] || []).map((o, i) => {
        let cls = '', tag = '';
        if (done) {
          if (i === ans) { cls = ' ok'; tag = `<span class="pqk">${ic('check', 'xs')}${p.pick === i ? 'Your answer' : 'Answer'}</span>`; }
          else if (i === p.pick) { cls = ' bad'; tag = `<span class="pqk bad">${ic('x', 'xs')}Your pick</span>`; }
        }
        const out = p.out[i] && !done ? ' out' : '';
        return `<div class="pqrow${out}"><button class="pqopt${cls}" data-pqa="${i}"${done ? ' disabled' : ''}><span class="pql">${'abcd'[i]}</span><span class="pqt">${esc(o)}</span>${tag}</button>${done ? '' : `<button class="pqs" data-pqs="${i}" aria-label="Rule out option ${'abcd'[i]}" aria-pressed="${p.out[i] ? 'true' : 'false'}">${ic('x', 'xs')}</button>`}</div>`;
      }).join('');
      let after = '';
      if (done) {
        const res = p.pick === -1 ? `<div class="pqres">Answer: (${'abcd'[ans]})</div>` : p.pick === ans ? `<div class="pqres ok">Correct.</div>` : `<div class="pqres bad">Not this one. The answer is (${'abcd'[ans]}).</div>`;
        const ex = J(N.pwExplain ? N.pwExplain(key) : '', null); // your own explanation file (imported with Restore), if any
        const paras = x => esc(x).split(/\n+/).filter(Boolean).map(y => `<p>${y}</p>`).join('');
        const exHtml = ex && ex.e ? `<div class="pqx"><div class="pqxh">Explanation</div>${paras(ex.e)}</div>${ex.h ? `<details class="pqxd"><summary>Super hint</summary>${paras(ex.h)}</details>` : ''}${ex.x ? `<details class="pqxd"><summary>Extra edge</summary>${paras(ex.x)}</details>` : ''}` : `<div class="note">${q[4] ? 'Answer as given in the PW solutions, not yet checked against the UPSC answer key.' : 'Answer as per the UPSC key.'}</div>`;
        after = res + exHtml;
      } else {
        after = `<div class="pqhelp"><span>Tap an option to answer. Use ${ic('x', 'xs')} to rule one out first.</span><button class="btn ghost sm" data-pqa="-1">Show answer</button></div>`;
      }
      let nav = '';
      if (p.set.length > 1) {
        const tried = p.set.filter(x => tries()[x.k]), right = tried.filter(x => tries()[x.k][0] === 1).length;
        nav = `<div class="pqnav"><span class="pqcount">Question ${p.i + 1} of ${p.set.length} on this story${tried.length ? `. ${right} of ${tried.length} right so far` : ''}</span>${p.i > 0 ? `<button class="btn ghost sm" data-pqn="-1">Previous</button>` : ''}${p.i < p.set.length - 1 ? `<button class="btn sm" data-pqn="1">Next question</button>` : ''}</div>`;
      }
      const lastHtml = last && !done ? `<div class="pqlast">Last try: ${last[0] ? 'right' : 'missed'}, ${esc(ago(last[1]))}</div>` : '';
      const st = keepScroll ? $('#sheet').scrollTop : 0;
      const lost = q[3] === 1 ? '<div class="pqlost">Part of this question (its list or table) did not come through from the source paper. You can still try it, or tap Show answer.</div>' : '';
      const title = q[4] ? `UPSC ${esc(q[4])}` : `UPSC Prelims ${esc(yr)}, question ${esc(qn)}`; // CDS / CAPF carry their exam and paper
      openSheet(`<h3>${title}</h3>${ctx.length ? `<div class="pqc">${ctx.join('<br>')}</div>` : ''}${lastHtml}<div class="pqq">${stem}</div>${lost}<div class="pqopts">${opts}</div>${after}${nav}`);
      $('#sheet').scrollTop = st;
      if (done) { const r = $('#sheet .pqres'); if (r) r.scrollIntoView({ block: 'nearest', behavior: 'smooth' }); }
    } catch (err) { console.log('pyq open failed: ' + err); toast('Could not open the question'); }
  }
  /** Question in bold; numbered statements on their own lines; the closing ask ("Which of the …") in bold again. */
  function stemHtml(s) {
    const src = String(s || '');
    // 1. / I. statements, "Statement-I :", list headings; A. B. C. D. only in match-the-list questions (not initials like B. R. Ambedkar)
    const lists = /\bList[-–‑ ]?I\b/.test(src) && /\sA\.\s/.test(src) && /\sB\.\s/.test(src);
    const st = '(?:\\d{1,2}|I{1,3}|IV|V|VI' + (lists ? '|[A-F]' : '') + ')\\.\\s|Statement[-–‑ ]?(?:I{1,3}|IV|V)\\s*:' + (lists ? '|List[-–‑ ]?I{1,2}\\s*[(:]' : '');
    const raw = src.split(new RegExp('\\s(?=' + st + ')'));
    const parts = [];
    raw.forEach(x => { if (parts.length && x.trim().length < 6) parts[parts.length - 1] += ' ' + x; else parts.push(x); }); // "age of 14. 2. …" or "Statement I. 3. …": a bare number is not a new line
    const ASK = /(?=(?:Which|How many|Select|Choose|In which|Of the above|What)\b|Code\s*:)/;
    const out = [`<p class="pqqh">${esc(parts[0])}</p>`];
    parts.slice(1).forEach((x, i) => {
      // the closing ask can follow the last statement ("… rooms. Which of the following …")
      let k = x.search(new RegExp('[.?]\\s+' + ASK.source));
      if (k > 0) k = x.indexOf(' ', k) ; else if (i === parts.length - 2) { const m = x.search(new RegExp('\\s' + ASK.source)); k = m > 0 ? m : -1; }
      const body = k > 0 ? x.slice(0, k) : x, ask = k > 0 ? x.slice(k + 1).trim() : '';
      out.push(/^List[-–‑ ]?I/.test(body) ? `<p class="pqqh pql2">${esc(body)}</p>` : `<p class="pqqs">${esc(body)}</p>`);
      if (ask) out.push(`<p class="pqqh">${esc(ask)}</p>`);
    });
    return out.join('');
  }
  function pqAnswer(i) {
    const p = S.pq;
    if (!p || !p.q || p.pick != null) return;
    p.pick = i;
    if (i >= 0) saveTry(p.set[p.i].k, i === p.q[2], i);
    showPyq(true);
    if (S.ed || S.live) render({ keepScroll: true }); // the year marks on the card update
  }
  function openSheet(html) { $('#sheet').innerHTML = '<div class="grab"></div>' + html; $('#sheet').classList.add('on'); $('#scrim').classList.add('on'); S.sheetOpen = true; }
  function closeSheet() { $('#sheet').classList.remove('on'); $('#scrim').classList.remove('on'); S.sheetOpen = false; }
  function findTopic(id) {
    const pools = [];
    if (S.ed && S.ed.topics) pools.push(S.ed.topics);
    if (S.ed && S.ed.catchup) pools.push(S.ed.catchup);
    if (S.live && S.live.topics) pools.push(S.live.topics);
    pools.push(S.bookmarks);
    if (S.tab === 'revise' && S.searchRes) pools.push(S.searchRes);
    for (const p of pools) { const t = p.find(x => x.id === id); if (t) return t; }
    return null;
  }

  /* ---------------- events ---------------- */
  function onClick(e) {
    const el = e.target.closest('[data-up],[data-prov],[data-tab],[data-date],[data-mode],[data-act],[data-lf],[data-lmode],[data-fs],[data-ps],[data-py],[data-rev],[data-open],[data-url],[data-fdel],[data-int],[data-theme-set],[data-ts],[data-dlv],[data-span],[data-unrep],[data-goto],[data-shtheme],[data-shfull],[data-cu],[data-pyq],[data-pqa],[data-pqs],[data-pqn],[data-calm],[data-gsf]');
    if (!el) return;
    const d = el.dataset;
    if (d.tab) return go(d.tab);
    if (d.date) { if (S.sheetOpen) closeSheet(); S.date = d.date; S.ed = null; S.modeChosen = false; render(); return; }
    if (d.calm) { S.calYm = d.calm; openSheet(calSheet()); return; }
    if (d.gsf) { S.gsF = S.gsF === d.gsf ? null : d.gsf; render({ keepScroll: true }); readBar(); return; }
    if (d.lf) { S.liveFilter = d.lf; render({ keepScroll: true }); return; }
    if (d.lmode) { S.liveMode = d.lmode; render(); return; }
    if (d.fs !== undefined && el.classList.contains('chip')) { S.feedSrc = d.fs; render({ keepScroll: true }); return; }
    if (d.rev) { S.rev = d.rev; render(); return; }
    if (d.open) { S.tab = 'edition'; S.date = d.open; S.ed = null; S.modeChosen = false; renderTabs(); render(); return; }
    if (d.url) { N.openUrl(d.url); return; }
    if (d.fdel !== undefined) {
      if (el.dataset.armed) { const all = facts(); all.splice(Number(d.fdel), 1); N.saveFactbank(JSON.stringify(all)); render({ keepScroll: true }); toast('Fact removed'); }
      else { el.dataset.armed = '1'; el.style.color = 'var(--red)'; toast('Tap ✕ again to delete'); setTimeout(() => { delete el.dataset.armed; el.style.color = ''; }, 2500); }
      return;
    }
    if (d.pyq) { if (d.pyq === '-') toast('No full question saved for this one'); else { const art = el.closest('article.card'); openPyq(d.pyq, art ? art.dataset.id : null); } return; }
    if (d.pqa !== undefined) { pqAnswer(Number(d.pqa)); return; }
    if (d.pqs !== undefined) { const p = S.pq; if (p && p.pick == null) { p.out[d.pqs] = !p.out[d.pqs]; showPyq(true); } return; }
    if (d.pqn) { const p = S.pq; if (p) { p.i = Math.max(0, Math.min(p.set.length - 1, p.i + Number(d.pqn))); loadPq(); } return; }
    if (d.up) { const ord = (S.boot.settings.order || []).slice(); const i = ord.indexOf(d.up); if (i > 0) { ord.splice(i, 1); ord.splice(i - 1, 0, d.up); } N.setString('aiOrder', ord.join(',')); S.boot.settings.order = ord; openPage(settingsPage()); toast((S.boot.settings.providers[ord[0]] || {}).name + ' is now primary'); return; }
    if (d.prov) { N.setString('aiProvider', d.prov); S.aitest = ''; openPage(settingsPage()); return; }
    if (d.int) { N.setInt('intervalHours', Number(d.int)); S.boot.settings.intervalHours = Number(d.int); openPage(settingsPage()); toast('Sync every ' + d.int + 'h'); return; }
    if (d.themeSet) { N.setString('theme', d.themeSet); S.boot.settings.theme = d.themeSet; applyTheme(); openPage(settingsPage()); return; }
    if (d.shtheme) { S.shareOpt.theme = d.shtheme; $$('[data-shtheme]').forEach(x => x.classList.toggle('on', x === el)); renderShare(); return; }
    if (d.shfull !== undefined) { S.shareOpt.full = d.shfull === '1'; $$('[data-shfull]').forEach(x => x.classList.toggle('on', x === el)); renderShare(); return; }
    if (d.cu) {
      const id = d.cu;
      if (S.cuOpen.has(id)) S.cuOpen.delete(id);
      else {
        S.cuOpen.add(id);
        const t = (S.ed && S.ed.catchup || []).find(x => x.id === id);
        if (t && !t.ai && t.needAi !== false && (S.boot.settings || {}).hasKey && !S.cuBusy[id] && N.generateAiCard) { // AI only when opened, and only for a thin card
          const r = N.generateAiCard(S.date, id);
          if (r === 'busy') toast('Another card is being written; try again in a moment');
          else { S.cuBusy[id] = true; delete S.cuErr[id]; }
        }
      }
      render({ keepScroll: true });
      return;
    }
    if (d.goto) { const c = $(`#view > .card[data-n="${Number(d.goto)}"]`); if (c) c.scrollIntoView({ behavior: 'smooth', block: 'start' }); return; }
    if (d.dlv) { N.setInt('deliverAt', Number(d.dlv)); S.boot.settings.deliverAt = Number(d.dlv); openPage(S.setupOpen ? setupPage() : settingsPage()); toast('Morning edition at ' + dlvLabel(Number(d.dlv)) + ' IST'); return; }
    if (d.span) { S.revSpan = Number(d.span); render({ keepScroll: true }); return; }
    if (d.unrep) { S.reported = S.reported.filter(r => r.k !== d.unrep); N.saveReported(JSON.stringify(S.reported)); loadReported(); render({ keepScroll: true }); toast('Line visible again'); return; }
    if (d.ts) { N.setInt('textScale', Number(d.ts)); S.boot.settings.textScale = Number(d.ts); applyTheme(); openPage(settingsPage()); return; }
    const card = el.closest('.card');
    const t = card ? findTopic(card.dataset.id) : null;
    switch (d.act) {
      case 'asked': { const a = el.nextElementSibling; a.classList.toggle('on'); el.innerHTML = ic(a.classList.contains('on') ? 'x' : 'down', 'xs') + el.textContent; break; }
      case 'links': if (t) openSheet(`<h3>Sources</h3><div class="list">${(t.links || []).map(l => `<div class="li" data-url="${esc(l.u)}"><span class="ic">${ic('link', 'xs')}</span><div class="t"><b>${esc(SRC_NAME[l.s] || l.s)}</b><small>${esc(l.t)}</small></div>${ic('chev', 'sm')}</div>`).join('') || '<div class="note">No links.</div>'}</div><div class="note">Paywalled articles: News Crux never guesses at paywalled content — only what the outlet publishes openly is used.</div>`); break;
      case 'copy': if (t) { N.copy(topicText(t)); toast('Copied in digest format'); } break;
      case 'share': if (t) shareSheet(t); break;
      case 'bm': if (t) { const i = S.bookmarks.findIndex(b => b.id === t.id); if (i >= 0) S.bookmarks.splice(i, 1); else S.bookmarks.push(Object.assign({ savedAt: Date.now(), date: t.date || (S.tab === 'live' ? S.boot.today : S.date) }, t)); N.saveBookmarks(JSON.stringify(S.bookmarks)); el.classList.toggle('on', i < 0); toast(i < 0 ? 'Saved for revision' : 'Removed from saved'); if (S.tab === 'revise') render({ keepScroll: true }); } break;
      case 'sync': doSync(); break;
      case 'settings': S.setupOpen = false; openPage(settingsPage()); break;
      case 'close': S.setupOpen = false; closePage(); loadBoot(); applyTheme(); render({ keepScroll: true }); break;
      case 'back-settings': S.subPage = false; openPage(settingsPage()); break;
      case 'rules': S.subPage = true; openPage(rulesPage()); break;
      case 'checklog': S.subPage = true; openPage(checklogPage()); break;
      case 'feeds': { const f = el.previousElementSibling; f.style.display = f.style.display === 'none' ? 'block' : 'none'; break; }
      case 'battery': N.openBatterySettings(); break;
      case 'appsettings': N.openAppSettings(); break;
      case 'notifsettings': N.openNotifSettings(); break;
      case 'notifperm': N.requestNotifPermission(); break;
      case 'setup': S.setupOpen = true; openPage(setupPage()); break;
      case 'setupdone': N.setBool('setupDone', true); S.boot.settings.setupDone = true; S.setupOpen = false; closePage(); loadBoot(); render({ keepScroll: true }); break;
      case 'setupkey': {
        const v = ($('#setupkey').value || '').replace(/\s+/g, '');
        if (v.length < 16) { toast('That key looks too short'); break; }
        const det = N.detectProvider ? N.detectProvider(v) : '';
        if (!det) { toast('Unknown key. Pick its provider in Settings'); S.setupOpen = false; openPage(settingsPage()); break; }
        N.setString('aiProvider', det); N.setString('key_' + det, v);
        const ord = (S.boot.settings.order || []).filter(p => p !== det); ord.push(det); N.setString('aiOrder', ord.join(','));
        S.aitest = '<div class="tl">Testing ' + esc(((S.boot.settings.providers || {})[det] || { name: det }).name) + ' key…</div>';
        openPage(setupPage()); N.testAi && N.testAi();
        break;
      }
      case 'thread': { const b = el.nextElementSibling; if (b) b.classList.toggle('on'); break; }
      case 'pqall': { const box = el.closest('.pyq'); if (box) { const on = box.classList.toggle('all'); el.textContent = on ? 'Show fewer' : el.dataset.lab; } break; }
      case 'report': if (t) reportSheet(t); break;
      case 'repsave': {
        const picked = $$('[data-rep]').filter(x => x.checked).map(x => S.repRows[Number(x.dataset.rep)]);
        if (!picked.length) { toast('Tick at least one line'); break; }
        const note = ($('#repnote').value || '').trim(), tp = S.repTopic || {};
        picked.forEach(r => S.reported.push({ k: rkey(r.k), line: r.line, h: tp.head || '', d: tp.date || (S.tab === 'live' ? 'live' : S.date) || '', note, at: Date.now() }));
        N.saveReported(JSON.stringify(S.reported)); loadReported(); closeSheet(); render({ keepScroll: true });
        toast(picked.length + ' line' + (picked.length > 1 ? 's' : '') + ' hidden and saved in Library, Reported');
        break;
      }
      case 'repshare': N.share('News Crux — reported lines\n\n' + S.reported.map(r => `• ${r.line}${r.note ? '  [note: ' + r.note + ']' : ''}${r.h ? '  (topic: ' + r.h + (r.d ? ', ' + r.d : '') + ')' : ''}`).join('\n')); break;
      case 'pdf': { const D = keyParts(editions()[0] ? editions()[0].date : S.boot.today || '2026-01-01'); N.printHtml(sheetHtml(), `NewsCrux-revision-${S.revSpan === 7 ? 'week' : 'month'}-${D.d}${D.mon}${D.y}`); toast('Opening print — choose “Save as PDF”'); break; }
      case 'sheetshare': N.share(sheetText()); break;
      case 'upd-check': N.checkUpdate(); S.boot.update = Object.assign({}, S.boot.update, { busy: true }); openPage(settingsPage()); break;
      case 'upd-install': if (!S.updBusy) { S.updBusy = true; N.installUpdate(); openPage(settingsPage()); } break;
      case 'backup': N.backup(); break;
      case 'restore': N.restore(); break;
      case 'testnotif': N.testNotification(); toast('Test notification sent'); break;
      case 'savekey': {
        const v = ($('#apikey').value || '').replace(/\s+/g, '');
        if (v.length < 16) { toast('That key looks too short'); break; }
        let prov = S.boot.settings.provider || 'gemini';
        const det = N.detectProvider ? N.detectProvider(v) : '';
        if (det && det !== prov) { prov = det; N.setString('aiProvider', det); toast('Recognised a ' + ((S.boot.settings.providers || {})[det] || { name: det }).name + ' key — saved there'); }
        else toast('Key saved — testing it…');
        N.setString('key_' + prov, v);
        { const ord = (S.boot.settings.order || []).filter(p => p !== prov); const had = (S.boot.settings.order || []).indexOf(prov); if (had >= 0) ord.splice(had, 0, prov); else ord.push(prov); S.boot.settings.order = ord; N.setString('aiOrder', ord.join(',')); }
        S.aitest = '<div class="tl">Testing…</div>';
        openPage(settingsPage());
        N.testAi && N.testAi();
        break;
      }
      case 'testkey': { S.aitest = '<div class="tl">Testing…</div>'; const box = $('#aitest'); if (box) box.innerHTML = S.aitest; N.testAi && N.testAi(); break; }
      case 'clearkey': { const pr = S.boot.settings.provider || 'gemini'; const ord = (S.boot.settings.order || []).filter(p => p !== pr); S.boot.settings.order = ord; N.setString('aiOrder', ord.join(',')); }
        N.setString('key_' + (S.boot.settings.provider || 'gemini'), ''); S.aitest = ''; openPage(settingsPage()); toast('Key removed'); break;
      case 'cites': openSheet(`<h3>Sources checked</h3><div class="list">${(S.cites || []).map(c => `<div class="li" data-url="${esc(c.u)}"><span class="ic ok">${ic('link', 'xs')}</span><div class="t"><b>${esc(c.t || domain(c.u))}</b><small>${esc(domain(c.u))}</small></div>${ic('chev', 'sm')}</div>`).join('')}</div>`); break;
      case 'bgmore': { const b = el.nextElementSibling; if (b) b.classList.add('on'); el.remove(); break; }
      case 'smore': { const box = el.parentElement.querySelector('.sfx'); if (box) { box.classList.add('on'); el.remove(); } break; }
      case 'copyerr': N.copy(el.dataset.err || ''); toast('Error copied'); break;
      case 'ai-gen': case 'ai-regen': { const r = N.generateAi(S.date); if (r === 'busy') toast('AI is already working on the cards…'); else { S.aiBusy = S.date; S.aiStage = 'draft'; render({ keepScroll: true }); } break; }
      case 'cal': S.calYm = (S.date || '').slice(0, 7); openSheet(calSheet()); break;
      case 'tocheck': { const c = $('#checked'); if (c) c.scrollIntoView({ behavior: 'smooth', block: 'start' }); break; }
      case 'tocatch': { const c = $('#catchup'); if (c) c.scrollIntoView({ behavior: 'smooth', block: 'start' }); break; }
      case 'missed': { S.missedOpen = !S.missedOpen; const l = $('.missedl'); if (l) l.style.display = S.missedOpen ? 'block' : 'none'; break; }
      case 'shimg': shareNow(false); break;
      case 'shsave': shareNow(true); break;
      case 'shtext': if (S.shareT) { closeSheet(); N.share(topicText(S.shareT) + '\n— via News Crux'); } break;
      case 'shcopy': if (S.shareT) { N.copy(topicText(S.shareT) + '\n— via News Crux'); toast('Copied as text'); } break;
      case 'fexport': { const all = facts(); N.share(all.map(f => `[${f.d || ''}] ${f.t}`).join('\n')); break; }
    }
  }
  function onChange(e) {
    const el = e.target;
    if (el.dataset.sw) {
      const k = el.dataset.sw;
      if (k.startsWith('src:')) {
        const code = k.slice(4);
        const set = new Set((S.boot.settings.disabledSources || '').split(',').filter(Boolean));
        if (el.checked) set.delete(code); else set.add(code);
        S.boot.settings.disabledSources = Array.from(set).join(',');
        N.setString('disabledSources', S.boot.settings.disabledSources);
        toast(el.checked ? code + ' enabled' : code + ' disabled');
      } else { N.setBool(k, el.checked); S.boot.settings[k] = el.checked; if (k === 'gsView') { S.gsF = null; render({ keepScroll: true }); } }
      return;
    }
    if (el.id === 'model') {
      let v = el.value;
      if (v === '__custom') { v = (window.prompt('Model ID (e.g. claude-sonnet-5-5)', S.boot.settings.model) || '').trim(); if (!v) { openPage(settingsPage()); return; } }
      N.setString('model_' + (S.boot.settings.provider || 'gemini'), v); toast('Model: ' + v); openPage(settingsPage());
    }
  }
  let qT;
  function onInput(e) {
    if (e.target.id === 'gq') { clearTimeout(qT); const v = e.target.value; qT = setTimeout(() => { S.gq = v; render({ keepScroll: true }); const i = $('#gq'); i.focus(); i.setSelectionRange(v.length, v.length); }, 300); }
    if (e.target.id === 'fq') { clearTimeout(qT); const v = e.target.value; qT = setTimeout(() => { S.factQ = v; render({ keepScroll: true }); const i = $('#fq'); i.focus(); i.setSelectionRange(v.length, v.length); }, 220); }
  }
  function doSync() {
    const r = N.sync();
    if (r === 'busy') toast('Sync already running…');
    else { S.syncing = true; setSyncBtn(); toast('Syncing all sources…'); }
  }

  /* ---------------- pull to refresh ---------------- */
  function ptr() {
    const m = $('#main'), p = $('#ptr');
    let y0 = null, dy = 0;
    m.addEventListener('touchstart', e => { y0 = m.scrollTop <= 0 ? e.touches[0].clientY : null; dy = 0; }, { passive: true });
    m.addEventListener('touchmove', e => { if (y0 == null) return; dy = e.touches[0].clientY - y0; if (dy > 0) { p.style.transition = 'none'; p.style.height = Math.min(dy * 0.45, 70) + 'px'; p.textContent = dy > 140 ? 'Release to sync' : 'Pull to sync'; } }, { passive: true });
    m.addEventListener('touchend', () => { if (y0 == null) return; p.style.transition = ''; p.style.height = '0'; if (dy > 140) doSync(); y0 = null; });
  }

  /* ---------------- native callbacks ---------------- */
  window.NB = {
    onNative(ev) {
      if (!ev) return;
      if (ev.type === 'sync') {
        if (ev.state === 'start') { S.syncing = true; S.syncStage = ''; setProg(6); setSyncBtn(); }
        else if (ev.state === 'progress') { setProg(Math.max(6, Math.round(70 * ev.done / Math.max(ev.total, 1)))); }
        else if (ev.state === 'stage') { S.syncStage = ev.stage; setProg(ev.stage === 'coaching' ? 78 : 88); setSyncBtn(); }
        else {
          S.syncing = false; setProg(100);
          setTimeout(() => { if (!S.syncing) setProg(null); }, 700);
          loadBoot();
          if (ev.state === 'done') toast(`Synced. ${ev.added} new from ${ev.feedsOk} of ${ev.feedsTotal} sources`);
          else toast('Sync failed — will retry');
          if (!S.date || !editions().some(e => e.date === S.date)) S.date = defaultDate();
          S.ed = null; S.live = null; S.sheetCache = null; loadLive(); renderTabs();
          if (!S.pageOpen) render({ keepScroll: true });
        }
      } else if (ev.type === 'backup' || ev.type === 'restore' || ev.type === 'print') {
        toast(ev.msg || (ev.ok ? 'Done' : 'Failed'));
        if (ev.type === 'restore' && ev.ok) { TRIES = null; loadBoot(); applyTheme(); S.bookmarks = J(N.bookmarks(), []); loadReported(); S.ed = null; if (S.pageOpen) openPage(settingsPage()); render({ keepScroll: true }); }
      } else if (ev.type === 'update') {
        S.boot.update = ev;
        if (ev.state === 'install') {
          S.updBusy = false;
          if (ev.result === 'perm') toast('Allow installs from News Crux, then tap Install again');
          else if (ev.result === 'none') toast('You already have the latest version');
          else if (ev.result !== 'ok') toast(String(ev.result || '').indexOf('Update failed') === 0 ? 'Update failed. Try again in Settings' : (ev.result || 'Update failed'));
        } else if (ev.newer && S.updToast !== ev.latest) { S.updToast = ev.latest; toast('News Crux ' + ev.latest + ' is out. Install it in Settings'); }
        else if (!ev.newer && S.pageOpen && !S.subPage) toast(ev.error ? 'Could not check for updates' : 'You have the latest version');
        if (S.pageOpen && !S.subPage && !S.setupOpen) openPage(settingsPage());
      } else if (ev.type === 'aitest') {
        S.aitest = (ev.lines || []).map(l => `<div class="tl ${/^✓/.test(l) ? 'ok' : /^✗/.test(l) ? 'bad' : ''}">${esc(l)}</div>`).join('');
        const box = $('#aitest'); if (box) box.innerHTML = S.aitest;
        toast(ev.ok ? 'Key works' : 'Key test failed — see details');
      } else if (ev.type === 'aicard') {
        if (ev.state === 'done') { delete S.cuBusy[ev.id]; delete S.cuErr[ev.id]; toast(ev.chk === 'web' ? 'Card written and checked on the web' : 'Card written and checked'); }
        else if (ev.state === 'error') { delete S.cuBusy[ev.id]; S.cuErr[ev.id] = ev.error || 'The card could not be written.'; }
        else if (ev.state === 'busy') { delete S.cuBusy[ev.id]; toast('Another card is being written; try again in a moment'); }
        if (ev.state !== 'start' && S.tab === 'edition' && S.date === ev.date && !S.pageOpen) { S.ed = null; render({ keepScroll: true }); }
      } else if (ev.type === 'ai') {
        if (ev.state === 'start') { S.aiBusy = ev.date; S.aiStage = 'draft'; if (S.tab === 'edition' && S.date === ev.date && !S.pageOpen) render({ keepScroll: true }); }
        else if (ev.state === 'stage') { S.aiStage = ev.stage; const box = $('#checked'); if (box && S.tab === 'edition' && S.date === ev.date && !S.pageOpen) render({ keepScroll: true }); }
        else {
          S.aiBusy = null; S.sheetCache = null; loadBoot();
          if (ev.state === 'done') { if (ev.cards) toast(ev.verified ? 'Cards rewritten and checked on the web' : ev.crossChecked ? 'Cards rewritten and checked' : 'Cards rewritten'); }
          else toast('AI pass failed. Details are under Checked.');
          S.ed = null; if (S.tab === 'edition' && S.date === ev.date) render({ keepScroll: true });
        }
      }
    },
    onResume() {
      loadBoot(); applyTheme();
      if (S.setupOpen) { const k = $('#setupkey'); const v = k ? k.value : ''; openPage(setupPage()); const k2 = $('#setupkey'); if (k2 && v) k2.value = v; return; }
      if (S.pageOpen && !S.subPage) { const b = $('#page .body'); const y = b ? b.scrollTop : 0; const k = $('#apikey'); const v = k ? k.value : ''; openPage(settingsPage()); const b2 = $('#page .body'); if (b2) b2.scrollTop = y; const k2 = $('#apikey'); if (k2 && v) k2.value = v; return; }
      if (!S.pageOpen && !S.sheetOpen) { S.ed = null; S.live = null; S.sheetCache = null; render({ keepScroll: true }); }
    },
    openTarget(t) {
      closeSheet(); closePage();
      if (t === 'update') { loadBoot(); openPage(settingsPage()); return; }
      if (t === 'live') { S.tab = 'live'; }
      else if (t.indexOf('edition:') === 0) { S.tab = 'edition'; S.date = t.slice(8); }
      else if (t.indexOf('ai:') === 0) { S.tab = 'edition'; S.date = t.slice(3); }
      loadBoot(); S.ed = null; renderTabs(); render();
    },
    back() {
      if (S.sheetOpen) { closeSheet(); return true; }
      if (S.pageOpen && S.subPage) { S.subPage = false; openPage(settingsPage()); return true; }
      if (S.pageOpen) { if (S.setupOpen) { N.setBool('setupDone', true); S.setupOpen = false; } closePage(); loadBoot(); applyTheme(); render({ keepScroll: true }); return true; }
      if (S.tab !== 'edition') { go('edition'); return true; }
      const def = defaultDate();
      if (S.date !== def) { S.date = def; S.ed = null; render(); return true; }
      return false;
    }
  };

  /* ---------------- init ---------------- */
  function init() {
    loadBoot();
    applyTheme();
    S.bookmarks = J(N.bookmarks(), []);
    loadReported();
    $('#btnSettings').innerHTML = ic('gear');
    $('#btnSettings').onclick = () => { S.setupOpen = false; openPage(settingsPage()); };
    $('#btnSync').onclick = doSync;
    $('#main').addEventListener('scroll', readBar, { passive: true });
    $('#scrim').onclick = closeSheet;
    document.addEventListener('click', onClick);
    document.addEventListener('change', onChange);
    document.addEventListener('input', onInput);
    if (window.matchMedia) { const mq = window.matchMedia('(prefers-color-scheme: light)'); if (mq.addEventListener) mq.addEventListener('change', applyTheme); }
    ptr();
    S.date = defaultDate();
    loadLive();
    renderTabs();
    render();
    if (!(S.boot.settings || {}).setupDone && window.NBNative) { S.setupOpen = true; openPage(setupPage()); }
    setInterval(setSyncBtn, 60000);
  }
  init();
})();
