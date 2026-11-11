const http = require('http');
const fs = require('fs');
const path = require('path');
const puppeteer = require(path.resolve('frontend/node_modules/puppeteer-core'));

const distDir = path.resolve('frontend/dist');
const outputDir = path.resolve('scratch/screenshots/ui_redesign');

if (!fs.existsSync(outputDir)) {
  fs.mkdirSync(outputDir, { recursive: true });
}

const mimeTypes = {
  '.html': 'text/html',
  '.js': 'text/javascript',
  '.css': 'text/css',
  '.json': 'application/json',
  '.png': 'image/png',
  '.jpg': 'image/jpg',
  '.svg': 'image/svg+xml',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2'
};

const server = http.createServer((req, res) => {
  if (req.url.startsWith('/api')) {
    // Proxy API requests to Spring Boot on 8080
    const options = {
      hostname: 'localhost',
      port: 8080,
      path: req.url,
      method: req.method,
      headers: req.headers
    };

    const proxyReq = http.request(options, (proxyRes) => {
      res.writeHead(proxyRes.statusCode, proxyRes.headers);
      proxyRes.pipe(res);
    });

    proxyReq.on('error', (err) => {
      res.writeHead(502, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Backend unreachable', details: err.message }));
    });

    req.pipe(proxyReq);
    return;
  }

  // Serve static files from frontend/dist
  let filePath = path.join(distDir, req.url.split('?')[0]);
  if (filePath.endsWith('/') || !path.extname(filePath)) {
    filePath = path.join(distDir, 'index.html');
  }

  fs.readFile(filePath, (err, data) => {
    if (err) {
      // Fallback to index.html for SPA routing
      fs.readFile(path.join(distDir, 'index.html'), (err2, data2) => {
        if (err2) {
          res.writeHead(404);
          res.end('Not found');
        } else {
          res.writeHead(200, { 'Content-Type': 'text/html' });
          res.end(data2);
        }
      });
      return;
    }
    const ext = path.extname(filePath);
    res.writeHead(200, { 'Content-Type': mimeTypes[ext] || 'application/octet-stream' });
    res.end(data);
  });
});

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function run() {
  const PORT = 3001;
  server.listen(PORT, async () => {
    console.log(`Preview server listening on port ${PORT}...`);
    try {
      const chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
      console.log('Launching headless Chrome...');
      const browser = await puppeteer.launch({
        executablePath: chromePath,
        headless: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox', '--window-size=1440,900'],
        defaultViewport: { width: 1440, height: 900, deviceScaleFactor: 1.5 }
      });

      const page = await browser.newPage();

      // 1. Login Screen
      console.log('1. Capturing 01_login_screen.png...');
      await page.goto(`http://localhost:${PORT}`, { waitUntil: 'networkidle0', timeout: 30000 });
      await sleep(1500);
      await page.screenshot({ path: path.join(outputDir, '01_login_screen.png') });

      // 2. Perform Login as Branch Manager
      console.log('Logging in as Branch Manager (brmgr_user)...');
      await page.type('#username-input', 'brmgr_user');
      await page.type('#password-input', 'password123');
      await sleep(300);

      const submitBtn = await page.$('button[type="submit"]');
      if (submitBtn) await submitBtn.click();
      await sleep(4000);

      // 2. Dashboard
      console.log('2. Capturing 02_dashboard_overview.png...');
      await page.screenshot({ path: path.join(outputDir, '02_dashboard_overview.png') });

      // Helper to click sidebar nav
      async function clickNav(text) {
        await page.evaluate((navText) => {
          const links = Array.from(document.querySelectorAll('aside a, aside button, nav a, nav button, [role="navigation"] button, [role="navigation"] a'));
          const found = links.find(l => l.textContent && l.textContent.toLowerCase().includes(navText.toLowerCase()));
          if (found) found.click();
        }, text);
        await sleep(2500);
      }

      // 3. Collaterals
      console.log('3. Capturing 03_collateral_master.png...');
      await clickNav('Collaterals');
      await page.screenshot({ path: path.join(outputDir, '03_collateral_master.png') });

      // 4. Open Collateral Registration Wizard
      console.log('4. Capturing 04_collateral_wizard.png...');
      await page.evaluate(() => {
        const btns = Array.from(document.querySelectorAll('button'));
        const regBtn = btns.find(b => b.textContent && (b.textContent.includes('Register Collateral') || b.textContent.includes('New Collateral') || b.textContent.includes('+ Register')));
        if (regBtn) regBtn.click();
      });
      await sleep(1500);
      await page.screenshot({ path: path.join(outputDir, '04_collateral_wizard.png') });

      // Close Wizard
      await page.evaluate(() => {
        const cancelBtn = Array.from(document.querySelectorAll('button')).find(b => b.textContent && (b.textContent.includes('Cancel') || b.textContent.includes('Close')));
        if (cancelBtn) cancelBtn.click();
      });
      await sleep(1000);

      // 5. Facilities
      console.log('5. Capturing 05_facilities_master.png...');
      await clickNav('Facilities');
      await page.screenshot({ path: path.join(outputDir, '05_facilities_master.png') });

      // 6. Insurance Policies
      console.log('6. Capturing 06_insurance_policies.png...');
      await clickNav('Insurance');
      await page.screenshot({ path: path.join(outputDir, '06_insurance_policies.png') });

      // 7. Approvals
      console.log('7. Capturing 07_workflow_approvals.png...');
      await clickNav('Approvals');
      await page.screenshot({ path: path.join(outputDir, '07_workflow_approvals.png') });

      // 8. Reports
      console.log('8. Capturing 08_reports_catalog.png...');
      await clickNav('Reports');
      await page.screenshot({ path: path.join(outputDir, '08_reports_catalog.png') });

      // 9. Audit Trail
      console.log('9. Capturing 09_audit_trail.png...');
      await clickNav('Audit');
      await page.screenshot({ path: path.join(outputDir, '09_audit_trail.png') });

      // 10. Admin Portal
      console.log('10. Capturing 10_admin_portal.png...');
      await clickNav('Admin');
      await page.screenshot({ path: path.join(outputDir, '10_admin_portal.png') });

      await browser.close();
      console.log('All 10 key screen screenshots captured successfully!');
    } catch (err) {
      console.error('Error during capture:', err);
    } finally {
      server.close();
      process.exit(0);
    }
  });
}

run();
