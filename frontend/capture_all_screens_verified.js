import puppeteer from 'puppeteer-core';
import fs from 'fs';
import path from 'path';

const chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const outputDir = path.resolve('scratch/screenshots');

if (!fs.existsSync(outputDir)) {
  fs.mkdirSync(outputDir, { recursive: true });
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function run() {
  console.log('Launching browser to capture verified screenshots...');
  const browser = await puppeteer.launch({
    executablePath: chromePath,
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--window-size=1440,920'],
    defaultViewport: { width: 1440, height: 920, deviceScaleFactor: 1.5 }
  });

  const page = await browser.newPage();

  console.log('Navigating to http://localhost:5173...');
  await page.goto('http://localhost:5173', { waitUntil: 'networkidle0', timeout: 30000 });
  await sleep(1000);

  // 1. Capture pristine Login Screen
  console.log('1. Capturing Login Screen...');
  await page.screenshot({ path: path.join(outputDir, '01_login_screen.png') });

  // Type credentials for sysadmin
  console.log('Typing credentials for sysadmin...');
  const userInput = await page.$('input[type="text"]');
  const passInput = await page.$('input[type="password"]');

  await userInput.click();
  await userInput.type('sysadmin', { delay: 40 });
  await passInput.click();
  await passInput.type('password123', { delay: 40 });

  console.log('Clicking Sign In...');
  const submitBtn = await page.$('button[type="submit"]');
  await submitBtn.click();

  // Wait for Dashboard to mount
  await page.waitForFunction(() => {
    return document.body.innerText.includes('My Dashboard') || document.querySelector('aside') !== null;
  }, { timeout: 15000 });
  await sleep(3000);
  console.log('Logged in as sysadmin! Dashboard mounted.');

  // 2. Capture Dashboard
  console.log('2. Capturing Dashboard Overview...');
  await page.screenshot({ path: path.join(outputDir, '02_dashboard_overview.png') });

  // Helper to click sidebar nav items by exact or partial label
  async function navigate(label) {
    console.log(`Navigating to: ${label}...`);
    const success = await page.evaluate((navText) => {
      const btns = Array.from(document.querySelectorAll('aside button'));
      const target = btns.find(b => (b.innerText || '').toLowerCase().includes(navText.toLowerCase()));
      if (target) {
        target.click();
        return true;
      }
      return false;
    }, label);

    if (!success) {
      console.warn(`WARNING: Could not click sidebar item: ${label}`);
    }
    await sleep(2500);
  }

  // 3. Collaterals Portfolio
  await navigate('Collateral');
  console.log('3. Capturing Collateral Portfolio...');
  await page.screenshot({ path: path.join(outputDir, '03_collateral_portfolio.png') });

  // 4. Open Collateral 360 Detail Modal
  console.log('4. Opening Collateral 360 View...');
  await page.evaluate(() => {
    const row = document.querySelector('tbody tr');
    if (row) row.click();
  });
  await sleep(2500);
  console.log('Capturing Collateral 360 View...');
  await page.screenshot({ path: path.join(outputDir, '04_collateral_360_view.png') });

  // Close 360 View
  console.log('Closing 360 View...');
  await page.evaluate(() => {
    const btns = Array.from(document.querySelectorAll('button'));
    const closeBtn = btns.find(b => (b.innerText || '').includes('Close') || b.querySelector('svg.lucide-x') !== null);
    if (closeBtn) closeBtn.click();
  });
  await sleep(1500);

  // 5. Open Collateral Registration Wizard
  console.log('5. Opening Collateral Registration Wizard...');
  await page.evaluate(() => {
    const btns = Array.from(document.querySelectorAll('button'));
    const regBtn = btns.find(b => (b.innerText || '').includes('Register New Collateral'));
    if (regBtn) regBtn.click();
  });
  await sleep(2500);
  console.log('Capturing Collateral Registration Wizard...');
  await page.screenshot({ path: path.join(outputDir, '05_collateral_registration_wizard.png') });

  // Close Wizard
  console.log('Closing Wizard...');
  await page.evaluate(() => {
    const btns = Array.from(document.querySelectorAll('button'));
    const closeBtn = btns.find(b => (b.innerText || '').includes('Cancel') || (b.innerText || '').includes('Close') || b.querySelector('svg.lucide-x') !== null);
    if (closeBtn) closeBtn.click();
  });
  await sleep(1500);

  // 6. Facilities & Linkage
  await navigate('Facilities');
  console.log('6. Capturing Facilities & Loan Linkage...');
  await page.screenshot({ path: path.join(outputDir, '06_loan_collateral_linkage.png') });

  // 7. Insurance Policies
  await navigate('Insurance');
  console.log('7. Capturing Insurance Management...');
  await page.screenshot({ path: path.join(outputDir, '07_insurance_management.png') });

  // 8. Workflow Approvals
  await navigate('Workflow');
  console.log('8. Capturing Workflow Approvals...');
  await page.screenshot({ path: path.join(outputDir, '08_workflow_and_approvals.png') });

  // 9. Administration
  await navigate('Administration');
  console.log('9. Capturing Administration...');
  await page.screenshot({ path: path.join(outputDir, '09_admin_configuration.png') });

  // 10. Reports
  await navigate('Reports');
  console.log('10. Capturing Reports...');
  await page.screenshot({ path: path.join(outputDir, '10_reports_and_analytics.png') });

  // 11. Exceptions
  await navigate('Exceptions');
  console.log('11. Capturing Exceptions...');
  await page.screenshot({ path: path.join(outputDir, '11_exception_management.png') });

  // 12. Audit Trail
  await navigate('Audit');
  console.log('12. Capturing Audit Trail...');
  await page.screenshot({ path: path.join(outputDir, '12_audit_trail.png') });

  await browser.close();
  console.log('SUCCESS: All 12 authentic, high-resolution UI screens captured successfully!');
}

run().catch(err => {
  console.error('Fatal capture error:', err);
  process.exit(1);
});
