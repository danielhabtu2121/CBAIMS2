import puppeteer from 'puppeteer-core';
import fs from 'fs';
import path from 'path';

const chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const outputDir = path.resolve('scratch/screenshots');

if (!fs.existsSync(outputDir)) {
  fs.mkdirSync(outputDir, { recursive: true });
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function capture() {
  console.log('Launching Chrome from:', chromePath);
  const browser = await puppeteer.launch({
    executablePath: chromePath,
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--window-size=1440,920'],
    defaultViewport: { width: 1440, height: 920, deviceScaleFactor: 1.5 }
  });

  const page = await browser.newPage();

  console.log('Navigating to http://localhost:5173...');
  await page.goto('http://localhost:5173', { waitUntil: 'networkidle0', timeout: 30000 });
  await sleep(1500);

  // 1. Login Screen
  console.log('Capturing Login Screen...');
  await page.screenshot({ path: path.join(outputDir, '01_login_screen.png') });

  // Look for persona button: Branch Manager
  console.log('Logging in as Branch Manager (brmgr_user)...');
  const personaClicked = await page.evaluate(() => {
    const btns = Array.from(document.querySelectorAll('button'));
    const brmgr = btns.find(b => b.textContent.includes('Branch Manager'));
    if (brmgr) {
      brmgr.click();
      return true;
    }
    return false;
  });

  if (!personaClicked) {
    // Fill credentials manually
    await page.evaluate(() => {
      const inputs = Array.from(document.querySelectorAll('input'));
      if (inputs[0]) inputs[0].value = 'brmgr_user';
      if (inputs[0]) inputs[0].dispatchEvent(new Event('input', { bubbles: true }));
      if (inputs[1]) inputs[1].value = 'password123';
      if (inputs[1]) inputs[1].dispatchEvent(new Event('input', { bubbles: true }));
    });
  }

  // Click Sign In
  const submitBtn = await page.$('button[type="submit"]');
  if (submitBtn) {
    await submitBtn.click();
  }
  await sleep(3500);

  // 2. Dashboard Overview
  console.log('Capturing Dashboard Overview...');
  await page.screenshot({ path: path.join(outputDir, '02_dashboard_overview.png') });

  // Helper to click sidebar nav items
  async function clickNav(name) {
    await page.evaluate((navText) => {
      const navLinks = Array.from(document.querySelectorAll('nav a, nav button, aside button, aside a, .sidebar-link'));
      const target = navLinks.find(el => el.textContent.toLowerCase().includes(navText.toLowerCase()));
      if (target) target.click();
    }, name);
    await sleep(2000);
  }

  // 3. Collaterals Portfolio
  console.log('Navigating to Collaterals...');
  await clickNav('Collaterals');
  await page.screenshot({ path: path.join(outputDir, '03_collateral_portfolio.png') });

  // 4. Open Collateral 360 View (click first collateral row or view button)
  console.log('Opening Collateral 360 View...');
  await page.evaluate(() => {
    const rows = Array.from(document.querySelectorAll('tbody tr, .collateral-card, button'));
    const viewBtn = rows.find(el => el.textContent.includes('View') || el.textContent.includes('360') || el.textContent.includes('Details'));
    if (viewBtn) viewBtn.click();
  });
  await sleep(2000);
  await page.screenshot({ path: path.join(outputDir, '04_collateral_360_view.png') });

  // 5. Open Collateral Registration Wizard
  console.log('Opening Collateral Registration Wizard...');
  await page.evaluate(() => {
    // Close modal if open
    const closeBtn = document.querySelector('button[aria-label="Close"], .modal-close');
    if (closeBtn) closeBtn.click();
  });
  await sleep(1000);
  await page.evaluate(() => {
    const btns = Array.from(document.querySelectorAll('button'));
    const regBtn = btns.find(b => b.textContent.includes('Register Collateral') || b.textContent.includes('New Collateral') || b.textContent.includes('Add Collateral'));
    if (regBtn) regBtn.click();
  });
  await sleep(2000);
  await page.screenshot({ path: path.join(outputDir, '05_collateral_registration_wizard.png') });

  // Close wizard / modal
  await page.evaluate(() => {
    const cancelBtn = Array.from(document.querySelectorAll('button')).find(b => b.textContent.includes('Cancel') || b.textContent.includes('Close'));
    if (cancelBtn) cancelBtn.click();
  });
  await sleep(1000);

  // 6. Loan Collateral Linkage
  console.log('Navigating to Facilities...');
  await clickNav('Facilities');
  await page.screenshot({ path: path.join(outputDir, '06_loan_collateral_linkage.png') });

  // 7. Insurance Management
  console.log('Navigating to Insurance...');
  await clickNav('Insurance');
  await page.screenshot({ path: path.join(outputDir, '07_insurance_management.png') });

  // 8. Approvals & Workflow Tasks
  console.log('Navigating to Approvals...');
  await clickNav('Approvals');
  await page.screenshot({ path: path.join(outputDir, '08_workflow_and_approvals.png') });

  // 9. Admin Configuration
  console.log('Navigating to Administration...');
  await clickNav('Admin');
  await page.screenshot({ path: path.join(outputDir, '09_admin_configuration.png') });

  // 10. Reports Center
  console.log('Navigating to Reports...');
  await clickNav('Reports');
  await page.screenshot({ path: path.join(outputDir, '10_reports_and_analytics.png') });

  await browser.close();
  console.log('Screenshot capture completed successfully!');
}

capture().catch(err => {
  console.error('Error during capture:', err);
  process.exit(1);
});
