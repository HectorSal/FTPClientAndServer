module.exports = {
  prefix: 't-',
  content: ['./src/**/*.{js,jsx,ts,tsx}'],
  important: 'body',
  theme: {
    extend: {},
    fontFamily: {
      'sans': ['Helvetica', 'Arial', 'sans-serif'],
      'serif': ['Inter', 'sans-serif'],
      'body': ['Open Sans']
    },
    screens: {
      xs: '0px',
      sm: '576px',
      md: '768px',
      lg: '992px',
      xl: '1200px',
    },
  },
  plugins: [],
}