/**
 * Dashen Bank CBAIMS Design Tokens
 * 
 * Authoritative color, spacing, typography, and elevation tokens.
 * Primary: Dashen Corporate Navy (#00205B / #0E284E)
 * Secondary / Accent: Dashen Warm Gold (#D48806 / #EAA023)
 */

export const colors = {
  // Dashen Bank Brand Palette
  brand: {
    50: '#F0F5FA',
    100: '#DCE8F4',
    200: '#B8D1E8',
    300: '#8FB4DA',
    400: '#5A8ECA',
    500: '#1F4E7A',
    600: '#1B4580',
    700: '#143666',
    800: '#0E284E',
    900: '#00205B', // Official Dashen Navy
    950: '#07152B',
  },

  // Dashen Signature Warm Gold Accent
  accent: {
    50: '#FFFBF0',
    100: '#FEF3D6',
    200: '#FCE0A6',
    300: '#F5B853',
    400: '#EAA023',
    500: '#D48806', // Dashen Gold
    600: '#B87802',
    700: '#9E6700',
    800: '#7E5200',
    900: '#5A3A00',
  },

  // Neutral / Slate Foundation
  neutral: {
    50: '#F8FAFC',
    100: '#F1F5F9',
    200: '#E2E8F0',
    300: '#CBD5E1',
    400: '#94A3B8',
    500: '#64748B',
    600: '#475569',
    700: '#334155',
    800: '#1E293B',
    900: '#0F172A',
    950: '#020617',
  },

  // Semantic Status Colors
  status: {
    success: {
      DEFAULT: '#16A34A',
      light: '#DCFCE7',
      border: '#86EFAC',
      dark: '#14532D',
    },
    warning: {
      DEFAULT: '#D97706',
      light: '#FEF3C7',
      border: '#FCD34D',
      dark: '#78350F',
    },
    danger: {
      DEFAULT: '#DC2626',
      light: '#FEE2E2',
      border: '#FCA5A5',
      dark: '#7F1D1D',
    },
    info: {
      DEFAULT: '#0284C7',
      light: '#E0F2FE',
      border: '#7DD3FC',
      dark: '#0C4A6E',
    },
  },

  // Surfaces & Backgrounds
  surface: {
    white: '#FFFFFF',
    canvas: '#F8FAFC',
    subtle: '#F1F5F9',
    muted: '#E2E8F0',
    dark: '#0E284E',
  },
} as const;

export const shadows = {
  xs: '0 1px 2px 0 rgba(15, 23, 42, 0.05)',
  sm: '0 1px 3px 0 rgba(15, 23, 42, 0.08), 0 1px 2px -1px rgba(15, 23, 42, 0.08)',
  md: '0 4px 6px -1px rgba(15, 23, 42, 0.08), 0 2px 4px -2px rgba(15, 23, 42, 0.06)',
  lg: '0 10px 15px -3px rgba(15, 23, 42, 0.08), 0 4px 6px -4px rgba(15, 23, 42, 0.04)',
  xl: '0 20px 25px -5px rgba(15, 23, 42, 0.1), 0 8px 10px -6px rgba(15, 23, 42, 0.06)',
} as const;

export const radii = {
  sm: '0.375rem', // 6px
  md: '0.5rem',   // 8px
  lg: '0.75rem',  // 12px
  xl: '1rem',     // 16px
  full: '9999px',
} as const;

export const chartColors = [
  '#00205B', // Dashen Navy
  '#D48806', // Dashen Gold
  '#16A34A', // Success Green
  '#0284C7', // Info Blue
  '#7C3AED', // Royal Purple
  '#E11D48', // Rose
  '#0D9488', // Teal
  '#F59E0B', // Amber
];

