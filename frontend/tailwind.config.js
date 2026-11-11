/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Dashen Bank Corporate Brand Blue / Navy
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
        surface: {
          DEFAULT: '#FFFFFF',
          canvas: '#F8FAFC',
          subtle: '#F1F5F9',
          muted: '#E2E8F0',
          dark: '#0E284E',
        },
        border: {
          DEFAULT: '#E2E8F0',
          light: '#F1F5F9',
          strong: '#CBD5E1',
        },
        text: {
          primary: '#0F172A',
          secondary: '#475569',
          muted: '#94A3B8',
        },
        success: {
          DEFAULT: '#16A34A',
          bg: '#F0FDF4',
          border: '#BBF7D0',
          dark: '#14532D',
        },
        warning: {
          DEFAULT: '#D97706',
          bg: '#FFFBEB',
          border: '#FDE68A',
          dark: '#78350F',
        },
        danger: {
          DEFAULT: '#DC2626',
          bg: '#FEF2F2',
          border: '#FECACA',
          dark: '#7F1D1D',
        },
        info: {
          DEFAULT: '#0284C7',
          bg: '#F0F9FF',
          border: '#BAE6FD',
          dark: '#0C4A6E',
        },
        neutral: {
          DEFAULT: '#64748B',
          bg: '#F1F5F9',
        }
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'sans-serif'],
        mono: ['JetBrains Mono', 'SF Mono', 'Consolas', 'monospace'],
      },
      boxShadow: {
        xs: '0 1px 2px 0 rgba(15, 23, 42, 0.05)',
        sm: '0 1px 3px 0 rgba(15, 23, 42, 0.08), 0 1px 2px -1px rgba(15, 23, 42, 0.08)',
        md: '0 4px 6px -1px rgba(15, 23, 42, 0.08), 0 2px 4px -2px rgba(15, 23, 42, 0.06)',
        lg: '0 10px 15px -3px rgba(15, 23, 42, 0.08), 0 4px 6px -4px rgba(15, 23, 42, 0.04)',
        xl: '0 20px 25px -5px rgba(15, 23, 42, 0.1), 0 8px 10px -6px rgba(15, 23, 42, 0.06)',
      },
      borderRadius: {
        xs: '0.25rem', // 4px
        sm: '0.375rem', // 6px
        md: '0.5rem', // 8px
        lg: '0.75rem', // 12px
        xl: '1rem', // 16px
      }
    },
  },
  plugins: [],
}
