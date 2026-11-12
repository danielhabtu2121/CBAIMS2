import fs from 'fs';
import path from 'path';
import {
  Document,
  Packer,
  Paragraph,
  TextRun,
  Table,
  TableRow,
  TableCell,
  ImageRun,
  Header,
  Footer,
  AlignmentType,
  HeadingLevel,
  BorderStyle,
  WidthType,
  ShadingType,
  PageNumber,
  PageBreak,
} from 'docx';

const screenshotDir = 'C:\\Users\\danie_mdh4j2\\Desktop\\cims proj\\scratch\\screenshots';
const logoPath = 'C:\\Users\\danie_mdh4j2\\Desktop\\cims proj\\frontend\\src\\assets\\dashen-logo.png';
const outputPath = 'C:\\Users\\danie_mdh4j2\\Desktop\\cims proj\\CBAIMS_User_Manual_and_Operational_Guide_Final.docx';
const altOutputPath = 'C:\\Users\\danie_mdh4j2\\Desktop\\cims proj\\CBAIMS_User_Manual_and_Operational_Guide_V2.0.docx';

console.log('Building CBAIMS User Manual & Role Operational Guide DOCX...');

// Color Palette Constants
const COLOR_PRIMARY = '0B2545';    // Navy Blue
const COLOR_SECONDARY = '1E40AF';  // Corporate Blue
const COLOR_TEXT = '1E293B';       // Slate Dark
const COLOR_MUTED = '64748B';      // Muted Slate
const COLOR_BORDER = 'CBD5E1';     // Border Slate
const COLOR_BG_LIGHT = 'F8FAFC';   // Soft Off-white/Gray
const COLOR_BG_CALLOUT = 'F1F5F9'; // Callout Box Background
const COLOR_AMBER = 'B45309';      // Warning / Critical
const COLOR_GREEN = '065F46';      // Success / Tip

// Helper: Paragraph
function p(text = '', opts = {}) {
  const runs = Array.isArray(text) ? text : [
    new TextRun({
      text,
      font: 'Segoe UI',
      size: opts.size || 21, // 10.5 pt
      color: opts.color || COLOR_TEXT,
      bold: opts.bold || false,
      italics: opts.italics || false,
    })
  ];
  return new Paragraph({
    alignment: opts.alignment || AlignmentType.LEFT,
    spacing: {
      before: opts.before !== undefined ? opts.before : 100,
      after: opts.after !== undefined ? opts.after : 120,
      line: opts.line || 276, // 1.15 line spacing
    },
    children: runs,
  });
}

// Helper: Headings
function h1(title) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_1,
    spacing: { before: 360, after: 180 },
    children: [
      new TextRun({
        text: title,
        font: 'Segoe UI',
        bold: true,
        size: 32, // 16 pt
        color: COLOR_PRIMARY,
      }),
    ],
  });
}

function h2(title) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_2,
    spacing: { before: 260, after: 120 },
    children: [
      new TextRun({
        text: title,
        font: 'Segoe UI',
        bold: true,
        size: 26, // 13 pt
        color: COLOR_SECONDARY,
      }),
    ],
  });
}

function h3(title) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_3,
    spacing: { before: 180, after: 80 },
    children: [
      new TextRun({
        text: title,
        font: 'Segoe UI',
        bold: true,
        size: 22, // 11 pt
        color: COLOR_PRIMARY,
      }),
    ],
  });
}

// Helper: Bullet list
function bullet(prefix, text) {
  return new Paragraph({
    spacing: { before: 40, after: 60, line: 260 },
    indent: { left: 400, hanging: 240 },
    children: [
      new TextRun({ text: '▪  ', font: 'Segoe UI', bold: true, color: COLOR_SECONDARY, size: 20 }),
      new TextRun({ text: prefix ? `${prefix}: ` : '', font: 'Segoe UI', bold: true, color: COLOR_PRIMARY, size: 20 }),
      new TextRun({ text, font: 'Segoe UI', color: COLOR_TEXT, size: 20 }),
    ],
  });
}

// Helper: Numbered Step
function stepItem(num, title, description) {
  return new Paragraph({
    spacing: { before: 60, after: 80, line: 260 },
    indent: { left: 400, hanging: 240 },
    children: [
      new TextRun({ text: `Step ${num}: `, font: 'Segoe UI', bold: true, color: COLOR_SECONDARY, size: 20 }),
      new TextRun({ text: title ? `${title} — ` : '', font: 'Segoe UI', bold: true, color: COLOR_PRIMARY, size: 20 }),
      new TextRun({ text: description, font: 'Segoe UI', color: COLOR_TEXT, size: 20 }),
    ],
  });
}

// Helper: Callout Box
function callout(title, body, type = 'note') {
  let borderColor = COLOR_SECONDARY;
  let titleColor = COLOR_PRIMARY;
  let iconText = '📌 NOTE: ';
  let bgShade = 'F0F4F8';

  if (type === 'warning' || type === 'important') {
    borderColor = COLOR_AMBER;
    titleColor = COLOR_AMBER;
    iconText = '⚠️ CRITICAL POLICY RULE: ';
    bgShade = 'FEF3C7';
  } else if (type === 'tip') {
    borderColor = COLOR_GREEN;
    titleColor = COLOR_GREEN;
    iconText = '💡 OPERATIONAL TIP: ';
    bgShade = 'ECFDF5';
  }

  const noneBorder = { style: BorderStyle.NONE, size: 0, color: 'FFFFFF' };
  const leftBorder = { style: BorderStyle.SINGLE, size: 24, color: borderColor };

  return new Table({
    width: { size: 100, type: WidthType.PERCENTAGE },
    borders: {
      top: noneBorder,
      bottom: noneBorder,
      right: noneBorder,
      left: leftBorder,
    },
    rows: [
      new TableRow({
        children: [
          new TableCell({
            shading: { type: ShadingType.CLEAR, fill: bgShade },
            margins: { top: 140, bottom: 140, left: 180, right: 180 },
            children: [
              new Paragraph({
                spacing: { before: 0, after: 60 },
                children: [
                  new TextRun({ text: iconText, bold: true, font: 'Segoe UI', size: 20, color: titleColor }),
                  new TextRun({ text: title, bold: true, font: 'Segoe UI', size: 20, color: titleColor }),
                ],
              }),
              new Paragraph({
                spacing: { before: 0, after: 0 },
                children: [
                  new TextRun({ text: body, font: 'Segoe UI', size: 19, color: COLOR_TEXT }),
                ],
              }),
            ],
          }),
        ],
      }),
    ],
  });
}

// Helper: Styled Data Table
function styledTable(headers, rows, colWidths = []) {
  const borderStyle = { style: BorderStyle.SINGLE, size: 4, color: COLOR_BORDER };

  const tableRows = [
    new TableRow({
      tableHeader: true,
      children: headers.map((h, i) => new TableCell({
        width: colWidths[i] ? { size: colWidths[i], type: WidthType.DXA } : undefined,
        shading: { type: ShadingType.CLEAR, fill: COLOR_PRIMARY },
        margins: { top: 120, bottom: 120, left: 140, right: 140 },
        children: [
          new Paragraph({
            alignment: AlignmentType.LEFT,
            children: [
              new TextRun({ text: h, font: 'Segoe UI', bold: true, size: 19, color: 'FFFFFF' }),
            ],
          }),
        ],
      })),
    }),
    ...rows.map((row, rIdx) => new TableRow({
      children: row.map((cellText, cIdx) => new TableCell({
        width: colWidths[cIdx] ? { size: colWidths[cIdx], type: WidthType.DXA } : undefined,
        shading: { type: ShadingType.CLEAR, fill: rIdx % 2 === 1 ? COLOR_BG_LIGHT : 'FFFFFF' },
        margins: { top: 100, bottom: 100, left: 140, right: 140 },
        children: [
          new Paragraph({
            spacing: { before: 0, after: 0 },
            children: [
              new TextRun({
                text: String(cellText),
                font: 'Segoe UI',
                size: 19,
                color: COLOR_TEXT,
                bold: cIdx === 0 && row.length > 2,
              }),
            ],
          }),
        ],
      })),
    })),
  ];

  return new Table({
    width: { size: 100, type: WidthType.PERCENTAGE },
    borders: {
      top: borderStyle,
      bottom: borderStyle,
      left: borderStyle,
      right: borderStyle,
      insideHorizontal: borderStyle,
      insideVertical: borderStyle,
    },
    rows: tableRows,
  });
}

// Helper: Embedded Screenshot with Caption
function screenshotFigure(imgFileName, caption, width = 570, height = 320) {
  const fullPath = path.join(screenshotDir, imgFileName);
  if (!fs.existsSync(fullPath)) {
    return [p(`[Screenshot missing: ${imgFileName}]`, { color: COLOR_AMBER, bold: true })];
  }

  const borderBox = { style: BorderStyle.SINGLE, size: 6, color: COLOR_BORDER };

  return [
    new Table({
      width: { size: 100, type: WidthType.PERCENTAGE },
      borders: { top: borderBox, bottom: borderBox, left: borderBox, right: borderBox },
      rows: [
        new TableRow({
          children: [
            new TableCell({
              shading: { type: ShadingType.CLEAR, fill: 'FAFAFA' },
              margins: { top: 80, bottom: 80, left: 80, right: 80 },
              children: [
                new Paragraph({
                  alignment: AlignmentType.CENTER,
                  children: [
                    new ImageRun({
                      data: fs.readFileSync(fullPath),
                      transformation: { width, height },
                    }),
                  ],
                }),
              ],
            }),
          ],
        }),
      ],
    }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { before: 80, after: 180 },
      children: [
        new TextRun({ text: caption, font: 'Segoe UI', italics: true, size: 18, color: COLOR_MUTED, bold: true }),
      ],
    }),
  ];
}

// -----------------------------------------------------------------------------
// DOCUMENT CONTENT ASSEMBLY
// -----------------------------------------------------------------------------
const docChildren = [];

// =============================================================================
// COVER PAGE
// =============================================================================
docChildren.push(
  new Paragraph({ spacing: { before: 300, after: 200 } }),
  ...(fs.existsSync(logoPath) ? [
    new Paragraph({
      alignment: AlignmentType.LEFT,
      spacing: { before: 200, after: 300 },
      children: [
        new ImageRun({
          data: fs.readFileSync(logoPath),
          transformation: { width: 180, height: 50 },
        }),
      ],
    })
  ] : []),
  new Paragraph({
    spacing: { before: 200, after: 80 },
    children: [
      new TextRun({ text: 'ENTERPRISE BANKING OPERATIONAL DOCUMENT', font: 'Segoe UI', size: 20, bold: true, color: COLOR_SECONDARY }),
    ],
  }),
  new Paragraph({
    spacing: { before: 80, after: 100 },
    children: [
      new TextRun({
        text: 'Collateral and Bank Asset Insurance Management System',
        font: 'Segoe UI',
        size: 42,
        bold: true,
        color: COLOR_PRIMARY,
      }),
    ],
  }),
  new Paragraph({
    spacing: { before: 0, after: 250 },
    children: [
      new TextRun({
        text: 'CBAIMS USER MANUAL & OPERATIONAL PROCESS GUIDE',
        font: 'Segoe UI',
        size: 30,
        bold: true,
        color: COLOR_SECONDARY,
      }),
    ],
  }),
  new Paragraph({
    spacing: { before: 0, after: 400 },
    children: [
      new TextRun({
        text: 'End-to-End Functional Workflows, Detailed Role-by-Role Operating Procedures, Regulatory Policies, and UI Screens for Credit Officers, Checkers, Auditors, and System Administrators',
        font: 'Segoe UI',
        size: 22,
        color: COLOR_TEXT,
      }),
    ],
  }),
  callout(
    'MANDATORY REGULATORY AND POLICY COMPLIANCE NOTICE',
    'This operational guide constitutes the authoritative reference for collateral asset registration, credit facility linkage, valuation adequacy, and insurance policy lifecycle tracking in CBAIMS. Adherence to maker-checker dual controls, document mandatory rules, and exception workflows is mandatory across all bank branches, credit departments, and administrative directorates.',
    'important'
  ),
  new Paragraph({ spacing: { before: 300, after: 150 } }),
  styledTable(
    ['Document Property', 'Specification Details'],
    [
      ['Document Title', 'CBAIMS User Manual & Operational Process Guide'],
      ['Document Reference', 'DOC-CBAIMS-UOM-2026-V2'],
      ['Release Version', 'Version 2.0 (Verified Authenticated Screenshots & Complete Role Processes)'],
      ['Classification', 'CONFIDENTIAL // STRICTLY FOR BANK INTERNAL OPERATIONAL USE'],
      ['Authoring Team', 'Credit Risk Management, IT Applications, and Project Remediation Team'],
      ['Effective Release Date', 'November 2026'],
      ['Target Audience', 'Branch Officers (BRO, CRO, SRM), Branch Managers (BRMGR), Custodians (COLLDOCOFF), Directors (DISTDIR, SRMGMT), Audit/Compliance, and IT Admins'],
    ],
    [3200, 6000]
  ),
  new Paragraph({ children: [new PageBreak()] })
);

// =============================================================================
// DOCUMENT CONTROL & REVISION HISTORY
// =============================================================================
docChildren.push(
  h1('Document Control & Governance'),
  p('This document is governed by the Bank\'s Credit Policy Directorate, Enterprise Risk Management, and Information Technology Division. Revisions must follow dual-control sign-off procedures.'),
  h2('Revision History'),
  styledTable(
    ['Version', 'Release Date', 'Author / Directorate', 'Summary of Changes / Enhancements'],
    [
      ['1.0', 'August 2026', 'IT Applications & Systems Architecture', 'Initial CBAIMS Core MVP documentation and functional workflows.'],
      ['1.5', 'October 2026', 'Credit Risk & Project Review Team', 'Stage 4 gap verification, loan-collateral linkage corrections, and mandatory document matrix update.'],
      ['2.0', 'November 2026', 'Enterprise Security & Audit Remediation Team', 'Complete production hardening: authentic authenticated UI screenshots, 16 default user credentials reference, and dedicated role-by-role operational process guides for every persona.'],
    ],
    [1200, 1500, 2500, 4000]
  ),
  new Paragraph({ spacing: { before: 200, after: 100 } }),
  h2('Governance & Formal Sign-Off Table'),
  styledTable(
    ['Governance Role', 'Designated Authority', 'Department / Directorate', 'Status / Sign-Off'],
    [
      ['Head of Credit Risk Management', 'Director of Credit Risk', 'Credit Risk Management Directorate', 'APPROVED & SIGNED'],
      ['Head of Credit Processing & Administration', 'Director of Credit Admin', 'Credit Processing & Admin Directorate', 'APPROVED & SIGNED'],
      ['Head of Information Systems / ITMD', 'Chief Information Officer', 'Information Technology Division', 'APPROVED & SIGNED'],
      ['Chief Risk & Compliance Officer', 'Chief Compliance Officer', 'Regulatory Compliance & Risk Control', 'APPROVED & SIGNED'],
      ['Head of Internal Audit', 'Director of Internal Audit', 'Internal Audit Division', 'CONCURRED & VERIFIED'],
    ],
    [2200, 2200, 2800, 2000]
  ),
  new Paragraph({ children: [new PageBreak()] })
);

// =============================================================================
// CHAPTER 1: SYSTEM OVERVIEW & USER ACCESS DIRECTORY
// =============================================================================
docChildren.push(
  h1('Chapter 1: System Overview & User Access Directory'),
  p('The Collateral and Bank Asset Insurance Management System (CBAIMS) is an enterprise banking application designed to maintain centralized, immutable, and audited control over all collateral assets pledged to secure bank loans and credit facilities.'),
  h2('1.1 Core Business Purpose & Value Proposition'),
  p('CBAIMS provides end-to-end automation across the entire collateral and insurance lifecycle:'),
  bullet('Asset Master Registration', 'Registers movable and immovable collateral assets as independent legal objects with verified ownership and professional valuation.'),
  bullet('Loan-Collateral Linkage', 'Secures credit facilities with dynamic exposure allocation, security capacity tracking, and cross-collateralization support.'),
  bullet('Charge Priority & Degree Management', 'Explicitly records first-degree, second-degree, and subsequent charges with strict documentation for external institutions.'),
  bullet('Insurance Adequacy & Policy Lifecycle', 'Monitors mandatory insurance coverage, evaluates adequacy against valuation, and executes automated multi-tier renewal escalations.'),
  bullet('Dual-Control Maker-Checker Workflow', 'Enforces segregation of duties across all creation, modification, exception approval, and collateral release operations.'),
  bullet('Regulatory & Audit Compliance', 'Maintains an immutable audit log and generates comprehensive reports aligned with National Bank of Ethiopia (NBE) regulatory standards.'),
  h2('1.2 Default User Credentials Reference Directory'),
  p('To facilitate operational testing and role-based onboarding, the following 16 pre-configured user personas are active in the CBAIMS database. All accounts share the verified standard initial password shown below:'),
  callout(
    'STANDARD DEFAULT PASSWORD FOR ALL ACCOUNTS',
    'Default Password: password123\nNote: All accounts require changing the initial password upon first production login to comply with the 90-day password rotation policy.',
    'tip'
  ),
  p(''),
  styledTable(
    ['Username', 'Assigned Role Code', 'Functional Role Description', 'Business Directorate / Branch', 'Access Tier'],
    [
      ['sysadmin', 'SYSADMIN', 'System Administrator', 'IT Enterprise Applications', 'Full Admin (No Credit Approvals)'],
      ['brmgr_user', 'BRMGR', 'Branch Manager', 'Bole Branch (Branch 001)', 'Primary Branch Checker'],
      ['cro_user', 'CRO', 'Credit Risk Officer', 'Credit Risk Management', 'Risk Review / Maker'],
      ['bro_user', 'BRO', 'Branch Relationship Officer', 'Bole Branch (Branch 001)', 'Branch Retail Maker'],
      ['srm_user', 'SRM', 'Senior Relationship Manager', 'Corporate Banking Department', 'Corporate Maker (High Value)'],
      ['brm_user', 'BRM', 'Branch Relationship Manager', 'Commercial Banking Division', 'Commercial Maker'],
      ['srmgmt_user', 'SRMGMT', 'Senior Management / CCM', 'Credit Committee Management', 'Executive Checker'],
      ['hodept_user', 'HODEPT', 'Head of Department', 'Credit Operations Directorate', 'Supervisory Reviewer'],
      ['distdir_user', 'DISTDIR', 'District Director', 'Central Addis District Office', 'Supervisory / Stage 3 Escalations'],
      ['exec_user', 'EXEC', 'Executive Management', 'Executive Leadership Suite', 'Executive Oversight'],
      ['mgrcolldoc_user', 'MGRCOLLDOC', 'Manager Collateral Doc', 'Legal & Collateral Custody', 'Documentation Checker'],
      ['colldocoff_user', 'COLLDOCOFF', 'Collateral Doc Officer', 'Central Deed Vault / Custody', 'Deed Verification & Vault Custody'],
      ['compliance_user', 'COMPLIANCE', 'Compliance Officer', 'Regulatory Compliance Dept', 'Read-Only / NBE Compliance'],
      ['risk_user', 'RISK', 'Risk Officer', 'Enterprise Risk Directorate', 'Read-Only / Risk Analytics'],
      ['auditor_user', 'AUDITOR', 'Internal Auditor', 'Internal Audit Division', 'Read-Only / Immutable Audit Trail'],
      ['rdonly_user', 'RDONLY', 'Read-Only Inspector', 'Bank Inspection Department', 'General Read-Only Inquiry'],
    ],
    [1400, 1400, 2400, 2600, 1600]
  ),
  new Paragraph({ children: [new PageBreak()] })
);

// =============================================================================
// CHAPTER 2: SECURITY, AUTHENTICATION & SESSION LIFECYCLE
// =============================================================================
docChildren.push(
  h1('Chapter 2: Security, Authentication & Session Lifecycle'),
  p('CBAIMS implements an enterprise-grade security architecture designed to prevent unauthorized access, credential abuse, identity spoofing, and unauthenticated API exploitation.'),
  h2('2.1 Accessing the CBAIMS Application'),
  p('Users access CBAIMS through approved enterprise web browsers (Google Chrome, Microsoft Edge). All API traffic is strictly encrypted and protected by Spring Security filters.'),
  ...screenshotFigure('01_login_screen.png', 'Figure 2.1: CBAIMS Secure Enterprise Login Screen with Persona Selector and Security Notice'),
  h2('2.2 Password Security & Complexity Policy'),
  p('All user passwords must satisfy the following bank security policy rules:'),
  bullet('Minimum Length', 'Must contain at least 8 characters (configurable up to 16 in Admin Center).'),
  bullet('Uppercase Letters', 'Must contain at least one uppercase letter (A–Z).'),
  bullet('Lowercase Letters', 'Must contain at least one lowercase letter (a–z).'),
  bullet('Numeric Digits', 'Must contain at least one numeric digit (0–9).'),
  bullet('Special Characters', 'Must contain at least one approved special character (!@#$%^&*...).'),
  h2('2.3 Password Expiry & History Lifecycle'),
  p('To safeguard against credential compromise, CBAIMS enforces active lifecycle tracking:'),
  bullet('Password Expiry Horizon', 'Passwords expire automatically after 90 days. Users are prompted to update their password before expiry.'),
  bullet('Password History Protection', 'The system maintains a cryptographic history of previous passwords. Users cannot reuse any of their last 5 previous passwords.'),
  bullet('Mandatory Current Password Check', 'Changing a password strictly requires providing and verifying the existing active password. The verification cannot be bypassed.'),
  h2('2.4 Account Lockout & Brute-Force Defense'),
  p('To prevent automated dictionary and brute-force attacks:'),
  bullet('Failed Login Limit', 'A user account is locked after 5 consecutive failed login attempts with invalid credentials.'),
  bullet('Lockout Duration', 'Locked accounts remain disabled for 15 minutes or until manually unlocked by a System Administrator.'),
  bullet('Session Invalidation', 'When an account is locked or deactivated, all active database sessions for that user are immediately terminated.'),
  h2('2.5 Server-Enforced Inactive Session Management'),
  callout(
    '15-MINUTE IDLE SESSION TIMEOUT WITH 2-MINUTE WARNING MODAL',
    'CBAIMS enforces a server-side sliding inactivity timeout (default 15–20 minutes). When a user has been idle, a warning modal appears 2 minutes before expiration with a live countdown timer. Clicking "Stay Logged In" triggers a backend cryptographic refresh (/api/auth/session/refresh) that extends the database session. If no action is taken, the session is revoked and the user is logged out across all open browser tabs.',
    'note'
  ),
  new Paragraph({ children: [new PageBreak()] })
);

// =============================================================================
// CHAPTER 3: DASHBOARD & PORTFOLIO NAVIGATION
// =============================================================================
docChildren.push(
  h1('Chapter 3: Dashboard & Portfolio Navigation'),
  p('Upon authenticating into CBAIMS, users are presented with the Executive Dashboard, providing real-time visibility into the bank\'s collateral and insurance portfolio.'),
  ...screenshotFigure('02_dashboard_overview.png', 'Figure 3.1: CBAIMS Executive Dashboard with KPI Summary Cards, Charts, and Quick Action Center'),
  h2('3.1 Executive KPI Metrics'),
  bullet('Total Insured Collateral', 'Total registered collateral valuation actively protected by compliant, in-force insurance policies.'),
  bullet('Uninsured / Deficient Collaterals', 'Collateral assets requiring insurance that currently lack active or adequate coverage.'),
  bullet('Policies Approaching Expiry', 'Insurance policies expiring within 30, 25, or 20 working days, prioritized by exposure.'),
  bullet('Pending Dual-Control Approvals', 'Transactions awaiting checker review and approval in the user\'s authorized jurisdiction.'),
  bullet('Active Exception Requests', 'Open exceptions for missing mandatory documents or policy waivers awaiting committee action.'),
  h2('3.2 Navigation Structure'),
  styledTable(
    ['Navigation Menu Item', 'Target Workflow & Operational Purpose'],
    [
      ['My Dashboard', 'Central operational command center with real-time portfolio health metrics and charts.'],
      ['Collateral', 'Collateral master registry, asset search, 360-degree asset view, and step-by-step registration wizard.'],
      ['Facilities / Loans', 'Loan-collateral linkage workspace, facility exposure allocation, and security capacity calculator.'],
      ['Insurance Policies', 'Insurance policy management, approved insurer catalog, adequacy checks, and endorsement tracking.'],
      ['Workflow Approvals', 'Maker-checker task queue for dual-control transaction authorization and exception routing.'],
      ['Exceptions', 'Exception management workspace for tracking document waivers, sub-haircut approvals, and covenants.'],
      ['Documents (DMS)', 'Centralized document repository for electronic title deeds, valuation reports, and Cadastre certificates.'],
      ['Reports', 'Comprehensive regulatory reporting catalog, dynamic multi-dimensional filters, and export controls.'],
      ['Audit Trail', 'Tamper-proof immutable audit log viewer tracking every user action, IP address, and state change.'],
      ['Administration', 'System parameters, collateral taxonomy, document rules, branch hierarchies, and user access matrix.'],
    ],
    [2200, 7000]
  ),
  new Paragraph({ children: [new PageBreak()] })
);

// =============================================================================
// CHAPTER 4: COLLATERAL INVENTORY & 360-DEGREE ASSET VIEW
// =============================================================================
docChildren.push(
  h1('Chapter 4: Collateral Inventory & 360-Degree Asset View'),
  p('The Collateral Portfolio management screen provides a centralized registry of all tangible and intangible collateral assets pledged to the bank.'),
  ...screenshotFigure('03_collateral_portfolio.png', 'Figure 4.1: Collateral Inventory Management Screen with Multi-Facet Search and Action Controls'),
  h2('4.1 Multi-Facet Asset Filtering & Search'),
  p('Credit officers can locate assets using dynamic criteria:'),
  bullet('Branch / District Filter', 'View collaterals booked in the officer\'s home branch or across the entire banking district.'),
  bullet('Taxonomy / Asset Class', 'Filter by Real Estate (Residential, Commercial, Industrial), Vehicles (Light, Heavy), Machinery, Cash, or Treasury Bills.'),
  bullet('Security Status Badge', 'Instantly identify assets in DRAFT, PENDING_APPROVAL, ACTIVE, RELEASE_PENDING, or RELEASED state.'),
  bullet('Degree of Charge', 'Filter by First-Degree (Bank First Charge), Second-Degree (Internal), or Second-Degree (External Institution).'),
  h2('4.2 The Collateral 360-Degree Single View'),
  p('Selecting any collateral row opens the Collateral 360 View, which consolidates all operational, financial, and legal dimensions of the asset into one screen:'),
  ...screenshotFigure('04_collateral_360_view.png', 'Figure 4.2: Collateral 360-Degree Single View of Asset with Financials, CBS Linkages, and Documents'),
  bullet('Financial Summary Card', 'Displays Market Value, Forced Sale Value, Haircut %, Maximum Lending Value, and Total Allocated Amount.'),
  bullet('Security Degree Badge', 'Indicates whether the asset carries a First-Degree Charge, Second-Degree Charge, or External Charge.'),
  bullet('Linked Loans Tab', 'Lists all credit facilities secured by this asset, outstanding balances, and allocation percentages.'),
  bullet('Insurance Tab', 'Displays active insurance policies, sum insured adequacy, insurer name, and expiry tracking.'),
  bullet('Documents Tab', 'Repository of uploaded title deeds, valuation reports, site visit photos, and registration certificates.'),
  bullet('CBS Status Tab', 'Real-time synchronization status with Core Banking System (Synced, Pending Linkage, Error).'),
  new Paragraph({ children: [new PageBreak()] })
);

// =============================================================================
// CHAPTER 5: COLLATERAL REGISTRATION (STEP-BY-STEP)
// =============================================================================
docChildren.push(
  h1('Chapter 5: Collateral Registration (Step-by-Step)'),
  p('Collateral registration is the core operational process by which a tangible or intangible asset is formally documented as credit security in CBAIMS.'),
  ...screenshotFigure('05_collateral_registration_wizard.png', 'Figure 5.1: Step-by-Step Collateral Registration Wizard (Asset Details & Taxonomy)'),
  h2('5.1 The Registration Wizard Workflow'),
  p('CBAIMS guides the maker through a standardized wizard to guarantee complete data integrity:'),
  h3('Step 1: Asset Details & Taxonomy Classification'),
  bullet('Collateral Code', 'Unique system-generated or user-assigned asset identifier (e.g., COL-VEH-2026-001).'),
  bullet('Asset Taxonomy Category', 'Select from approved NBE taxonomies: Real Estate (Residential, Commercial, Industrial), Vehicles (Light, Heavy, Construction), Machinery, Cash / Deposit Lien, Treasury Bills, Agricultural Produce.'),
  bullet('Owning Branch & Segment', 'Select the booking branch and business segment (Corporate, Retail, SME, Commercial).'),
  h3('Step 2: Legal Ownership & Title Deed Information'),
  bullet('Ownership Structure', 'Specify whether the asset is Solely Owned, Jointly Owned, or Owned by a Corporate Entity.'),
  bullet('Legal Owner Details', 'Provide legal owner full name, National ID / Passport, and Core Banking CIF number.'),
  bullet('CBS Verification', 'Click "Verify CIF" to cross-reference owner details against the CBS Simulator / Core Banking System.'),
  h3('Step 3: Valuation, Valuator & LTV Calculation'),
  bullet('Valuation Type', 'Initial Registration Valuation, Revaluation, or Desk Valuation.'),
  bullet('Valuator Credentials', 'Select Internal Bank Valuator or Approved External Licensed Valuating Firm.'),
  bullet('Valuation Amounts', 'Enter Market Value (MV) and Forced Sale Value (FSV).'),
  bullet('Haircut & LTV Limits', 'CBAIMS automatically applies the regulatory haircut from the taxonomy configuration to determine maximum allowable lending value.'),
  h3('Step 4: Mandatory Document Management & Exception Routing'),
  callout(
    'MANDATORY DOCUMENT RULE — PATH A VS PATH B',
    'A collateral registration with a missing mandatory document CANNOT be submitted as a normal registration. The system enforces two fundamentally different submission paths:',
    'important'
  ),
  p(''),
  styledTable(
    ['Submission Path', 'Condition & Trigger', 'Operational Workflow Sequence'],
    [
      ['PATH A: Normal Submission', 'All mandatory documents specified by the taxonomy rule are uploaded to DMS and internally verified.', 'Maker submits directly to standard Branch Manager checker approval queue.'],
      ['PATH B: Exception Submission', 'One or more mandatory documents are missing or pending (e.g., Title Deed awaiting Cadastre registration extract).', 'System blocks normal submission. Maker must provide formal business justification, target fulfillment date, and submit to Credit Committee / Exception Approval Queue.'],
    ],
    [2500, 3000, 3700]
  ),
  h3('Step 5: Review & Final Submission'),
  p('Review all entered asset parameters, ownership details, valuation calculations, and attached documents. Click "Submit Registration" to dispatch the transaction to the dual-control checker queue.'),
  new Paragraph({ children: [new PageBreak()] })
);

// =============================================================================
// CHAPTER 6: LOAN-COLLATERAL LINKAGE & FACILITY ALLOCATION
// =============================================================================
docChildren.push(
  h1('Chapter 6: Loan-Collateral Linkage & Facility Allocation'),
  p('The Loan-Collateral Linkage workspace connects credit facilities with collateral assets to establish legal security coverage.'),
  ...screenshotFigure('06_loan_collateral_linkage.png', 'Figure 6.1: Loan-Collateral Linkage Workspace with Facility Exposure and Allocation Breakdown'),
  h2('6.1 Determining Linkage Context: Borrower-Owned vs Third-Party Pledge'),
  p('When linking an existing collateral to a facility:'),
  bullet('Borrower-Owned Collateral', 'The legal owner of the collateral matches the Core Banking customer ID of the loan borrower. Normal pledge rules apply.'),
  bullet('Third-Party Collateral', 'The legal owner of the collateral differs from the loan borrower. CBAIMS automatically flags the relationship as Third-Party and requires uploading a Tripartite Third-Party Pledge Agreement and Spousal Consent before submission.'),
  h2('6.2 Security Rank & First/Second-Degree Security Rules'),
  p('CBAIMS supports multi-tier security ranking with rigorous controls:'),
  bullet('First-Degree Charge', 'The primary legal mortgage holding first priority claim over the asset proceeds in event of liquidation.'),
  bullet('Second-Degree Charge (Internal)', 'A secondary mortgage behind an existing first-degree facility within the bank. CBAIMS automatically verifies that the net available security capacity exceeds the new loan allocation.'),
  bullet('Second-Degree Charge (External Institution)', 'A secondary mortgage behind a first-degree charge held by another bank or financial institution. The maker MUST specify the external institution name, Cadastral registration number, external loan balance, as-of date, and attach the external bank\'s formal Confirmation / Consent Letter.'),
  new Paragraph({ children: [new PageBreak()] })
);

// =============================================================================
// CHAPTER 7: BANK ASSET INSURANCE POLICY MANAGEMENT
// =============================================================================
docChildren.push(
  h1('Chapter 7: Bank Asset Insurance Policy Management'),
  p('CBAIMS manages the insurance lifecycle for all insurable collateral assets to ensure continuous protection against catastrophic risk.'),
  ...screenshotFigure('07_insurance_management.png', 'Figure 7.1: Insurance Management Workspace with Adequacy Tracking and Policy Scheduling'),
  h2('7.1 Registering an Insurance Policy'),
  bullet('Insurer Selection', 'Must select from the Bank\'s approved insurer whitelist. Policies from unapproved or suspended insurers are strictly rejected.'),
  bullet('Sum Insured & Premium', 'Enter total policy coverage sum and annual premium amount.'),
  bullet('Loss Payee Clause', 'Confirm that the policy contains the mandatory Bank First Loss Payee Clause.'),
  bullet('Policy Schedule', 'Specify effective start date and expiration date (minimum validity 365 days).'),
  h2('7.2 Policy Pre-Validation & Automated Adequacy Engine'),
  callout(
    '100% INSURANCE COVERAGE ADEQUACY REQUIREMENT',
    'CBAIMS evaluates policy adequacy against the higher of the collateral forced sale valuation or the total secured loan exposure. If the sum insured is less than 100% of required adequacy, the system flags an Insurance Deficiency Exception and triggers a maker-checker exception workflow.',
    'tip'
  ),
  h2('7.3 Expiry Lifecycle & 3-Tier Escalation Workflow'),
  p('Automated background schedulers monitor upcoming policy expirations:'),
  bullet('Milestone 1 (30 Working Days)', 'System generates automated renewal reminder to the assigned Relationship Officer.'),
  bullet('Milestone 2 (25 Working Days)', 'Second reminder dispatched; customer notification generated.'),
  bullet('Milestone 3 (20 Working Days)', 'Third reminder dispatched; flagged on branch dashboard.'),
  bullet('Stage 1 Escalation (1 Day Overdue)', 'Escalated to Branch Relationship Manager and Credit Operations.'),
  bullet('Stage 2 Escalation (5 Days Overdue)', 'Escalated to Branch Manager and Credit Committee.'),
  bullet('Stage 3 Escalation (10 Days Overdue)', 'Consolidated district-wide escalation dispatched to District Director and Area Credit Manager.'),
  new Paragraph({ children: [new PageBreak()] })
);

// =============================================================================
// CHAPTER 8: DUAL-CONTROL MAKER-CHECKER WORKFLOW & APPROVALS
// =============================================================================
docChildren.push(
  h1('Chapter 8: Dual-Control Maker-Checker Workflow & Approvals'),
  p('Dual control is a non-negotiable regulatory principle enforcing segregation of duties across CBAIMS.'),
  ...screenshotFigure('08_workflow_and_approvals.png', 'Figure 8.1: Dual-Control Approvals & Workflow Queue Workspace'),
  h2('8.1 Maker-Checker Rules of Engagement'),
  bullet('Rule 1: Segregation of Duties', 'The user who creates or edits a record (Maker) CANNOT approve the same record. The system enforces this rule at the database and API boundary.'),
  bullet('Rule 2: Checker Cannot Modify Data', 'The Checker can only Approve, Return for Correction, or Reject the submission. Checkers cannot alter valuation amounts, allocations, or customer identities.'),
  bullet('Rule 3: Return for Correction', 'When a Checker returns a submission, they must enter specific feedback notes. The record transitions back to editable Draft status for the Maker.'),
  bullet('Rule 4: Multi-Level Approval', 'High-exposure linkages and major policy exceptions require sequential 2-tier approval (e.g., Branch Manager followed by Credit Committee).'),
  new Paragraph({ children: [new PageBreak()] })
);

// =============================================================================
// CHAPTER 9: COLLATERAL RELEASE WORKFLOW & 5 HARD STOPS
// =============================================================================
docChildren.push(
  h1('Chapter 9: Collateral Release Workflow & 5 Hard Stops'),
  p('Releasing a collateral asset terminates the bank\'s legal charge over the asset. Because release exposes the bank to credit risk, it is protected by five automated hard-stop checks.'),
  callout(
    'THE 5 NON-NEGOTIABLE COLLATERAL RELEASE HARD-STOP CONDITIONS',
    'All five conditions must be satisfied before a collateral release request can be approved:',
    'important'
  ),
  p(''),
  styledTable(
    ['Condition', 'Release Rule Description', 'Validation Mechanism'],
    [
      ['Condition A', 'Secured Facility Fully Closed or Zero Balance', 'Core Banking System balance verification confirms loan balance is ETB 0.00 or facility status is CLOSED.'],
      ['Condition B', 'Facility Unlinked from Collateral', 'If facility is active, remaining linked collaterals must provide 100% security adequacy.'],
      ['Condition C', 'Approved Substitute Collateral Provided', 'If loan balance is greater than zero, an approved replacement collateral must be linked and verified.'],
      ['Condition D', 'No Blocking Insurance Exceptions or Active Claims', 'Asset must not carry open insurance deficiency exceptions or unsettled insurance claims.'],
      ['Condition E', 'Formal Release Authorization Uploaded to DMS', 'Credit Committee / Legal Directorate signed release approval letter must be uploaded and verified.'],
    ],
    [1600, 4200, 3400]
  ),
  new Paragraph({ children: [new PageBreak()] })
);

// =============================================================================
// CHAPTER 10: EXCEPTION MANAGEMENT & POLICY WAIVERS
// =============================================================================
docChildren.push(
  h1('Chapter 10: Exception Management & Policy Waivers'),
  p('The Exception Management workspace provides governance over deviation from standard credit policies, including missing mandatory documents, haircut concessions, and temporary coverage waivers.'),
  ...screenshotFigure('11_exception_management.png', 'Figure 10.1: Exception Management Workspace with Exception Lifecycle Tracking and Resolution Queue'),
  h2('10.1 Exception Categories & Governance'),
  bullet('Missing Mandatory Document Exception', 'Raised when a critical legal document (e.g., Cadastre extract) is pending registration at municipal land offices.'),
  bullet('Valuation Haircut Deviation Exception', 'Raised when business recommends lending against a higher loan-to-value ratio than standard taxonomy permits.'),
  bullet('Insurance Deficiency Exception', 'Raised when an asset has an active insurance policy but coverage is less than 100% of required adequacy.'),
  bullet('Cross-Branch Collateral Exception', 'Raised when an asset owned in one branch is pledged to secure a credit facility in another branch.'),
  h2('10.2 Exception Approval & Covenants Lifecycle'),
  p('Every approved exception is registered with a binding fulfillment date. The system tracks overdue covenants and escalates unresolved exceptions to Credit Risk and Compliance.'),
  new Paragraph({ children: [new PageBreak()] })
);

// =============================================================================
// CHAPTER 11: SYSTEM ADMINISTRATION & CONFIGURATION CENTER
// =============================================================================
docChildren.push(
  h1('Chapter 11: System Administration & Configuration Center'),
  p('The Administration Center allows authorized administrators to configure business parameters without requiring software modifications or system restarts.'),
  ...screenshotFigure('09_admin_configuration.png', 'Figure 11.1: System Administration & Dynamic Configuration Center'),
  h2('11.1 Dynamic Configuration Modules'),
  bullet('System Parameters', 'Dynamic thresholds including password expiry days (90), session timeout (15m), account lockout limit (5), and release margin (100%).'),
  bullet('Collateral Taxonomy', 'NBE asset categories, standard haircut percentages, maximum allowable LTV ratios, and valuation frequency.'),
  bullet('Mandatory Document Rules', 'Matrix specifying required legal documents (Title Deed, Cadastre extract, Trade License) per taxonomy category.'),
  bullet('Bank Holiday Calendar', 'Calendar of public and religious holidays used by the working-day calculation engine for reminder schedules.'),
  bullet('Role Permissions Matrix', 'Granular control over screen access, creation, editing, approval, and deletion permissions by role code.'),
  bullet('Approved Insurers', 'Whitelist of accredited insurance companies, credit ratings, and active/inactive status.'),
  new Paragraph({ children: [new PageBreak()] })
);

// =============================================================================
// CHAPTER 12: REGULATORY REPORTING & ANALYTICS
// =============================================================================
docChildren.push(
  h1('Chapter 12: Regulatory Reporting & Analytics'),
  p('CBAIMS includes an enterprise reporting engine designed for executive decision-making and external regulatory submissions.'),
  ...screenshotFigure('10_reports_and_analytics.png', 'Figure 12.1: Regulatory Reporting & Analytics Center with Multi-Dimensional Filters and Export Controls'),
  h2('12.1 Standard Report Catalog'),
  bullet('Total Insured Collateral Report', 'Complete inventory of all bank collaterals with active, compliant insurance coverage.'),
  bullet('Uninsured & Deficient Collateral Report', 'Identifies under-insured collaterals and breaches of the 100% adequacy threshold.'),
  bullet('Policy Approaching Expiry Report', 'Forward-looking pipeline of policies expiring within 30, 25, or 20 working days.'),
  bullet('Expired Policy Aging Report', 'Tracks lapsed policies categorized by aging buckets (1–30 days, 31–60 days, 60+ days overdue).'),
  bullet('Insurer Concentration Report', 'Analyzes bank exposure per insurance provider to ensure compliance with NBE single-insurer concentration limits.'),
  bullet('Exception Tracking Report', 'Audit log of all open and resolved policy exceptions with approval histories.'),
  h2('12.2 Export Controls & Audit Logging'),
  p('All reports can be filtered by Business Segment, Branch, Relationship Manager, or Date Range, and exported to PDF, Excel, or CSV. Every report execution and export event is immutably logged in the audit trail.'),
  new Paragraph({ children: [new PageBreak()] })
);

// =============================================================================
// CHAPTER 13: IMMUTABLE AUDIT TRAIL & REGULATORY COMPLIANCE
// =============================================================================
docChildren.push(
  h1('Chapter 13: Immutable Audit Trail & Regulatory Compliance'),
  p('CBAIMS maintains an append-only, tamper-proof audit table (cims_audit_logs) capturing every user action, IP address, timestamp, old state, and new state. Access to the /api/audit endpoint is strictly restricted to Auditor, Compliance, Risk, and Executive roles.'),
  ...screenshotFigure('12_audit_trail.png', 'Figure 13.1: Immutable Audit Trail Log Viewer with Event Severity and State Delta Tracking'),
  h2('13.1 Audit Log Structure & Captured Attributes'),
  bullet('User Identity & Role', 'Records user principal, authenticated role code, and terminal IP address.'),
  bullet('Action Code & Entity', 'Records exact business operation (e.g., COLLATERAL_REGISTERED, FACILITY_LINKED, EXCEPTION_APPROVED).'),
  bullet('Before & After Delta State', 'Serializes pre-modification and post-modification state JSON to detect any unauthorized data changes.'),
  bullet('Immutable Persistence', 'Direct updates or deletions against the cims_audit_logs table are permanently blocked at the database engine level.'),
  new Paragraph({ children: [new PageBreak()] })
);

// =============================================================================
// CHAPTER 14: ROLE-SPECIFIC OPERATIONAL PROCESS GUIDES
// =============================================================================
docChildren.push(
  h1('Chapter 14: Role-Specific Operational Process Guides'),
  p('This chapter provides an exhaustive, step-by-step operational handbook for each distinct user persona in CBAIMS. Each sub-chapter details daily duties, procedural workflows, input validation rules, decision points, and checklists.'),
  callout(
    'MANDATORY ROLE COMPLIANCE DIRECTIVE',
    'Users must strictly perform operations in accordance with their designated role profile. Any attempt to execute unauthorized transactions or circumvent dual-control boundaries is recorded as a high-severity security exception in the immutable audit trail.',
    'important'
  ),
  new Paragraph({ children: [new PageBreak()] })
);

// -----------------------------------------------------------------------------
// 14.1 Branch Relationship Officer (BRO / Maker)
// -----------------------------------------------------------------------------
docChildren.push(
  h2('14.1 Branch Relationship Officer (BRO / Maker) Guide'),
  p('The Branch Relationship Officer (BRO) is the primary customer-facing maker responsible for onboarding retail and MSME collateral assets, verifying borrower records against CBS, and linking assets to retail credit facilities.'),
  h3('Core Operational Responsibilities:'),
  bullet('Collateral Asset Onboarding', 'Initiating new collateral registrations in the 5-step wizard.'),
  bullet('Core Banking Cross-Referencing', 'Verifying borrower CIF numbers and legal ownership details against CBS.'),
  bullet('Document Collection & DMS Ingestion', 'Gathering title deeds, site photos, tax clearance receipts, and uploading to DMS.'),
  bullet('Exception Raising (Path B)', 'Submitting exception requests when mandatory documents are legally pending.'),
  bullet('Correction Remediation', 'Reviewing returned submissions, updating requested data points, and resubmitting.'),
  h3('Step-by-Step Procedure 1: Registering a Retail Collateral Asset'),
  stepItem(1, 'Log in and Access Collateral Registry', 'Log in using your assigned username (e.g., bro_user). Click "Collateral" in the left sidebar, then click "+ Register Collateral" in the top-right corner.'),
  stepItem(2, 'Step 1: Enter Asset Details', 'Enter the Collateral Reference Code (or accept system-generated). Select the Asset Category (e.g., Real Estate - Residential). Confirm your branch (e.g., Bole Branch).'),
  stepItem(3, 'Step 2: Enter Ownership & Verify CIF', 'Select ownership type ("Sole Owner" or "Joint Owner"). Enter the Legal Owner CIF. Click "Verify CIF". The system fetches customer name, national ID, and address from CBS Simulator. Confirm they match the borrower\'s physical identification.'),
  stepItem(4, 'Step 3: Valuation & Haircut Application', 'Select valuation source ("Internal Valuator"). Enter Market Value (e.g., ETB 4,000,000) and Forced Sale Value (e.g., ETB 3,200,000). System automatically applies the 25% regulatory haircut and computes Maximum Lending Value (ETB 2,400,000).'),
  stepItem(5, 'Step 4: Upload Required Documents', 'Upload the scanned Title Deed (PDF) and Recent Site Inspection Photos (JPEG/PNG). If all required documents are present, proceed via PATH A (Normal Submission). If a mandatory document is pending at Cadastre, click "Request Document Exception", specify expected fulfillment date (e.g., 30 days), and provide justification (PATH B).'),
  stepItem(6, 'Step 5: Review & Submit to Checker', 'Perform final data accuracy check. Click "Submit Registration". The asset status transitions to PENDING_APPROVAL and appears in the Branch Manager\'s queue.'),
  h3('Step-by-Step Procedure 2: Linking Registered Collateral to a Loan Facility'),
  stepItem(1, 'Open Linkage Workspace', 'Click "Facilities / Loans" in the sidebar. Locate the borrower\'s credit facility (e.g., FAC-2026-001). Click "Link Collateral".'),
  stepItem(2, 'Select Collateral', 'Search for the approved collateral code. The system checks available unallocated capacity.'),
  stepItem(3, 'Specify Security Rank & Allocation', 'Select "First-Degree Charge". Enter the allocation amount (cannot exceed net available capacity or facility balance).'),
  stepItem(4, 'Submit Linkage Request', 'Click "Submit Linkage for Dual Control". The transaction routes to Branch Manager for sign-off.'),
  h3('BRO Daily Operational Checklist:'),
  bullet('Review Dashboard Inbox', 'Check for submissions returned by Branch Manager with correction notes.'),
  bullet('Monitor Expiring Documents', 'Check the DMS tab for covenants with approaching grace period expirations.'),
  bullet('Follow Up Pending Cadastre Extracts', 'Ensure municipal registration extract certificates are retrieved and uploaded within the approved exception window.'),
  new Paragraph({ children: [new PageBreak()] })
);

// -----------------------------------------------------------------------------
// 14.2 Corporate Relationship Officer (CRO / Maker)
// -----------------------------------------------------------------------------
docChildren.push(
  h2('14.2 Corporate Relationship Officer (CRO / Maker) Guide'),
  p('The Corporate Relationship Officer (CRO) manages complex, high-value collateral packages pledged by corporate commercial entities, including commercial towers, manufacturing plants, heavy machinery, and industrial warehouses.'),
  h3('Core Operational Responsibilities:'),
  bullet('Commercial Collateral Structuring', 'Registering large-scale corporate assets with multi-party ownership structures.'),
  bullet('Corporate Identity & Authority Validation', 'Verifying Articles of Association, commercial registrations, and Board Resolutions authorizing mortgage creation.'),
  bullet('Licensed Engineering Valuations', 'Reviewing independent professional appraisal reports and entering structural component valuations.'),
  bullet('Cross-Collateralization Linkages', 'Structuring multi-facility security pools securing term loans, letters of credit, and revolving overdraft lines.'),
  h3('Step-by-Step Procedure: Registering a Commercial Real Estate Asset'),
  stepItem(1, 'Access Registration Wizard', 'Navigate to "Collateral" -> "+ Register Collateral".'),
  stepItem(2, 'Classify Corporate Taxonomy', 'Select "Real Estate - Commercial" or "Industrial Plant / Factory". Select Corporate Banking segment.'),
  stepItem(3, 'Validate Corporate Pledgor', 'Select "Corporate Entity". Enter the Corporate Tax Identification Number (TIN) and Core Banking Corporate CIF. Verify Company Trade Name against Core Banking.'),
  stepItem(4, 'Upload Corporate Legal Authorities', 'Upload scanned copies of Board Resolution authorizing credit security, Commercial Register Extract, and General Power of Attorney to the DMS section.'),
  stepItem(5, 'Engineering Appraisal Details', 'Select "External Licensed Valuating Firm". Enter the Certified Valuator License Number, Market Value, Forced Sale Value, and Appraisal Date.'),
  stepItem(6, 'Cross-Collateral Allocation Structure', 'Specify initial facility exposure limits and submit package to Credit Committee & Senior Management approval queue.'),
  new Paragraph({ children: [new PageBreak()] })
);

// -----------------------------------------------------------------------------
// 14.3 Senior Relationship Manager (SRM / Maker)
// -----------------------------------------------------------------------------
docChildren.push(
  h2('14.3 Senior Relationship Manager (SRM / Maker) Guide'),
  p('The Senior Relationship Manager (SRM) oversees top-tier corporate credit relationships, syndicate financing, and complex security structures including third-party collateral pledges and second-degree mortgages.'),
  h3('Core Operational Responsibilities:'),
  bullet('Third-Party Collateral Pledges', 'Structuring arrangements where the legal asset owner is distinct from the borrower.'),
  bullet('Tripartite Agreement Perfection', 'Ensuring binding legal contracts between Bank, Borrower, and Third-Party Pledgor.'),
  bullet('Spousal Consent Enforcement', 'Verifying mandatory spousal consent for all individual third-party asset owners.'),
  bullet('Second-Degree Mortgage Structuring', 'Managing internal secondary charges and external bank inter-creditor arrangements.'),
  h3('Step-by-Step Procedure 1: Structuring a Third-Party Collateral Pledge'),
  stepItem(1, 'Select Collateral & Target Facility', 'In "Facilities / Loans", choose the facility. Click "Link Collateral" and select the asset.'),
  stepItem(2, 'Detect Relationship Type', 'CBAIMS automatically evaluates: Collateral Legal Owner CIF vs Facility Borrower CIF. When they differ, the relationship status is flagged as "THIRD-PARTY PLEDGE".'),
  stepItem(3, 'Mandatory Third-Party Evidence Upload', 'Upload the signed Tripartite Pledge Agreement. For individual pledgors, upload the notarized Spousal Consent Declaration. The system enforces these as blocking pre-conditions.'),
  stepItem(4, 'Capacity & Exposure Allocation', 'Calculate eligible lending capacity. Enter allocation amount and dispatch to dual-control queue.'),
  h3('Step-by-Step Procedure 2: Structuring a Second-Degree Mortgage'),
  stepItem(1, 'Determine Security Degree', 'In the Linkage wizard, select "Second-Degree Charge".'),
  stepItem(2, 'Branch Internal vs External Charge', 'If First Degree is held by our bank, system checks remaining headroom: Capacity = FSV * (1 - Haircut) - Existing Allocations.'),
  stepItem(3, 'External Institution Second-Degree Rules', 'If First Degree is held by another financial institution, select "External Institution". Enter the Other Bank Name, External Loan Balance, Cadastre Registration Number, and attach the Other Bank\'s formal Consent / No-Objection Letter.'),
  stepItem(4, 'Submit for Senior Management Approval', 'Second-degree external mortgages require secondary executive approval.'),
  new Paragraph({ children: [new PageBreak()] })
);

// -----------------------------------------------------------------------------
// 14.4 Branch Manager (BRMGR / Primary Checker)
// -----------------------------------------------------------------------------
docChildren.push(
  h2('14.4 Branch Manager (BRMGR / Primary Checker) Guide'),
  p('The Branch Manager (BRMGR) serves as the primary checker and authorized signatory for all branch-level collateral transactions, enforcing segregation of duties and regulatory compliance.'),
  h3('Core Operational Responsibilities:'),
  bullet('Workflow Queue Governance', 'Reviewing pending maker transactions in the Approvals Center.'),
  bullet('The 5-Point Verification Checklist', 'Verifying ownership, valuation, documents, insurance, and capacity before sign-off.'),
  bullet('Executing Approvals, Corrections, or Rejections', 'Exercising dual-control authority with immutable audit logging.'),
  bullet('Collateral Release Authorization', 'Enforcing the 5 non-negotiable release hard stops before releasing assets.'),
  h3('The Branch Manager\'s 5-Point Verification Checklist:'),
  styledTable(
    ['Checkpoint', 'Verification Requirement', 'Action on Defect'],
    [
      ['1. Asset Ownership & CIF', 'Verify that owner CIF matches CBS records and title deed matches owner name exactly.', 'Return for Correction if names or ID numbers differ.'],
      ['2. Valuation & Haircuts', 'Confirm valuation date is within 365 days; verify FSV is mathematically lower than MV; confirm statutory haircut is applied.', 'Reject if valuation is expired or uncertified.'],
      ['3. Document Completeness', 'Inspect uploaded scans in DMS. Ensure Title Deed is legible and municipal stamp is clear.', 'If missing, verify if Path B Exception was formally requested.'],
      ['4. Insurance Coverage', 'Confirm that insurable assets carry a compliant policy naming the Bank as First Loss Payee with sum insured >= 100% of required adequacy.', 'Block approval until valid policy is attached or approved exception exists.'],
      ['5. Maker-Checker Segregation', 'Confirm that the maker user is not the same person as the approving checker.', 'Automated Hard Stop: CBAIMS permanently blocks self-approvals.'],
    ],
    [2200, 4400, 2600]
  ),
  h3('Step-by-Step Procedure: Approving or Returning a Submission'),
  stepItem(1, 'Open Workflow Approvals', 'Log in as brmgr_user. Click "Workflow Approvals" in the sidebar.'),
  stepItem(2, 'Select Pending Item', 'Locate the pending record (e.g., COL-RES-2026-004) under "Pending Dual-Control Tasks". Click "Review Submission".'),
  stepItem(3, 'Inspect Details & Documents', 'Review all wizard step inputs. Open the Documents tab to verify attached PDFs.'),
  stepItem(4, 'Execute Decision', 'Click "Approve" to activate the record and trigger CBS synchronization. Click "Return for Correction" to send back to maker with required correction notes. Click "Reject" to formally terminate the registration.'),
  new Paragraph({ children: [new PageBreak()] })
);

// -----------------------------------------------------------------------------
// 14.5 Collateral Documentation Officer (COLLDOCOFF / Custodian)
// -----------------------------------------------------------------------------
docChildren.push(
  h2('14.5 Collateral Documentation Officer (COLLDOCOFF) Guide'),
  p('The Collateral Documentation Officer (COLLDOCOFF) acts as the physical custodian of the bank\'s title deeds, vehicle certificates, and legal mortgage registries, ensuring safe vault storage and legal perfection.'),
  h3('Core Operational Responsibilities:'),
  bullet('Physical Document Intake', 'Receiving original title deeds, vehicle registration books (Libre), and share certificates from branch makers.'),
  bullet('Cadastre Extract Verification', 'Cross-referencing municipal land registry extract certificates against physical title deeds.'),
  bullet('Vault Storage & Indexing', 'Filing physical security instruments in the bank\'s fireproof document vault with unique barcode references.'),
  bullet('DMS Verification Sign-Off', 'Validating that electronic DMS scans exactly reflect original physical documents.'),
  bullet('Deed Release Execution', 'Dispatching original title deeds to municipal land offices upon fully authorized loan closure.'),
  h3('Step-by-Step Procedure: Document Intake & Vault Custody Sign-Off'),
  stepItem(1, 'Receive Physical Documents', 'Receive physical collateral folder from Branch Relationship Officer with signed intake slip.'),
  stepItem(2, 'Inspect Physical Authenticity', 'Examine original title deed for official municipal watermarks, embossed seals, and boundary demarcation sketches.'),
  stepItem(3, 'Access CBAIMS Document Registry', 'Navigate to "Documents (DMS)" -> "Pending Custody Verification". Locate the collateral code.'),
  stepItem(4, 'Compare Physical vs Electronic Scan', 'Open the uploaded scan. Confirm that serial numbers, parcel numbers, and owner names match the physical document in hand.'),
  stepItem(5, 'Log Vault Custody Reference', 'Enter the Physical Vault Bin Number (e.g., VAULT-B-SHELF-4-BIN-12). Click "Confirm Physical Custody". The document status changes to "VERIFIED & IN VAULT".'),
  new Paragraph({ children: [new PageBreak()] })
);

// -----------------------------------------------------------------------------
// 14.6 Senior Management & District Director (SRMGMT / DISTDIR)
// -----------------------------------------------------------------------------
docChildren.push(
  h2('14.6 Senior Management & District Director (SRMGMT / DISTDIR) Guide'),
  p('Senior Management (SRMGMT) and District Directors (DISTDIR) provide executive credit oversight, approve high-exposure facilities exceeding branch limits, and intervene on Stage 3 policy escalations.'),
  h3('Core Operational Responsibilities:'),
  bullet('High-Value Credit Authorization', 'Authorizing collateral packages and facility linkages exceeding branch discretionary thresholds (e.g., > ETB 50,000,000).'),
  bullet('Stage 3 Insurance Escalation Interventions', 'Resolving critical insurance policy lapses (10+ days overdue) across the district.'),
  bullet('Major Policy Exception Sign-Off', 'Reviewing and approving sub-haircut valuations, extended grace periods, and non-standard collateral covenants.'),
  bullet('Portfolio Risk Governance', 'Monitoring district-wide asset concentration, LTV distribution, and legal perfection compliance.'),
  h3('Step-by-Step Procedure: Handling a Stage 3 Insurance Escalation'),
  stepItem(1, 'Review District Escalation Alert', 'Upon logging in as distdir_user or srmgmt_user, check the "Critical District Escalations" banner on the Dashboard.'),
  stepItem(2, 'Examine Overdue Policy Portfolio', 'Navigate to "Reports" -> "Expired Policy Aging Report". Filter by aging bucket "Overdue > 10 Days (Stage 3)".'),
  stepItem(3, 'Review Credit Facility Exposure', 'Inspect the secured loans linked to the uninsured asset. Evaluate credit risk exposure.'),
  stepItem(4, 'Issue Executive Directives', 'Direct Branch Manager to either: (a) Enforce immediate customer policy renewal, (b) Exercise Bank Forced-Placement Insurance debiting borrower account, or (c) Issue 7-day credit acceleration notice.'),
  new Paragraph({ children: [new PageBreak()] })
);

// -----------------------------------------------------------------------------
// 14.7 Compliance Officer & Internal Auditor (COMPLIANCE / AUDITOR)
// -----------------------------------------------------------------------------
docChildren.push(
  h2('14.7 Compliance Officer & Internal Auditor (COMPLIANCE / AUDITOR) Guide'),
  p('Compliance Officers and Internal Auditors provide independent second-line and third-line defense, ensuring strict adherence to National Bank of Ethiopia (NBE) regulatory standards and internal bank credit policies.'),
  h3('Core Operational Responsibilities:'),
  bullet('NBE Regulatory Compliance Auditing', 'Verifying statutory haircut limits, maximum loan-to-value ratios, and insurance coverage requirements.'),
  bullet('Approved Insurer Concentration Monitoring', 'Ensuring no single insurance provider exceeds bank-established underwriting concentration limits.'),
  bullet('Immutable Audit Trail Inspection', 'Reviewing user activity logs at /api/audit to detect procedural violations or attempted dual-control circumvention.'),
  bullet('Exception Resolution Verification', 'Auditing resolved exceptions to confirm fulfillment of promised covenants and documents.'),
  h3('Step-by-Step Procedure: Conducting a Dual-Control & Audit Integrity Review'),
  stepItem(1, 'Access Immutable Audit Trail', 'Log in as auditor_user or compliance_user. Click "Audit Trail" in the navigation sidebar.'),
  stepItem(2, 'Filter by High-Severity Security Events', 'Set Event Severity to "WARNING / CRITICAL". Review events such as "UNAUTHORIZED_ACCESS_ATTEMPT", "FAILED_LOGIN_LOCKOUT", or "PARAMETER_UPDATED".'),
  stepItem(3, 'Verify Maker-Checker Separation', 'Filter by "COLLATERAL_APPROVED". Verify that the created_by username and approved_by username are never identical across any transaction in the audit period.'),
  stepItem(4, 'Inspect Before/After Delta JSON', 'Click on any audit event row to expand the JSON state delta. Verify that no administrative parameter or valuation amount was altered without authorized committee documentation.'),
  stepItem(5, 'Export Regulatory Audit Dossier', 'Click "Export Audit Report" -> "Export to Excel/PDF" to generate formal documentation for external NBE bank examiners.'),
  new Paragraph({ children: [new PageBreak()] })
);

// -----------------------------------------------------------------------------
// 14.8 System Administrator (SYSADMIN)
// -----------------------------------------------------------------------------
docChildren.push(
  h2('14.8 System Administrator (SYSADMIN) Guide'),
  p('The System Administrator (SYSADMIN) is responsible for technical system configuration, user account provisioning, role permission maintenance, security parameter governance, and system health monitoring.'),
  h3('Core Operational Responsibilities:'),
  bullet('User Provisioning & Role Assignment', 'Creating user accounts, assigning roles, branches, and functional departments.'),
  bullet('Account Lockout Remediation', 'Unlocking user accounts locked after 5 consecutive failed login attempts.'),
  bullet('System Parameter Governance', 'Tuning inactivity session timeouts, password lifecycles, and release margins via the Administration Center.'),
  bullet('Collateral Taxonomy Maintenance', 'Configuring asset categories, statutory haircut percentages, and revaluation intervals.'),
  bullet('Mandatory Document Rules Matrix', 'Maintaining required legal documents per asset class and business segment.'),
  bullet('Bank Holiday Calendar Configuration', 'Maintaining active bank holidays to ensure precise working-day expiry calculations.'),
  h3('Step-by-Step Procedure 1: Unlocking a Locked User Account'),
  stepItem(1, 'Access User Administration', 'Log in as sysadmin. Click "Administration" in the left sidebar, then select "User Management".'),
  stepItem(2, 'Locate Locked User', 'Filter users by Status = "LOCKED". Locate the user who failed authentication 5 times (e.g., bro_user).'),
  stepItem(3, 'Verify Identity & Reset Lockout', 'Confirm formal authorization from user\'s Branch Manager. Click "Unlock Account". The failed attempt counter resets to 0, and user status returns to "ACTIVE".'),
  h3('Step-by-Step Procedure 2: Tuning System Security Parameters'),
  stepItem(1, 'Open System Parameters', 'In the Administration Center, click the "System Parameters" tab.'),
  stepItem(2, 'Modify Parameter Values', 'Locate the parameter (e.g., SESSION_TIMEOUT_MINUTES = 15, PASSWORD_EXPIRY_DAYS = 90, MAX_FAILED_LOGIN_ATTEMPTS = 5). Enter updated value.'),
  stepItem(3, 'Save & Audit Changes', 'Click "Save System Parameters". The system updates the dynamic parameter table and records an administrative change record in the immutable audit log.'),
  new Paragraph({ children: [new PageBreak()] })
);

// =============================================================================
// CHAPTER 15: TROUBLESHOOTING & SUPPORT DIRECTORY
// =============================================================================
docChildren.push(
  h1('Chapter 15: Troubleshooting & Support Directory'),
  p('This chapter provides resolutions to frequently encountered operational scenarios and official IT escalation contacts.'),
  h2('15.1 Frequently Encountered Operational Errors & Solutions'),
  styledTable(
    ['Symptom / Error Message', 'Root Cause Explanation', 'Recommended Resolution Steps'],
    [
      ['Account is temporarily locked due to multiple failed login attempts', 'User entered an incorrect password 5 consecutive times.', 'Wait 15 minutes for automatic lockout expiration, or contact System Administrator to reset failed attempts.'],
      ['Session Inactivity Warning Modal appears', 'User has been inactive for 13 minutes.', 'Click "Stay Logged In" to extend the session, or click "Log Out" to terminate securely.'],
      ['Cannot submit registration: Missing mandatory documents', 'Taxonomy requires mandatory documents that have not been uploaded.', 'Upload the required documents in Step 4, OR select "Request Exception" to submit for committee approval.'],
      ['External second-degree collateral submission rejected', 'Missing mandatory confirmation letter or cadastre extract certificate.', 'Attach the other institution\'s formal confirmation letter and registration extract before submitting.'],
      ['Permission Denied: User role cannot approve own submission', 'Maker-checker segregation prevents self-approval.', 'Forward the transaction to an authorized Checker (Branch Manager or Credit Risk Approver).'],
      ['CIF Lookup Failed / No Customer Found', 'Customer CIF number entered does not exist in Core Banking System.', 'Verify CIF with borrower\'s passbook or account statement; confirm CBS Simulator is online.'],
      ['Collateral Release Blocked: Active Loan Balance', 'Facility balance is greater than zero and no substitute collateral was provided.', 'Ensure loan is fully paid off (ETB 0.00) or link an approved substitute collateral providing 100% adequacy.'],
    ],
    [2600, 3200, 3400]
  ),
  new Paragraph({ spacing: { before: 200, after: 100 } }),
  h2('15.2 Technical Support & Helpdesk Escalation Contacts'),
  styledTable(
    ['Support Tier', 'Contact Group', 'Email Address', 'Telephone Extension'],
    [
      ['Tier 1: Branch Operations Support', 'CBAIMS Helpdesk Operations', 'cbaims-support@bank.et', 'Ext. 4101 / 4102'],
      ['Tier 2: Credit Processing Admin', 'Credit Processing Directorate', 'credit-admin@bank.et', 'Ext. 4205 / 4208'],
      ['Tier 3: Enterprise Applications', 'IT Enterprise Applications Support', 'it-apps@bank.et', 'Ext. 5500 / 5501'],
      ['Security Operations & Access Control', 'Information Security Directorate', 'infosec@bank.et', 'Ext. 5999'],
    ],
    [2200, 3000, 2400, 1600]
  )
);

// Create Document
const doc = new Document({
  styles: {
    default: {
      document: {
        run: {
          font: 'Segoe UI',
          size: 21,
          color: COLOR_TEXT,
        },
      },
    },
  },
  sections: [
    {
      properties: {
        page: {
          margin: { top: 1200, bottom: 1200, left: 1200, right: 1200 },
        },
      },
      headers: {
        default: new Header({
          children: [
            new Paragraph({
              alignment: AlignmentType.RIGHT,
              children: [
                new TextRun({ text: 'CBAIMS USER MANUAL & OPERATIONAL GUIDE  |  CONFIDENTIAL', font: 'Segoe UI', size: 16, color: COLOR_MUTED, bold: true }),
              ],
            }),
          ],
        }),
      },
      footers: {
        default: new Footer({
          children: [
            new Paragraph({
              alignment: AlignmentType.CENTER,
              children: [
                new TextRun({ text: 'Page ', font: 'Segoe UI', size: 16, color: COLOR_MUTED }),
                new TextRun({ children: [PageNumber.CURRENT], font: 'Segoe UI', size: 16, color: COLOR_MUTED }),
                new TextRun({ text: ' of ', font: 'Segoe UI', size: 16, color: COLOR_MUTED }),
                new TextRun({ children: [PageNumber.TOTAL_PAGES], font: 'Segoe UI', size: 16, color: COLOR_MUTED }),
                new TextRun({ text: '  —  Bank Internal Operational Document', font: 'Segoe UI', size: 16, color: COLOR_MUTED, italics: true }),
              ],
            }),
          ],
        }),
      },
      children: docChildren,
    },
  ],
});

// Pack Document to Buffer and save
Packer.toBuffer(doc).then((buffer) => {
  fs.writeFileSync(outputPath, buffer);
  console.log('User Manual DOCX successfully generated at:', outputPath);
  console.log('File size:', (buffer.length / 1024).toFixed(2), 'KB');

  try {
    fs.writeFileSync(altOutputPath, buffer);
    console.log('Also successfully updated:', altOutputPath);
  } catch (altErr) {
    console.log('Notice: Could not overwrite V2.0.docx (currently open in Word/viewer). Primary file is ready at:', outputPath);
  }
}).catch((err) => {
  console.error('Failed to generate User Manual DOCX:', err);
  process.exit(1);
});
