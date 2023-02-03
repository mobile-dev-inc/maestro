import React from 'react';
import DeprecatedInspectPage from './DeprecatedInspectPage';
import InteractPage from './InteractPage';
import MockPage from './MockPage';
import WelcomePage from './WelcomePage';

function App() {
  switch (window.location.pathname) {
    case '/inspect':
      return <DeprecatedInspectPage />
    
      case '/mock':
        return <MockPage />

      case '/interact':
        return <InteractPage />

      default:
        return <WelcomePage />
  }
}

export default App;
