/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{js,jsx,ts,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        slate: {
          750: 'rgb(41 53 72)',
          850: 'rgb(22 32 51)'
        }
      }
    },
  },
  plugins: [],
  darkMode: 'class'
}
