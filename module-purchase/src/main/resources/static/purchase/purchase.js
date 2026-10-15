const apiRoot = '/purchase-api';
const yearSelect = document.getElementById('year-select');
const monthSelect = document.getElementById('month-select');
const categoryFilter = document.getElementById('category-filter');
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

function renderMonthlySummary(points) {
  const root = document.getElementById('monthly-summary-body');
  const uploaded = (points || []).filter((point) => point.uploaded);
  const totalPlan = uploaded.reduce((sum, point) => sum + Number(point.planAmount || 0), 0);
  const totalActual = uploaded.reduce((sum, point) => sum + Number(point.actualAmount || 0), 0);
  const totalRate = totalPlan ? totalActual / totalPlan * 100 : 0;
  const totalVariance = totalActual - totalPlan;
  document.getElementById('coverage-label').textContent = uploaded.length ? `${uploaded.length}/12개월 자료` : '자료 없음';
  root.innerHTML = (points || []).map((point) => {
    const variance = Number(point.varianceAmount || 0);
    return `<tr class="monthly-summary-row${point.uploaded ? '' : ' is-unuploaded'}" data-month="${point.month}" ${point.uploaded ? 'tabindex="0" role="button"' : ''}>
      <td>${point.month}월</td>
      <td class="number-cell">${point.uploaded ? money(point.planAmount) : '—'}</td>
      <td class="number-cell">${point.uploaded ? money(point.actualAmount) : '—'}</td>
      <td class="number-cell">${point.uploaded ? pct(point.achievementRate) : '—'}</td>
      <td class="number-cell ${variance > 0 ? 'text-positive' : variance < 0 ? 'text-negative' : ''}">${point.uploaded ? `${variance > 0 ? '+' : ''}${money(variance)}` : '—'}</td>
    </tr>`;
  }).join('') + `<tr class="annual-total-row"><th>누계</th><th class="number-cell">${money(totalPlan)}</th><th class="number-cell">${money(totalActual)}</th><th class="number-cell">${pct(totalRate)}</th><th class="number-cell ${totalVariance > 0 ? 'text-positive' : totalVariance < 0 ? 'text-negative' : ''}">${totalVariance > 0 ? '+' : ''}${money(totalVariance)}</th></tr>`;
  root.querySelectorAll('.monthly-summary-row:not(.is-unuploaded)').forEach((row) => {
    const selectMonth = () => { monthSelect.value = row.dataset.month; activePage = 0; loadDetails(); };
    row.addEventListener('click', selectMonth);
    row.addEventListener('keydown', (event) => { if (event.key === 'Enter' || event.key === ' ') selectMonth(); });
  });
}

function renderCompactGraph(points) {
  const root = document.getElementById('monthly-graph');
  const uploaded = (points || []).filter((point) => point.uploaded);
  if (!uploaded.length) {
    root.innerHTML = '<p class="empty-inline">그래프에 표시할 월별 자료가 없습니다.</p>';
    return;
  }
  const maxAmount = Math.max(1, ...uploaded.flatMap((point) => [Number(point.planAmount || 0), Number(point.actualAmount || 0)]));
  const left = 42, right = 468, top = 16, bottom = 142;
  const x = (month) => left + (right - left) * (month - 1) / 11;
  const y = (value) => bottom - Number(value || 0) / maxAmount * (bottom - top);
  const makePath = (key) => {
    let previousMonth = 0;
    return (points || []).map((point) => {
      if (!point.uploaded) { previousMonth = 0; return ''; }
      const command = previousMonth > 0 && previousMonth === point.month - 1 ? 'L' : 'M';
      previousMonth = point.month;
      return `${command} ${x(point.month)} ${y(point[key])}`;
    }).filter(Boolean).join(' ');
  };
  const grid = [0, 1, 2, 3].map((tick) => {
    const value = maxAmount * (1 - tick / 3);
    const yy = top + (bottom - top) * tick / 3;
    return `<line x1="${left}" x2="${right}" y1="${yy}" y2="${yy}" class="chart-gridline"/><text x="${left - 7}" y="${yy + 3}" text-anchor="end" class="axis-label">${money(value)}</text>`;
  }).join('');
  const labels = Array.from({ length: 12 }, (_, index) => `<text x="${x(index + 1)}" y="${bottom + 20}" text-anchor="middle" class="axis-label">${index + 1}월</text>`).join('');
  const pointsMarkup = uploaded.map((point) => `<a href="#detail-title" class="graph-month-point" data-month="${point.month}" aria-label="${point.month}월 상세 보기"><circle cx="${x(point.month)}" cy="${y(point.planAmount)}" r="3" class="trend-point plan-point"><title>${point.month}월 계획 ${money(point.planAmount)}백만원</title></circle><circle cx="${x(point.month)}" cy="${y(point.actualAmount)}" r="3" class="trend-point actual-point"><title>${point.month}월 실적 ${money(point.actualAmount)}백만원</title></circle></a>`).join('');
  root.innerHTML = `<svg viewBox="0 0 490 174" role="img" aria-label="월별 계획과 실적 꺾은선 그래프" preserveAspectRatio="none">
    ${grid}<path d="${makePath('planAmount')}" class="trend-line plan-trend"/><path d="${makePath('actualAmount')}" class="trend-line actual-trend"/>${pointsMarkup}${labels}</svg>`;
  root.querySelectorAll('[data-month]').forEach((point) => point.addEventListener('click', () => {
    monthSelect.value = point.dataset.month;
    activePage = 0;
    loadDetails();
  }));
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
      <div class="breakdown-track"><i class="breakdown-fill" style="width:${width}%"></i></div>
      <div class="breakdown-share">누계 실적의 ${pct(item.shareRate)}</div>
    </div>`;
  }).join('');
}

function setFilterOptions(items) {
  const current = categoryFilter.value;
  const names = [...new Set((items || []).map((item) => item.name))];
  categoryFilter.innerHTML = '<option value="">전체 구분</option>' + names.map((name) => `<option value="${escapeHtml(name)}">${escapeHtml(name)}</option>`).join('');
  if (names.includes(current)) categoryFilter.value = current;
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
    renderMonthlySummary(data.monthly || []);
    renderCompactGraph(data.monthly || []);
    renderBreakdown('business-breakdown', data.categoryTotals, '첫 번째 시트에 실적 구분이 없습니다.');
    setFilterOptions(data.categoryTotals || []);
    setMonthOptions(data.monthly || []);
    if (monthSelect.value) loadDetails();
    else renderEmptyDetails('해당 연도에 업로드된 월 자료가 없습니다.');
  } catch (error) {
    showNotice(error.message, 'error');
    renderEmptyDetails('대시보드 데이터를 불러오지 못했습니다.');
  }
}

function renderEmptyDetails(message) {
  document.getElementById('detail-body').innerHTML = `<tr><td colspan="8" class="empty-state">${escapeHtml(message)}</td></tr>`;
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
  if (categoryFilter.value) params.set('categoryName', categoryFilter.value);
  try {
    const page = await requestJson(`${apiRoot}/details?${params.toString()}`);
    const rows = page.content || [];
    if (!rows.length) return renderEmptyDetails('조건에 맞는 상세 내역이 없습니다.');
    document.getElementById('detail-body').innerHTML = rows.map((category) => `<tr>
      <td><span class="business-chip">${escapeHtml(category.categoryName)}</span></td>
      <td class="number-cell">${money(category.previousActualAmount)}</td>
      <td class="number-cell">${money(category.planAmount)}</td>
      <td class="number-cell">${money(category.actualAmount)}</td>
      <td class="number-cell ${Number(category.planVarianceAmount || 0) > 0 ? 'text-positive' : Number(category.planVarianceAmount || 0) < 0 ? 'text-negative' : ''}">${Number(category.planVarianceAmount || 0) > 0 ? '+' : ''}${money(category.planVarianceAmount)}</td>
      <td class="number-cell ${Number(category.previousMonthVarianceAmount || 0) > 0 ? 'text-positive' : Number(category.previousMonthVarianceAmount || 0) < 0 ? 'text-negative' : ''}">${Number(category.previousMonthVarianceAmount || 0) > 0 ? '+' : ''}${money(category.previousMonthVarianceAmount)}</td>
      <td class="note-cell" title="${escapeHtml(category.planVarianceReason || '')}">${escapeHtml(category.planVarianceReason || '—')}</td>
      <td class="note-cell" title="${escapeHtml(category.previousMonthVarianceReason || '')}">${escapeHtml(category.previousMonthVarianceReason || '—')}</td>
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
    showNotice(`${result.purchaseYear}년 ${result.purchaseMonth}월 자료를 반영했습니다. 첫 시트 요약 ${result.summaryRowCount}건`, 'success');
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
