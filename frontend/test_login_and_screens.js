import puppeteer from 'puppeteer-core';
import fs from 'fs';
import path from 'path';

const chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const outputDir = path.resolve('../scratch/screenshots');

if (!fs.existsSync(outputDir)) {
  fs.mkdirSync(outputDir, { recursive: true });
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function run() {
  console.log('Launching browser...');
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

  // 1. Screenshot Login Screen before typing
  console.log('1. Capturing Login Screen...');
  await page.screenshot({ path: path.join(outputDir, '01_login_screen.png') });

  // Type credentials into the input fields
  console.log('Typing credentials for brmgr_user...');
  const userInput = await page.$('input[type="text"]');
  const passInput = await page.$('input[type="password"]');

  if (!userInput || !passInput) {
    throw new Error('Input fields not found on login screen!');
  }

  await userInput.click();
  await userInput.type('brmgr_user', { delay: 50 });
  await passInput.click();
  await passInput.type('password123', { delay: 50 });

  console.log('Submitting login form...');
  const submitBtn = await page.$('button[type="submit"]');
  await submitBtn.click();

  // Wait for login to complete and dashboard to mount
  console.log('Waiting for dashboard to mount...');
  await page.waitForFunction(() => {
    return document.body.innerText.includes('Dashboard') || document.body.innerText.includes('Collaterals') || document.querySelector('nav') !== null;
  }, { timeout: 15000 });

  await sleep(3000);
  console.log('Dashboard mounted successfully!');

  // 2. Screenshot Dashboard Overview
  console.log('2. Capturing Dashboard Overview...');
  await page.screenshot({ path: path.join(outputDir, '02_dashboard_overview.png') });

  // Helper to click sidebar nav items
  async function clickSidebar(name) {
    const clicked = await page.evaluate((navText) => {
      const items = Array.from(document.querySelectorAll('nav a, nav button, aside a, aside button, button'));
      const target = items.find(el => {
        const text = (el.innerText || el.textContent || '').trim().toLowerCase();
        return text === navText.toLowerCase() || text.startsWith(navText.toLowerCase());
      });
      if (target) {
        target.click();
        return true;
      }
      return false;
    }, name);

    if (!clicked) {
      console.warn(`Could not find nav button for: ${name}`);
    }
    await sleep(2500);
  }

  // 3. Collaterals
  console.log('3. Navigating to Collaterals...');
  await clickSidebar('Collaterals');
  await page.screenshot({ path: path.join(outputDir, '03_collateral_portfolio.png') });

  // 4. Open Collateral 360 View (click a row or view button)
  console.log('4. Opening Collateral 360 View...');
  const viewClicked = await page.evaluate(() => {
    const btns = Array.from(document.querySelectorAll('button, a'));
    const viewBtn = btns.find(b => b.innerText && (b.innerText.includes('View') || b.innerText.includes('360') || b.innerText.includes('Details')));
    if (viewBtn) {
      viewBtn.click();
      return true;
    }
    const row = document.querySelector('tbody tr');
    if (row) {
      row.click();
      return true;
    }
    return false;
  });
  await sleep(2500);
  await page.screenshot({ path: path.join(outputDir, '04_collateral_360_view.png') });

  // Close 360 modal or back
  await page.evaluate(() => {
    const close = Array.from(document.querySelectorAll('button')).find(b => b.innerText && (b.innerText.includes('Close') || b.innerText.includes('Back') || b.getAttribute('aria-label') === 'Close'));
    if (close) close.click();
  });
  await sleep(1000);

  // 5. Open Collateral Registration Wizard
  console.log('5. Opening Collateral Registration Wizard...');
  await page.evaluate(() => {
    const btns = Array.from(document.querySelectorAll('button'));
    const reg = btns.find(b => b.innerText && (b.innerText.includes('Register') || b.innerText.includes('New Collateral') || b.innerText.includes('Add Collateral')));
    if (reg) reg.click();
  });
  await sleep(2500);
  await page.screenshot({ path: path.join(outputDir, '05_collateral_registration_wizard.png') });

  // Close wizard
  await page.evaluate(() => {
    const cancel = Array.from(document.querySelectorAll('button')).find(b => b.innerText && (b.innerText.includes('Cancel') || b.innerText.includes('Close')));
    if (cancel) cancel.click();
  });
  await sleep(1000);

  // 6. Facilities / Linkage
  console.log('6. Navigating to Facilities / Linkage...');
  await clickSidebar('Facilities');
  await page.screenshot({ path: path.join(outputDir, '06_loan_collateral_linkage.png') });

  // 7. Insurance
  console.log('7. Navigating to Insurance...');
  await clickSidebar('Insurance');
  await page.screenshot({ path: path.join(outputDir, '07_insurance_management.png') });

  // 8. Approvals
  console.log('8. Navigating to Approvals...');
  await clickSidebar('Approvals');
  await page.screenshot({ path: path.join(outputDir, '08_workflow_and_approvals.png') });

  // 9. Admin
  console.log('9. Navigating to Admin...');
  await clickSidebar('Admin');
  await page.screenshot({ path: path.join(outputDir, '09_admin_configuration.png') });

  // 10. Reports
  console.log('10. Navigating to Reports...');
  await clickSidebar('Reports');
  await page.screenshot({ path: path.join(outputDir, '10_reports_and_analytics.png') });

  await browser.close();
  console.log('ALL SCREENSHOTS CAPTURED WITH AUTHENTICATED REAL UI!');
}

run().catch(err => {
  console.error('Capture failed:', err);
  process.exit(1);
});

