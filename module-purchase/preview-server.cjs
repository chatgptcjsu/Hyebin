// Local-only preview harness. Uses first-sheet sample data and never writes to MariaDB.
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');

const staticRoot = path.join(__dirname, 'src/main/resources/static/purchase');
const port = Number(process.env.PORT || 3000);
const monthly = [
  [897.7069, 682.606],
  [3782.048, 3264.7314909],
  [841.14, 795.84],
  [3734.85, 3953.621182],
  [241.116222, 325.15],
  [1870, 2106.366222]
].map(([planAmount, actualAmount], index) => ({
  month: index + 1,
  uploaded: true,
  planAmount,
  actualAmount,
  varianceAmount: actualAmount - planAmount,
  achievementRate: Number((actualAmount / planAmount * 100).toFixed(2))
}));
while (monthly.length < 12) monthly.push({ month: monthly.length + 1, uploaded: false, planAmount: 0, actualAmount: 0, varianceAmount: 0, achievementRate: 0 });

const categoriesByMonth = {
  1: [{ categoryName: '기계설비공사', previousActualAmount: 10336.786152, planForeignAmount: 0, planDomesticAmount: 897.7069, planAmount: 897.7069, actualForeignAmount: 0, actualDomesticAmount: 682.606, actualAmount: 682.606, planVarianceAmount: -215.1009, previousMonthVarianceAmount: -9654.180152, planVarianceReason: '복합보일러 질소산화물 저감설비 투자공사 및 Winder 설비 개조공사 잔금이 전월 선지급되어 계획 대비 감소', previousMonthVarianceReason: '에너지 재활용 시설 기성금과 정부지원사업 대금이 전월 지급에 포함되어 전월 대비 감소' }],
  2: [{ categoryName: '기계설비공사', previousActualAmount: 208.72, planForeignAmount: 0, planDomesticAmount: 3782.048, planAmount: 3782.048, actualForeignAmount: 94.4334909, actualDomesticAmount: 3170.298, actualAmount: 3264.7314909, planVarianceAmount: -517.3165091, previousMonthVarianceAmount: 3056.0114909, planVarianceReason: '자동화 구축공사 및 에너지재활용 시설 소방공사 완료 지연으로 잔금 지급이 지연되어 계획 대비 감소', previousMonthVarianceReason: '에너지 재활용 시설 기성금과 설비 개선공사 대금 반영으로 전월 대비 증가' }],
  3: [{ categoryName: '기계설비공사', previousActualAmount: 3264.7314909, planForeignAmount: 0, planDomesticAmount: 841.14, planAmount: 841.14, actualForeignAmount: 0, actualDomesticAmount: 795.84, actualAmount: 795.84, planVarianceAmount: -45.3, previousMonthVarianceAmount: -2468.8914909, planVarianceReason: '에너지재활용 시설 소방공사 검수 지연으로 잔금 지급 지연', previousMonthVarianceReason: '전월 에너지 재활용 시설 기성금이 반영되어 실적 감소' }],
  4: [{ categoryName: '기계설비공사', previousActualAmount: 795.84, planForeignAmount: 0, planDomesticAmount: 3734.85, planAmount: 3734.85, actualForeignAmount: 0, actualDomesticAmount: 3953.621182, actualAmount: 3953.621182, planVarianceAmount: 218.771182, previousMonthVarianceAmount: 3157.781182, planVarianceReason: '에너지 시설 감리 기성금 및 지붕 강풍 피해 보수공사 선급금 반영으로 계획 대비 증가', previousMonthVarianceReason: '에너지 재활용 시설 13차 기성금 반영으로 전월 대비 증가' }],
  5: [{ categoryName: '기계설비공사', previousActualAmount: 3953.621182, planForeignAmount: 0, planDomesticAmount: 241.116222, planAmount: 241.116222, actualForeignAmount: 0, actualDomesticAmount: 325.15, actualAmount: 325.15, planVarianceAmount: 84.033778, previousMonthVarianceAmount: -3628.471182, planVarianceReason: '지붕 보수공사와 에너지 재활용 시설 소방공사 잔금 반영으로 계획 대비 증가', previousMonthVarianceReason: '전월 대규모 시설 기성금이 반영되어 전월 대비 감소' }],
  6: [{ categoryName: '기계설비공사', previousActualAmount: 325.15, planForeignAmount: 0, planDomesticAmount: 1870, planAmount: 1870, actualForeignAmount: 0, actualDomesticAmount: 2106.366222, actualAmount: 2106.366222, planVarianceAmount: 236.366222, previousMonthVarianceAmount: 1781.216222, planVarianceReason: '태양광 발전설비 잔금과 계획 외 환경개선 공사 대금 반영으로 계획 대비 증가', previousMonthVarianceReason: '에너지 재활용 시설 14차 기성금과 태양광 설비 잔금 반영으로 전월 대비 증가' }]
};
let previewUploadApplied = false;

function currentMonths() {
  return monthly.map((item) => ({ ...item }));
}

function dashboard() {
  const months = currentMonths();
  const uploaded = months.filter((item) => item.uploaded);
  const totalPlanAmount = uploaded.reduce((sum, item) => sum + item.planAmount, 0);
  const totalActualAmount = uploaded.reduce((sum, item) => sum + item.actualAmount, 0);
  return {
    year: 2026,
    totalPlanAmount,
    totalActualAmount,
    totalVarianceAmount: totalActualAmount - totalPlanAmount,
    achievementRate: totalPlanAmount ? Number((totalActualAmount / totalPlanAmount * 100).toFixed(2)) : 0,
    monthly: months,
    categoryTotals: [{ name: '기계설비공사', amount: totalActualAmount, shareRate: totalActualAmount ? 100 : 0 }]
  };
}

function json(response, status, data) {
  response.writeHead(status, { 'content-type': 'application/json; charset=utf-8', 'cache-control': 'no-store' });
  response.end(JSON.stringify({ data }));
}

const server = http.createServer((request, response) => {
  const url = new URL(request.url, `http://${request.headers.host || 'localhost'}`);
  if (request.method === 'GET' && url.pathname === '/purchase-api/dashboard') {
    const year = Number(url.searchParams.get('year')) || 2026;
    if (year === 2026) return json(response, 200, dashboard());
    return json(response, 200, {
      year, totalPlanAmount: 0, totalActualAmount: 0, totalVarianceAmount: 0, achievementRate: 0,
      monthly: Array.from({ length: 12 }, (_, index) => ({ month: index + 1, uploaded: false, planAmount: 0, actualAmount: 0, varianceAmount: 0, achievementRate: 0 })),
      categoryTotals: []
    });
  }
  if (request.method === 'GET' && url.pathname === '/purchase-api/details') {
    const month = Number(url.searchParams.get('month'));
    let rows = categoriesByMonth[month] || [];
    if (previewUploadApplied && month === 7) rows = [{
      categoryName: '기계설비공사', previousActualAmount: 0, planForeignAmount: 0, planDomesticAmount: 2100,
      planAmount: 2100, actualForeignAmount: 0, actualDomesticAmount: 1890, actualAmount: 1890,
      planVarianceAmount: -210, previousMonthVarianceAmount: 1890,
      planVarianceReason: '샌드박스 미리보기 업로드 예시입니다.', previousMonthVarianceReason: '샌드박스 가상 데이터입니다.'
    }];
    const categoryName = (url.searchParams.get('categoryName') || '').toLowerCase();
    if (categoryName) rows = rows.filter((item) => item.categoryName.toLowerCase().includes(categoryName));
    return json(response, 200, { content: rows, page: 0, size: 50, totalElements: rows.length, totalPages: rows.length ? 1 : 0 });
  }
  if (request.method === 'POST' && url.pathname === '/purchase-api/uploads') {
    let bytes = 0;
    request.on('data', (chunk) => { bytes += chunk.length; if (bytes > 21 * 1024 * 1024) request.destroy(); });
    request.on('end', () => {
      previewUploadApplied = true;
      monthly[6] = { month: 7, uploaded: true, planAmount: 2100, actualAmount: 1890, varianceAmount: -210, achievementRate: 90 };
      return json(response, 200, {
        uploadBatchId: 999, purchaseYear: 2026, purchaseMonth: 7, originalFilename: 'preview-upload.xlsx',
        uploadStatus: 'CURRENT', planAmount: 2100, actualAmount: 1890, summaryRowCount: 1,
        alreadyCurrent: false, requiresReplacementConfirmation: false
      });
    });
    return;
  }
  if (request.method === 'GET' && url.pathname === '/favicon.ico') {
    response.writeHead(204);
    return response.end();
  }
  if (request.method === 'GET') {
    let filePath = url.pathname === '/' ? path.join(staticRoot, 'index.html')
      : path.join(staticRoot, url.pathname.replace(/^\/purchase\/?/, ''));
    filePath = path.resolve(filePath);
    if (!filePath.startsWith(path.resolve(staticRoot) + path.sep) && filePath !== path.join(path.resolve(staticRoot), 'index.html')) {
      response.writeHead(403); return response.end('Forbidden');
    }
    fs.readFile(filePath, (error, content) => {
      if (error) { response.writeHead(404); return response.end('Not found'); }
      const ext = path.extname(filePath).toLowerCase();
      const mime = { '.html': 'text/html; charset=utf-8', '.css': 'text/css; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.svg': 'image/svg+xml' }[ext] || 'application/octet-stream';
      if (ext === '.html') {
        content = Buffer.from(content.toString('utf8').replace(
          '<div id="notice" class="notice" role="status" aria-live="polite" hidden></div>',
          '<div class="notice info" role="status">샌드박스 미리보기 · 실제 엑셀 데이터 미리보기 · 업로드는 가상 반영입니다.</div><div id="notice" class="notice" role="status" aria-live="polite" hidden></div>'
        ));
      }
      response.writeHead(200, { 'content-type': mime, 'cache-control': 'no-store' });
      response.end(content);
    });
    return;
  }
  response.writeHead(405); response.end('Method not allowed');
});

server.listen(port, '0.0.0.0', () => console.log(`Purchase preview listening on ${port}`));
