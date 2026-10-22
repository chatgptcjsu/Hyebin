const apiRoot = '/purchase-api';
const yearSelect = document.getElementById('year-select');
const notice = document.getElementById('notice');

const wholeNumberFormat = new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 0 });
const roundToWhole = (value) => {
  const number = Number(value || 0);
  return Number.isFinite(number) ? Math.sign(number) * Math.floor(Math.abs(number) + 0.5) : 0;
};
const money = (value) => wholeNumberFormat.format(roundToWhole(value));
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

function openDetailSheet(month) {
  const query = new URLSearchParams({ year: yearSelect.value, month: String(month) });
  const windowName = `purchase-detail-${yearSelect.value}-${month}`;
  const detailWindow = window.open(
    `/purchase/detail-sheet.html?${query.toString()}`,
    windowName,
    'popup=yes,width=1600,height=1000,resizable=yes,scrollbars=yes'
  );
  if (!detailWindow) showNotice('상세 창이 차단되었습니다. 브라우저에서 팝업을 허용해 주세요.', 'error');
}

function renderMonthlySummary(points, totals = {}) {
  const root = document.getElementById('monthly-summary-body');
  const uploaded = (points || []).filter((point) => point.uploaded);
  const totalPlan = Number(totals.totalPlanAmount ?? uploaded.reduce((sum, point) => sum + Number(point.planAmount || 0), 0));
  const totalActual = Number(totals.totalActualAmount ?? uploaded.reduce((sum, point) => sum + Number(point.actualAmount || 0), 0));
  const totalPlanVariance = Number(totals.totalVarianceAmount ?? (totalActual - totalPlan));
  const totalPreviousVariance = Number(totals.totalPreviousMonthVarianceAmount ?? (points || []).reduce((sum, point) => sum + Number(point.previousMonthVarianceAmount || 0), 0));
  document.getElementById('coverage-label').textContent = uploaded.length ? `${uploaded.length}/12개월 자료` : '자료 없음';
  root.innerHTML = (points || []).map((point) => {
    const planVariance = roundToWhole(point.varianceAmount);
    const previousVariance = roundToWhole(point.previousMonthVarianceAmount);
    return `<tr class="monthly-summary-row${point.uploaded ? '' : ' is-unuploaded'}" data-month="${point.month}" ${point.uploaded ? `tabindex="0" role="link" aria-label="${yearSelect.value}년 ${point.month}월 상세 시트를 새 창으로 엽니다" title="클릭하면 업체별 상세 시트를 새 창으로 엽니다"` : ''}>
      <td>${point.month}월</td>
      <td class="number-cell">${point.uploaded ? money(point.planAmount) : '—'}</td>
      <td class="number-cell">${point.uploaded ? money(point.actualAmount) : '—'}</td>
      <td class="number-cell ${planVariance > 0 ? 'text-positive' : planVariance < 0 ? 'text-negative' : ''}">${point.uploaded ? `${planVariance > 0 ? '+' : ''}${money(planVariance)}` : '—'}</td>
      <td class="number-cell ${previousVariance > 0 ? 'text-positive' : previousVariance < 0 ? 'text-negative' : ''}">${point.uploaded ? `${previousVariance > 0 ? '+' : ''}${money(previousVariance)}` : '—'}</td>
    </tr>`;
  }).join('') + `<tr class="annual-total-row"><th>누계</th><th class="number-cell">${money(totalPlan)}</th><th class="number-cell">${money(totalActual)}</th><th class="number-cell ${roundToWhole(totalPlanVariance) > 0 ? 'text-positive' : roundToWhole(totalPlanVariance) < 0 ? 'text-negative' : ''}">${roundToWhole(totalPlanVariance) > 0 ? '+' : ''}${money(totalPlanVariance)}</th><th class="number-cell ${roundToWhole(totalPreviousVariance) > 0 ? 'text-positive' : roundToWhole(totalPreviousVariance) < 0 ? 'text-negative' : ''}">${roundToWhole(totalPreviousVariance) > 0 ? '+' : ''}${money(totalPreviousVariance)}</th></tr>`;
  root.querySelectorAll('.monthly-summary-row:not(.is-unuploaded)').forEach((row) => {
    row.addEventListener('click', () => openDetailSheet(row.dataset.month));
    row.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        openDetailSheet(row.dataset.month);
      }
    });
  });
}

function renderMonthlyGraph(points) {
  const root = document.getElementById('monthly-graph');
  const uploaded = (points || []).filter((point) => point.uploaded);
  if (!uploaded.length) {
    root.innerHTML = '<p class="empty-inline">그래프에 표시할 월별 자료가 없습니다.</p>';
    return;
  }
  const maxAmount = Math.max(1, ...uploaded.flatMap((point) => [Number(point.planAmount || 0), Number(point.actualAmount || 0)]));
  const left = 58, right = 950, top = 18, bottom = 270;
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
  const grid = [0, 1, 2, 3, 4].map((tick) => {
    const value = maxAmount * (1 - tick / 4);
    const yy = top + (bottom - top) * tick / 4;
    return `<line x1="${left}" x2="${right}" y1="${yy}" y2="${yy}" class="chart-gridline"/><text x="${left - 9}" y="${yy + 4}" text-anchor="end" class="axis-label">${money(value)}</text>`;
  }).join('');
  const labels = Array.from({ length: 12 }, (_, index) => `<text x="${x(index + 1)}" y="${bottom + 25}" text-anchor="middle" class="axis-label">${index + 1}월</text>`).join('');
  const markers = uploaded.map((point) => `<g class="graph-month-point" data-month="${point.month}" tabindex="0" role="button" aria-label="${yearSelect.value}년 ${point.month}월 상세 시트를 새 창으로 엽니다"><circle cx="${x(point.month)}" cy="${y(point.planAmount)}" r="5" class="trend-point plan-point"><title>${point.month}월 계획 ${money(point.planAmount)}백만원</title></circle><circle cx="${x(point.month)}" cy="${y(point.actualAmount)}" r="5" class="trend-point actual-point"><title>${point.month}월 실적 ${money(point.actualAmount)}백만원</title></circle></g>`).join('');
  root.innerHTML = `<svg viewBox="0 0 1000 320" role="img" aria-label="월별 계획과 실적 꺾은선 그래프" preserveAspectRatio="none">
    ${grid}<path d="${makePath('planAmount')}" class="trend-line plan-trend"/><path d="${makePath('actualAmount')}" class="trend-line actual-trend"/>${markers}${labels}</svg>`;
  root.querySelectorAll('.graph-month-point').forEach((point) => {
    point.addEventListener('click', () => openDetailSheet(point.dataset.month));
    point.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        openDetailSheet(point.dataset.month);
      }
    });
  });
}

function renderMonthlyReasons(reasons, year) {
  document.getElementById('reason-year').textContent = String(year);
  const root = document.getElementById('reason-body');
  const rows = reasons || [];
  if (!rows.length) {
    root.innerHTML = '<tr><td colspan="8" class="empty-state">해당 연도에 업로드된 월 자료가 없습니다.</td></tr>';
    return;
  }
  root.innerHTML = rows.map((item) => {
    const planVariance = roundToWhole(item.planVarianceAmount);
    const previousVariance = roundToWhole(item.previousMonthVarianceAmount);
    const month = Number(item.month);
    if (!item.uploaded) {
      return `<tr class="reason-unuploaded" data-month="${month}"><td class="reason-month">${month}월</td><td>미업로드</td><td class="number-cell">—</td><td class="number-cell">—</td><td class="number-cell">—</td><td class="number-cell">—</td><td class="reason-text">—</td><td class="reason-text">—</td></tr>`;
    }
    return `<tr data-month="${month}">
      <td class="reason-month">${month}월</td><td>${escapeHtml(item.categoryName || '—')}</td>
      <td class="number-cell">${money(item.planAmount)}</td><td class="number-cell">${money(item.actualAmount)}</td>
      <td class="number-cell ${planVariance > 0 ? 'text-positive' : planVariance < 0 ? 'text-negative' : ''}">${planVariance > 0 ? '+' : ''}${money(planVariance)}</td>
      <td class="number-cell ${previousVariance > 0 ? 'text-positive' : previousVariance < 0 ? 'text-negative' : ''}">${previousVariance > 0 ? '+' : ''}${money(previousVariance)}</td>
      <td class="reason-text" title="${escapeHtml(item.planVarianceReason || '')}">${escapeHtml(item.planVarianceReason || '—')}</td>
      <td class="reason-text" title="${escapeHtml(item.previousMonthVarianceReason || '')}">${escapeHtml(item.previousMonthVarianceReason || '—')}</td>
    </tr>`;
  }).join('');
}

async function loadDashboard() {
  try {
    showNotice('', 'info');
    const data = await requestJson(`${apiRoot}/dashboard?year=${encodeURIComponent(yearSelect.value)}`);
    renderMonthlySummary(data.monthly || [], data);
    renderMonthlyGraph(data.monthly || []);
    renderMonthlyReasons(data.monthlyReasons || [], data.year || yearSelect.value);
  } catch (error) {
    showNotice(error.message, 'error');
    renderMonthlyReasons([], yearSelect.value);
    document.getElementById('reason-body').innerHTML = `<tr><td colspan="8" class="empty-state">대시보드 데이터를 불러오지 못했습니다.</td></tr>`;
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
  yearSelect.addEventListener('change', loadDashboard);
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
