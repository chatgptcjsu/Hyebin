const apiRoot = '/purchase-api';
const yearSelect = document.getElementById('evaluation-year');
const formYear = document.getElementById('form-year');
const form = document.getElementById('evaluation-form');
const notice = document.getElementById('evaluation-notice');
const criteriaRoot = document.getElementById('criteria-list');
const listRoot = document.getElementById('evaluation-list');
const today = new Date();
const yearNow = today.getFullYear();
const dateInput = document.getElementById('evaluation-date');
const gradeNames = { A: '우수', B: '양호', C: '보통', D: '미흡' };
let criteria = [];

const escapeHtml = (value) => String(value ?? '').replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char]);

function showNotice(message, kind = 'info') {
  notice.hidden = !message;
  notice.textContent = message || '';
  notice.className = `notice ${kind}`;
}

async function requestJson(url, options = {}) {
  const response = await fetch(url, { credentials: 'same-origin', ...options });
  let body;
  try { body = await response.json(); } catch { throw new Error(`서버 응답을 읽을 수 없습니다. (HTTP ${response.status})`); }
  if (!response.ok) throw new Error(body?.message || body?.error?.message || `요청에 실패했습니다. (HTTP ${response.status})`);
  return body && Object.prototype.hasOwnProperty.call(body, 'data') ? body.data : body;
}

function setupYears() {
  const values = [];
  for (let year = yearNow + 1; year >= yearNow - 8; year -= 1) values.push(year);
  const options = values.map((year) => `<option value="${year}">${year}년</option>`).join('');
  yearSelect.innerHTML = options;
  formYear.innerHTML = options;
  yearSelect.value = String(yearNow);
  formYear.value = String(yearNow);
  dateInput.value = today.toISOString().slice(0, 10);
}

function renderCriteria() {
  const sections = new Map();
  criteria.forEach((item) => {
    if (!sections.has(item.section)) sections.set(item.section, []);
    sections.get(item.section).push(item);
  });
  let criterionIndex = 0;
  let sectionIndex = 0;
  criteriaRoot.innerHTML = Array.from(sections.entries()).map(([section, items]) => {
    sectionIndex += 1;
    const sectionMaximum = items.reduce((sum, item) => sum + Number(item.maximumScore), 0);
    return `
    <section class="criteria-section" aria-label="${escapeHtml(section)} ${sectionMaximum}점">
      <div class="criteria-section-heading"><span>${String(sectionIndex).padStart(2, '0')}</span><h3>${escapeHtml(section)} <small>합계 ${sectionMaximum}점</small></h3></div>
      <div class="criteria-grid">${items.map((item) => {
        criterionIndex += 1;
        return `<div class="criterion-card" data-criterion="${escapeHtml(item.code)}" role="group" aria-labelledby="criterion-${escapeHtml(item.code)}-label">
          <div class="criterion-title" id="criterion-${escapeHtml(item.code)}-label"><span class="criterion-number">${String(criterionIndex).padStart(2, '0')}</span><span class="criterion-name">${escapeHtml(item.name)}</span><small>배점 ${Number(item.maximumScore)}점</small></div>
          <div class="criterion-options" role="radiogroup" aria-label="${escapeHtml(item.name)} 선택">
            ${item.options.map((option) => `<label class="criterion-option">
              <input type="radio" name="answer-${escapeHtml(item.code)}" value="${escapeHtml(option.code)}" data-score="${Number(option.score)}">
              <span class="option-copy"><span>${escapeHtml(option.label)}</span><strong>${Number(option.score)}점</strong></span>
            </label>`).join('')}
          </div>
        </div>`;
      }).join('')}</div>
    </section>`;
  }).join('');
  criteriaRoot.querySelectorAll('input[type="radio"]').forEach((input) => input.addEventListener('change', updateScorePreview));
  updateScorePreview();
}

function updateScorePreview() {
  const selected = criteriaRoot.querySelectorAll('input[type="radio"]:checked');
  const complete = criteria.length > 0 && criteriaRoot.querySelectorAll('.criterion-card').length === criteria.length
    && Array.from(criteriaRoot.querySelectorAll('.criterion-card')).every((card) => card.querySelector('input:checked'));
  if (!complete) {
    document.getElementById('total-score').innerHTML = '—<small> / 100점</small>';
    setGrade(null);
    return;
  }
  const total = Array.from(selected).reduce((sum, input) => sum + Number(input.dataset.score), 0);
  document.getElementById('total-score').innerHTML = `${total.toFixed(1)}<small> / 100점</small>`;
  setGrade(total >= 90 ? 'A' : total >= 80 ? 'B' : total >= 70 ? 'C' : 'D');
}

function setGrade(grade) {
  const badge = document.getElementById('final-grade');
  badge.textContent = grade ? `${grade} · ${gradeNames[grade]}` : '—';
  badge.className = `grade-pill ${grade ? `grade-${grade.toLowerCase()}` : 'grade-pending'}`;
}

function resetForm() {
  form.reset();
  document.getElementById('evaluation-id').value = '';
  formYear.value = yearSelect.value;
  dateInput.value = today.toISOString().slice(0, 10);
  document.getElementById('form-title').textContent = '새 협력업체 평가';
  document.getElementById('edit-state').textContent = '신규 입력';
  document.getElementById('save-evaluation').textContent = '평가 저장';
  updateScorePreview();
}

function renderEvaluations(entries) {
  document.getElementById('evaluation-count').textContent = `${entries.length}개 업체`;
  document.getElementById('list-year').textContent = yearSelect.value;
  if (!entries.length) {
    listRoot.innerHTML = '<tr><td colspan="6" class="empty-state">선택한 연도의 평가 자료가 없습니다. 새 평가를 입력해 주세요.</td></tr>';
    return;
  }
  listRoot.innerHTML = entries.map((entry) => `<tr>
    <td>${escapeHtml(entry.evaluationDate || '—')}</td>
    <td class="supplier-cell">${escapeHtml(entry.supplierName)}</td>
    <td>${escapeHtml(entry.evaluatorName)}</td>
    <td class="number-cell">${Number(entry.totalScore).toFixed(1)}점</td>
    <td><span class="grade-pill grade-${escapeHtml(String(entry.finalGrade).toLowerCase())}">${escapeHtml(entry.finalGrade)} · ${escapeHtml(gradeNames[entry.finalGrade] || '')}</span></td>
    <td><button class="table-action" type="button" data-edit-id="${Number(entry.purchaseVendorEvaluationId)}">수정</button></td>
  </tr>`).join('');
  listRoot.querySelectorAll('[data-edit-id]').forEach((button) => button.addEventListener('click', () => loadForEdit(button.dataset.editId)));
}

async function loadEvaluations() {
  listRoot.innerHTML = '<tr><td colspan="6" class="empty-state">평가 자료를 불러오는 중입니다.</td></tr>';
  try {
    const entries = await requestJson(`${apiRoot}/vendor-evaluations?year=${encodeURIComponent(yearSelect.value)}`, { cache: 'no-store' });
    renderEvaluations(entries);
  } catch (error) {
    listRoot.innerHTML = `<tr><td colspan="6" class="empty-state">${escapeHtml(error.message)}</td></tr>`;
  }
}

async function loadForEdit(id) {
  try {
    const entry = await requestJson(`${apiRoot}/vendor-evaluations/${encodeURIComponent(id)}`, { cache: 'no-store' });
    document.getElementById('evaluation-id').value = entry.purchaseVendorEvaluationId;
    formYear.value = String(entry.evaluationYear);
    document.getElementById('supplier-name').value = entry.supplierName;
    document.getElementById('evaluator-name').value = entry.evaluatorName;
    dateInput.value = entry.evaluationDate;
    Object.entries(entry.answers || {}).forEach(([code, value]) => {
      const input = criteriaRoot.querySelector(`input[name="answer-${CSS.escape(code)}"][value="${CSS.escape(value)}"]`);
      if (input) input.checked = true;
    });
    document.getElementById('form-title').textContent = '협력업체 평가 수정';
    document.getElementById('edit-state').textContent = '수정 중';
    document.getElementById('save-evaluation').textContent = '변경사항 저장';
    updateScorePreview();
    document.getElementById('form-title').scrollIntoView({ behavior: 'smooth', block: 'center' });
  } catch (error) {
    showNotice(error.message, 'error');
  }
}

async function submitEvaluation(event) {
  event.preventDefault();
  if (!form.reportValidity()) return;
  const answers = {};
  criteria.forEach((item) => {
    const selected = form.querySelector(`input[name="answer-${CSS.escape(item.code)}"]:checked`);
    if (selected) answers[item.code] = selected.value;
  });
  if (Object.keys(answers).length !== criteria.length) {
    showNotice('12개 평가 항목을 모두 선택해 주세요.', 'error');
    return;
  }
  const id = document.getElementById('evaluation-id').value;
  const payload = {
    evaluationYear: Number(formYear.value),
    supplierName: document.getElementById('supplier-name').value.trim(),
    evaluatorName: document.getElementById('evaluator-name').value.trim(),
    evaluationDate: dateInput.value,
    answers
  };
  const button = document.getElementById('save-evaluation');
  button.disabled = true;
  try {
    const saved = await requestJson(id ? `${apiRoot}/vendor-evaluations/${encodeURIComponent(id)}` : `${apiRoot}/vendor-evaluations`, {
      method: id ? 'PUT' : 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    yearSelect.value = String(saved.evaluationYear);
    await loadEvaluations();
    resetForm();
    showNotice(`${saved.supplierName} 평가를 ${Number(saved.totalScore).toFixed(1)}점 / ${saved.finalGrade}등급으로 저장했습니다.`, 'success');
  } catch (error) {
    showNotice(error.message, 'error');
  } finally {
    button.disabled = false;
  }
}

async function initialize() {
  setupYears();
  formYear.addEventListener('change', () => { yearSelect.value = formYear.value; loadEvaluations(); });
  yearSelect.addEventListener('change', () => { formYear.value = yearSelect.value; resetForm(); loadEvaluations(); });
  document.getElementById('new-evaluation').addEventListener('click', () => {
    resetForm();
    document.getElementById('form-title').scrollIntoView({ behavior: 'smooth', block: 'start' });
  });
  document.getElementById('cancel-edit').addEventListener('click', resetForm);
  form.addEventListener('submit', submitEvaluation);
  try {
    criteria = await requestJson(`${apiRoot}/vendor-evaluations/criteria`, { cache: 'no-store' });
    renderCriteria();
    await loadEvaluations();
  } catch (error) {
    criteriaRoot.innerHTML = `<p class="empty-inline">평가 기준을 불러오지 못했습니다: ${escapeHtml(error.message)}</p>`;
    showNotice(error.message, 'error');
  }
}

initialize();
