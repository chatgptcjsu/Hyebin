const parameters = new URLSearchParams(window.location.search);
const year = Number(parameters.get('year'));
const month = Number(parameters.get('month'));
const numericColumns = new Set([5, 6, 7, 8, 9, 10, 13]);

const escapeHtml = (value) => String(value ?? '').replace(/[&<>"']/g, (char) => ({
  '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
})[char]);

function setStatus(message, isError = false) {
  const status = document.getElementById('sheet-status');
  status.hidden = !message;
  status.textContent = message || '';
  status.classList.toggle('error', isError);
}

function rowMarkup(row, isSummary = false) {
  const cells = Array.from({ length: 14 }, (_, index) => {
    const value = row[index] || '';
    const classes = [numericColumns.has(index) ? 'numeric-cell' : 'text-cell'];
    if (index === 0) classes.push('item-cell');
    if (index === 12) classes.push('vendor-cell');
    return `<td class="${classes.join(' ')}">${escapeHtml(value || '—')}</td>`;
  }).join('');
  return `<tr${isSummary ? ' class="summary-row"' : ''}>${cells}</tr>`;
}

function renderNotes(elementId, emptyId, items, ordered) {
  const target = document.getElementById(elementId);
  const empty = document.getElementById(emptyId);
  const rows = (items || []).filter((item) => String(item || '').trim());
  target.innerHTML = rows.map((item) => `<li>${escapeHtml(item)}</li>`).join('');
  empty.hidden = rows.length > 0;
  if (!ordered) target.style.listStyle = 'none';
}

async function loadDetailSheet() {
  if (!Number.isInteger(year) || !Number.isInteger(month) || month < 1 || month > 12) {
    setStatus('조회 연월이 올바르지 않습니다.', true);
    return;
  }
  try {
    const query = new URLSearchParams({ year: String(year), month: String(month) });
    const response = await fetch(`/purchase-api/detail-sheet?${query}`, { credentials: 'same-origin' });
    const body = await response.json();
    if (!response.ok) throw new Error(body?.message || body?.error?.message || '상세 시트를 불러오지 못했습니다.');
    const detail = body?.data ?? body;
    if (!detail.available) {
      document.title = `${detail.purchaseYear}년 ${detail.purchaseMonth}월 구매실적 상세`;
      document.getElementById('sheet-title').textContent = `${detail.purchaseYear}년 ${detail.purchaseMonth}월 구매실적`;
      document.getElementById('sheet-subtitle').textContent = '두 번째 시트 상세 자료';
      document.getElementById('sheet-content').hidden = true;
      setStatus(detail.message || '해당 월의 두 번째 시트 자료가 없습니다.', true);
      return;
    }

    document.title = `${detail.purchaseYear}년 ${detail.purchaseMonth}월 구매실적 상세`;
    document.getElementById('sheet-title').textContent = `${detail.purchaseYear}년 ${detail.purchaseMonth}월 구매실적`;
    document.getElementById('sheet-subtitle').textContent = `${detail.sheetName || '두 번째 시트'} · 업로드 Excel 상세 자료`;
    document.getElementById('sheet-section-title').textContent = detail.sectionTitle || detail.title || '업체별 구매실적';
    document.getElementById('sheet-unit').textContent = detail.unitLabel || '단위: 백만원, VAT 별도';
    document.getElementById('previous-period').textContent = detail.previousMonthLabel || '전월 실적';
    document.getElementById('plan-period').textContent = detail.planMonthLabel || `${detail.purchaseMonth}월 계획`;
    document.getElementById('actual-period').textContent = detail.actualMonthLabel || `${detail.purchaseMonth}월 실적`;
    document.getElementById('detail-rows').innerHTML = (detail.rows || []).map((row) => rowMarkup(row)).join('');
    document.getElementById('summary-rows').innerHTML = (detail.summaryRows || []).map((row) => rowMarkup(row, true)).join('');
    renderNotes('main-issues', 'no-issues', detail.mainIssues, true);
    renderNotes('future-improvements', 'no-improvements', detail.futureImprovements, false);
    document.getElementById('sheet-content').hidden = false;
    setStatus('');
  } catch (error) {
    setStatus(error.message || '상세 시트를 불러오지 못했습니다.', true);
  }
}

document.getElementById('print-button').addEventListener('click', () => window.print());
document.getElementById('close-button').addEventListener('click', () => window.close());
loadDetailSheet();
