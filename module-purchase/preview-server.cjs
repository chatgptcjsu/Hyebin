// Local-only preview harness. Uses synthetic data and never writes to MariaDB.
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');

const staticRoot = path.join(__dirname, 'src/main/resources/static/purchase');
const port = Number(process.env.PORT || 3000);
const months = [
  { month: 1, uploaded: true, planAmount: 1840, actualAmount: 1620 },
  { month: 2, uploaded: true, planAmount: 2380, actualAmount: 2510 },
  { month: 3, uploaded: true, planAmount: 1960, actualAmount: 1810 },
  { month: 4, uploaded: true, planAmount: 2740, actualAmount: 2930 },
  { month: 5, uploaded: true, planAmount: 2180, actualAmount: 2040 },
  { month: 6, uploaded: true, planAmount: 2520, actualAmount: 2690 },
  ...Array.from({ length: 6 }, (_, index) => ({ month: index + 7, uploaded: false, planAmount: 0, actualAmount: 0 }))
].map((item) => ({
  ...item,
  varianceAmount: item.actualAmount - item.planAmount,
  achievementRate: item.planAmount ? Number((item.actualAmount / item.planAmount * 100).toFixed(2)) : 0
}));

const activeLines = {
  1: [
    { businessUnit: '공통', importType: '내자', supplierName: '샘플기술A', planAmount: 620, actualAmount: 590, varianceAmount: -30, noteText: '정기 설비 개선 공사 1차 기성' },
    { businessUnit: 'PS', importType: '내자', supplierName: '샘플엔지니어링', planAmount: 470, actualAmount: 430, varianceAmount: -40, noteText: '포장라인 자동화 설비 중도금' },
    { businessUnit: 'HL', importType: '외자', supplierName: 'Demo Industrial', planAmount: 210, actualAmount: 195, varianceAmount: -15, noteText: '수입 부품 납품' }
  ],
  2: [
    { businessUnit: '공통', importType: '내자', supplierName: '샘플기술A', planAmount: 950, actualAmount: 1030, varianceAmount: 80, noteText: '에너지 절감설비 공사 기성금' },
    { businessUnit: 'PS', importType: '내자', supplierName: '샘플엔지니어링', planAmount: 680, actualAmount: 720, varianceAmount: 40, noteText: '생산설비 개선공사' }
  ],
  3: [
    { businessUnit: '공통', importType: '내자', supplierName: '테스트종합건설', planAmount: 810, actualAmount: 740, varianceAmount: -70, noteText: '배관 보수 및 점검' },
    { businessUnit: 'PS', importType: '내자', supplierName: '샘플기술B', planAmount: 520, actualAmount: 495, varianceAmount: -25, noteText: '유틸리티 설비 교체' }
  ],
  4: [
    { businessUnit: '공통', importType: '내자', supplierName: '샘플기술A', planAmount: 1300, actualAmount: 1450, varianceAmount: 150, noteText: '공장 에너지 시설 정기 기성' },
    { businessUnit: 'PS', importType: '내자', supplierName: '샘플엔지니어링', planAmount: 760, actualAmount: 810, varianceAmount: 50, noteText: '자동화 설비 설치 및 시운전' }
  ],
  5: [
    { businessUnit: '공통', importType: '내자', supplierName: '테스트종합건설', planAmount: 870, actualAmount: 830, varianceAmount: -40, noteText: '안전시설 개선공사' },
    { businessUnit: 'HL', importType: '내자', supplierName: '샘플기술B', planAmount: 420, actualAmount: 390, varianceAmount: -30, noteText: '펌프실 점검 및 부품 교체' }
  ],
  6: [
    { businessUnit: '공통', importType: '내자', supplierName: '샘플기술A', planAmount: 1100, actualAmount: 1190, varianceAmount: 90, noteText: '환경설비 구축공사 기성' },
    { businessUnit: 'PS', importType: '외자', supplierName: 'Demo Industrial', planAmount: 630, actualAmount: 660, varianceAmount: 30, noteText: '수입 제어장치 도입' }
  ]
};

function json(response, status, data) {
  response.writeHead(status, { 'content-type': 'application/json; charset=utf-8', 'cache-control': 'no-store' });
  response.end(JSON.stringify({ data }));
}

function dashboard() {
  const dataMonths = months.filter((item) => item.uploaded);
  const totalPlanAmount = dataMonths.reduce((sum, item) => sum + item.planAmount, 0);
  const totalActualAmount = dataMonths.reduce((sum, item) => sum + item.actualAmount, 0);
  const businessUnits = [
    { name: '공통', amount: 6610, shareRate: 48.6 },
    { name: 'PS', amount: 5150, shareRate: 37.8 },
    { name: 'HL', amount: 1840, shareRate: 13.6 }
  ];
  const topSuppliers = [
    { name: '샘플기술A', amount: 4280, shareRate: 31.5 },
    { name: '샘플엔지니어링', amount: 3260, shareRate: 24.0 },
    { name: '테스트종합건설', amount: 2240, shareRate: 16.5 },
    { name: '샘플기술B', amount: 1680, shareRate: 12.4 },
    { name: 'Demo Industrial', amount: 1140, shareRate: 8.4 }
  ];
  return {
    year: 2026,
    totalPlanAmount,
    totalActualAmount,
    totalVarianceAmount: totalActualAmount - totalPlanAmount,
    achievementRate: Number((totalActualAmount / totalPlanAmount * 100).toFixed(2)),
    monthly: months,
    businessUnits,
    topSuppliers
  };
}

const history = months.filter((item) => item.uploaded).map((item) => ({
  uploadBatchId: item.month,
  purchaseYear: 2026,
  purchaseMonth: item.month,
  originalFilename: `2026-${String(item.month).padStart(2, '0')}-purchase-demo.xlsx`,
  uploadStatus: 'CURRENT',
  planAmount: item.planAmount,
  actualAmount: item.actualAmount,
  detailRowCount: (activeLines[item.month] || []).length,
  createdAt: `2026-${String(item.month).padStart(2, '0')}-28T09:30:00`
}));

const server = http.createServer((request, response) => {
  const url = new URL(request.url, `http://${request.headers.host || 'localhost'}`);
  if (request.method === 'GET' && url.pathname === '/purchase-api/dashboard') {
    return json(response, 200, Number(url.searchParams.get('year')) === 2026 ? dashboard() : {
      year: Number(url.searchParams.get('year')) || 2026,
      totalPlanAmount: 0, totalActualAmount: 0, totalVarianceAmount: 0, achievementRate: 0,
      monthly: Array.from({ length: 12 }, (_, index) => ({ month: index + 1, uploaded: false, planAmount: 0, actualAmount: 0, varianceAmount: 0, achievementRate: 0 })),
      businessUnits: [], topSuppliers: []
    });
  }
  if (request.method === 'GET' && url.pathname === '/purchase-api/uploads') {
    return json(response, 200, Number(url.searchParams.get('year')) === 2026 ? history : []);
  }
  if (request.method === 'GET' && url.pathname === '/purchase-api/details') {
    const month = Number(url.searchParams.get('month'));
    const businessUnit = url.searchParams.get('businessUnit');
    const supplierName = (url.searchParams.get('supplierName') || '').toLowerCase();
    const rows = (activeLines[month] || []).filter((item) => (!businessUnit || item.businessUnit === businessUnit)
      && (!supplierName || item.supplierName.toLowerCase().includes(supplierName)));
    return json(response, 200, { content: rows, page: 0, size: 50, totalElements: rows.length, totalPages: rows.length ? 1 : 0 });
  }
  if (request.method === 'POST' && url.pathname === '/purchase-api/uploads') {
    let bytes = 0;
    request.on('data', (chunk) => { bytes += chunk.length; if (bytes > 21 * 1024 * 1024) request.destroy(); });
    request.on('end', () => {
      const result = { uploadBatchId: 999, purchaseYear: 2026, purchaseMonth: 7, originalFilename: 'preview-upload.xlsx',
        uploadStatus: 'CURRENT', planAmount: 2100, actualAmount: 1890, detailRowCount: 4,
        alreadyCurrent: false, requiresReplacementConfirmation: false };
      if (!url.searchParams.has('confirmReplace')) {
        result.uploadStatus = 'PENDING_REPLACEMENT_CONFIRMATION';
        result.requiresReplacementConfirmation = true;
      }
      return json(response, 200, result);
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
        content = Buffer.from(content.toString('utf8').replace('<div id="notice" class="notice" role="status" aria-live="polite" hidden></div>', '<div class="notice info" role="status">샌드박스 미리보기 · 가상 데이터 · 업로드 내역은 저장되지 않습니다.</div><div id="notice" class="notice" role="status" aria-live="polite" hidden></div>'));
      }
      response.writeHead(200, { 'content-type': mime, 'cache-control': 'no-store' });
      response.end(content);
    });
    return;
  }
  response.writeHead(405); response.end('Method not allowed');
});

server.listen(port, '0.0.0.0', () => console.log(`Purchase preview listening on ${port}`));
