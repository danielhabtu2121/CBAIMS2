const fs = require('fs');
const path = require('path');

const targetFile = path.resolve('frontend/src/components/admin/AdminPortal.tsx');
let content = fs.readFileSync(targetFile, 'utf8');

// Replace color tokens carefully
content = content.replace(/bg-\[#102E4A\]\/[0-9]+/g, 'bg-[#00205B]/60 backdrop-blur-2xs');
content = content.replace(/hover:bg-\[#1F4E7A\]/g, 'hover:bg-[#0E284E]');
content = content.replace(/active:bg-\[#1F4E7A\]/g, 'active:bg-[#0E284E]');
content = content.replace(/bg-\[#102E4A\]/g, 'bg-[#00205B]');
content = content.replace(/bg-\[#1F4E7A\]/g, 'bg-[#00205B]');
content = content.replace(/text-\[#102E4A\]/g, 'text-[#00205B]');
content = content.replace(/text-\[#1F4E7A\]/g, 'text-[#00205B]');
content = content.replace(/border-\[#102E4A\]/g, 'border-[#00205B]');
content = content.replace(/border-\[#1F4E7A\]/g, 'border-[#00205B]');
content = content.replace(/hover:text-\[#102E4A\]/g, 'hover:text-[#00205B]');
content = content.replace(/hover:text-\[#1F4E7A\]/g, 'hover:text-[#00205B]');
content = content.replace(/hover:border-\[#102E4A\]/g, 'hover:border-[#00205B]');
content = content.replace(/hover:border-\[#1F4E7A\]/g, 'hover:border-[#00205B]');
content = content.replace(/focus:ring-\[#1F4E7A\]/g, 'focus:ring-[#00205B]');
content = content.replace(/focus:border-\[#1F4E7A\]/g, 'focus:border-[#00205B]');
content = content.replace(/focus:ring-\[#102E4A\]/g, 'focus:ring-[#00205B]');
content = content.replace(/focus:border-\[#102E4A\]/g, 'focus:border-[#00205B]');

// Update modal dialogs rounded corners and shadows
content = content.replace(/className="bg-white rounded-lg border border-\[#E2E8F0\] shadow-xl/g, 'className="bg-white rounded-2xl border border-slate-300 shadow-2xl');
content = content.replace(/className="bg-white rounded-lg border border-slate-200 shadow-xl/g, 'className="bg-white rounded-2xl border border-slate-300 shadow-2xl');

fs.writeFileSync(targetFile, content, 'utf8');
console.log('Successfully modernized AdminPortal.tsx styling tokens.');

