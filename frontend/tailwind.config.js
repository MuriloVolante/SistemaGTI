export default {
  content: ['./index.html', './src/**/*.{vue,js}'],
  theme: {
    extend: {
      colors: {
        primaria: {
          DEFAULT: '#0a6ed1',
          hover: '#085caf',
          active: '#064a8f',
          light: '#e8f1fb',
          text: '#074d96'
        },
        bg: '#f4f5f7',
        surface: {
          DEFAULT: '#ffffff',
          alt: '#f7f8f9'
        },
        borda: {
          DEFAULT: '#dfe1e6',
          forte: '#c1c7d0'
        },
        texto: {
          DEFAULT: '#172b4d',
          sub: '#5e6c84',
          fraco: '#8993a4'
        },
        sucesso: { bg: '#f0fdf4', text: '#166534' },
        perigo: { bg: '#fef2f2', text: '#991b1b', borda: '#f1aeb2', forte: '#ae2e24' },
        aviso: { bg: '#fefce8', text: '#854d0e', borda: '#fde68a' },
        info: { bg: '#eff6ff', text: '#1e40af' },
        roxo: { bg: '#fdf4ff', text: '#6b21a8' }
      },
      borderRadius: {
        raio: '6px',
        'raio-sm': '4px'
      },
      boxShadow: {
        sombra: '0 1px 1px rgba(9,30,66,.10), 0 0 1px rgba(9,30,66,.12)',
        'sombra-md': '0 3px 8px rgba(9,30,66,.12), 0 0 1px rgba(9,30,66,.14)'
      },
      fontFamily: {
        sans: ['-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'sans-serif'],
        mono: ['ui-monospace', 'SFMono-Regular', 'Menlo', 'monospace']
      },
      fontSize: {
        '2xs': '10px',
        xs: '11px',
        sm: '12px',
        base: '13px',
        md: '14px',
        lg: '15px',
        xl: '16px',
        '2xl': '18px',
        '3xl': '20px',
        '4xl': '24px',
        '5xl': '26px',
        '6xl': '28px'
      }
    }
  },
  plugins: []
}