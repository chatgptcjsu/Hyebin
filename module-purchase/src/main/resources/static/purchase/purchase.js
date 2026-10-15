const apiRoot = '/purchase-api';
const yearSelect = document.getElementById('year-select');
const monthSelect = document.getElementById('month-select');
const businessFilter = document.getElementById('business-filter');
const supplierFilter = document.getElementById('supplier-filter');
const notice = document.getElementById('notice');
let activePage = 0;

const money = (value) => new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 1 }).format(Number(value || 0));
const pct = (value) => `${Number(value || 0).toFixed(1)}%`;
const escapeHtml = (value) => String(value ?? '').replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char]);

function showNotice(message, kind = 'info') {
  notice.hidden = !message;
  notice.textContent = message || '';
  notice.className = `notice ${kind}`;
}

async function requestJson(url, options = {}) {
  const response = await fetch(url, { credentials: 'same-origin', ...options });
  let body;
  try {
    body = await response.json();
  } catch {
    throw new Error(`서버 응답을 읽을 수 없습니다. (HTTP ${response.status})`);
  }
  if (!response.ok) {
    throw new Error(body?.message || body?.error?.message || `요청에 실패했습니다. (HTTP ${response.status})`);
  }
  return body && Object.prototype.hasOwnProperty.call(body, 'data') ? body.data : body;
}

function setKpis(data) {
  document.getElementById('kpi-plan').textContent = money(data.totalPlanAmount);
  document.getElementById('kpi-actual').textContent = money(data.totalActualAmount);
  document.getElementById('kpi-rate').textContent = pct(data.achievementRate);
  const variance = Number(data.totalVarianceAmount || 0);
  const varianceElement = document.getElementById('kpi-variance');
  varianceElement.textContent = `${variance > 0 ? '+' : ''}${money(variance)}`;
  varianceElement.classList.toggle('is-positive', variance > 0);
  varianceElement.classList.toggle('is-negative', variance < 0);
  const uploadedCount = (data.monthly || []).filter((point) => point.uploaded).length;
  document.getElementById('kpi-actual-note').textContent = `백만원 · VAT 별도 · ${uploadedCount}/12개월 반영`;
  document.getElementById('coverage-label').textContent = uploadedCount ? `${uploadedCount}개월 데이터 반영` : '업로드된 자료가 없습니다';
}

function renderMonthly(points) {
  const root = document.getElementById('monthly-chart');
  const maxValue = Math.max(1, ...points.flatMap((point) => [Number(point.planAmount || 0), Number(point.actualAmount || 0)]));
  root.innerHTML = points.map((point) => {
    const plan = Number(point.planAmount || 0);
    const actual = Number(point.actualAmount || 0);
    const planHeight = Math.max(plan > 0 ? 2 : 0, plan / maxValue * 100);
    const actualHeight = Math.max(actual > 0 ? 2 : 0, actual / maxValue * 100);
    const classes = `month-column${point.uploaded ? '' : ' is-empty'}`;
    return `<button class="${classes}" type="button" data-month="${point.month}" ${point.uploaded ? '' : 'disabled'} aria-label="${point.month}월 계획 ${money(plan)}백만원, 실적 ${money(actual)}백만원">
      <span class="bar-area"><i class="bar plan-bar" style="height:${planHeight}%" title="계획 ${money(plan)}"></i><i class="bar actual-bar" style="height:${actualHeight}%" title="실적 ${money(actual)}"></i></span>
      <span class="month-label">${String(point.month).padStart(2, '0')}월</span>
      ${point.uploaded ? `<span class="month-rate">${pct(point.achievementRate)}</span>` : '<span class="month-rate muted">미등록</span>'}
    </button>`;
  }).join('');
  root.querySelectorAll('[data-month]').forEach((button) => button.addEventListener('click', () => {
    monthSelect.value = button.dataset.month;
    activePage = 0;
    loadDetails();
  }));
}

function renderCumulative(points) {
  const root = document.getElementById('cumulative-chart');
  const cumulative = [];
  let planSum = 0;
  let actualSum = 0;
  points.forEach((point) => {
    planSum += Number(point.planAmount || 0);
    actualSum += Number(point.actualAmount || 0);
    cumulative.push({ month: point.month, plan: planSum, actual: actualSum });
  });
  const maxValue = Math.max(1, ...cumulative.flatMap((point) => [point.plan, point.actual]));
  const left = 44, right = 700, top = 18, bottom = 184;
  const x = (index) => left + (right - left) * index / 11;
  const y = (value) => bottom - (value / maxValue) * (bottom - top);
  const planPath = cumulative.map((point, index) => `${index ? 'L' : 'M'} ${x(index)} ${y(point.plan)}`).join(' ');
  const actualPath = cumulative.map((point, index) => `${index ? 'L' : 'M'} ${x(index)} ${y(point.actual)}`).join(' ');
  const grid = [0, 1, 2, 3].map((tick) => {
    const yy = top + (bottom - top) * tick / 3;
    return `<line x1="${left}" x2="${right}" y1="${yy}" y2="${yy}" class="chart-gridline"/><text x="${left - 8}" y="${yy + 4}" text-anchor="end" class="axis-label">${money(maxValue * (1 - tick / 3))}</text>`;
  }).join('');
  const labels = cumulative.map((point, index) => `<text x="${x(index)}" y="${bottom + 22}" text-anchor="middle" class="axis-label">${point.month}월</text>`).join('');
  root.innerHTML = `<svg viewBox="0 0 720 224" role="img" aria-label="누계 계획과 실적 선그래프" preserveAspectRatio="none">
    ${grid}<path d="${planPath}" class="trend-line plan-trend"/><path d="${actualPath}" class="trend-line actual-trend"/>
    ${cumulative.map((point, index) => `<circle cx="${x(index)}" cy="${y(point.plan)}" r="2.5" class="trend-point plan-point"><title>${point.month}월 누계 계획 ${money(point.plan)}백만원</title></circle><circle cx="${x(index)}" cy="${y(point.actual)}" r="2.5" class="trend-point actual-point"><title>${point.month}월 누계 실적 ${money(point.actual)}백만원</title></circle>`).join('')}
    ${labels}</svg>`;
}

function renderBreakdown(rootId, items, emptyText) {
  const root = document.getElementById(rootId);
  if (!items?.length) {
    root.innerHTML = `<p class="empty-inline">${escapeHtml(emptyText)}</p>`;
    return;
  }
  const maxAmount = Math.max(1, ...items.map((item) => Number(item.amount || 0)));
  root.innerHTML = items.map((item, index) => {
    const width = Math.max(Number(item.amount) > 0 ? 2 : 0, Number(item.amount || 0) / maxAmount * 100);
    return `<div class="breakdown-item">
      <div class="breakdown-label"><span class="rank">${String(index + 1).padStart(2, '0')}</span><strong title="${escapeHtml(item.name)}">${escapeHtml(item.name)}</strong><span class="breakdown-amount">${money(item.amount)}</span></div>
      <div class="breakdown-track"><i class="breakdown-fill ${rootId === 'supplier-breakdown' ? 'supplier-fill' : ''}" style="width:${width}%"></i></div>
      <div class="breakdown-share">누계 실적의 ${pct(item.shareRate)}</div>
    </div>`;
  }).join('');
}

function setFilterOptions(items) {
  const current = businessFilter.value;
  const names = [...new Set((items || []).map((item) => item.name))];
  businessFilter.innerHTML = '<option value="">전체 사업부</option>' + names.map((name) => `<option value="${escapeHtml(name)}">${escapeHtml(name)}</option>`).join('');
  if (names.includes(current)) businessFilter.value = current;
}

function setMonthOptions(points) {
  const current = monthSelect.value;
  const latest = [...(points || [])].reverse().find((point) => point.uploaded)?.month;
  monthSelect.innerHTML = '<option value="">월 선택</option>' + (points || []).map((point) => `<option value="${point.month}" ${point.uploaded ? '' : 'disabled'}>${point.month}월${point.uploaded ? '' : ' · 자료 없음'}</option>`).join('');
  if (points?.some((point) => String(point.month) === current && point.uploaded)) monthSelect.value = current;
  else if (latest) monthSelect.value = String(latest);
  else monthSelect.value = '';
}

async function loadDashboard() {
  const year = yearSelect.value;
  try {
    showNotice('', 'info');
    const data = await requestJson(`${apiRoot}/dashboard?year=${encodeURIComponent(year)}`);
    setKpis(data);
    renderMonthly(data.monthly || []);
    renderCumulative(data.monthly || []);
    renderBreakdown('business-breakdown', data.businessUnits, '사업부 실적 자료가 없습니다.');
    renderBreakdown('supplier-breakdown', data.topSuppliers, '협력업체 자료가 없습니다.');
    setFilterOptions(data.businessUnits || []);
    setMonthOptions(data.monthly || []);
    if (monthSelect.value) loadDetails();
    else renderEmptyDetails('해당 연도에 업로드된 월 자료가 없습니다.');
    loadHistory();
  } catch (error) {
    showNotice(error.message, 'error');
    renderEmptyDetails('대시보드 데이터를 불러오지 못했습니다.');
  }
}

async function loadHistory() {
  const root = document.getElementById('history-body');
  try {
    const rows = await requestJson(`${apiRoot}/uploads?year=${encodeURIComponent(yearSelect.value)}`);
    if (!rows?.length) {
      root.innerHTML = '<tr><td colspan="7" class="empty-state">해당 연도의 업로드 이력이 없습니다.</td></tr>';
      return;
    }
    root.innerHTML = rows.map((item) => {
      const statusLabel = item.uploadStatus === 'CURRENT' ? '현재 반영' : '교체됨';
      const statusClass = item.uploadStatus === 'CURRENT' ? 'history-status current' : 'history-status replaced';
      const uploadedAt = item.createdAt ? new Date(item.createdAt).toLocaleString('ko-KR', { dateStyle: 'short', timeStyle: 'short' }) : '—';
      return `<tr><td>${item.purchaseYear}년 ${String(item.purchaseMonth).padStart(2, '0')}월</td><td><span class="${statusClass}">${statusLabel}</span></td>
        <td class="history-filename" title="${escapeHtml(item.originalFilename)}">${escapeHtml(item.originalFilename)}</td>
        <td class="number-cell">${Number(item.detailRowCount || 0)}</td><td class="number-cell">${money(item.planAmount)}</td>
        <td class="number-cell">${money(item.actualAmount)}</td><td>${escapeHtml(uploadedAt)}</td></tr>`;
    }).join('');
  } catch (error) {
    root.innerHTML = `<tr><td colspan="7" class="empty-state">이력을 불러오지 못했습니다: ${escapeHtml(error.message)}</td></tr>`;
  }
}

function renderEmptyDetails(message) {
  document.getElementById('detail-body').innerHTML = `<tr><td colspan="7" class="empty-state">${escapeHtml(message)}</td></tr>`;
  document.getElementById('detail-pagination').textContent = '';
}

async function loadDetails() {
  const month = monthSelect.value;
  if (!month) return renderEmptyDetails('조회할 월을 선택해 주세요.');
  const params = new URLSearchParams({
    year: yearSelect.value,
    month,
    page: String(activePage),
    size: '50'
  });
  if (businessFilter.value) params.set('businessUnit', businessFilter.value);
  if (supplierFilter.value.trim()) params.set('supplierName', supplierFilter.value.trim());
  try {
    const page = await requestJson(`${apiRoot}/details?${params.toString()}`);
    const rows = page.content || [];
    if (!rows.length) return renderEmptyDetails('조건에 맞는 상세 내역이 없습니다.');
    document.getElementById('detail-body').innerHTML = rows.map((line) => `<tr>
      <td><span class="business-chip">${escapeHtml(line.businessUnit || '미분류')}</span></td>
      <td>${escapeHtml(line.importType || '—')}</td>
      <td class="supplier-cell">${escapeHtml(line.supplierName)}</td>
      <td class="number-cell">${money(line.planAmount)}</td>
      <td class="number-cell">${money(line.actualAmount)}</td>
      <td class="number-cell ${Number(line.varianceAmount || 0) > 0 ? 'text-positive' : Number(line.varianceAmount || 0) < 0 ? 'text-negative' : ''}">${Number(line.varianceAmount || 0) > 0 ? '+' : ''}${money(line.varianceAmount)}</td>
      <td class="note-cell" title="${escapeHtml(line.noteText || '')}">${escapeHtml(line.noteText || '—')}</td>
    </tr>`).join('');
    const pageNumber = Number(page.page || 0);
    const pageCount = Number(page.totalPages || 0);
    document.getElementById('detail-pagination').innerHTML = `<span>${Number(page.totalElements || 0)}건 · ${pageCount ? `${pageNumber + 1} / ${pageCount}페이지` : '0페이지'}</span>
      <div><button class="pagination-button" id="prev-page" type="button" ${pageNumber <= 0 ? 'disabled' : ''}>이전</button><button class="pagination-button" id="next-page" type="button" ${pageNumber + 1 >= pageCount ? 'disabled' : ''}>다음</button></div>`;
    document.getElementById('prev-page').addEventListener('click', () => { activePage = Math.max(0, activePage - 1); loadDetails(); });
    document.getElementById('next-page').addEventListener('click', () => { activePage += 1; loadDetails(); });
  } catch (error) {
    showNotice(error.message, 'error');
    renderEmptyDetails('상세 내역을 불러오지 못했습니다.');
  }
}

async function uploadFile(file, confirmReplace = false) {
  const form = new FormData();
  form.append('file', file);
  const params = new URLSearchParams();
  if (confirmReplace) params.set('confirmReplace', 'true');
  const query = params.toString();
  const result = await requestJson(`${apiRoot}/uploads${query ? `?${query}` : ''}`, { method: 'POST', body: form });
  if (result.requiresReplacementConfirmation && !confirmReplace) {
    const accepted = window.confirm(`${result.purchaseYear}년 ${result.purchaseMonth}월 자료가 이미 있습니다.\n기존에 반영된 자료를 교체하고 업로드 이력을 보존할까요?`);
    if (!accepted) {
      showNotice('기존 자료는 변경하지 않았습니다.', 'info');
      return;
    }
    return uploadFile(file, true);
  }
  if (result.alreadyCurrent) {
    showNotice(`${result.purchaseYear}년 ${result.purchaseMonth}월 동일한 파일이 이미 반영되어 있습니다.`, 'info');
  } else {
    showNotice(`${result.purchaseYear}년 ${result.purchaseMonth}월 자료를 반영했습니다. 상세 ${result.detailRowCount}건`, 'success');
  }
  yearSelect.value = String(result.purchaseYear);
  await loadDashboard();
}

function initializeControls() {
  const nowYear = new Date().getFullYear();
  for (let year = Math.max(2020, nowYear - 5); year <= nowYear + 1; year += 1) {
    const option = document.createElement('option');
    option.value = String(year);
    option.textContent = `${year}년`;
    yearSelect.append(option);
  }
  yearSelect.value = String(nowYear);
  for (let month = 1; month <= 12; month += 1) {
    const option = document.createElement('option');
    option.value = String(month);
    option.textContent = `${month}월`;
    monthSelect.append(option);
  }
  yearSelect.addEventListener('change', loadDashboard);
  document.getElementById('search-button').addEventListener('click', () => { activePage = 0; loadDetails(); });
  supplierFilter.addEventListener('keydown', (event) => {
    if (event.key === 'Enter') { activePage = 0; loadDetails(); }
  });
  document.getElementById('upload-file').addEventListener('change', async (event) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    if (!/\.xlsx?$/i.test(file.name)) return showNotice('Excel 파일(.xlsx 또는 .xls)만 업로드할 수 있습니다.', 'error');
    if (file.size > 20 * 1024 * 1024) return showNotice('파일 크기는 20MB 이하만 업로드할 수 있습니다.', 'error');
    try {
      showNotice('파일을 검증하고 있습니다. 잠시만 기다려 주세요.', 'info');
      await uploadFile(file);
    } catch (error) {
      showNotice(`업로드 실패: ${error.message}`, 'error');
    }
  });
}

initializeControls();
loadDashboard();
