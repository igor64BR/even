/* Rateio — camada de dados mockada do protótipo.
   Tudo em localStorage; nenhuma chamada de rede. computeSettlement() é um
   algoritmo guloso real (não é decoração), só que simplificado pra fins de
   demonstração — a versão de produção é especificada em specs/001-mvp-expense-splitting. */

const STORAGE_KEY = 'rateio:v1';
const THEME_KEY = 'rateio:theme';

/**
 * Botão de tema (sol/lua) no `.topbar` de toda tela, lado oposto ao título — componente global,
 * não uma feature de uma tela só. O ícone mostrado é sempre o do modo PRA ONDE o toque leva (sol
 * visível = "toque pra clarear", lua visível = "toque pra escurecer"), nunca o modo atual.
 */
const THEME_ICONS = {
  sun: '<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="4"/><path d="M12 2v2"/><path d="M12 20v2"/><path d="m4.93 4.93 1.41 1.41"/><path d="m17.66 17.66 1.41 1.41"/><path d="M2 12h2"/><path d="M20 12h2"/><path d="m6.34 17.66-1.41 1.41"/><path d="m19.07 4.93-1.41 1.41"/></svg>',
  moon: '<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"/></svg>',
};

function initThemeToggle() {
  const btn = document.getElementById('theme-toggle');
  if (!btn) return;
  const render = () => {
    const dark = document.documentElement.dataset.theme === 'dark';
    btn.innerHTML = dark ? THEME_ICONS.sun : THEME_ICONS.moon;
    const label = dark ? 'Mudar para tema claro' : 'Mudar para tema escuro';
    btn.setAttribute('aria-label', label);
    btn.title = label;
  };
  render();
  btn.addEventListener('click', () => {
    const dark = document.documentElement.dataset.theme === 'dark';
    document.documentElement.dataset.theme = dark ? 'light' : 'dark';
    localStorage.setItem(THEME_KEY, dark ? 'light' : 'dark');
    render();
  });
}
document.addEventListener('DOMContentLoaded', initThemeToggle);

function seed() {
  return {
    user: null,
    groups: [
      {
        id: 'churras',
        nome: 'Churras de sábado',
        categoria: 'Churrasco',
        synced: false,
        participantes: [
          { id: 'voce', nome: 'Você', isYou: true },
          { id: 'marina', nome: 'Marina' },
          { id: 'theo', nome: 'Théo' },
          { id: 'bia', nome: 'Bia' },
        ],
        despesas: [
          { id: 'e1', descricao: 'Carne', valor: 180, pagadorId: 'voce', data: '2026-09-13', tipo: 'igual', divisao: [ { p: 'voce', v: 45 }, { p: 'marina', v: 45 }, { p: 'theo', v: 45 }, { p: 'bia', v: 45 } ] },
          { id: 'e2', descricao: 'Bebidas', valor: 96, pagadorId: 'marina', data: '2026-09-13', tipo: 'igual', divisao: [ { p: 'voce', v: 24 }, { p: 'marina', v: 24 }, { p: 'theo', v: 24 }, { p: 'bia', v: 24 } ] },
          { id: 'e3', descricao: 'Carvão e gelo', valor: 40, pagadorId: 'theo', data: '2026-09-14', tipo: 'igual', divisao: [ { p: 'voce', v: 10 }, { p: 'marina', v: 10 }, { p: 'theo', v: 10 }, { p: 'bia', v: 10 } ] },
        ],
        quitacoes: [],
      },
      {
        id: 'praia',
        nome: 'Viagem pra praia',
        categoria: 'Viagem',
        synced: true,
        participantes: [
          { id: 'voce', nome: 'Você', isYou: true },
          { id: 'carlos', nome: 'Carlos', autenticado: true },
          { id: 'duda', nome: 'Duda' },
        ],
        despesas: [
          { id: 'e4', descricao: 'Hospedagem', valor: 900, pagadorId: 'voce', data: '2026-09-05', tipo: 'igual', divisao: [ { p: 'voce', v: 300 }, { p: 'carlos', v: 300 }, { p: 'duda', v: 300 } ] },
          { id: 'e5', descricao: 'Mercado da semana', valor: 150, pagadorId: 'duda', data: '2026-09-06', tipo: 'percentual', divisao: [ { p: 'voce', v: 60 }, { p: 'carlos', v: 45 }, { p: 'duda', v: 45 } ] },
        ],
        quitacoes: [
          { id: 'q1', deId: 'carlos', paraId: 'voce', valor: 100, data: '2026-09-10' },
        ],
      },
      {
        id: 'republica',
        nome: 'República — contas de outubro',
        categoria: 'República',
        synced: false,
        participantes: [
          { id: 'voce', nome: 'Você', isYou: true },
          { id: 'pedro', nome: 'Pedro' },
        ],
        despesas: [],
        quitacoes: [],
      },
    ],
    notificacoes: [
      { id: 'n1', texto: 'Carlos quitou R$ 100,00 com você em "Viagem pra praia".', data: '2026-09-10T18:22:00', lida: true },
      { id: 'n2', texto: 'Duda lançou "Mercado da semana" — R$ 150,00 — em "Viagem pra praia".', data: '2026-09-06T12:05:00', lida: true },
    ],
  };
}

function getState() {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) {
    const initial = seed();
    localStorage.setItem(STORAGE_KEY, JSON.stringify(initial));
    return initial;
  }
  try { return JSON.parse(raw); } catch (e) { const initial = seed(); localStorage.setItem(STORAGE_KEY, JSON.stringify(initial)); return initial; }
}

function saveState(state) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function resetState() {
  localStorage.removeItem(STORAGE_KEY);
}

function fmtMoney(v) {
  const n = Number(v) || 0;
  return 'R$ ' + n.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function fmtDate(iso) {
  const d = new Date(iso);
  return d.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' });
}

function fmtRelative(iso) {
  const diffMs = Date.now() - new Date(iso).getTime();
  const days = Math.floor(diffMs / 86400000);
  if (days <= 0) return 'hoje';
  if (days === 1) return 'ontem';
  if (days < 30) return `há ${days} dias`;
  const months = Math.floor(days / 30);
  return `há ${months} ${months === 1 ? 'mês' : 'meses'}`;
}

const FA_CLOUD_PATH = 'M0 336c0 79.5 64.5 144 144 144l368 0c70.7 0 128-57.3 128-128c0-61.9-44-113.6-102.4-125.4c4.1-10.7 6.4-22.4 6.4-34.6c0-53-43-96-96-96c-19.7 0-38.1 6-53.3 16.2C367 64.2 315.3 32 256 32C167.6 32 96 103.6 96 192c0 2.7 .1 5.4 .2 8.1C40.2 219.8 0 273.2 0 336z';

// Ícones fa-cloud / fa-cloud-slash (Font Awesome Free, CC BY 4.0). A versão
// "slash" não existe no pacote free — reconstruída em traço (outline) a
// partir do mesmo path da nuvem, cortado por uma linha diagonal; a versão
// sincronizada fica sólida (filled) pra diferenciar as duas de relance.
function syncIcon(synced) {
  const title = synced ? 'Sincronizado' : 'Local — só neste aparelho';
  const body = synced
    ? `<path d="${FA_CLOUD_PATH}" fill="currentColor"/>`
    : `<path d="${FA_CLOUD_PATH}" fill="none" stroke="currentColor" stroke-width="44" stroke-linejoin="round"/>
       <line x1="60" y1="452" x2="580" y2="60" stroke="currentColor" stroke-width="44" stroke-linecap="round"/>`;
  return `<span class="sync-icon" title="${title}"><svg viewBox="0 0 640 512">${body}</svg></span>`;
}

function initials(nome) {
  return nome.split(' ').filter(Boolean).slice(0, 2).map(p => p[0]).join('').toUpperCase();
}

function computeBalances(group) {
  const saldos = {};
  group.participantes.forEach(p => saldos[p.id] = 0);
  group.despesas.forEach(d => {
    saldos[d.pagadorId] = (saldos[d.pagadorId] || 0) + d.valor;
    d.divisao.forEach(item => { saldos[item.p] = (saldos[item.p] || 0) - item.v; });
  });
  group.quitacoes.forEach(q => {
    saldos[q.deId] = (saldos[q.deId] || 0) + q.valor;
    saldos[q.paraId] = (saldos[q.paraId] || 0) - q.valor;
  });
  return saldos;
}

// Algoritmo guloso de simplificação de dívidas: casa o maior devedor com o
// maior credor repetidamente até zerar os saldos. Minimiza o número de
// transações na prática, embora não seja garantidamente ótimo em todo caso.
function computeSettlement(saldos) {
  const EPS = 0.01;
  const devedores = [];
  const credores = [];
  Object.entries(saldos).forEach(([id, v]) => {
    if (v < -EPS) devedores.push({ id, v: -v });
    else if (v > EPS) credores.push({ id, v });
  });
  devedores.sort((a, b) => b.v - a.v);
  credores.sort((a, b) => b.v - a.v);

  const transacoes = [];
  let i = 0, j = 0;
  while (i < devedores.length && j < credores.length) {
    const d = devedores[i], c = credores[j];
    const valor = Math.min(d.v, c.v);
    transacoes.push({ de: d.id, para: c.id, valor });
    d.v -= valor; c.v -= valor;
    if (d.v < EPS) i++;
    if (c.v < EPS) j++;
  }
  return transacoes;
}

function participantName(group, id) {
  const p = group.participantes.find(p => p.id === id);
  return p ? p.nome : id;
}

function notify(state, texto) {
  state.notificacoes.unshift({ id: 'n' + Date.now(), texto, data: new Date().toISOString(), lida: false });
}

function unreadCount(state) {
  return state.notificacoes.filter(n => !n.lida).length;
}

// ---- helpers de UI compartilhados entre telas ----

function showOverlay(text) {
  const el = document.getElementById('overlay');
  if (!el) return;
  el.querySelector('span').textContent = text || 'Carregando…';
  el.classList.add('show');
}
function hideOverlay() {
  const el = document.getElementById('overlay');
  if (el) el.classList.remove('show');
}
function showToast(text) {
  const el = document.getElementById('toast');
  if (!el) return;
  el.querySelector('.msg').textContent = text;
  el.classList.add('show');
  setTimeout(() => el.classList.remove('show'), 2200);
}

function renderHeaderAuth(state, active) {
  const slot = document.getElementById('auth-slot');
  if (!slot) return;
  const inBar = slot.closest('.bottombar');
  const tabCls = 'tab' + (active ? ' active' : '');
  if (state.user) {
    slot.innerHTML = inBar
      ? `<a class="${tabCls}" href="login.html"><span class="avatar" style="width:22px;height:22px;font-size:10px">${initials(state.user.nome)}</span>Perfil</a>`
      : `<a class="avatar" href="login.html" title="${state.user.nome}">${initials(state.user.nome)}</a>`;
  } else {
    const icon = '<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="8" r="3.4"/><path d="M5 20c1.4-3.8 4.4-5.6 7-5.6s5.6 1.8 7 5.6"/></svg>';
    slot.innerHTML = inBar
      ? `<a class="${tabCls}" href="login.html">${icon}Entrar</a>`
      : `<a class="icon-btn" href="login.html" title="Entrar">${icon}</a>`;
  }
}

function renderNotifBell(state, active) {
  const slot = document.getElementById('bell-slot');
  if (!slot) return;
  const inBar = slot.closest('.bottombar');
  const tabCls = 'tab' + (active ? ' active' : '');
  const n = unreadCount(state);
  const icon = '<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M6 9a6 6 0 1 1 12 0c0 4 1.5 5.5 1.5 5.5H4.5S6 13 6 9Z"/><path d="M9.5 17a2.5 2.5 0 0 0 5 0"/></svg>';
  const dot = n > 0 ? '<span class="badge-dot"></span>' : '';
  slot.innerHTML = inBar
    ? `<a class="${tabCls}" href="notificacoes.html">${icon}${dot}Avisos</a>`
    : `<a class="icon-btn" href="notificacoes.html" title="Notificações">${icon}${dot}</a>`;
}

function statusbarNow() {
  const el = document.querySelector('.statusbar .time');
  if (el) el.textContent = new Date().toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}

document.addEventListener('DOMContentLoaded', statusbarNow);
