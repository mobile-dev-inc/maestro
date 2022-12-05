import React, { useEffect } from 'react';
import './App.css';

function App() {
  useEffect(() => {
    (async () => {
      const response = await fetch('/api/hello')
      const body = await response.text()
      console.log(body)
    })()
  }, [])
  return (
    <div className="App">
    </div>
  );
}

export default App;
